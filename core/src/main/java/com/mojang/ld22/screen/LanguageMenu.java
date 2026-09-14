package com.mojang.ld22.screen;

import java.util.Arrays;

import com.mojang.ld22.gfx.Color;
import com.mojang.ld22.gfx.Font;
import com.mojang.ld22.gfx.Screen;
import com.mojang.ld22.i18n.Languages;
import com.mojang.ld22.i18n.Messages;
import com.mojang.ld22.ui.MenuList;
import com.mojang.ld22.ui.Text;
import com.mojang.ld22.ui.MenuBackground;

public class LanguageMenu extends Menu {
	private final Menu parent;
	private final MenuBackground bg = MenuBackground.get();

	private final Languages[] langs = Languages.values();

	/** 列表区起始 y。 */
	private static final int LIST_Y = 104;

	/** 每行高度。 */
	private static final int ROW_H = 16;

	/** 列表可见行数。5 行给描述区让出空间。 */
	private static final int VISIBLE_ROWS = 5;

	/** 描述区起始 y。列表 5 行到 184，描述区从 192 开始。 */
	private static final int DESC_Y = 192;

	/** 描述区最多行数。3 行，加上 y=192 起，最底到 y=216。 */
	private static final int DESC_MAX_LINES = 3;

	/** 左右边距。 */
	private static final int MARGIN_X = 16;

	private final MenuList<Languages> list =
			new MenuList<>(Arrays.asList(langs), ROW_H, VISIBLE_ROWS);

	public LanguageMenu(Menu parent) {
		this.parent = parent;
		for (int i = 0; i < langs.length; i++) {
			if (langs[i] == Messages.lang()) {
				list.select(i);
				break;
			}
		}
	}

	public void tick() {
		bg.update(1f / 60f);

		list.tick(input);

		if (input.attack.clicked) {
			Messages.setup(list.selected());
			game.setMenu(parent, -1);
			return;
		}

		if (input.menu.clicked) {
			game.setMenu(parent, -1);
		}
	}

	public void render(Screen screen) {
		bg.render(screen);

		String title = Messages.get(this, "title");
		Text.centered(screen, title, 16, Color.get(0, 555, 555, 555));

		Languages cur = Messages.lang();

		String curLabel = Messages.get(this, "current", cur.label());
		Text.centered(screen, curLabel, 44, Color.get(0, 444, 444, 444));

		String curStatus = Messages.get(this, "status." + cur.status().name().toLowerCase());
		Text.centered(screen, curStatus, 56, statusColor(cur.status()));

		String heading = Messages.get(this, "available");
		Font.draw(heading, screen, MARGIN_X, 84, Color.get(0, 333, 333, 333));

		int leftX = MARGIN_X;
		int rightPad = MARGIN_X;

		int highlightIdx = list.highlightIndex();
		int io = list.firstVisible();
		int rows = (list.visibleRows() > 0) ? list.visibleRows() : list.size();

		for (int i = 0; i < rows && (i + io) < list.size(); i++) {
			int itemIdx = i + io;
			Languages l = list.get(itemIdx);
			int y = LIST_Y + i * ROW_H;
			boolean isSelected = (itemIdx == highlightIdx);
			boolean isCurrent = (l == cur);

			int nameCol;
			if (isSelected) {
				nameCol = Color.get(0, 555, 555, 555);
			} else if (isCurrent) {
				nameCol = Color.get(0, 550, 550, 550);
			} else {
				nameCol = Color.get(0, 222, 222, 222);
			}
			Font.draw(l.label(), screen, leftX, y, nameCol);

			String stText = Messages.get(this, "status." + l.status().name().toLowerCase());
			int stCol = isSelected ? nameCol : statusColor(l.status());
			int stX = screen.w - rightPad - stText.length() * 8;
			Font.draw(stText, screen, stX, y, stCol);

			if (isCurrent) {
				String mark = Messages.get(this, "currentmark");
				int markX = stX - mark.length() * 8 - 8;
				int markCol = isSelected ? nameCol : Color.get(0, 333, 333, 333);
				Font.draw(mark, screen, markX, y, markCol);
			}
		}

		// 箭头，跟随 list 的滑动位置。
		{
			int arrowY = LIST_Y + Math.round(list.arrowYOnScreen());
			Font.draw(">", screen, leftX - 12, arrowY, Color.get(0, 555, 555, 555));
		}

		// 描述区：显示当前高亮项对应的状态说明，自动换行。
		if (list.size() > 0) {
			Languages sel = list.get(list.highlightIndex());
			String descKey = "describe." + sel.status().name().toLowerCase();
			String desc = Messages.get(this, descKey);

			Text.wrapped(screen, desc, MARGIN_X, DESC_Y,
					screen.w - MARGIN_X * 2,
					Color.get(0, 444, 444, 444),
					DESC_MAX_LINES);
		}

		String hint = Messages.get(this, "hint");
		int hintY = screen.h - 16;
		Text.centered(screen, hint, hintY, Color.get(0, 111, 111, 111));
	}

	private int statusColor(Languages.Status s) {
		switch (s) {
			case OFFICIAL:   return Color.get(0, 550, 550, 550);
			case COMPLETE:   return Color.get(0, 555, 555, 555);
			case UNREVIEWED: return Color.get(0, 444, 444, 444);
			case UNFINISHED: return Color.get(0, 333, 333, 333);
		}
		return Color.get(0, 222, 222, 222);
	}
}
