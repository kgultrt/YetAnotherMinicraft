package com.mojang.ld22.screen;

import com.mojang.ld22.entity.Player;
import com.mojang.ld22.gfx.Font;
import com.mojang.ld22.gfx.Screen;
import com.mojang.ld22.i18n.Messages;
import com.mojang.ld22.item.Item;
import com.mojang.ld22.ui.ItemList;

public class InventoryMenu extends Menu {
	private final Player player;
	private final ItemList itemList;

	public InventoryMenu(Player player) {
		this.player = player;

		if (player.activeItem != null) {
			player.inventory.items.add(0, player.activeItem);
			player.activeItem = null;
		}

		this.itemList = new ItemList(player.inventory.items, 1, 1, 12, 11);
	}

	public void tick() {
		if (input.menu.clicked) game.setMenu(null);

		itemList.tick(input);

		if (input.attack.clicked && itemList.size() > 0) {
			Item item = player.inventory.items.remove(itemList.selectedIndex());
			player.activeItem = item;
			game.setMenu(null);
		}
	}

	public void render(Screen screen) {
		Font.renderFrame(screen, Messages.get(this, "title"), 1, 1, 12, 11);
		itemList.render(screen);
	}
}