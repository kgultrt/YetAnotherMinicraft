package com.mojang.ld22.screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.mojang.ld22.crafting.Recipe;
import com.mojang.ld22.entity.Player;
import com.mojang.ld22.gfx.Color;
import com.mojang.ld22.gfx.Font;
import com.mojang.ld22.gfx.Screen;
import com.mojang.ld22.i18n.Messages;
import com.mojang.ld22.item.Item;
import com.mojang.ld22.item.ResourceItem;
import com.mojang.ld22.sound.Sound;
import com.mojang.ld22.ui.ItemList;

public class CraftingMenu extends Menu {
	private final Player player;
	private final List<Recipe> recipes;

	private final ItemList recipeList;

	public CraftingMenu(List<Recipe> recipes, Player player) {
		this.recipes = new ArrayList<Recipe>(recipes);
		this.player = player;

		for (int i = 0; i < recipes.size(); i++) {
			this.recipes.get(i).checkCanCraft(player);
		}

		// 可合成的排前面。排序在构造时做一次，之后顺序不再变。
		Collections.sort(this.recipes, new Comparator<Recipe>() {
			public int compare(Recipe r1, Recipe r2) {
				if (r1.canCraft && !r2.canCraft) return -1;
				if (!r1.canCraft && r2.canCraft) return 1;
				return 0;
			}
		});

		this.recipeList = new ItemList(this.recipes, 0, 1, 11, 11);
	}

	public void tick() {
		if (input.menu.clicked) game.setMenu(null);

		recipeList.tick(input);

		if (input.attack.clicked && recipeList.size() > 0) {
			Recipe r = recipes.get(recipeList.selectedIndex());
			r.checkCanCraft(player);
			if (r.canCraft) {
				r.deductCost(player);
				r.craft(player);
				Sound.craft.play();
			}
			// 合成后玩家物品变了，所有配方的可用状态都要重算。
			// 注意：recipes 列表长度不变，只是 canCraft 标志变化，
			// 所以 ItemList 的光标索引依然有效，不用 setCount。
			for (int i = 0; i < recipes.size(); i++) {
				recipes.get(i).checkCanCraft(player);
			}
		}
	}

	public void render(Screen screen) {
		Font.renderFrame(screen, Messages.get(this, "have"), 12, 1, 19, 3);
		Font.renderFrame(screen, Messages.get(this, "cost"), 12, 4, 19, 11);
		Font.renderFrame(screen, Messages.get(this, "title"), 0, 1, 11, 11);
		recipeList.render(screen);

		if (recipeList.size() > 0) {
			Recipe recipe = recipes.get(recipeList.selectedIndex());
			int hasResultItems = player.inventory.count(recipe.resultTemplate);
			int xo = 13 * 8;
			screen.render(xo, 2 * 8, recipe.resultTemplate.getSprite(), recipe.resultTemplate.getColor(), 0);
			Font.draw("" + hasResultItems, screen, xo + 8, 2 * 8, Color.get(-1, 555, 555, 555));

			List<Item> costs = recipe.costs;
			for (int i = 0; i < costs.size(); i++) {
				Item item = costs.get(i);
				int yo = (5 + i) * 8;
				screen.render(xo, yo, item.getSprite(), item.getColor(), 0);
				int requiredAmt = 1;
				if (item instanceof ResourceItem) {
					requiredAmt = ((ResourceItem) item).count;
				}
				int has = player.inventory.count(item);
				int color = Color.get(-1, 555, 555, 555);
				if (has < requiredAmt) {
					color = Color.get(-1, 222, 222, 222);
				}
				if (has > 99) has = 99;
				Font.draw("" + requiredAmt + "/" + has, screen, xo + 8, yo, color);
			}
		}
	}
}
