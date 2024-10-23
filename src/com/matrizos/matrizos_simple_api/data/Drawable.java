package com.matrizos.matrizos_simple_api.data;

import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.awt.image.BufferedImage;

@Getter
public abstract class Drawable {
    private final int width, height;
    @Setter private float scale = 1f;
    @Setter private boolean enable = true;

    public Drawable(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public BufferedImage toImage() {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        toDraw((Graphics2D)img.getGraphics(), 0, 0);
        img.getGraphics().dispose();
        return img;
    }

    protected abstract void toDraw(Graphics2D g, int offX, int offY);

    public void draw(Graphics2D g, int offX, int offY) {
        if(enable)
            toDraw(g, offX, offY);
    }
}
