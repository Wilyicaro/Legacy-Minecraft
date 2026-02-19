package wily.legacy.CustomModelSkins.cpm.shared.util;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record Msg(String key, Object... args) {
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("key", key);
        List<Object> a = new ArrayList<>(args.length);
        for (Object o : args) a.add(o instanceof Msg mt ? mt.toMap() : String.valueOf(o));
        m.put("args", a);
        return m;
    }

    public Component toComponent() {
        Object[] a = new Object[args.length];
        for (int i = 0; i < args.length; i++)
            a[i] = args[i] instanceof Msg mt ? mt.toComponent() : String.valueOf(args[i]);
        return Component.translatable(key, a);
    }
}
