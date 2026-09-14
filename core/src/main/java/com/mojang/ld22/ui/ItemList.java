package com.mojang.ld22.ui;

import java.util.List;

import com.mojang.ld22.InputHandler;
import com.mojang.ld22.gfx.Color;
import com.mojang.ld22.gfx.Font;
import com.mojang.ld22.gfx.Screen;

/**
 * 物品栏类列表。管滚动、选中、光标的滑动动画。
 *
 * <p><b>自动同步列表长度</b>：每 tick 开头会拿 items.size() 同步 Cursor。
 * 所以物品被移走/放入后，无需调用方手动处理索引越界。
 *
 * <p><b>单位说明</b>：Cursor 的 rowH 用 8（像素），不是 1（行号）。
 * 因为 Cursor.y() 在渲染时要经过 Math.round 取整，如果用行号作单位，
 * 从第 0 行到第 1 行整个动画过程中只会出现 0 和 1 两个整数，
 * 中间态全被 round 吃掉，视觉上就是瞬间跳变。
 */
public class ItemList {

    private final List<? extends ListItem> items;
    private final Cursor cursor;

    private final int xo, yo, x1, y1;
    private final int visibleRows;

    private boolean cursorVisible = true;

    public ItemList(List<? extends ListItem> items, int xo, int yo, int x1, int y1) {
        this.items = items;
        this.cursor = new Cursor(items.size(), 8, true);   // 8 像素一行
        this.xo = xo;
        this.yo = yo;
        this.x1 = x1;
        this.y1 = y1;
        this.visibleRows = y1 - yo - 1;
    }

    public int size() { return items.size(); }

    public int selectedIndex() { return cursor.selectedIndex(); }

    public ListItem selected() {
        int i = cursor.selectedIndex();
        if (i < 0 || i >= items.size()) return null;
        return items.get(i);
    }

    public void setCursorVisible(boolean v) { this.cursorVisible = v; }

    public void select(int i) { cursor.snapTo(i); }

    /** 处理上下键 + 推进动画。每 tick 调一次。会自动同步列表长度。 */
    public void tick(InputHandler input) {
        cursor.setCount(items.size());
        if (items.isEmpty()) return;
        if (input.up.clicked) cursor.move(-1);
        if (input.down.clicked) cursor.move(+1);
        cursor.advance();
    }

    /** 只推进动画，不同步、不处理输入。非活跃列表用。 */
    public void advance() {
        cursor.advance();
    }

    private int firstVisible() {
        if (visibleRows <= 0) return 0;
        int io = cursor.selectedIndex() - visibleRows / 2;
        int maxIo = items.size() - visibleRows;
        if (io > maxIo) io = maxIo;
        if (io < 0) io = 0;
        return io;
    }

    public void render(Screen screen) {
        if (items.isEmpty()) return;

        int w = x1 - xo;
        int io = firstVisible();

        for (int i = 0; i < visibleRows && (i + io) < items.size(); i++) {
            items.get(i + io).renderInventory(screen, (1 + xo) * 8, (i + 1 + yo) * 8);
        }

        if (cursorVisible) {
            // cursor.y() 是像素单位。io * 8 把行号偏移转像素。
            int arrowY = (yo + 1) * 8 + Math.round(cursor.y()) - io * 8;
            Font.draw(">", screen, xo * 8, arrowY, Color.get(5, 555, 555, 555));
            Font.draw("<", screen, (xo + w) * 8, arrowY, Color.get(5, 555, 555, 555));
        }
    }
}