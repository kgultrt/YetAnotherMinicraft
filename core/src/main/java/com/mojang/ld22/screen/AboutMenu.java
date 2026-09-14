package com.mojang.ld22.screen;

import com.mojang.ld22.gfx.Color;
import com.mojang.ld22.gfx.Font;
import com.mojang.ld22.gfx.Screen;
import com.mojang.ld22.i18n.Messages;
import com.mojang.ld22.ui.MenuBackground;

public class AboutMenu extends Menu {
	private final Menu parent;
	private final MenuBackground bg = MenuBackground.get();

	public AboutMenu(Menu parent) {
		this.parent = parent;
	}

	public void tick() {
		bg.update(1f / 60f);

		if (input.attack.clicked || input.menu.clicked) {
			game.setMenu(parent, -1);
		}
	}

	public void render(Screen screen) {
		bg.render(screen);

		Font.draw(Messages.get(this, "title"), screen, 2 * 8 + 4, 1 * 8, Color.get(0, 555, 555, 555));

		String[] lines = {
			"line1", "line2", "line3", "line4", "line5"
		};
		int y = 3;
		for (String key : lines) {
			Font.draw(Messages.get(this, key), screen, 0 * 8 + 4, y * 8, Color.get(0, 333, 333, 333));
			y++;
		}
		y++;

		String[] lines2 = { "line6", "line7" };
		for (String key : lines2) {
			Font.draw(Messages.get(this, key), screen, 0 * 8 + 4, y * 8, Color.get(0, 333, 333, 333));
			y++;
		}
	}
}