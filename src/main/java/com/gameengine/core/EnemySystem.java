package com.gameengine.core;

import com.gameengine.components.EnemyComponent;
import com.gameengine.components.PhysicsComponent;
import com.gameengine.components.TransformComponent;
import com.gameengine.math.Vector2;

import java.util.List;

public class EnemySystem {

    public void update(List<GameObject> gameObjects, GameObject player, float deltaTime) {
        if (player == null) {
            return;
        }

        TransformComponent playerTransform = player.getComponent(TransformComponent.class);
        if (playerTransform == null) {
            return;
        }

        for (GameObject go : gameObjects) {
            if (go.hasComponent(EnemyComponent.class)) {
                EnemyComponent enemy = go.getComponent(EnemyComponent.class);
                TransformComponent transform = go.getComponent(TransformComponent.class);
                PhysicsComponent physics = go.getComponent(PhysicsComponent.class);

                if (transform != null && physics != null) {
                    float dx = playerTransform.getPosition().x - transform.getPosition().x;
                    float dy = playerTransform.getPosition().y - transform.getPosition().y;

                    float distance = (float) Math.sqrt(dx * dx + dy * dy);
                    if (distance > 0) {
                        float vx = (dx / distance) * enemy.getSpeed();
                        float vy = (dy / distance) * enemy.getSpeed();
                        physics.setVelocity(new Vector2(vx, vy));
                    }
                }
            }
        }
    }
}

