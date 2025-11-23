package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.core.GameObject;
import com.gameengine.math.Vector2;

public class EnemyComponent extends Component<EnemyComponent> {
    private GameObject player;
    private float speed;

    public EnemyComponent(GameObject player, float speed) {
        this.player = player;
        this.speed = speed;
    }

    @Override
    public void initialize() {
    }

    @Override
    public void update(float deltaTime) {
    }

    @Override
    public void render() {
    }


    public float getSpeed() {
        return speed;
    }

    public GameObject getPlayer() {
        return player;
    }
}

