package com.mojang.ld22.screen;

import java.util.Arrays;

import com.badlogic.gdx.Gdx;
import com.mojang.ld22.Game;
import com.mojang.ld22.gfx.Color;
import com.mojang.ld22.gfx.Font;
import com.mojang.ld22.gfx.Screen;
import com.mojang.ld22.i18n.Messages;
import com.mojang.ld22.sound.Sound;
import com.mojang.ld22.ui.MenuList;
import com.mojang.ld22.ui.MenuBackground;

public class TitleMenu extends Menu {
	/** 用于驱动标题浮动。 */
	private int animTick = 0;

	private final MenuBackground bg = MenuBackground.get();

	private static final String[] OPTION_KEYS = {
		"option.start", "option.howto", "option.about", "option.language", "option.quit"
	};

	/** 选项列表。行高 8px。 */
	private final MenuList<String> list = new MenuList<>(Arrays.asList(OPTION_KEYS), 8);

	public TitleMenu() {
	}

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
					game.setMenu(new InstructionsMenu(this), +1);
					break;
				case 2:
					game.setMenu(new AboutMenu(this), +1);
					break;
				case 3:
					game.setMenu(new LanguageMenu(this), +1);
					break;
				case 4:
					Gdx.app.exit();
					break;
			}
		}
	}

	public void render(Screen screen) {
		bg.render(screen);

		// 主标题上下浮动 1px，周期 120 tick（2 秒），缓入缓出。
		int titleBob = titleBob();

		int h = 2;
		int w = 13;
		int titleColor = Color.get(-1, 010, 131, 551);
		int xo = (screen.w - w * 8) / 2;
		int yo = 24 + titleBob;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				screen.render(xo + x * 8, yo + y * 8, x + (y + 6) * 32, titleColor, 0);
			}
		}

		// 选项框宽度取最长选项，所有文本在框内居中。
		// 这样箭头水平位置固定，不随选项文字长度变。
		int maxLen = 0;
		for (int i = 0; i < list.size(); i++) {
			int len = Messages.get(this, list.get(i)).length();
			if (len > maxLen) maxLen = len;
		}
		int boxW = maxLen * 8;
		int boxX = (screen.w - boxW) / 2;

		// 高亮跟随箭头当前位置（动画中会连续变化）。
		int highlightIdx = list.highlightIndex();

		for (int i = 0; i < list.size(); i++) {
			String msg = Messages.get(this, list.get(i));
			int y = (8 + i) * 8;
			int col = (i == highlightIdx)
					? Color.get(0, 555, 555, 555)
					: Color.get(0, 222, 222, 222);
			int x = boxX + (boxW - msg.length() * 8) / 2;
			Font.draw(msg, screen, x, y, col);
		}

		// 箭头。垂直位置跟随 list.arrowY() 缓动，水平位置固定。
		{
			int arrowY = 8 * 8 + Math.round(list.arrowY());
			int col = Color.get(0, 555, 555, 555);
			Font.draw(">", screen, boxX - 2 * 8, arrowY, col);
			Font.draw("<", screen, boxX + boxW + 8, arrowY, col);
		}

		String hint = Messages.get(this, "hint");
		Font.draw(hint, screen, 0, screen.h - 8, Color.get(0, 111, 111, 111));

		String version = Messages.get("app.version", Game.version);
		Font.draw(version, screen, 4, 4, Color.get(0, 111, 111, 111));
	}

	/**
	 * 主标题浮动。周期 120 tick，幅度 ±1。
	 *
	 * <p>用 easeInOutQuad 而非 sin —— sin 过零点附近变化快，1px 幅度下
	 * 看起来像"跳"；easeInOutQuad 两端慢中间快，是"呼吸"的感觉。
	 */
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