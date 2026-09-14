package com.mojang.ld22.screen;

import java.util.Arrays;

import com.badlogic.gdx.Gdx;
import com.mojang.ld22.Game;
import com.mojang.ld22.gfx.Color;
import com.mojang.ld22.gfx.Font;
import com.mojang.ld22.gfx.Screen;
import com.mojang.ld22.i18n.Messages;
import com.mojang.ld22.save.SaveManager;
import com.mojang.ld22.sound.Sound;
import com.mojang.ld22.ui.MenuList;
import com.mojang.ld22.ui.MenuBackground;

public class TitleMenu extends Menu {
    /** 用于驱动标题浮动。 */
    private int animTick = 0;

    private final MenuBackground bg = MenuBackground.get();

    // === 新增 "option.continue" ===
    private static final String[] OPTION_KEYS = {
            "option.start", "option.continue", "option.howto",
            "option.about", "option.language", "option.quit"
    };

    /** 选项列表。行高 8px。 */
    private final MenuList<String> list = new MenuList<>(Arrays.asList(OPTION_KEYS), 8);

    public TitleMenu() {}

    public void tick() {
        animTick++;
        bg.update(1f / 60f);

        list.tick(input);

        if (input.attack.clicked || input.menu.clicked) {
            switch (list.selectedIndex()) {
                case 0:
                    Sound.test.play();
                    game.startGame();
                    break;
                case 1:
                    Sound.test.play();
                    if (!game.loadGame()) {
                        // 没存档，退化成新游戏
                        game.startGame();
                    }
                    break;
                case 2:
                    game.setMenu(new InstructionsMenu(this), +1);
                    break;
                case 3:
                    game.setMenu(new AboutMenu(this), +1);
                    break;
                case 4:
                    game.setMenu(new LanguageMenu(this), +1);
                    break;
                case 5:
                    Gdx.app.exit();
                    break;
            }
        }
    }

    public void render(Screen screen) {
        bg.render(screen);

        int titleBob = titleBob();

        int h = 2;
        int w = 13;
        int titleColor = Color.get(-1, 010, 131, 551);
        int xo = (screen.w - w * 8) / 2;
        int yo = 24 + titleBob;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                screen.render(
                        xo + x * 8,
                        yo + y * 8,
                        x + (y + 6) * 32,
                        titleColor,
                        0
                );
            }
        }

        int boxW = 0;

        for (int i = 0; i < list.size(); i++) {
            String msg = Messages.get(this, list.get(i));
            boxW = Math.max(boxW, Font.measure(msg));
        }

        int boxX = (screen.w - boxW) / 2;

        int highlightIdx = list.highlightIndex();
        boolean saveExists = SaveManager.exists();

        for (int i = 0; i < list.size(); i++) {
            String msg = Messages.get(this, list.get(i));

            int y = (8 + i) * 8;

            int col = (i == highlightIdx)
                    ? Color.get(0, 555, 555, 555)
                    : Color.get(0, 222, 222, 222);

            // "继续游戏" 无存档时显示为灰色
            if (i == 1 && !saveExists) {
                col = Color.get(0, 111, 111, 111);
            }

            int textW = Font.measure(msg);
            int x = boxX + (boxW - textW) / 2;

            Font.draw(msg, screen, x, y, col);
        }

        {
            int arrowY = 8 * 8 + Math.round(list.arrowY());
            int col = Color.get(0, 555, 555, 555);
            int arrowGap = 4;

            int leftArrowX = boxX - arrowGap - Font.measure(">");
            int rightArrowX = boxX + boxW + arrowGap;

            Font.draw(">", screen, leftArrowX, arrowY, col);
            Font.draw("<", screen, rightArrowX, arrowY, col);
        }

        String hint = Messages.get(this, "hint");
        Font.draw(
                hint,
                screen,
                0,
                screen.h - 8,
                Color.get(0, 111, 111, 111)
        );

        String version = Messages.get("app.version", Game.version);
        Font.draw(
                version,
                screen,
                4,
                4,
                Color.get(0, 111, 111, 111)
        );
    }

    private int titleBob() {
        int period = 120;
        int half = period / 2;
        int phase = animTick % period;
        float t = (phase < half)
                ? phase / (float) half
                : (period - phase) / (float) half;
        float eased = (t < 0.5f)
                ? 2f * t * t
                : 1f - 2f * (1f - t) * (1f - t);
        return Math.round((eased * 2f - 1f) * 1f);
    }
}
