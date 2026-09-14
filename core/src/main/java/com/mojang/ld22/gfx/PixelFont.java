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
    private static final int SPACE_ADVANCE = 4; // 空格 / 无字形时的推进量
    private static final int MIN_ADVANCE = 3; // 最小推进量
    private static final int GAP = 1; // 字形右侧留白

    // 所有可打印 ASCII。一次把这批字符交给 FreeType，
    // 才能让 ascent / yoffset 共享同一条基线。
    private static final String ASCII_CHARS = " !\"#$%&'()*+,-./0123456789:;<=>?@" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`" +
            "abcdefghijklmnopqrstuvwxyz{|}~";

    private static FreeTypeFontGenerator generator;
    private static int alphaThreshold = 128;

    /** 行顶 → 基线的像素数。init 时确定，所有字形共享。 */
    private static int lineAscent = 6;

    /**
     * LibGDX 不同版本里 Glyph.yoffset 的符号约定略有差异： 屏幕风格（主流）：yoffset = -bitmapTop，大写字母为负； 数学风格（少数）：yoffset
     * = +bitmapTop，大写字母为正。 init 时用 'H' 判定。
     */
    private static boolean yoffsetScreenStyle = true;

    private static final Map<Character, boolean[]> cache = new HashMap<>();
    private static final Map<Character, Integer> advances = new HashMap<>();

    public static void init(String fontPath) {
        generator = new FreeTypeFontGenerator(Gdx.files.internal(fontPath));

        FreeTypeFontParameter param = new FreeTypeFontParameter();
        param.size = GLYPH_SIZE;
        param.characters = ASCII_CHARS;
        param.mono = true;
        param.hinting = FreeTypeFontGenerator.Hinting.None;
        param.minFilter = Texture.TextureFilter.Nearest;
        param.magFilter = Texture.TextureFilter.Nearest;
        param.color = Color.WHITE;
        param.incremental = false;

        BitmapFont asciiFont = generator.generateFont(param);
        BitmapFont.BitmapFontData data = asciiFont.getData();

        // —— 判定 yoffset 符号约定 ——
        BitmapFont.Glyph gH = data.getGlyph('H');
        if (gH != null) {
            yoffsetScreenStyle = (gH.yoffset <= 0);
        }

        // —— 确定基线位置 ——
        // data.ascent 在小字号下常常偏小（甚至为 0），不能直接信任。
        // 'H' 的字形顶到基线距离 = |yoffset|，这是最可靠的行度量，
        // 因为大写字母顶部就是行顶附近。
        int a = Math.round(data.ascent);
        if (a <= 0) {
            a = Math.round(data.lineHeight + data.descent); // descent 为负
        }
        if (gH != null) {
            int hAscent = yoffsetScreenStyle ? -gH.yoffset : gH.yoffset;
            if (a < hAscent) a = hAscent;
        }
        if (a <= 0) a = GLYPH_SIZE - 2;
        if (a > GLYPH_SIZE) a = GLYPH_SIZE;
        lineAscent = a;

        // —— 一次光栅化所有 ASCII 字形 ——
        Texture tex = asciiFont.getRegions().first().getTexture();
        Pixmap pm = tex.getTextureData().consumePixmap();
        for (int i = 0; i < ASCII_CHARS.length(); i++) {
            rasterize(asciiFont, pm, ASCII_CHARS.charAt(i));
        }
        pm.dispose();
        asciiFont.dispose();
    }

    public static boolean[] get(char c) {
        boolean[] cached = cache.get(c);
        if (cached != null) return cached;

        if (generator == null) {
            boolean[] mask = new boolean[GLYPH_SIZE * GLYPH_SIZE];
            cache.put(c, mask);
            advances.put(c, GLYPH_SIZE);
            return mask;
        }

        // 非 ASCII（CJK 等）：单独生成。
        // 用 init 时确定好的 lineAscent / yoffsetScreenStyle 来对齐，
        // 让中文字形也落在同一条基线上（CJK 满格，等于自然对齐）。
        FreeTypeFontParameter param = new FreeTypeFontParameter();
        param.size = GLYPH_SIZE;
        param.characters = String.valueOf(c);
        param.mono = true;
        param.hinting = FreeTypeFontGenerator.Hinting.None;
        param.minFilter = Texture.TextureFilter.Nearest;
        param.magFilter = Texture.TextureFilter.Nearest;
        param.color = Color.WHITE;
        param.incremental = false;

        BitmapFont f = null;
        try {
            f = generator.generateFont(param);
            Texture tex = f.getRegions().first().getTexture();
            Pixmap pm = tex.getTextureData().consumePixmap();
            rasterize(f, pm, c);
            pm.dispose();
        } catch (Exception e) {
            e.printStackTrace();
            if (!cache.containsKey(c)) cache.put(c, new boolean[GLYPH_SIZE * GLYPH_SIZE]);
            if (!advances.containsKey(c)) advances.put(c, GLYPH_SIZE);
        } finally {
            if (f != null) f.dispose();
        }

        boolean[] m = cache.get(c);
        return m != null ? m : new boolean[GLYPH_SIZE * GLYPH_SIZE];
    }

    /** 返回该字符应占用的水平推进量（像素） */
    public static int advance(char c) {
        if (!cache.containsKey(c)) get(c);
        Integer a = advances.get(c);
        return a != null ? a : GLYPH_SIZE;
    }

    private static void rasterize(BitmapFont font, Pixmap pm, char c) {
        boolean[] mask = new boolean[GLYPH_SIZE * GLYPH_SIZE];

        BitmapFont.BitmapFontData data = font.getData();
        BitmapFont.Glyph g = data.getGlyph(c);

        if (g == null || g.width <= 0 || g.height <= 0) {
            cache.put(c, mask);
            advances.put(c, SPACE_ADVANCE);
            return;
        }

        int pmW = pm.getWidth();
        int pmH = pm.getHeight();

        int gw = Math.min(g.width, GLYPH_SIZE);
        int gh = Math.min(g.height, GLYPH_SIZE);

        /*
         * ------------------------------------------------------------
         * 1. 从 Pixmap 读取 glyph bitmap
         * ------------------------------------------------------------
         *
         * 这里先不考虑 baseline。
         * 我们只负责把 FreeType 实际生成的像素拿出来。
         */
        boolean[][] bitmap = new boolean[gh][gw];

        for (int py = 0; py < gh; py++) {
            int sy = g.srcY + py;

            if (sy < 0 || sy >= pmH)
                continue;

            for (int px = 0; px < gw; px++) {
                int sx = g.srcX + px;

                if (sx < 0 || sx >= pmW)
                    continue;

                int rgba = pm.getPixel(sx, sy);

                // Pixmap RGBA8888 的 alpha 在最低 8 bit
                if ((rgba & 0xFF) >= alphaThreshold) {
                    bitmap[py][px] = true;
                }
            }
        }

        /*
         * ------------------------------------------------------------
         * 2. 找实际 bitmap 的上下边界
         * ------------------------------------------------------------
         *
         * 不依赖 g.height 是否精确。
         */
        int minY = gh;
        int maxY = -1;
        int maxX = -1;

        for (int y = 0; y < gh; y++) {
            for (int x = 0; x < gw; x++) {
                if (!bitmap[y][x])
                    continue;

                if (y < minY)
                    minY = y;

                if (y > maxY)
                    maxY = y;

                if (x > maxX)
                    maxX = x;
            }
        }

        /*
         * 空字形。
         *
         * 例如：
         *   space
         */
        if (maxY < 0) {
            cache.put(c, mask);
            advances.put(c, SPACE_ADVANCE);
            return;
        }

        /*
         * ------------------------------------------------------------
         * 3. 实际字形高度
         * ------------------------------------------------------------
         */
        int actualHeight = maxY - minY + 1;

        /*
         * ------------------------------------------------------------
         * 4. 把 glyph 放到统一 baseline
         * ------------------------------------------------------------
         *
         * 你的 8x8 字体采用：
         *
         *     0
         *     1
         *     2
         *     3
         *     4
         *     5
         *     6  <- baseline
         *     7
         *
         * 所以普通字母底部落在 baseline - 1。
         */
        int baseline = lineAscent;

        int offY = baseline - actualHeight;

        /*
         * 防止超过 8x8。
         */
        if (offY < 0)
            offY = 0;

        if (offY + actualHeight > GLYPH_SIZE)
            offY = GLYPH_SIZE - actualHeight;

        /*
         * ------------------------------------------------------------
         * 5. 写入最终 8x8 mask
         * ------------------------------------------------------------
         */
        for (int py = minY; py <= maxY; py++) {
            int dy = offY + (py - minY);

            if (dy < 0 || dy >= GLYPH_SIZE)
                continue;

            for (int px = 0; px < gw; px++) {
                if (bitmap[py][px]) {
                    mask[dy * GLYPH_SIZE + px] = true;
                }
            }
        }

        /*
         * ------------------------------------------------------------
         * 6. 计算水平 advance
         * ------------------------------------------------------------
         */
        int advance;

        if (maxX < 0) {
            advance = SPACE_ADVANCE;
        } else {
            advance = Math.min(
                    maxX + 1 + GAP,
                    GLYPH_SIZE
            );

            if (advance < MIN_ADVANCE)
                advance = MIN_ADVANCE;
        }

        cache.put(c, mask);
        advances.put(c, advance);
    }

    public static void dispose() {
        if (generator != null) {
            generator.dispose();
            generator = null;
        }
        cache.clear();
        advances.clear();
    }
}
