package com.mojang.ld22.save;

import com.mojang.ld22.Game;
import com.mojang.ld22.entity.AirWizard;
import com.mojang.ld22.entity.Entity;
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
 * <p>粒子 / 投射物在 {@code Level.isPersistent()} 里已过滤掉，不需要在这里注册。
 */
public final class EntityCodec {

    public interface Factory {
        Entity create(DataInputStream in, Level level, Game game) throws IOException;
    }

    private static final Map<Class<?>, String> NAMES     = new HashMap<>();
    private static final Map<String, Factory>  FACTORIES = new HashMap<>();

    static {
        // === 有状态、需要持久化的实体 ===

        register("zombie", Zombie.class, (in, lv, g) -> {
            Zombie z = new Zombie(1);   // lvl 参数会被 read 覆盖
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

        // ItemEntity 的静态工厂见 ItemEntity.java 里的 readFromSave
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

    public static String nameOf(Class<?> cls) {
        String id = NAMES.get(cls);
        if (id == null) {
            throw new IllegalStateException("未注册实体类型: " + cls.getName()
                    + "。请在 EntityCodec 的 static 块里 register。");
        }
        return id;
    }

    public static Entity create(String id, DataInputStream in, Level lv, Game game) throws IOException {
        Factory f = FACTORIES.get(id);
        if (f == null) throw new IOException("未知实体类型: " + id);
        return f.create(in, lv, game);
    }
}
