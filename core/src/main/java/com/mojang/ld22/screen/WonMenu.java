package com.mojang.ld22.screen;

import com.mojang.ld22.gfx.Color;
import com.mojang.ld22.gfx.Font;
import com.mojang.ld22.gfx.Screen;
import com.mojang.ld22.i18n.Messages;

public class WonMenu extends Menu {
	private int inputDelay = 60;

	public WonMenu() {
	}

	public void tick() {
		if (inputDelay > 0)
			inputDelay--;
		else if (input.attack.clicked || input.menu.clicked) {
			game.setMenu(new TitleMenu());
		}
	}

	public void render(Screen screen) {
		Font.renderFrame(screen, "", 1, 3, 18, 9);
		Font.draw(Messages.get(this, "title"), screen, 2 * 8, 4 * 8, Color.get(-1, 555, 555, 555));

		int seconds = game.gameTime / 60;
		int minutes = seconds / 60;
		int hours = minutes / 60;
		minutes %= 60;
		seconds %= 60;

		String timeString;
		if (hours > 0) {
			timeString = Messages.get(this, "time.hours", hours, pad2(minutes));
		} else {
			timeString = Messages.get(this, "time.minutes", minutes, pad2(seconds));
		}
		Font.draw(Messages.get(this, "time"), screen, 2 * 8, 5 * 8, Color.get(-1, 555, 555, 555));
		Font.draw(timeString, screen, (2 + 5) * 8, 5 * 8, Color.get(-1, 550, 550, 550));
		Font.draw(Messages.get(this, "score"), screen, 2 * 8, 6 * 8, Color.get(-1, 555, 555, 555));
		Font.draw("" + game.player.score, screen, (2 + 6) * 8, 6 * 8, Color.get(-1, 550, 550, 550));
		Font.draw(Messages.get(this, "press"), screen, 2 * 8, 8 * 8, Color.get(-1, 333, 333, 333));
	}

	private static String pad2(int n) {
		return n < 10 ? "0" + n : "" + n;
	}
}