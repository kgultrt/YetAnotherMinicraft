package com.mojang.ld22.save;

import com.mojang.ld22.item.ToolType;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * ToolType 的 name ↔ 实例 反查表。
 *
 * <p>ToolType 是普通类，所有实例都是静态字段（shovel / hoe / ...）。
 */
public final class ToolTypeCodec {

    private static final Map<String, ToolType> BY_NAME = new HashMap<>();

    static {
        for (Field f : ToolType.class.getFields()) {
            if (f.getType() != ToolType.class) continue;
            try {
                ToolType t = (ToolType) f.get(null);
                if (t != null) BY_NAME.put(t.name, t);
            } catch (IllegalAccessException ignored) {}
        }
    }

    private ToolTypeCodec() {}

    public static ToolType byName(String name) {
        ToolType t = BY_NAME.get(name);
        if (t == null) {
            throw new IllegalStateException("未知 ToolType name: " + name);
        }
        return t;
    }
}