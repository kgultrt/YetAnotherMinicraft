package com.mojang.ld22.save;

import com.mojang.ld22.Game;
import com.mojang.ld22.entity.AirWizard;
import com.mojang.ld22.entity.Entity;
import com.mojang.ld22.entity.Furniture;
import com.mojang.ld22.entity.ItemEntity;
import com.mojang.ld22.entity.Slime;
import com.mojang.ld22.entity.Zombie;
import com.mojang.ld22.level.Level;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 实体类型 → 工厂 的注册表。
 *
 * <p>Furniture 的子类不在这里显式注册——由 {@link FurnitureCodec} 统一管理，
 * 本类通过 {@link #nameOf} / {@link #create} 里的 fallback 委托过去。
 */
public final class EntityCodec {

    public interface Factory {
        Entity create(DataInputStream in, Level level, Game game) throws IOException;
    }

    private static final Map<Class<?>, String> NAMES     = new HashMap<>();
    private static final Map<String, Factory>  FACTORIES = new HashMap<>();

    static {
        register("zombie", Zombie.class, (in, lv, g) -> {
            Zombie z = new Zombie(1);
            z.read(in);
            return z;
        });

        register("slime", Slime.class, (in, lv, g) -> {
            Slime s = new Slime(1);
            s.read(in);
            return s;
        });

        register("airwizard", AirWizard.class, (in, lv, g) -> {
            AirWizard a = new AirWizard();
            a.read(in);
            return a;
        });

        register("item_entity", ItemEntity.class, (in, lv, g) -> {
            ItemEntity ie = ItemEntity.readFromSave(in);
            ie.init(lv);
            return ie;
        });
    }

    public static void register(String id, Class<? extends Entity> cls, Factory f) {
        NAMES.put(cls, id);
        FACTORIES.put(id, f);
    }

    /**
     * 查类名 → id。查不到就委托给 {@link FurnitureCodec}。
     */
    public static String nameOf(Class<?> cls) {
        String id = NAMES.get(cls);
        if (id != null) return id;

        // 试试是不是 Furniture 子类
        if (Furniture.class.isAssignableFrom(cls)) {
            return FurnitureCodec.nameOf(cls);
        }

        throw new IllegalStateException("未注册实体类型: " + cls.getName()
                + "。请在 EntityCodec 或 FurnitureCodec 里注册。");
    }

    /**
     * id → 实体。先查自己的表，查不到就委托给 {@link FurnitureCodec}。
     */
    public static Entity create(String id, DataInputStream in, Level lv, Game game) throws IOException {
        Factory f = FACTORIES.get(id);
        if (f != null) return f.create(in, lv, game);

        // 试试是不是 Furniture id
        if (isFurnitureId(id)) {
            Furniture fur = FurnitureCodec.create(id, in);
            fur.init(lv);
            return fur;
        }

        throw new IOException("未知实体类型: " + id);
    }

    private static boolean isFurnitureId(String id) {
        // 不抛异常的话，用一个只查表的辅助方法。FurnitureCodec 需要暴露 has()
        return FurnitureCodec.has(id);
    }
}
