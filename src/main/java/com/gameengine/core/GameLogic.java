package com.gameengine.core;

import com.gameengine.components.*;
import com.gameengine.example.GameObjectFactory;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class GameLogic {
    private Scene scene;
    private InputManager inputManager;
    private Random random;
    private boolean gameOver;
    private GameEngine gameEngine;
    private Map<GameObject, Vector2> aiTargetVelocities;
    private Map<GameObject, Float> aiTargetUpdateTimers;
    private ExecutorService avoidanceExecutor;
    private ExecutorService physicsExecutor;
    private ExecutorService collidersExecutor;

    public GameObjectFactory gameObjectFactory; // 添加工厂成员

    private float lastShotTime = 0;
    private static final float SHOT_COOLDOWN = 0.2f;
    private static final int PLAYER_BULLET_DAMAGE = 25;

    private int killCount = 0;
    private final Object killCountLock = new Object(); // 用于同步killCount的锁
    private float survivalTime = 0;

    public GameLogic(Scene scene) {
        this.scene = scene;
        this.inputManager = InputManager.getInstance();
        this.random = new Random();
        this.gameOver = false;
        this.aiTargetVelocities = new HashMap<>();
        this.aiTargetUpdateTimers = new HashMap<>();
        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        this.avoidanceExecutor = Executors.newFixedThreadPool(threadCount);
        this.physicsExecutor = Executors.newFixedThreadPool(threadCount);
        this.collidersExecutor = Executors.newFixedThreadPool(threadCount);
    }

    public void cleanup() {
        if (avoidanceExecutor != null && !avoidanceExecutor.isShutdown()) {
            avoidanceExecutor.shutdown();
            try {
                if (!avoidanceExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    avoidanceExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                avoidanceExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // 添加一个新的 update 方法来处理独立于其他逻辑的计时
    public void update(float deltaTime) {
        if (!gameOver) {
            survivalTime += deltaTime;
        }
    }

    public void setGameEngine(GameEngine engine) {
        this.gameEngine = engine;
        // 在这里初始化工厂，因为它需要 Renderer
        if (engine != null) {
            this.gameObjectFactory = new GameObjectFactory(this.scene, engine);
        }
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public GameObject getUserPlayer() {
        for (GameObject obj : scene.getGameObjects()) {
            if (obj.getName().equals("Player") && obj.hasComponent(PhysicsComponent.class)) {
                return obj;
            }
        }
        return null;
    }

    public List<GameObject> getEnemies() {
        return scene.getGameObjects().stream()
                .filter(obj -> "Enemy".equals(obj.getName()) && obj.isActive())
                .collect(Collectors.toList());
    }

    public List<GameObject> getBullets() {
        return scene.getGameObjects().stream()
                .filter(obj -> "Bullet".equals(obj.getName()))
                .filter(GameObject::isActive)
                .collect(Collectors.toList());
    }

    public void handlePlayerInput(float deltaTime) {
        if (gameOver) return;

        GameObject player = getUserPlayer();
        if (player == null) return;

        TransformComponent transform = player.getComponent(TransformComponent.class);
        PhysicsComponent physics = player.getComponent(PhysicsComponent.class);

        if (transform == null || physics == null) return;

        Vector2 movement = new Vector2();

        // W / UpArrow (AWT=38, GLFW=265)
        if (inputManager.isKeyPressed(87) || inputManager.isKeyPressed(38) || inputManager.isKeyPressed(265)) {
            movement.y -= 1;
        }
        // S / DownArrow (AWT=40, GLFW=264)
        if (inputManager.isKeyPressed(83) || inputManager.isKeyPressed(40) || inputManager.isKeyPressed(264)) {
            movement.y += 1;
        }
        // A / LeftArrow (AWT=37, GLFW=263)
        if (inputManager.isKeyPressed(65) || inputManager.isKeyPressed(37) || inputManager.isKeyPressed(263)) {
            movement.x -= 1;
        }
        // D / RightArrow (AWT=39, GLFW=262)
        if (inputManager.isKeyPressed(68) || inputManager.isKeyPressed(39) || inputManager.isKeyPressed(262)) {
            movement.x += 1;
        }

        if (movement.magnitude() > 0) {
            movement = movement.normalize().multiply(200);
            physics.setVelocity(movement);
        }

        Vector2 pos = transform.getPosition();
        int screenW = gameEngine != null && gameEngine.getRenderer() != null ? gameEngine.getRenderer().getWidth() : 1920;
        int screenH = gameEngine != null && gameEngine.getRenderer() != null ? gameEngine.getRenderer().getHeight() : 1080;
        if (pos.x < 0) pos.x = 0;
        if (pos.y < 0) pos.y = 0;
        if (pos.x > screenW - 20) pos.x = screenW - 20;
        if (pos.y > screenH - 20) pos.y = screenH - 20;
        transform.setPosition(pos);

        // 处理射击输入
        lastShotTime += deltaTime;
        if (inputManager.isMouseButtonPressed(1) && lastShotTime >= SHOT_COOLDOWN) {
            Vector2 playerPos = transform.getPosition();
            Vector2 mousePos = inputManager.getMousePosition();
            // 使用工厂创建子弹
            gameObjectFactory.createBullet(
                    playerPos,
                    mousePos,
                    PLAYER_BULLET_DAMAGE,
                    80f, // speed
                    new Vector2(20, 20), // size
                    new RenderComponent.Color(1.0f, 1.0f, 0.0f, 1.0f) // color
            );
            lastShotTime = 0;
        }
    }

    public void updatePhysics() {
        updatePhysicsSerial();
//        updatePhysicsParallel();
    }

    public void updatePhysicsSerial() {
        if (gameOver) return;

        List<PhysicsComponent> physicsComponents = scene.getComponents(PhysicsComponent.class);
        for (PhysicsComponent physics : physicsComponents) {
            updateSinglePhysics(physics);
        }
    }

    public void updatePhysicsParallel() {
        if (gameOver) return;

        List<PhysicsComponent> physicsComponents = scene.getComponents(PhysicsComponent.class);
        if (physicsComponents.isEmpty()) return;

        int threadCount = Runtime.getRuntime().availableProcessors() - 1;
        threadCount = Math.max(2, threadCount);
        int batchSize = Math.max(1, physicsComponents.size() / threadCount + 1);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < physicsComponents.size(); i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, physicsComponents.size());

            futures.add(physicsExecutor.submit(() -> {
                for (int j = start; j < end; j++) {
                    updateSinglePhysics(physicsComponents.get(j));
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateSinglePhysics(PhysicsComponent physics) {
        TransformComponent transform = physics.getOwner().getComponent(TransformComponent.class);
        if (transform != null) {
            Vector2 pos = transform.getPosition();
            Vector2 velocity = physics.getVelocity();

            boolean velocityChanged = false;
            String ownerName = physics.getOwner().getName();

            // 子弹飞出屏幕后销毁
            if (ownerName.equals("Bullet")) {
                if (pos.x < -10 || pos.x > 1930 || pos.y < -10 || pos.y > 1090) {
                    physics.getOwner().setActive(false);
                    return;
                }
                return; // 子弹不反弹
            }

            // 追踪型敌人不受边界反弹影响
            if ("Enemy".equals(ownerName)) {
                return;
            }

            if (pos.x <= 0 || pos.x >= 1920 - 15) {
                velocity.x = -velocity.x;
                velocityChanged = true;
            }
            if (pos.y <= 0 || pos.y >= 1080 - 15) {
                velocity.y = -velocity.y;
                velocityChanged = true;
            }

            if (pos.x < 0) pos.x = 0;
            if (pos.y < 0) pos.y = 0;
            if (pos.x > 1920 - 15) pos.x = 1920 - 15;
            if (pos.y > 1080 - 15) pos.y = 1080 - 15;

            transform.setPosition(pos);

            if (velocityChanged) {
                physics.setVelocity(velocity);
            }
        }
    }

    public void checkCollisions() {
        if (gameOver) {
            return;
        }

        GameObject player = getUserPlayer();
        if (player == null || !player.isActive()) {
            return;
        }

        TransformComponent playerTransform = player.getComponent(TransformComponent.class);
        HealthComponent playerHealth = player.getComponent(HealthComponent.class);
        if (playerHealth == null) {
            return;
        }

        if (playerTransform == null) {
            return;
        }

        Vector2 playerPos = playerTransform.getPosition();
        List<GameObject> enemies = getEnemies();

        for (GameObject enemy : enemies) {
            if (!enemy.isActive()) {
                continue;
            }

            TransformComponent enemyTransform = enemy.getComponent(TransformComponent.class);
            DamageComponent enemyDamage = enemy.getComponent(DamageComponent.class);

            if (enemyTransform != null && enemyDamage != null) {
                Vector2 enemyPos = enemyTransform.getPosition();
                // 简单的基于距离的碰撞检测
                float distance = playerPos.distance(enemyPos);
                // 碰撞阈值，可根据玩家和敌人的大小调整
                float collisionThreshold = 30.0f;

                if (distance < collisionThreshold) {
                    // 玩家受到伤害
                    playerHealth.takeDamage(enemyDamage.getDamage());
                    System.out.println("玩家受到攻击！当前生命值: " + playerHealth.getHealth());

                    // 敌人消失
                    enemy.setActive(false);

                    // 检查玩家是否死亡
                    if (playerHealth.isDead()) {
                        gameOver = true;
                        // System.out.println("游戏结束！");
                        break; // 游戏结束，停止检测
                    }
                }
            }
        }
    }

    public void checkBulletCollisions() {
        checkBulletCollisionsSerial();
//        checkBulletCollisionsParallel();
    }

    public void checkBulletCollisionsSerial() {
        if (gameOver) return;

        List<GameObject> bullets = getBullets();
        List<GameObject> enemies = getEnemies();

        for (GameObject bullet : bullets) {
            if (!bullet.isActive()) continue;

            TransformComponent bulletTransform = bullet.getComponent(TransformComponent.class);
            DamageComponent bulletDamage = bullet.getComponent(DamageComponent.class);
            if (bulletTransform == null || bulletDamage == null) continue;

            Vector2 bulletPos = bulletTransform.getPosition();

            for (GameObject enemy : enemies) { // 遍历敌人
                if (!enemy.isActive()) continue;

                TransformComponent enemyTransform = enemy.getComponent(TransformComponent.class);
                HealthComponent enemyHealth = enemy.getComponent(HealthComponent.class);
                if (enemyTransform == null || enemyHealth == null) continue;

                // 简单的基于距离的碰撞检测
                float distance = bulletPos.distance(enemyTransform.getPosition());
                float collisionThreshold = 20.0f; // 碰撞半径

                if (distance < collisionThreshold) {
                    // 敌人受到伤害
                    enemyHealth.takeDamage(bulletDamage.getDamage());
                    System.out.println(enemy.getName() + " 被击中！剩余生命: " + enemyHealth.getHealth());

                    // 子弹击中后消失
                    bullet.setActive(false);

                    // 检查敌人是否死亡
                    if (enemyHealth.isDead()) {
                        System.out.println(enemy.getName() + " 已被消灭。");
                        enemy.setActive(false); // 敌人死亡后消失
                        killCount++; // 记录击杀敌人数
                    }
                    break; // 一颗子弹只击中一个目标
                }
            }
        }
    }

    public void checkBulletCollisionsParallel() {
        if (gameOver) return;

        List<GameObject> bullets = getBullets();
        List<GameObject> enemies = getEnemies();

        if (bullets.isEmpty() || enemies.isEmpty()) {
            return;
        }

        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        int batchSize = Math.max(1, bullets.size() / threadCount + 1);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < bullets.size(); i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, bullets.size());
            final List<GameObject> bulletSublist = bullets.subList(start, end);

            futures.add(collidersExecutor.submit(() -> {
                for (GameObject bullet : bulletSublist) {
                    if (!bullet.isActive()) continue;

                    TransformComponent bulletTransform = bullet.getComponent(TransformComponent.class);
                    DamageComponent bulletDamage = bullet.getComponent(DamageComponent.class);
                    if (bulletTransform == null || bulletDamage == null) continue;

                    Vector2 bulletPos = bulletTransform.getPosition();
                    float collisionThreshold = 20.0f;

                    for (GameObject enemy : enemies) {
                        if (!enemy.isActive()) continue;

                        TransformComponent enemyTransform = enemy.getComponent(TransformComponent.class);
                        if (enemyTransform == null) continue;

                        if (bulletPos.distance(enemyTransform.getPosition()) < collisionThreshold) {
                            HealthComponent enemyHealth = enemy.getComponent(HealthComponent.class);
                            if (enemyHealth == null) continue;

                            synchronized (enemy) {
                                if (enemy.isActive()) {
                                    enemyHealth.takeDamage(bulletDamage.getDamage());
                                    bullet.setActive(false);

                                    if (enemyHealth.isDead()) {
                                        enemy.setActive(false);
                                        synchronized (killCountLock) {
                                            killCount++;
                                        }
                                    }
                                }
                            }
                            break; // 一颗子弹只击中一个敌人
                        }
                    }
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    // getter
    public int getKillCount() {
        synchronized (killCountLock) {
            return killCount;
        }
    }

    public float getSurvivalTime() {
        return survivalTime;
    }

    public int getEnemyCount() {
        return getEnemies().size();
    }
}
