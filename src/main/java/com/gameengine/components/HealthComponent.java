package com.gameengine.components;

import com.gameengine.core.Component;

public class HealthComponent extends Component<HealthComponent> {
    private int health;
    private int maxHealth;

    public HealthComponent(int maxHealth) {
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    public void takeDamage(int damage) {
        this.health -= damage;
        if (this.health < 0) {
            this.health = 0;
        }
    }

    public boolean isDead() {
        return this.health <= 0;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void setHealth(int health) {
        this.health = health;
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
