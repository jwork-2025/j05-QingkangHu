package com.gameengine.components;

import com.gameengine.core.Component;

public class DamageComponent extends Component<DamageComponent> {
    private int damage;

    public DamageComponent(int damage) {
        this.damage = damage;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
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
}
