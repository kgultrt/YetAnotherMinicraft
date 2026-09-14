package com.mojang.ld22.entity;

import java.util.Locale;

import com.mojang.ld22.gfx.Screen;
import com.mojang.ld22.i18n.Messages;
import com.mojang.ld22.item.FurnitureItem;
import com.mojang.ld22.item.PowerGloveItem;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Furniture extends Entity {
    private int pushTime = 0;
    private int pushDir = -1;
    public int col, sprite;

    /** i18n key，从类名自动推导（Workbench → workbench）。 */
    public String key;

    /** 英文 fallback，properties 找不到时使用。 */
    public String name;

    private Player shouldTake;

    public Furniture(String name) {
        String simple = getClass().getSimpleName();
        if (simple.isEmpty()) {
            this.key = "furniture";
        } else {
            this.key = simple.toLowerCase(Locale.ENGLISH);
        }
        this.name = name;
        xr = 3;
        yr = 3;
    }

    public String getName() {
        String translated = Messages.get("furniture." + key + ".name");
        if (Messages.NO_TEXT_FOUND.equals(translated)) {
            return name;
        }
        return translated;
    }

    public void tick() {
        if (shouldTake != null) {
            if (shouldTake.activeItem instanceof PowerGloveItem) {
                remove();
                shouldTake.inventory.add(0, shouldTake.activeItem);
                shouldTake.activeItem = new FurnitureItem(this);
            }
            shouldTake = null;
        }
        if (pushDir == 0) move(0, +1);
        if (pushDir == 1) move(0, -1);
        if (pushDir == 2) move(-1, 0);
        if (pushDir == 3) move(+1, 0);
        pushDir = -1;
        if (pushTime > 0) pushTime--;
    }

    public void render(Screen screen) {
        screen.render(x - 8, y - 8 - 4, sprite * 2 + 8 * 32, col, 0);
        screen.render(x - 0, y - 8 - 4, sprite * 2 + 8 * 32 + 1, col, 0);
        screen.render(x - 8, y - 0 - 4, sprite * 2 + 8 * 32 + 32, col, 0);
        screen.render(x - 0, y - 0 - 4, sprite * 2 + 8 * 32 + 33, col, 0);
    }

    public boolean blocks(Entity e) {
        return true;
    }

    protected void touchedBy(Entity entity) {
        if (entity instanceof Player && pushTime == 0) {
            pushDir = ((Player) entity).dir;
            pushTime = 10;
        }
    }

    public void take(Player player) {
        shouldTake = player;
    }

    /** 写基类字段。子类 override 时先调 super.write(out)，再写自己的。 注意：name/key 从类名推导，不存。 */
    public void write(DataOutputStream out) throws IOException {
        super.write(out); // Entity
        out.writeInt(pushTime);
        out.writeInt(pushDir);
        out.writeInt(col);
        out.writeInt(sprite);
    }

    public void read(DataInputStream in) throws IOException {
        super.read(in);
        pushTime = in.readInt();
        pushDir = in.readInt();
        col = in.readInt();
        sprite = in.readInt();
        shouldTake = null;
    }
}