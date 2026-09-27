package com.mojang.ld22.screen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mojang.ld22.gfx.Color;
import com.mojang.ld22.gfx.Font;
import com.mojang.ld22.gfx.Screen;
import com.mojang.ld22.i18n.Messages;
import com.mojang.ld22.ui.MenuBackground;
import com.mojang.ld22.ui.MenuList;
import com.mojang.ld22.ui.Text;

/**
 * 设置菜单。两栏布局，参考 Minecraft Java 版的设置界面。
 * 顶栏居中显示当前分类名，左侧是分类列表，右侧是当前分类下的设置项。
 *
 * <p>焦点在两栏之间用 left/right 切换，上下键在当前栏内移动，attack 激活，menu 返回。
 */
public class SettingsMenu extends Menu {

	private final Menu parent;
	private final MenuBackground bg = MenuBackground.get();

	// ---- 布局常量 ----
	private static final int MARGIN_X   = 8;
	private static final int LEFT_W     = 88;
	private static final int COL_GAP    = 12;   // 左右栏间距
	private static final int TOP        = 24;   // 顶栏下方起画位置
	private static final int BOTTOM_PAD = 16;
	private static final int ROW_H      = 16;   // 行高，滑条那一行要额外 8px

	/** 箭头追赶目标的插值系数。越大越干脆，越小越黏。 */
	private static final float ARROW_LERP = 0.35f;

	private static final String[] CATEGORY_KEYS = {
			"cat.accessibility",
			"cat.controls",
			"cat.audio",
			"cat.video",
			"cat.social",
	};

	private final MenuList<String> categories =
			new MenuList<>(Arrays.asList(CATEGORY_KEYS), ROW_H);

	private final List<Setting> current = new ArrayList<>();
	private int selectedSetting = 0;
	private int rightScroll = 0;

	/** 0 = 左栏（分类），1 = 右栏（设置项）。 */
	private int focus = 1;

	/** 右栏箭头当前 y 坐标（像素）。NaN = 尚未初始化，下一帧直接吸附到目标。 */
	private float rightArrowY = Float.NaN;

	/** 最近一次 render 时的屏幕高度。tick 里算可见行数要用。 */
	private int lastScreenH = 192;

	public SettingsMenu(Menu parent) {
		this.parent = parent;
		rebuildForCategory(0);
	}

	// ============================================================
	// 逻辑
	// ============================================================

	private void rebuildForCategory(int idx) {
		current.clear();
		switch (idx) {
			case 0: // 可访问性
				current.add(new Toggle("hideSubtitles", false));
				current.add(new Toggle("deviceTts", false));
				current.add(new Toggle("uiTts", false));
				current.add(new Toggle("chatTts", false));
				current.add(new Slider("ttsVolume", 100, 0, 100, 5));
				current.add(new Toggle("chatNarration", false));
				current.add(new Slider("chatBgOpacity", 60, 0, 100, 5));
				current.add(new Slider("chatOpacity", 70, 0, 100, 5));
				current.add(new Slider("actionBarOpacity", 60, 0, 100, 5));
				break;
			case 1: // 控制
				current.add(new Slider("mouseSensitivity", 50, 0, 100, 5));
				current.add(new Toggle("invertMouse", false));
				current.add(new Toggle("autoJump", true));
				break;
			case 2: // 音频
				current.add(new Slider("masterVolume", 80, 0, 100, 5));
				current.add(new Slider("musicVolume", 70, 0, 100, 5));
				current.add(new Slider("sfxVolume", 90, 0, 100, 5));
				break;
			case 3: // 视频
				current.add(new Toggle("fullscreen", false));
				current.add(new Toggle("vsync", true));
				current.add(new Slider("guiScale", 2, 1, 4, 1));
				break;
			case 4: // 社交
				current.add(new Toggle("chatVisibility", true));
				current.add(new Toggle("allowFriendRequests", true));
				break;
		}
		selectedSetting = 0;
		rightScroll = 0;
		// 换分类时让右栏箭头下一帧直接归位，别从上一分类的位置滑过来
		rightArrowY = Float.NaN;
	}

