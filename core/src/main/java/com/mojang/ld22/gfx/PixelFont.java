package com.mojang.ld22.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

import java.util.HashMap;
import java.util.Map;

public class PixelFont {

	public static final int GLYPH_SIZE = 8;

	private static FreeTypeFontGenerator generator;
	private static int alphaThreshold = 128;

	private static final Map<Character, boolean[]> cache = new HashMap<>();

	public static void init(String fontPath) {
		generator = new FreeTypeFontGenerator(Gdx.files.internal(fontPath));
	}

	public static boolean[] get(char c) {
		boolean[] cached = cache.get(c);
		if (cached != null) return cached;

		boolean[] mask;
		if (generator == null) {
			mask = new boolean[GLYPH_SIZE * GLYPH_SIZE];
		} else {
			mask = rasterize(c);
		}
		cache.put(c, mask);
		return mask;
	}

	private static boolean[] rasterize(char c) {
		boolean[] mask = new boolean[GLYPH_SIZE * GLYPH_SIZE];

		FreeTypeFontParameter param = new FreeTypeFontParameter();
		param.size = GLYPH_SIZE;
		param.characters = String.valueOf(c);
		param.mono = true;
		param.hinting = FreeTypeFontGenerator.Hinting.None;
		param.minFilter = Texture.TextureFilter.Nearest;
		param.magFilter = Texture.TextureFilter.Nearest;
		param.color = Color.WHITE;
		param.incremental = false;

		BitmapFont font = null;
		try {
			font = generator.generateFont(param);
			BitmapFont.BitmapFontData data = font.getData();
			BitmapFont.Glyph g = data.getGlyph(c);

			if (g == null || g.width <= 0 || g.height <= 0) {
				return mask;
			}

			Texture tex = font.getRegions().first().getTexture();
			Pixmap pm = tex.getTextureData().consumePixmap();
			int pmW = pm.getWidth();
			int pmH = pm.getHeight();

			// 字形可能占不满 8×8，居中放进格子
			int gw = Math.min(g.width, GLYPH_SIZE);
			int gh = Math.min(g.height, GLYPH_SIZE);
			int offX = (GLYPH_SIZE - gw) / 2;
			int offY = (GLYPH_SIZE - gh) / 2;

			for (int py = 0; py < gh; py++) {
				int sy = g.srcY + py;
				if (sy < 0 || sy >= pmH) continue;
				for (int px = 0; px < gw; px++) {
					int sx = g.srcX + px;
					if (sx < 0 || sx >= pmW) continue;

					int rgba = pm.getPixel(sx, sy);
					if ((rgba & 0xFF) >= alphaThreshold) {
						int mx = px + offX;
						int my = py + offY;
						if (mx >= 0 && mx < GLYPH_SIZE && my >= 0 && my < GLYPH_SIZE) {
							mask[my * GLYPH_SIZE + mx] = true;
						}
					}
				}
			}
			pm.dispose();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (font != null) font.dispose();
		}

		return mask;
	}

	public static void dispose() {
		if (generator != null) {
			generator.dispose();
			generator = null;
		}
		cache.clear();
	}
}