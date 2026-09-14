package com.mojang.ld22.item.resource;

import com.mojang.ld22.entity.Player;
import com.mojang.ld22.level.Level;
import com.mojang.ld22.level.tile.Tile;

public class PlantableResource extends Resource {
	private Tile targetTile;
	private Tile[] sourceTiles;

	public PlantableResource(String key, String name, int sprite, int color, Tile targetTile, Tile... sourceTiles) {
		super(key, name, sprite, color);
		this.targetTile = targetTile;
		this.sourceTiles = sourceTiles;
	}

	@Override
	public boolean interactOn(Tile tile, Level level, int xt, int yt, Player player, int attackDir) {
		for (Tile source : sourceTiles) {
			if (tile == source) {
				level.setTile(xt, yt, targetTile, 0);
				return true;
			}
		}
		return false;
	}
}