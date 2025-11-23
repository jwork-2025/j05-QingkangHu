package com.gameengine.resources;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ResourceManager {
    private static final Map<String, BufferedImage> imageCache = new HashMap<>();
    private static final Map<String, BufferedImage> scaledCache = new HashMap<>();

    public static BufferedImage getImage(String path) {
        if (imageCache.containsKey(path)) {
            return imageCache.get(path);
        }
        try (InputStream is = ResourceManager.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("资源未找到: " + path);
                return null;
            }
            BufferedImage img = ImageIO.read(is);
            imageCache.put(path, img);
            return img;
        } catch (IOException e) {
            System.err.println("图片加载失败: " + path + "，原因: " + e.getMessage());
            return null;
        }
    }

    // 获取按指定尺寸缩放并缓存的图片
    public static BufferedImage getScaledImage(String path, int width, int height) {
        String key = path + "@" + width + "x" + height;
        if (scaledCache.containsKey(key)) {
            return scaledCache.get(key);
        }
        BufferedImage original = getImage(path);
        if (original == null) return null;
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(original, 0, 0, width, height, null);
        g2.dispose();
        scaledCache.put(key, scaled);
        return scaled;
    }
}
