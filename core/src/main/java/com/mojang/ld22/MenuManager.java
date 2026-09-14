package com.mojang.ld22;

import com.mojang.ld22.gfx.Screen;
import com.mojang.ld22.screen.Menu;
import com.mojang.ld22.ui.MenuBackground;

/**
 * 菜单状态机与切换动画。
 *
 * <h3>切换规则</h3>
 * <ul>
 *   <li>无菜单 → 菜单：新菜单从一侧滑入</li>
 *   <li>菜单 → 菜单：旧的先被"推"出去，新的随后"滑"进来</li>
 *   <li>菜单 → 无菜单：旧的滑出，游戏露出</li>
 * </ul>
 *
 * <h3>关键设计</h3>
 * 旧菜单走 easeInQuad（慢起快走），新菜单走 easeOutQuad（快进慢停）。
 * 两条曲线方向相反，形成"接力"感 —— 旧的先懒一下再退场，新的趁这个
 * 空档冲进来占位。若两边都用 easeOut，会变成两块板同步平移，观感呆板。
 *
 * <h3>方向感知</h3>
 * {@link #set(Menu, int)} 的 direction 参数：+1 表示"前进"（新菜单从右侧来），
 * -1 表示"返回"（新菜单从左侧来）。这样深入子菜单和退回上级在视觉上是
 * 有区别的，UI 会显得有纵深。默认 +1。
 *
 * <p>当前稳定显示的菜单仍然存在 {@link Game#menu}，保持外部访问方式不变。
 */
public class MenuManager {

    /**
     * 动画时长（tick）。24 tick = 0.4 秒。
     * 像素游戏转场超过 0.4s 就开始显得拖沓，低于 0.3s 又看不清过程。
     */
    private static final int ANIM_DURATION = 24;

    /**
     * 菜单滑动的距离。默认一屏宽 —— 从完全在屏外滑到居中。
     * 想更紧凑可以改成 {@code Game.WIDTH * 0.5f}，两端各让半屏，
     * 中间几乎不留空白。代价是"从屏外来"的物理感弱一点。
     */
    private static final float SLIDE_DISTANCE = Game.WIDTH;

    private final Game game;
    private final InputHandler input;

    /**
     * 星空背景。动画期间菜单自身的 tick 不被调用，
     * 所以在这里补一次推进，否则 24 tick 里背景会僵住。
     */
    private final MenuBackground bg = MenuBackground.get();

    /** 即将进入的菜单。null 表示切回游戏。 */
    private Menu pending;

    /** 当前动画的播放方向：+1 右进左出，-1 左进右出。 */
    private int dir = 1;

    private boolean animating;
    private int animTicks;

    /** 当前菜单的水平偏移量（相对于最终位置的像素偏移，正值在右）。 */
    private float offset;

    /** 待进入菜单的水平偏移量。 */
    private float pendingOffset;

    public MenuManager(Game game, InputHandler input) {
        this.game = game;
        this.input = input;
    }

    public boolean isAnimating() {
        return animating;
    }

    /** 默认方向：前进（新菜单从右侧来）。 */
    public void set(Menu newMenu) {
        set(newMenu, 1);
    }

    /**
     * 请求切换菜单。
     *
     * @param newMenu   目标菜单；null 表示回到游戏
     * @param direction +1 前进 / -1 返回
     */
    public void set(Menu newMenu, int direction) {
        if (newMenu != null) newMenu.init(game, input);

        // 动画进行中时只覆盖目标，不打断当前动画，也不改方向。
        // 24 tick 内用户不可能点两次，所以方向取最初那次请求的即可。
        if (animating) {
            pending = newMenu;
            return;
        }

        if (newMenu == game.menu) return;

        pending = newMenu;
        dir = (direction >= 0) ? 1 : -1;
        animating = true;
        animTicks = 0;
        offset = 0f;
        pendingOffset = SLIDE_DISTANCE * dir;
    }

    /**
     * 直接切换，不走滑动动画。
     *
     * <p>用于"重置世界，立即回到游戏"这种场景 —— 硬切比滑出再滑入干净。
     * 从标题菜单进游戏时，先滑出标题再露出游戏会拖延 0.4 秒，
     * 而开场动画本身已经在做视觉过渡，滑动是多余的。
     */
    public void setImmediate(Menu newMenu) {
        if (newMenu != null) newMenu.init(game, input);
        game.menu = newMenu;
        pending = null;
        animating = false;
        animTicks = 0;
        offset = 0f;
        pendingOffset = 0f;
    }

    /** 推进一 tick 的切换动画。 */
    public void tick() {
        bg.update(1f / 60f);

        animTicks++;
        float t = animTicks / (float) ANIM_DURATION;
        if (t > 1f) t = 1f;

        // 旧菜单：easeInQuad —— 慢起快走，被"推"出去。
        float oldEased = easeInQuad(t);

        // 新菜单：easeOutQuad —— 快进慢停，稳稳落位。
        float newEased = easeOutQuad(t);

        offset        = -SLIDE_DISTANCE * dir * oldEased;
        pendingOffset =  SLIDE_DISTANCE * dir * (1f - newEased);

        if (t >= 1f) {
            animating = false;
            game.menu = pending;
            pending = null;
            offset = 0f;
            pendingOffset = 0f;
        }
    }

    /** 双缓冲渲染：旧菜单滑出 + 新菜单滑入。 */
    public void render(Screen screen) {
        int savedX = screen.xOffset;
        int savedY = screen.yOffset;

        if (game.menu != null) {
            screen.setOffset(savedX - Math.round(offset), savedY);
            game.menu.render(screen);
        }

        if (animating && pending != null) {
            screen.setOffset(savedX - Math.round(pendingOffset), savedY);
            pending.render(screen);
        }

        screen.setOffset(savedX, savedY);
    }

    // ---------------------------------------------------------------- 缓动

    /** 二次缓入：t²。起步慢，末段快。 */
    private static float easeInQuad(float t) {
        return t * t;
    }

    /** 二次缓出：1 - (1-t)²。起步快，末段慢。 */
    private static float easeOutQuad(float t) {
        float omt = 1f - t;
        return 1f - omt * omt;
    }

    // ------------------------------------------------------- 备选缓动函数
    //
    // 下面几个不参与默认动画，留作调节时的参考：
    //
    // easeOutCubic  —— 原版用的曲线，比 quad 收尾更"急刹车"。
    //                  观感上像是最后被地板黏住，所以换掉了。
    // easeOutQuart  —— 比 cubic 更冲，尾巴更长，适合更长的动画。
    // easeOutBack   —— 带过冲，冲过中央一点再回弹，有弹性感。
    //                  像素风里幅度要压小，c1 取 0.8 左右，冲过头约 5% 屏宽。

    @SuppressWarnings("unused")
    private static float easeOutCubic(float t) {
        float omt = 1f - t;
        return 1f - omt * omt * omt;
    }

    @SuppressWarnings("unused")
    private static float easeOutQuart(float t) {
        float omt = 1f - t;
        float omt2 = omt * omt;
        return 1f - omt2 * omt2;
    }

    @SuppressWarnings("unused")
    private static float easeOutBack(float t) {
        float c1 = 0.8f;
        float c3 = c1 + 1f;
        float tm = t - 1f;
        return 1f + c3 * tm * tm * tm + c1 * tm * tm;
    }
}