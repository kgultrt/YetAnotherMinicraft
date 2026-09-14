package com.mojang.ld22.screen;

import java.util.Arrays;

import com.mojang.ld22.gfx.Color;
import com.mojang.ld22.gfx.Font;
import com.mojang.ld22.gfx.Screen;
import com.mojang.ld22.i18n.Messages;
import com.mojang.ld22.ui.MenuList;

/**
 * 暂停菜单。盖在游戏画面上，不铺背景，靠 {@link #getScreenDarken()}
 * 让 Game 把游戏画面压暗。
 *
 * <p>菜单一被设置，{@code Game.tick()} 里 {@code menu != null} 分支会接管，
 * 世界逻辑（level.tick / 怪物 / 计时）全部冻结 —— 这就是暂停。
 */
public class PauseMenu extends Menu {

	/** 背景压暗档位。5/10 = 50%，游戏画面仍可辨认。 */
	private static final int DARKEN = 5;

	private static final String[] OPTION_KEYS = {
		"option.resume", "option.title"
	};

	private final MenuList<String> list = new MenuList<>(Arrays.asList(OPTION_KEYS), 8);

	@Override
	public int getScreenDarken() {
		return DARKEN;
	}

	public void tick() {
		// ESC / Android 返回键 → 继续游戏，跟菜单里的"继续游戏"等价。
		if (input.pause.clicked) {
			game.setMenu(null, -1);
			return;
		}

		list.tick(input);

		if (input.attack.clicked || input.menu.clicked) {
			switch (list.selectedIndex()) {
				case 0:
					game.setMenu(null, -1);
					break;
				case 1:
					game.setMenu(new TitleMenu(), -1);
					break;
			}
		}
	}

	public void render(Screen screen) {
		// 不铺背景 —— 游戏画面在底下，由 getScreenDarken 压暗。

		int cols = screen.w / 8;
		int rows = screen.h / 8;

		int boxW = 15;
		int boxH = 8;
		int xo = (cols - boxW) / 2;
		int yo = (rows - boxH) / 2;
		int x1 = xo + boxW;
		int y1 = yo + boxH;

		Font.renderFrame(screen, Messages.get(this, "title"), xo, yo, x1, y1);

		int optStartY = ((yo + y1) / 2 + 1) * 8;

		int highlightIdx = list.highlightIndex();
		int maxLen = 0;
		for (String k : OPTION_KEYS) {
			int len = Messages.get(this, k).length();
			if (len > maxLen) maxLen = len;
		}
		int boxInnerW = maxLen * 8;
		int boxInnerX = (screen.w - boxInnerW) / 2;

		for (int i = 0; i < OPTION_KEYS.length; i++) {
			String msg = Messages.get(this, OPTION_KEYS[i]);
			int y = optStartY + i * 8;
			int col = (i == highlightIdx)
					? Color.get(0, 555, 555, 555)
					: Color.get(0, 333, 333, 333);
			int x = boxInnerX + (boxInnerW - msg.length() * 8) / 2;
			Font.draw(msg, screen, x, y, col);
		}

		int arrowY = optStartY + Math.round(list.arrowY());
		Font.draw(">", screen, boxInnerX - 2 * 8, arrowY, Color.get(0, 555, 555, 555));
		Font.draw("<", screen, boxInnerX + boxInnerW + 8, arrowY, Color.get(0, 555, 555, 555));
	}
}
