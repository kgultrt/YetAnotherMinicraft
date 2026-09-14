package com.mojang.ld22.ui;

/**
 * 一行选择光标。跟踪选中索引、目标 y、当前 y，带缓动。
 *
 * <p>不知道列表里是什么，只知道"有多少行、行高多少、选在第几行"。
 * 渲染层用 {@link #y()} 拿当前的浮点位置，用 {@link #highlightIndex()}
 * 拿"光标最近的那一项的索引"，来决定哪一行高亮。
 *
 * <p>y 的单位由构造时的 rowH 决定 —— MenuList 传 8（像素），
 * ItemList 传 8（像素）。
 */
public class Cursor {

    /** 滑动动画时长。8 tick ≈ 0.13 秒，够快不拖。 */
    private static final int ANIM_TICKS = 8;

    private int count;
    private final int rowH;
    private final boolean wrap;

    private int selectedIdx = 0;
    private float y = 0f;
    private float fromY = 0f;
    private float toY = 0f;
    private int animTick = ANIM_TICKS;

    public Cursor(int count, int rowH, boolean wrap) {
        this.count = count;
        this.rowH = rowH;
        this.wrap = wrap;
    }

    public int selectedIndex() { return selectedIdx; }

    public float y() { return y; }

    public int highlightIndex() {
        int h = Math.round(y / rowH);
        if (h < 0) h = 0;
        if (h >= count) h = count - 1;
        return h;
    }

    /**
     * 同步列表长度。列表内容变化（物品被移走/放入）后，
     * count 会跟实际对不上，索引可能越界。调这个修正。
     *
     * <p>如果越界，直接 snap 到最后一个合法位置（不带动画），
     * 避免光标还"停在一个不存在的行上"。长度没变时早返回。
     */
    public void setCount(int n) {
        if (n == count) return;
        this.count = n;

        if (count <= 0) {
            selectedIdx = 0;
            y = 0;
            fromY = 0;
            toY = 0;
            animTick = ANIM_TICKS;
            return;
        }

        if (selectedIdx >= count) selectedIdx = count - 1;
        if (selectedIdx < 0) selectedIdx = 0;

        y = selectedIdx * rowH;
        fromY = y;
        toY = y;
        animTick = ANIM_TICKS;
    }

    public void snapTo(int index) {
        if (count <= 0) return;
        if (index < 0 || index >= count) return;
        selectedIdx = index;
        y = index * rowH;
        fromY = y;
        toY = y;
        animTick = ANIM_TICKS;
    }

    public void moveTo(int index) {
        if (count <= 0) return;
        if (index < 0 || index >= count) return;
        fromY = y;
        toY = index * rowH;
        animTick = 0;
        selectedIdx = index;
    }

    public void move(int delta) {
        if (count <= 0) return;
        int i = selectedIdx + delta;
        if (wrap) {
            if (i < 0) i += count;
            if (i >= count) i -= count;
        } else {
            if (i < 0) i = 0;
            if (i >= count) i = count - 1;
        }
        moveTo(i);
    }

    public void advance() {
        if (animTick < ANIM_TICKS) {
            animTick++;
            float t = animTick / (float) ANIM_TICKS;
            float eased = 1f - (1f - t) * (1f - t);   // easeOutQuad
            y = fromY + (toY - fromY) * eased;
        }
    }
}