package com.mojang.ld22.save;

import com.mojang.ld22.item.FurnitureItem;
import com.mojang.ld22.item.Item;
import com.mojang.ld22.item.PowerGloveItem;
import com.mojang.ld22.item.ResourceItem;
import com.mojang.ld22.item.ToolItem;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 物品类型 → 工厂 的注册表。
 *
 * <p>各子类的 write/read 见下列文件里的补丁：
 * <ul>
 *   <li>{@code ResourceItem} —— 存 resource.key + count</li>
 *   <li>{@code ToolItem} —— 存 type.name + level + dur</li>
 *   <li>{@code FurnitureItem} —— 存 furniture 的 FurnitureCodec id</li>
 *   <li>{@code PowerGloveItem} —— 无状态，不用 override</li>
 * </ul>
 *
 * <p>⚠️ 下面的 {@code new XxxItem(...)} 构造器参数请按你项目实际签名改。
 * 如果构造器不接受 null，把参数换成 {@code Resource.wood} 之类的真值。
 */
public final class ItemCodec {

    public interface Factory {
        Item create(DataInputStream in) throws IOException;
    }

    private static final Map<Class<?>, String> NAMES     = new HashMap<>();
    private static final Map<String, Factory>  FACTORIES = new HashMap<>();

    static {
        register("powerglove", PowerGloveItem.class, in -> {
            PowerGloveItem p = new PowerGloveItem();
            p.read(in);
            return p;
        });

        register("resource", ResourceItem.class, in -> {
            ResourceItem r = new ResourceItem(null);
            r.read(in);
            return r;
        });

        register("tool", ToolItem.class, in -> {
            ToolItem t = new ToolItem(null, 0);
            t.read(in);
            return t;
        });

        register("furniture", FurnitureItem.class, in -> {
            FurnitureItem f = new FurnitureItem(null);
            f.read(in);
            return f;
        });
    }

    public static void register(String id, Class<? extends Item> cls, Factory f) {
        NAMES.put(cls, id);
        FACTORIES.put(id, f);
    }

    public static String nameOf(Class<?> cls) {
        String id = NAMES.get(cls);
        if (id == null) {
            throw new IllegalStateException("未注册物品类型: " + cls.getName()
                    + "。请在 ItemCodec 的 static 块里 register。");
        }
        return id;
    }

    public static Item create(String id, DataInputStream in) throws IOException {
        Factory f = FACTORIES.get(id);
        if (f == null) throw new IOException("未知物品类型: " + id);
        return f.create(in);
    }
}