	@Override
	public void tick() {
		bg.update(1f / 60f);

		if (input.menu.clicked) {
			game.setMenu(parent, -1);
			return;
		}

		if (input.left.clicked && focus == 1) focus = 0;
		if (input.right.clicked && focus == 0) focus = 1;

		if (focus == 0) {
			int before = categories.selectedIndex();
			categories.tick(input);
			if (categories.selectedIndex() != before) {
				rebuildForCategory(categories.selectedIndex());
			}
			if (input.attack.clicked) focus = 1;
		} else {
			if (!current.isEmpty()) {
				if (input.up.clicked) {
					selectedSetting--;
					if (selectedSetting < 0) selectedSetting = current.size() - 1;
				}
				if (input.down.clicked) {
					selectedSetting++;
					if (selectedSetting >= current.size()) selectedSetting = 0;
				}
				if (input.attack.clicked) {
					current.get(selectedSetting).activate();
				}
			}
			clampScroll();
		}

		updateRightArrow();
	}

	/** 让右栏箭头平滑地追到当前选中行。 */
	private void updateRightArrow() {
		int targetRow = selectedSetting - rightScroll;
		float targetY = TOP + targetRow * ROW_H;

		if (Float.isNaN(rightArrowY)) {
			rightArrowY = targetY;
			return;
		}
		rightArrowY += (targetY - rightArrowY) * ARROW_LERP;
		if (Math.abs(targetY - rightArrowY) < 0.5f) rightArrowY = targetY;
	}

	private int visibleRightRows() {
		int avail = lastScreenH - TOP - BOTTOM_PAD;
		return Math.max(1, avail / ROW_H);
	}

	private void clampScroll() {
		int rows = visibleRightRows();
		if (selectedSetting < rightScroll) {
			rightScroll = selectedSetting;
		}
		if (selectedSetting >= rightScroll + rows) {
			rightScroll = selectedSetting - rows + 1;
		}
		int max = Math.max(0, current.size() - rows);
		if (rightScroll > max) rightScroll = max;
		if (rightScroll < 0) rightScroll = 0;
	}

	// ============================================================
	// 渲染
	// ============================================================

