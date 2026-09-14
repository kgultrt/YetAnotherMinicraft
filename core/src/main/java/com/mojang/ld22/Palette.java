package com.mojang.ld22;

/**
 * Minicraft 的 256 色调色板。
 *
 * <p>索引 0..254 是 6×6×6 色立方体（带亮度校正），255 表示透明。
 * {@link #colors} 是 0xRRGGBB，{@link #rgba} 是 RGBA8888。
 *
 * <p>{@link #darken} 是预计算的"变暗映射表"：
 * {@code darken[level][idx]} 是索引 idx 的像素压暗 level 档后最接近的索引。
 * level 0..10，每档 10%。用于软件混合式的整屏压暗。
 */
public final class Palette {

    /** 变暗档位数量。0=不压，10=全黑。 */
    public static final int DARKEN_LEVELS = 11;

    public final int[] colors = new int[256];
    public final int[] rgba = new int[256];

    /** {@code darken[level][index]} → 压暗后的索引。 */
    public final int[][] darken = new int[DARKEN_LEVELS][256];

    public void build() {
        int pp = 0;
        for (int r = 0; r < 6; r++) {
            for (int g = 0; g < 6; g++) {
                for (int b = 0; b < 6; b++) {
                    int rr = (r * 255 / 5);
                    int gg = (g * 255 / 5);
                    int bb = (b * 255 / 5);
                    int mid = (rr * 30 + gg * 59 + bb * 11) / 100;

                    int r1 = ((rr + mid * 1) / 2) * 230 / 255 + 10;
                    int g1 = ((gg + mid * 1) / 2) * 230 / 255 + 10;
                    int b1 = ((bb + mid * 1) / 2) * 230 / 255 + 10;
                    colors[pp++] = r1 << 16 | g1 << 8 | b1;
                }
            }
        }
        colors[255] = 0;

        for (int i = 0; i < 256; i++) {
            rgba[i] = (colors[i] << 8) | 0xFF;
        }
        rgba[255] = 0;

        buildDarkenTable();
    }

    /**
     * 预计算变暗表。对每个 (level, index)，
     * 把该索引的颜色乘 (10-level)/10，然后找调色板里最接近的索引。
     *
     * <p>因为目标颜色一定落在调色板里（无非是更暗的采样点），
     * 这个映射是稳定的、可逆的（除了最暗的档位会塌缩到索引 0）。
     */
    private void buildDarkenTable() {
        for (int level = 0; level < DARKEN_LEVELS; level++) {
            int keep = 10 - level;
            if (keep <= 0) {
                for (int i = 0; i < 256; i++) darken[level][i] = 0;
                continue;
            }

            for (int i = 0; i < 256; i++) {
                if (i == 255) {
                    darken[level][i] = 255;   // 透明保持透明
                    continue;
                }

                int rgb = colors[i];
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                r = r * keep / 10;
                g = g * keep / 10;
                b = b * keep / 10;

                darken[level][i] = nearestCubeIndex(r, g, b);
            }
        }
    }

    /**
     * 找 6×6×6 立方体里跟给定 RGB 最接近的索引。
     * 立方体的采样点是均匀的 —— 每个通道按比例四舍五入即可。
     */
    private static int nearestCubeIndex(int r, int g, int b) {
        int ri = (r * 5 + 127) / 255;
        int gi = (g * 5 + 127) / 255;
        int bi = (b * 5 + 127) / 255;
        if (ri > 5) ri = 5;
        if (gi > 5) gi = 5;
        if (bi > 5) bi = 5;
        return ri * 36 + gi * 6 + bi;
    }
}