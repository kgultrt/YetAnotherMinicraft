package com.mojang.ld22.screen;

import java.util.Arrays;

import com.badlogic.gdx.Gdx;
import com.mojang.ld22.gfx.Color;
import com.mojang.ld22.gfx.Font;
import com.mojang.ld22.gfx.Screen;
import com.mojang.ld22.i18n.Messages;
import com.mojang.ld22.save.SaveManager;
import com.mojang.ld22.sound.Sound;
import com.mojang.ld22.ui.MenuList;

/**
 * 暂停菜单。盖在游戏画面上，不铺背景，靠 {@link #getScreenDarken()} 让 Game 把游戏画面压暗。
 *
 * <p>菜单一被设置，{@code Game.tick()} 里 {@code menu != null} 分支会接管， 世界逻辑（level.tick / 怪物 / 计时）全部冻结 ——
 * 这就是暂停。
 */
public class PauseMenu extends Menu {

    /** 背景压暗档位。5/10 = 50%，游戏画面仍可辨认。 */
    private static final int DARKEN = 5;

    private static final String[] OPTION_KEYS = {
            "option.resume", "option.save", "option.save_and_quit", "option.title"
    };

    /** "Saved!" 提示显示的时长（tick）。90 tick = 1.5 秒。 */
    private static final int SAVED_MESSAGE_DURATION = 90;

    private final MenuList<String> list = new MenuList<>(Arrays.asList(OPTION_KEYS), 8);

    /** 保存成功后的提示倒计时。0 = 不显示。 */
    private int savedMessageTicks = 0;

    @Override
    public int getScreenDarken() {
        return DARKEN;
    }

    public void tick() {
        if (savedMessageTicks > 0) savedMessageTicks--;

        // ESC / Android 返回键 → 继续游戏，跟菜单里的"继续游戏"等价。
        if (input.pause.clicked) {
            game.setMenu(null, -1);
            return;
        }

        list.tick(input);

        if (input.attack.clicked || input.menu.clicked) {
            switch (list.selectedIndex()) {
                case 0:  // 继续游戏
                    game.setMenu(null, -1);
                    break;
                case 1:  // 保存
                    if (SaveManager.save(game)) {
                        savedMessageTicks = SAVED_MESSAGE_DURATION;
                        Sound.test.play();
                        Gdx.app.log("PauseMenu", "saved");
                    } else {
                        Gdx.app.error("PauseMenu", "save failed");
                    }
                    break;
                case 2:  // 保存并返回标题
                    SaveManager.save(game);
                    game.setMenu(new TitleMenu(), -1);
                    break;
                case 3:  // 返回标题（不保存）
                    game.setMenu(new TitleMenu(), -1);
                    break;
            }
        }
    }

    public void render(Screen screen) {
        // 不铺背景 —— 游戏画面在底下，由 getScreenDarken 压暗。

        int cols = screen.w / 8;
        int rows = screen.h / 8;

        int boxW = 17;   // 加宽了，容纳最长选项 "Save & Quit"
        int boxH = 8;
        int xo = (cols - boxW) / 2;
        int yo = (rows - boxH) / 2;
        int x1 = xo + boxW;
        int y1 = yo + boxH;

        Font.renderFrame(
                screen,
                Messages.get(this, "title"),
                xo,
                yo,
                x1,
                y1
        );

        int optStartY = ((yo + y1) / 2 + 1) * 8;

        int highlightIdx = list.highlightIndex();

        int boxInnerW = 0;

        for (String k : OPTION_KEYS) {
            String msg = Messages.get(this, k);
            boxInnerW = Math.max(boxInnerW, Font.measure(msg));
        }

        int boxInnerX = (screen.w - boxInnerW) / 2;

        for (int i = 0; i < OPTION_KEYS.length; i++) {
            String msg = Messages.get(this, OPTION_KEYS[i]);

            int y = optStartY + i * 8;

            int col = (i == highlightIdx)
                    ? Color.get(0, 555, 555, 555)
                    : Color.get(0, 333, 333, 333);

            int textW = Font.measure(msg);
            int x = boxInnerX + (boxInnerW - textW) / 2;

            Font.draw(msg, screen, x, y, col);
        }

        int arrowY = optStartY + Math.round(list.arrowY());
        int arrowGap = 4;
        int arrowWidth = Font.measure(">");

        int leftArrowX = boxInnerX - arrowGap - arrowWidth;
        int rightArrowX = boxInnerX + boxInnerW + arrowGap;

        int arrowColor = Color.get(0, 555, 555, 555);

        Font.draw(">", screen, leftArrowX, arrowY, arrowColor);
        Font.draw("<", screen, rightArrowX, arrowY, arrowColor);

        // 保存成功提示
        if (savedMessageTicks > 0) {
            String msg = Messages.get(this, "saved");
            int mw = Font.measure(msg);
            int mx = (screen.w - mw) / 2;
            int my = screen.h - 16;

            // 闪烁效果：后半段交替亮/暗
            int col = (savedMessageTicks > SAVED_MESSAGE_DURATION / 2
                    || savedMessageTicks / 6 % 2 == 0)
                    ? Color.get(-1, 050, 550, 550)
                    : Color.get(-1, 000, 220, 220);

            Font.draw(msg, screen, mx, my, col);
        }
    }
}
