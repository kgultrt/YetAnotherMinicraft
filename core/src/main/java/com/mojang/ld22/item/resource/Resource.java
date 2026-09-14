package com.mojang.ld22.item.resource;

import com.mojang.ld22.entity.Player;
import com.mojang.ld22.gfx.Color;
import com.mojang.ld22.i18n.Messages;
import com.mojang.ld22.level.Level;
import com.mojang.ld22.level.tile.Tile;

public class Resource {
	public static Resource wood       = new Resource("wood",       "Wood",     1 + 4 * 32, Color.get(-1, 200, 531, 430));
	public static Resource stone      = new Resource("stone",      "Stone",    2 + 4 * 32, Color.get(-1, 111, 333, 555));
	public static Resource flower     = new PlantableResource("flower",  "Flower",   0 + 4 * 32, Color.get(-1, 10, 444, 330), Tile.flower, Tile.grass);
	public static Resource acorn      = new PlantableResource("acorn",   "Acorn",    3 + 4 * 32, Color.get(-1, 100, 531, 320), Tile.treeSapling, Tile.grass);
	public static Resource dirt       = new PlantableResource("dirt",    "Dirt",     2 + 4 * 32, Color.get(-1, 100, 322, 432), Tile.dirt, Tile.hole, Tile.water, Tile.lava);
	public static Resource sand       = new PlantableResource("sand",    "Sand",     2 + 4 * 32, Color.get(-1, 110, 440, 550), Tile.sand, Tile.grass, Tile.dirt);
	public static Resource cactusFlower = new PlantableResource("cactus","Cactus",   4 + 4 * 32, Color.get(-1, 10, 40, 50), Tile.cactusSapling, Tile.sand);
	public static Resource seeds      = new PlantableResource("seeds",   "Seeds",    5 + 4 * 32, Color.get(-1, 10, 40, 50), Tile.wheat, Tile.farmland);
	public static Resource wheat      = new Resource("wheat",      "Wheat",    6 + 4 * 32, Color.get(-1, 110, 330, 550));
	public static Resource bread      = new FoodResource("bread",  "Bread",    8 + 4 * 32, Color.get(-1, 110, 330, 550), 2, 5);
	public static Resource apple      = new FoodResource("apple",  "Apple",    9 + 4 * 32, Color.get(-1, 100, 300, 500), 1, 5);

	public static Resource coal       = new Resource("coal",       "COAL",     10 + 4 * 32, Color.get(-1, 000, 111, 111));
	public static Resource ironOre    = new Resource("iron_ore",   "I.ORE",    10 + 4 * 32, Color.get(-1, 100, 322, 544));
	public static Resource goldOre    = new Resource("gold_ore",   "G.ORE",    10 + 4 * 32, Color.get(-1, 110, 440, 553));
	public static Resource ironIngot  = new Resource("iron_ingot", "IRON",     11 + 4 * 32, Color.get(-1, 100, 322, 544));
	public static Resource goldIngot  = new Resource("gold_ingot", "GOLD",     11 + 4 * 32, Color.get(-1, 110, 330, 553));

	public static Resource slime      = new Resource("slime",      "SLIME",    10 + 4 * 32, Color.get(-1, 10, 30, 50));
	public static Resource glass      = new Resource("glass",      "glass",    12 + 4 * 32, Color.get(-1, 555, 555, 555));
	public static Resource cloth      = new Resource("cloth",      "cloth",    1 + 4 * 32, Color.get(-1, 25, 252, 141));
	public static Resource cloud      = new PlantableResource("cloud", "cloud", 2 + 4 * 32, Color.get(-1, 222, 555, 444), Tile.cloud, Tile.infiniteFall);
	public static Resource gem        = new Resource("gem",        "gem",      13 + 4 * 32, Color.get(-1, 101, 404, 545));

	/** i18n key，用于在 properties 里查找本地化名字。 */
	public final String key;
	/** 英文 fallback，如果 properties 里找不到对应项就用它。 */
	public final String name;
	public final int sprite;
	public final int color;

	public Resource(String key, String name, int sprite, int color) {
		this.key = key;
		this.name = name;
		this.sprite = sprite;
		this.color = color;
	}

	/** 本地化的显示名字。 */
	public String getDisplayName() {
		return Messages.get("item.resource." + key + ".name");
	}

	public boolean interactOn(Tile tile, Level level, int xt, int yt, Player player, int attackDir) {
		return false;
	}
}