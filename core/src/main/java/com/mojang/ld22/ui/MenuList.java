package com.mojang.ld22.ui;

import java.util.List;

import com.mojang.ld22.InputHandler;

/**
 * 一组可上下选择的菜单项。管选中、循环、箭头滑动、滚动。
 *
 * <p>不管渲染 —— 各菜单的排版差异太大。外部通过 {@link #selectedIndex()}、
 * {@link #arrowY()}、{@link #highlightIndex()}、{@link #firstVisible()}
 * 拿状态自己画。
 */
public class MenuList<T> {

    private final List<T> items;
    private final Cursor cursor;

    /** 可见行数。0 = 不限制，全部可见。 */
    private final int visibleRows;

    /** 一个选项的高度（像素）。滚动时需要用它换算像素偏移。 */
    private final int rowH;

    public MenuList(List<T> items, int rowH) {
        this(items, rowH, 0);
    }

    /**
     * @param visibleRows 可见行数；0 表示不滚动（所有项都画出来）
     */
    public MenuList(List<T> items, int rowH, int visibleRows) {
        this.items = items;
        this.rowH = rowH;
        this.visibleRows = visibleRows;
        this.cursor = new Cursor(items.size(), rowH, true);
    }

    public int size() { return items.size(); }
    public T get(int i) { return items.get(i); }
    public int selectedIndex() { return cursor.selectedIndex(); }
    public T selected() { return items.get(cursor.selectedIndex()); }
    public int highlightIndex() { return cursor.highlightIndex(); }
    public float arrowY() { return cursor.y(); }

    /** 可见行数。0 表示不滚动。 */
    public int visibleRows() { return visibleRows; }

    /**
     * 列表顶部第一行的逻辑索引。没滚动时恒为 0。
     * 渲染时，屏幕第 i 行画的是 items.get(firstVisible() + i)。
     */
    public int firstVisible() {
        if (visibleRows <= 0 || items.size() <= visibleRows) return 0;
        int io = cursor.selectedIndex() - visibleRows / 2;
        int maxIo = items.size() - visibleRows;
        if (io > maxIo) io = maxIo;
        if (io < 0) io = 0;
        return io;
    }

    /**
     * 光标在屏幕上的 y 偏移（相对列表头，像素）。
     * 已扣除滚动偏移 —— 无论列表滚到哪里，返回的都是相对可见窗口的位置。
     */
    public float arrowYOnScreen() {
        return cursor.y() - firstVisible() * rowH;
    }

    public void select(int i) { cursor.snapTo(i); }

    public void tick(InputHandler input) {
        if (input.up.clicked) cursor.move(-1);
        if (input.down.clicked) cursor.move(+1);
        cursor.advance();
    }
}