package com.mojang.ld22.screen;

import java.util.List;

import com.mojang.ld22.Game;
import com.mojang.ld22.InputHandler;
import com.mojang.ld22.gfx.Color;
import com.mojang.ld22.gfx.Font;
import com.mojang.ld22.gfx.Screen;
import com.mojang.ld22.ui.ListItem;

public class Menu {
    protected Game game;
    protected InputHandler input;

    public void init(Game game, InputHandler input) {
        this.input = input;
        this.game = game;
    }

    public void tick() {}

    public void render(Screen screen) {}

    /**
     * 这个菜单希望背景被压暗多少档。0 = 不压，10 = 全黑。
     *
     * <p>Game 在 renderFrame 里画完游戏画面、画菜单之前应用。 因为用的是软件混合（查表替换像素索引）， <b>菜单本身不会被压暗</b> —— 只有已画好的游戏画面变暗。
     */
    public int getScreenDarken() {
        return 0;
    }

    /**
     * @deprecated 无动画的旧接口。改用 {@link com.mojang.ld22.ui.ItemList}， 它会自动带箭头滑动动画。
     */
    @Deprecated
    public void renderItemList(Screen screen, int xo, int yo, int x1, int y1,
            List<? extends ListItem> listItems, int selected) {
        boolean renderCursor = true;
        if (selected < 0) {
            selected = -selected - 1;
            renderCursor = false;
        }
        int w = x1 - xo;
        int h = y1 - yo - 1;
        int io = selected - h / 2;
        if (io > listItems.size() - h) io = listItems.size() - h;
        if (io < 0) io = 0;

        for (int i = 0; i < Math.min(listItems.size(), h); i++) {
            listItems.get(i + io).renderInventory(screen, (1 + xo) * 8, (i + 1 + yo) * 8);
        }

        if (renderCursor) {
            int yy = selected + 1 - io + yo;
            Font.draw(">", screen, xo * 8, yy * 8, Color.get(5, 555, 555, 555));
            Font.draw("<", screen, (xo + w) * 8, yy * 8, Color.get(5, 555, 555, 555));
        }
    }
}
