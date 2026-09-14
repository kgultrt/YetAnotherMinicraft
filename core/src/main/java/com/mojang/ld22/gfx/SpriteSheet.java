package com.mojang.ld22.gfx;

import com.badlogic.gdx.graphics.Pixmap;

public class SpriteSheet {
	public int width, height;
	public int[] pixels;

	public SpriteSheet(Pixmap pixmap) {
		width = pixmap.getWidth();
		height = pixmap.getHeight();
		pixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int rgba = pixmap.getPixel(x, y);
				int b = (rgba >>> 8) & 0xff;
				pixels[x + y * width] = b / 64;
			}
		}
	}
}