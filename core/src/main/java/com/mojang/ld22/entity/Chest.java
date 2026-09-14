package com.mojang.ld22.entity;

import com.mojang.ld22.gfx.Color;
import com.mojang.ld22.screen.ContainerMenu;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Chest extends Furniture {
    public Inventory inventory = new Inventory();

    public Chest() {
        super("Chest");
        col = Color.get(-1, 110, 331, 552);
        sprite = 1;
    }

    public boolean use(Player player, int attackDir) {
        player.game.setMenu(new ContainerMenu(player, getName(), inventory));
        return true;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        super.write(out);
        inventory.write(out); // 递归
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        super.read(in);
        inventory.read(in);
    }
}