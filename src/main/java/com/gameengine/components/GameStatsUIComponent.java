package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.core.GameEngine;
import com.gameengine.core.GameLogic;
import com.gameengine.graphics.IRenderer;

import java.util.Map;

public class GameStatsUIComponent extends Component<GameStatsUIComponent> {
    private final IRenderer renderer;
    private final GameLogic gameLogic;
    private final GameEngine gameEngine;

    // 用于计算平均帧率
    private float fpsTimer = 0f;
    private int frameCount = 0;
    private float averageFps = 0f;

    public GameStatsUIComponent(IRenderer renderer, GameLogic gameLogic, GameEngine gameEngine) {
        this.renderer = renderer;
        this.gameLogic = gameLogic;
        this.gameEngine = gameEngine;
    }

    @Override
    public void initialize() {
        // 初始化
    }

    @Override
    public void update(float deltaTime) {
        // 计算平均帧率
        fpsTimer += deltaTime;
        frameCount++;
        if (fpsTimer >= 1.0f) {
            averageFps = frameCount / fpsTimer;
            frameCount = 0;
            fpsTimer = 0f;
        }
    }

    @Override
    public void render() {
        if (renderer == null || gameLogic == null) {
            return;
        }

        int killCount = gameLogic.getKillCount();
        float survivalTime = gameLogic.getSurvivalTime();
        int enemyCount = gameLogic.getEnemyCount();

        // 设置UI文本的属性
        int x = 20;
        int y = 30;
        int fontSize = 18;
        int lineHeight = 30;

        // 绘制击杀数
        String killText = "击杀数: " + killCount;
        renderer.drawText(x, y, killText, 1, 1, 1, 1);

        // 绘制生存时间
        y += lineHeight;
        String timeText = String.format("存活时间: %.1fs", survivalTime);
        renderer.drawText(x, y, timeText, 1, 1, 1, 1);

        // 绘制敌人数量
        y += lineHeight;
        String enemyText = "敌人数: " + enemyCount;
        renderer.drawText(x, y, enemyText, 1, 1, 1, 1);

        // 绘制平均帧率
        y += lineHeight;
        String fpsText = String.format("平均帧率: %.1f", averageFps);
        renderer.drawText(x, y, fpsText, 1, 1, 1, 1);
    }
}
