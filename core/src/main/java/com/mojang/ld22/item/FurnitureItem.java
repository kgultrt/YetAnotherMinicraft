package com.mojang.ld22.item;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.mojang.ld22.entity.Furniture;
import com.mojang.ld22.entity.ItemEntity;
import com.mojang.ld22.entity.Player;
import com.mojang.ld22.gfx.Color;
import com.mojang.ld22.gfx.Font;
import com.mojang.ld22.gfx.Screen;
import com.mojang.ld22.level.Level;
import com.mojang.ld22.level.tile.Tile;
import com.mojang.ld22.save.FurnitureCodec;

public class FurnitureItem extends Item {
	public Furniture furniture;
	public boolean placed = false;

	public FurnitureItem(Furniture furniture) {
		this.furniture = furniture;
	}

	public int getColor() {
		return furniture.col;
	}

	public int getSprite() {
		return furniture.sprite + 10 * 32;
	}

	public void renderIcon(Screen screen, int x, int y) {
		screen.render(x, y, getSprite(), getColor(), 0);
	}

	public void renderInventory(Screen screen, int x, int y) {
		screen.render(x, y, getSprite(), getColor(), 0);
		Font.draw(furniture.getName(), screen, x + 8, y, Color.get(-1, 555, 555, 555));
	}

	public void onTake(ItemEntity itemEntity) {
	}

	public boolean canAttack() {
		return false;
	}

	public boolean interactOn(Tile tile, Level level, int xt, int yt, Player player, int attackDir) {
		if (tile.mayPass(level, xt, yt, furniture)) {
			furniture.x = xt * 16 + 8;
			furniture.y = yt * 16 + 8;
			level.add(furniture);
			placed = true;
			return true;
		}
		return false;
	}

	public boolean isDepleted() {
		return placed;
	}

	public String getName() {
		return furniture.getName();
	}

	// ============================================================ 存档

	@Override
	public void write(DataOutputStream out) throws IOException {
		super.write(out);

		// furniture 理论上不会为 null，但加个 flag 更稳（防止存档损坏导致 NPE）
		if (furniture != null) {
			out.writeBoolean(true);
			out.writeUTF(FurnitureCodec.nameOf(furniture.getClass()));
			furniture.write(out);
		} else {
			out.writeBoolean(false);
		}

		out.writeBoolean(placed);
	}

	@Override
	public void read(DataInputStream in) throws IOException {
		super.read(in);

		if (in.readBoolean()) {
			String id = in.readUTF();
			furniture = FurnitureCodec.create(id, in);
		} else {
			furniture = null;
		}

		placed = in.readBoolean();
	}
}
