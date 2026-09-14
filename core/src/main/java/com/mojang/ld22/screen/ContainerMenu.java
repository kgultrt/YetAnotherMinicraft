package com.mojang.ld22.screen;

import com.mojang.ld22.entity.Inventory;
import com.mojang.ld22.entity.Player;
import com.mojang.ld22.gfx.Font;
import com.mojang.ld22.gfx.Screen;
import com.mojang.ld22.i18n.Messages;
import com.mojang.ld22.item.Item;
import com.mojang.ld22.ui.ItemList;

public class ContainerMenu extends Menu {
	/** 活跃窗口（玩家物品栏）时的水平偏移。 */
	private static final int WINDOW_SHIFT_X = 6 * 8;

	/** 窗口切换的水平滑动时长（tick）。10 tick ≈ 0.17 秒。 */
	private static final int SHIFT_ANIM_TICKS = 10;

	private final Player player;
	private final Inventory container;
	private final String title;

	private final ItemList containerList;
	private final ItemList playerList;

	/** 当前活跃窗口：0 = container，1 = player inventory。 */
	private int window = 0;

	/** 水平偏移动画。currentShift() 按 shiftAnimTick 缓动计算。 */
	private float shiftFrom = 0f;
	private float shiftTo = 0f;
	private int shiftAnimTick = SHIFT_ANIM_TICKS;

	public ContainerMenu(Player player, String title, Inventory container) {
		this.player = player;
		this.title = title;
		this.container = container;

		this.containerList = new ItemList(container.items, 1, 1, 12, 11);
		this.playerList    = new ItemList(player.inventory.items, 13, 1, 13 + 11, 11);

		updateCursorVisibility();
	}

	private void updateCursorVisibility() {
		containerList.setCursorVisible(window == 0);
		playerList.setCursorVisible(window == 1);
	}

	/** 切换活跃窗口并触发水平滑动动画。重复调用同一 window 时无操作。 */
	private void setWindow(int w) {
		if (window == w) return;
		shiftFrom = currentShift();
		window = w;
		shiftTo = (w == 1) ? WINDOW_SHIFT_X : 0f;
		shiftAnimTick = 0;
		updateCursorVisibility();
	}

	public void tick() {
		if (input.menu.clicked) game.setMenu(null);

		if (input.left.clicked)  setWindow(0);
		if (input.right.clicked) setWindow(1);

		if (shiftAnimTick < SHIFT_ANIM_TICKS) shiftAnimTick++;

		ItemList active   = (window == 0) ? containerList : playerList;
		ItemList inactive = (window == 0) ? playerList    : containerList;

		active.tick(input);
		inactive.advance();

		// 移物：从活跃列表移到非活跃列表的选中位置
		if (input.attack.clicked && active.size() > 0) {
			Inventory from = (window == 0) ? container : player.inventory;
			Inventory to   = (window == 0) ? player.inventory : container;

			int targetIdx = inactive.selectedIndex();
			if (targetIdx < 0) targetIdx = 0;
			if (targetIdx > to.items.size()) targetIdx = to.items.size();

			Item item = from.items.remove(active.selectedIndex());
			to.items.add(targetIdx, item);
			// 下一次 tick 里 ItemList.tick 开头的 setCount 会自动修正索引。
		}
	}

	public void render(Screen screen) {
		// 相对偏移。绝不能覆盖外层（MenuManager）在滑动动画期间
		// 设置的偏移，否则退出容器菜单时画面会跳。
		int savedX = screen.xOffset;
		int savedY = screen.yOffset;
		screen.setOffset(savedX + Math.round(currentShift()), savedY);

		Font.renderFrame(screen, title, 1, 1, 12, 11);
		containerList.render(screen);

		Font.renderFrame(screen, Messages.get(this, "inventory"), 13, 1, 13 + 11, 11);
		playerList.render(screen);

		screen.setOffset(savedX, savedY);
	}

	/**
	 * 当前水平偏移。easeInOutQuad：两端慢、中间快，
	 * 视觉上像"焦点在两栏之间平移"，比 easeOut 更贴切。
	 */
	private float currentShift() {
		if (shiftAnimTick >= SHIFT_ANIM_TICKS) return shiftTo;
		float t = shiftAnimTick / (float) SHIFT_ANIM_TICKS;
		float eased = (t < 0.5f)
				? 2f * t * t
				: 1f - 2f * (1f - t) * (1f - t);
		return shiftFrom + (shiftTo - shiftFrom) * eased;
	}
}