	@Override
	public void render(Screen screen) {
		lastScreenH = screen.h;
		bg.render(screen);

		int w = screen.w;
		int h = screen.h;

		// ---------- 顶栏：只留居中标题 ----------
		String catName = Messages.get(this, categories.get(categories.selectedIndex()));
		int catW = Font.measure(catName);
		Font.draw(catName, screen, (w - catW) / 2, 4, Color.get(0, 555, 555, 555));

		// ---------- 左栏：分类 ----------
		int catHl    = categories.highlightIndex();
		int catFirst = categories.firstVisible();
		int catRows  = categories.visibleRows() > 0
				? categories.visibleRows() : categories.size();

		for (int i = 0; i < catRows && i + catFirst < categories.size(); i++) {
			int idx = i + catFirst;
			int y = TOP + i * ROW_H;
			String label = Messages.get(this, categories.get(idx));

			boolean isSel = (idx == catHl);
			int col;
			if (isSel && focus == 0)      col = Color.get(0, 555, 555, 555);
			else if (isSel)               col = Color.get(0, 444, 444, 444);
			else                          col = Color.get(0, 333, 333, 333);

			Font.draw(label, screen, MARGIN_X + 4, y, col);
		}

		// 左栏箭头：跟随 MenuList 的平滑位置，不再逐行瞬移
		if (categories.size() > 0) {
			int arrowY = TOP + Math.round(categories.arrowYOnScreen());
			Font.draw(">", screen, MARGIN_X - 8, arrowY,
					focus == 0 ? Color.get(0, 555, 555, 555)
					           : Color.get(0, 333, 333, 333));
		}

		// ---------- 右栏：设置项 ----------
		int rightX = MARGIN_X + LEFT_W + COL_GAP;
		int rightW = w - rightX - MARGIN_X;

		int rows = visibleRightRows();
		clampScroll();

		for (int i = 0; i < rows; i++) {
			int idx = i + rightScroll;
			if (idx >= current.size()) break;
			Setting s = current.get(idx);
			int y = TOP + i * ROW_H;
			boolean isSel = (focus == 1 && idx == selectedSetting);
			renderSettingRow(screen, s, rightX, y, rightW, isSel);
		}

		// 右栏箭头：单独画，用插值后的 y
		if (focus == 1 && !current.isEmpty() && !Float.isNaN(rightArrowY)) {
			Font.draw(">", screen, rightX - 8, Math.round(rightArrowY),
					Color.get(0, 555, 555, 555));
		}

		// ---------- 滚动条（不画轨道，只画一个滑块） ----------
		if (current.size() > rows) {
			int barX = w - MARGIN_X - 4;
			int barTop = TOP;
			int barH = visibleRightRows() * ROW_H;
			if (barH > 0) {
				float ratio = (float) rows / current.size();
				int thumbH = Math.max(4, (int) (barH * ratio));
				int maxScroll = Math.max(1, current.size() - rows);
				int thumbY = barTop + (int) ((barH - thumbH)
						* ((float) rightScroll / maxScroll));
				for (int yy = 0; yy < thumbH; yy += 8) {
					Font.draw("|", screen, barX, thumbY + yy,
							Color.get(0, 444, 444, 444));
				}
			}
		}

		// ---------- 底部提示 ----------
		Text.centered(screen, Messages.get(this, "hint"),
				h - 12, Color.get(0, 111, 111, 111));
	}

	private void renderSettingRow(Screen screen, Setting s, int x, int y, int w, boolean sel) {
		int labelCol = sel ? Color.get(0, 555, 555, 555) : Color.get(0, 444, 444, 444);

		if (s instanceof Toggle) {
			Toggle t = (Toggle) s;
			String box = t.value ? "[x]" : "[ ]";
			Font.draw(box, screen, x, y, labelCol);
			Font.draw(Messages.get(this, s.labelKey()), screen, x + 32, y, labelCol);

		} else if (s instanceof Slider) {
			Slider sl = (Slider) s;
			String label = Messages.get(this, s.labelKey()) + ":" + sl.value;
			Font.draw(label, screen, x, y, labelCol);

			// 字符滑条：===|-----
			int trackCells = Math.max(6, (w - 8) / 8);
			float ratio = (sl.max == sl.min) ? 0f
					: (float) (sl.value - sl.min) / (sl.max - sl.min);
			int filled = Math.round((trackCells - 1) * ratio);

			StringBuilder sb = new StringBuilder(trackCells);
			for (int i = 0; i < trackCells; i++) {
				if (i == filled) sb.append('|');
				else if (i < filled) sb.append('=');
				else sb.append('-');
			}
			Font.draw(sb.toString(), screen, x, y + 8,
					sel ? Color.get(0, 555, 555, 555) : Color.get(0, 333, 333, 333));
		}
	}

	// ============================================================
	// 设置项数据结构
	// ============================================================

	private abstract static class Setting {
		private final String key;
		Setting(String key) { this.key = key; }
		String labelKey() { return key; }
		void activate() {}
	}

	private static class Toggle extends Setting {
		boolean value;
		Toggle(String key, boolean v) { super(key); value = v; }
		@Override void activate() { value = !value; }
	}

	private static class Slider extends Setting {
		int value, min, max, step;
		Slider(String key, int v, int mn, int mx, int st) {
			super(key);
			value = v; min = mn; max = mx; step = st;
		}
		@Override void activate() {
			value += step;
			if (value > max) value = min;
		}
	}
}