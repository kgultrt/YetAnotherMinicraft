package com.mojang.ld22.save;

import com.mojang.ld22.item.resource.Resource;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Resource 的 key ↔ 实例 反查表。
 *
 * <p>Resource 是普通类，所有实例都是静态字段（wood / stone / ...），
 * 所以序列化只需存 {@link Resource#key}，读时反查静态实例即可。
 *
 * <p>利用反射自动收集，新增 Resource 时不用改这里。
 */
public final class ResourceCodec {

    private static final Map<String, Resource> BY_KEY = new HashMap<>();

    static {
        for (Field f : Resource.class.getFields()) {
            if (!Resource.class.isAssignableFrom(f.getType())) continue;
            try {
                Resource r = (Resource) f.get(null);
                if (r != null) BY_KEY.put(r.key, r);
            } catch (IllegalAccessException ignored) {}
        }
    }

    private ResourceCodec() {}

    public static Resource byKey(String key) {
        Resource r = BY_KEY.get(key);
        if (r == null) {
            throw new IllegalStateException("未知 Resource key: " + key);
        }
        return r;
    }
}
