package com.gameengine.example;

import com.gameengine.components.*;
import com.gameengine.core.GameEngine;
import com.gameengine.core.GameObject;
import com.gameengine.graphics.IRenderer;
import com.gameengine.math.Vector2;
import com.gameengine.recording.RecordingService;
import com.gameengine.scene.Scene;

import java.awt.image.BufferedImage;
import java.util.Random;

public class GameObjectFactory {
    private final Scene scene;
    private final GameEngine engine;
    private final Random random;

    public GameObjectFactory(Scene scene, GameEngine engine) {
        this.scene = scene;
        this.engine = engine;
        this.random = new Random();
    }

    /**
     * 创建一个子弹对象。
     *
     * @param startPos  起始位置
     * @param targetPos 目标位置
     * @param damage    伤害值
     * @param speed     速度
     * @param size      大小
     * @param color     颜色
     */
    public void createBullet(Vector2 startPos, Vector2 targetPos, int damage, float speed, Vector2 size, RenderComponent.Color color) {
        GameObject bullet = new GameObject("Bullet");
        bullet.addComponent(new TransformComponent(startPos));

        RenderComponent render = bullet.addComponent(new RenderComponent(
                RenderComponent.RenderType.RECTANGLE,
                size,
                color
        ));
        render.setRenderer(engine.getRenderer());

        PhysicsComponent physics = bullet.addComponent(new PhysicsComponent(0.1f));
        Vector2 direction = targetPos.subtract(startPos).normalize();
        physics.setVelocity(direction.multiply(speed));
        physics.setFriction(1.0f); // 子弹不受摩擦力影响

        bullet.addComponent(new DamageComponent(damage));

        scene.addGameObject(bullet);

        // 获取 RecordingService 实例
        RecordingService recordingService = engine.getRecordingService(); // 假设 GameEngine 提供了此方法

        // 如果录制服务存在且正在录制，则记录子弹创建事件
        if (recordingService != null && recordingService.isRecording()) {
            // recordingService.recordEvent(bullet);
        }
    }

    /**
     * 创建一个敌人对象，它会主动追踪玩家。
     *
     * @param health 敌人的生命值
     * @param speed  敌人的移动速度
     * @param size   敌人的大小
     * @param sprite 敌人的图片
     */
    public void createEnemy(int health, float speed, Vector2 size, BufferedImage sprite) {
        GameObject enemy = new GameObject("Enemy");

        // 在屏幕外生成敌人
        Vector2 spawnPosition = getOffScreenSpawnPosition();
        enemy.addComponent(new TransformComponent(spawnPosition));

        // 添加渲染组件

        RenderComponent render = enemy.addComponent(new RenderComponent(
                RenderComponent.RenderType.RECTANGLE,
                size,
                new RenderComponent.Color(1.0f, 0.2f, 0.2f, 1.0f)
        ));
        render.setRenderer(engine.getRenderer());

        // 添加物理组件
        PhysicsComponent physics = enemy.addComponent(new PhysicsComponent(0.5f));
        physics.setFriction(0.98f);

        // 添加生命值组件
        enemy.addComponent(new HealthComponent(health));

        // 添加血条组件
        enemy.addComponent(new HealthBarComponent(engine.getRenderer()));

        // 添加新的AI追踪组件
        enemy.addComponent(new EnemyAIComponent(scene, speed));

        // 添加伤害组件（如果敌人碰撞玩家时需要造成伤害）
        enemy.addComponent(new DamageComponent(10));

        scene.addGameObject(enemy);
    }

    /**
     * 计算一个在屏幕外的随机生成点。
     *
     * @return 屏幕外的坐标
     */
    private Vector2 getOffScreenSpawnPosition() {
        int screenWidth = engine.getRenderer().getWidth();
        int screenHeight = engine.getRenderer().getHeight();
        int edge = random.nextInt(4); // 0: top, 1: right, 2: bottom, 3: left
        float x = 0, y = 0;
        float margin = 50; // 离屏幕边缘的距离

        switch (edge) {
            case 0: // Top
                x = random.nextFloat() * screenWidth;
                y = -margin;
                break;
            case 1: // Right
                x = screenWidth + margin;
                y = random.nextFloat() * screenHeight;
                break;
            case 2: // Bottom
                x = random.nextFloat() * screenWidth;
                y = screenHeight + margin;
                break;
            case 3: // Left
                x = -margin;
                y = random.nextFloat() * screenHeight;
                break;
        }
        return new Vector2(x, y);
    }
}
