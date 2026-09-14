package com.mojang.ld22.item.resource;

import com.mojang.ld22.entity.Player;

public class FoodResource extends Resource {
	private int heal;
	private int staminaCost;

	public FoodResource(String key, String name, int sprite, int color, int heal, int staminaCost) {
		super(key, name, sprite, color);
		this.heal = heal;
		this.staminaCost = staminaCost;
	}

	@Override
	public boolean interactOn(com.mojang.ld22.level.tile.Tile tile, com.mojang.ld22.level.Level level, int xt, int yt, Player player, int attackDir) {
		if (player.health < player.maxHealth && player.payStamina(staminaCost)) {
			player.heal(heal);
			return true;
		}
		return false;
	}
}