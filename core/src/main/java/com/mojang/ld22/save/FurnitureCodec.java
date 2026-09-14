package com.mojang.ld22.save;

import com.mojang.ld22.entity.Anvil;
import com.mojang.ld22.entity.Chest;
import com.mojang.ld22.entity.Furnace;
import com.mojang.ld22.entity.Furniture;
import com.mojang.ld22.entity.Lantern;
import com.mojang.ld22.entity.Oven;
import com.mojang.ld22.entity.Workbench;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class FurnitureCodec {

    public interface Factory {
        Furniture create(DataInputStream in) throws IOException;
    }

    private static final Map<Class<?>, String> NAMES     = new HashMap<>();
    private static final Map<String, Factory>  FACTORIES = new HashMap<>();

    static {
        register("workbench", Workbench.class, in -> {
            Workbench w = new Workbench();
            w.read(in);
            return w;
        });
        register("furnace", Furnace.class, in -> {
            Furnace f = new Furnace();
            f.read(in);
            return f;
        });
        register("oven", Oven.class, in -> {
            Oven o = new Oven();
            o.read(in);
            return o;
        });
        register("chest", Chest.class, in -> {
            Chest c = new Chest();
            c.read(in);
            return c;
        });
        register("lantern", Lantern.class, in -> {
            Lantern l = new Lantern();
            l.read(in);
            return l;
        });
        register("anvil", Anvil.class, in -> {
            Anvil a = new Anvil();
            a.read(in);
            return a;
        });
    }

    public static void register(String id, Class<? extends Furniture> cls, Factory f) {
        NAMES.put(cls, id);
        FACTORIES.put(id, f);
    }

    public static String nameOf(Class<?> cls) {
        String id = NAMES.get(cls);
        if (id == null) {
            throw new IllegalStateException("未注册家具类型: " + cls.getName());
        }
        return id;
    }

    public static Furniture create(String id, DataInputStream in) throws IOException {
        Factory f = FACTORIES.get(id);
        if (f == null) throw new IOException("未知家具类型: " + id);
        return f.create(in);
    }
}
