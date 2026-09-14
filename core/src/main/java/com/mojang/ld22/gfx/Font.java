package com.mojang.ld22.gfx;

public class Font {

	public static void draw(String msg, Screen screen, int x, int y, int col) {
		if (msg == null || msg.isEmpty()) return;

		int fg = (col >> 24) & 0xFF;
		int cx = x;

		for (int i = 0; i < msg.length(); i++) {
			char c = msg.charAt(i);
			boolean[] mask = PixelFont.get(c);
			if (mask != null) {
				screen.renderGlyphMask(mask, PixelFont.GLYPH_SIZE, PixelFont.GLYPH_SIZE, cx, y, fg);
			}
			cx += PixelFont.GLYPH_SIZE;
		}
	}

	public static int measure(String msg) {
		if (msg == null || msg.isEmpty()) return 0;
		return msg.length() * PixelFont.GLYPH_SIZE;
	}

	public static void renderFrame(Screen screen, String title, int x0, int y0, int x1, int y1) {
		for (int y = y0; y <= y1; y++) {
			for (int x = x0; x <= x1; x++) {
				if (x == x0 && y == y0)
					screen.render(x * 8, y * 8, 0 + 13 * 32, Color.get(-1, 1, 5, 445), 0);
				else if (x == x1 && y == y0)
					screen.render(x * 8, y * 8, 0 + 13 * 32, Color.get(-1, 1, 5, 445), 1);
				else if (x == x0 && y == y1)
					screen.render(x * 8, y * 8, 0 + 13 * 32, Color.get(-1, 1, 5, 445), 2);
				else if (x == x1 && y == y1)
					screen.render(x * 8, y * 8, 0 + 13 * 32, Color.get(-1, 1, 5, 445), 3);
				else if (y == y0)
					screen.render(x * 8, y * 8, 1 + 13 * 32, Color.get(-1, 1, 5, 445), 0);
				else if (y == y1)
					screen.render(x * 8, y * 8, 1 + 13 * 32, Color.get(-1, 1, 5, 445), 2);
				else if (x == x0)
					screen.render(x * 8, y * 8, 2 + 13 * 32, Color.get(-1, 1, 5, 445), 0);
				else if (x == x1)
					screen.render(x * 8, y * 8, 2 + 13 * 32, Color.get(-1, 1, 5, 445), 1);
				else
					screen.render(x * 8, y * 8, 2 + 13 * 32, Color.get(5, 5, 5, 5), 1);
			}
		}

		draw(title, screen, x0 * 8 + 8, y0 * 8, Color.get(5, 5, 5, 550));
	}
}