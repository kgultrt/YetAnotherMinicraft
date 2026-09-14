package com.mojang.ld22.gfx;

import com.mojang.ld22.Palette;

public class Screen {
	public int xOffset;
	public int yOffset;

	public static final int BIT_MIRROR_X = 0x01;
	public static final int BIT_MIRROR_Y = 0x02;

	public final int w, h;
	public int[] pixels;

	private SpriteSheet sheet;

	public Screen(int w, int h, SpriteSheet sheet) {
		this.sheet = sheet;
		this.w = w;
		this.h = h;

		pixels = new int[w * h];
	}

	public void clear(int color) {
		for (int i = 0; i < pixels.length; i++)
			pixels[i] = color;
	}

	public void render(int xp, int yp, int tile, int colors, int bits) {
		xp -= xOffset;
		yp -= yOffset;
		boolean mirrorX = (bits & BIT_MIRROR_X) > 0;
		boolean mirrorY = (bits & BIT_MIRROR_Y) > 0;

		int xTile = tile % 32;
		int yTile = tile / 32;
		int toffs = xTile * 8 + yTile * 8 * sheet.width;

		for (int y = 0; y < 8; y++) {
			int ys = y;
			if (mirrorY) ys = 7 - y;
			if (y + yp < 0 || y + yp >= h) continue;
			for (int x = 0; x < 8; x++) {
				if (x + xp < 0 || x + xp >= w) continue;

				int xs = x;
				if (mirrorX) xs = 7 - x;
				int col = (colors >> (sheet.pixels[xs + ys * sheet.width + toffs] * 8)) & 255;
				if (col < 255) pixels[(x + xp) + (y + yp) * w] = col;
			}
		}
	}

	/** 把一个任意尺寸的位掩码写进 pixels[]，被填充的像素用调色板索引 fg。 */
	public void renderGlyphMask(boolean[] mask, int mw, int mh, int x, int y, int fg) {
		if (mask == null || mw <= 0 || mh <= 0) return;

		x -= xOffset;
		y -= yOffset;

		for (int py = 0; py < mh; py++) {
			int sy = y + py;
			if (sy < 0 || sy >= h) continue;
			int rowBase = py * mw;
			int screenBase = sy * w;
			for (int px = 0; px < mw; px++) {
				if (!mask[rowBase + px]) continue;
				int sx = x + px;
				if (sx < 0 || sx >= w) continue;
				pixels[sx + screenBase] = fg;
			}
		}
	}

	public void setOffset(int xOffset, int yOffset) {
		this.xOffset = xOffset;
		this.yOffset = yOffset;
	}

	private int[] dither = new int[] { 0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5, };

	public void overlay(Screen screen2, int xa, int ya) {
		int[] oPixels = screen2.pixels;
		int i = 0;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				if (oPixels[i] / 10 <= dither[((x + xa) & 3) + ((y + ya) & 3) * 4]) pixels[i] = 0;
				i++;
			}
		}
	}

	public void renderLight(int x, int y, int r) {
		x -= xOffset;
		y -= yOffset;
		int x0 = x - r;
		int x1 = x + r;
		int y0 = y - r;
		int y1 = y + r;

		if (x0 < 0) x0 = 0;
		if (y0 < 0) y0 = 0;
		if (x1 > w) x1 = w;
		if (y1 > h) y1 = h;

		for (int yy = y0; yy < y1; yy++) {
			int yd = yy - y;
			yd = yd * yd;
			for (int xx = x0; xx < x1; xx++) {
				int xd = xx - x;
				int dist = xd * xd + yd;
				if (dist <= r * r) {
					int br = 255 - dist * 255 / (r * r);
					if (pixels[xx + yy * w] < br) pixels[xx + yy * w] = br;
				}
			}
		}
	}

	/**
	 * 把整个屏幕压暗指定档位。
	 *
	 * <p>直接改 pixels 数组 —— 查 {@link Palette#darken} 表，
	 * 把每个像素索引替换成变暗后的索引。这是**真正的软件混合**，
	 * 不是 alpha 透明。好处是：可以只压暗"已画好的内容"，
	 * 之后再画的东西（比如菜单）完全不受影响。
	 *
	 * @param palette 调色板
	 * @param level   压暗档位，0..10。0 = 不变，10 = 全黑
	 */
	public void darken(Palette palette, int level) {
		if (level <= 0) return;
		if (level >= Palette.DARKEN_LEVELS) level = Palette.DARKEN_LEVELS - 1;

		int[] map = palette.darken[level];
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = map[pixels[i]];
		}
	}
}
