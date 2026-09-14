package com.mojang.ld22.ui;

import java.util.Random;

import com.mojang.ld22.gfx.Screen;

/**
 * 菜单背景：两层不同速度的星空视差滚动。
 *
 * <ul>
 *   <li>两层同图案、不同速度 → 纵深感</li>
 *   <li>屏幕不动、图案自身滚动 → 背景"粘"在屏幕上，不跟菜单内容跑</li>
 *   <li>整体压暗 → 不抢标题和选项</li>
 * </ul>
 *
 * <p>做成单例，所有菜单共享滚动状态，切换菜单时星空是连续的。
 * 各菜单只需在 render 开头调 {@code MenuBackground.get().render(screen)}。
 *
 * <p>推进时机有两处：菜单自身的 {@code tick()}，以及 {@link MenuManager#tick()}。
 * 两者互斥（动画期间前者不跑，稳定期间后者不跑），所以同一 tick 只推进一次。
 *
 * <p>图案是 64×64 的 palette index 平铺，无需资源文件。
 * 底色用 6×6×6 cube 里的深蓝 {@code 001}，星星亮度分四档。
 */
public final class MenuBackground {

    /** 平铺图案的边长。必须是 2 的幂，blit 里用位与做 wrap。 */
    private static final int PATTERN = 64;

    /** 6×6×6 cube 里 (0,0,1) 对应的索引，深蓝。 */
    private static final int BASE_COLOR = 0 * 36 + 0 * 6 + 1;

    /** 远层滚动速度，px/秒。 */
    private static final float SPEED_FAR = 4f;

    /** 近层滚动速度，px/秒。约 2.75 倍速差，视差感刚好。 */
    private static final float SPEED_NEAR = 11f;

    private static MenuBackground INSTANCE;

    public static MenuBackground get() {
        if (INSTANCE == null) INSTANCE = new MenuBackground(0x5EED1CE5L);
        return INSTANCE;
    }

    /** 远层：星星稀疏、暗。0 表示透明。 */
    private final int[] farLayer;

    /** 近层：星星稍密、亮。0 表示透明。 */
    private final int[] nearLayer;

    private float scrollFar;
    private float scrollNear;

    private MenuBackground(long seed) {
        Random rng = new Random(seed);
        farLayer  = makeStars(rng, 26, 1, 3);  // 亮度 1..3，暗星
        nearLayer = makeStars(rng, 14, 3, 5);  // 亮度 3..5，亮星
    }

    /**
     * 推进滚动。每 tick 调一次，传入 1f/60f。
     *
     * <p>对 PATTERN 取模，避免长时间运行后 float 累积到精度问题。
     * 对正数而言取模结果恒在 [0, PATTERN)，render 里那个
     * {@code & (PATTERN - 1)} 依然成立。
     */
    public void update(float delta) {
        scrollFar  = (scrollFar  + SPEED_FAR  * delta) % PATTERN;
        scrollNear = (scrollNear + SPEED_NEAR * delta) % PATTERN;
    }

    /**
     * 把星空铺满整个 Screen。直接写 palette index，
     * 不经过 Screen.render，避免贴图开销。
     */
    public void render(Screen screen) {
        int[] dst = screen.pixels;
        int sw = screen.w;
        int sh = screen.h;

        int offFar  = ((int) scrollFar ) & (PATTERN - 1);
        int offNear = ((int) scrollNear) & (PATTERN - 1);

        for (int y = 0; y < sh; y++) {
            int rowF = ((y + offFar ) & (PATTERN - 1)) * PATTERN;
            int rowN = ((y + offNear) & (PATTERN - 1)) * PATTERN;
            int base = y * sw;

            for (int x = 0; x < sw; x++) {
                int px = x & (PATTERN - 1);

                // 近层优先，其次远层，最后底色
                int c = nearLayer[rowN + px];
                if (c == 0) c = farLayer[rowF + px];
                if (c == 0) c = BASE_COLOR;

                dst[base + x] = c;
            }
        }
    }

    // ------------------------------------------------------------ 生成

    /**
     * 随机撒 {@code count} 颗星星到 PATTERN×PATTERN 的图案里。
     * 同一格被撒中两次时保留第一次，避免叠出过亮的点。
     */
    private static int[] makeStars(Random rng, int count, int minBright, int maxBright) {
        int[] layer = new int[PATTERN * PATTERN];
        for (int i = 0; i < count; i++) {
            int x = rng.nextInt(PATTERN);
            int y = rng.nextInt(PATTERN);
            int idx = y * PATTERN + x;
            if (layer[idx] != 0) continue;

            int bright = minBright + rng.nextInt(maxBright - minBright + 1);
            layer[idx] = encodeGray(bright);
        }
        return layer;
    }

    /** 把 0..5 的亮度编成 6×6×6 cube 里的等灰度索引。 */
    private static int encodeGray(int level) {
        if (level < 0) level = 0;
        if (level > 5) level = 5;
        return level * 36 + level * 6 + level;
    }
}