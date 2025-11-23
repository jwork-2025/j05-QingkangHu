package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.graphics.IRenderer;
import com.gameengine.math.Vector2;

public class HealthBarComponent extends Component<HealthBarComponent> {

    private IRenderer renderer;
    private HealthComponent healthComponent;
    private TransformComponent transformComponent;

    // 血条样式配置
    private Vector2 offset = new Vector2(0, -15); // 相对于对象中心的偏移
    private float width = 30;
    private float height = 5;
    private RenderComponent.Color healthColor = new RenderComponent.Color(0.0f, 1.0f, 0.0f, 0.8f);
    private RenderComponent.Color backgroundColor = new RenderComponent.Color(1.0f, 0.0f, 0.0f, 0.8f);

    public HealthBarComponent(IRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void initialize() {
        // 获取宿主对象上必需的组件
        this.healthComponent = owner.getComponent(HealthComponent.class);
        this.transformComponent = owner.getComponent(TransformComponent.class);

        if (healthComponent == null || transformComponent == null) {
            System.err.println("HealthBarComponent needs HealthComponent and TransformComponent on the same GameObject.");
            this.setEnabled(false); // 如果缺少必要组件，则禁用此组件
        }
    }

    @Override
    public void update(float deltaTime) {

    }

    @Override
    public void render() {
        if (renderer == null || !isEnabled()) {
            return; // 如果未启用，则不渲染
        }

        Vector2 pos = transformComponent.getPosition();
        float healthPercentage = (float) healthComponent.getHealth() / healthComponent.getMaxHealth();

        float barX = pos.x - width / 2 + offset.x;
        float barY = pos.y + offset.y;

        // 绘制血条背景
        renderer.drawRect(barX, barY, width, height, backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a);
        // 绘制当前血量
        renderer.drawRect(barX, barY, width * healthPercentage, height, healthColor.r, healthColor.g, healthColor.b, healthColor.a);
    }

    // --- 你可以添加更多setter方法来自定义血条样式 ---
    public void setOffset(Vector2 offset) {
        this.offset = offset;
    }

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }
}
