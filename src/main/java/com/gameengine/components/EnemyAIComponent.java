package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.core.GameObject;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

public class EnemyAIComponent extends Component<EnemyAIComponent> {

    private GameObject player;
    private final float speed;
    private final Scene scene;
    private boolean active = true; // 添加 active 状态

    /**
     * 构造一个敌人AI组件。
     *
     * @param scene 游戏场景，用于查找玩家对象。
     * @param speed 敌人的移动速度。
     */
    public EnemyAIComponent(Scene scene, float speed) {
        this.scene = scene;
        this.speed = speed;
    }

    public void setActive(boolean active) { // 添加 setActive 方法
        this.active = active;
    }

    @Override
    public void initialize() {
        // 在场景中查找名为 "Player" 的对象。
        for (GameObject go : scene.getGameObjects()) {
            if (go.getName().equals("Player")) {
                this.player = go;
                break;
            }
        }
    }

    @Override
    public void update(float deltaTime) {
        // 如果玩家对象不存在、已失效，或者当前组件没有所有者，则不执行任何操作。
        if (player == null || !player.isActive() || owner == null || !active) {
            // 尝试重新查找玩家
            if (player == null || !player.isActive()) {
                initialize();
            }
            return;
        }

        TransformComponent myTransform = owner.getComponent(TransformComponent.class);
        TransformComponent playerTransform = player.getComponent(TransformComponent.class);
        PhysicsComponent myPhysics = owner.getComponent(PhysicsComponent.class);

        // 确保所有必需的组件都存在
        if (myTransform != null && playerTransform != null && myPhysics != null) {
            Vector2 myPosition = myTransform.getPosition();
            Vector2 playerPosition = playerTransform.getPosition();

            // 计算从当前位置到玩家位置的方向向量
            Vector2 direction = playerPosition.subtract(myPosition).normalize();

            // 设置物理组件的速度，使敌人朝向玩家移动
            myPhysics.setVelocity(direction.multiply(speed));
        }
    }

    @Override
    public void render() {

    }
}
