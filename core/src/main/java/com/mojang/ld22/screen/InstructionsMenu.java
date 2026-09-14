package com.mojang.ld22.screen;

import com.mojang.ld22.gfx.Color;
import com.mojang.ld22.gfx.Font;
import com.mojang.ld22.gfx.Screen;
import com.mojang.ld22.i18n.Messages;
import com.mojang.ld22.ui.MenuBackground;

public class InstructionsMenu extends Menu {
	private final Menu parent;
	private final MenuBackground bg = MenuBackground.get();

	public InstructionsMenu(Menu parent) {
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

		Font.draw(Messages.get(this, "title"), screen, 4 * 8 + 4, 1 * 8, Color.get(0, 555, 555, 555));

		for (int i = 0; i < 12; i++) {
			String key = "line" + (i + 1);
			Font.draw(Messages.get(this, key), screen, 0 * 8 + 4, (3 + i) * 8, Color.get(0, 333, 333, 333));
		}
	}
}