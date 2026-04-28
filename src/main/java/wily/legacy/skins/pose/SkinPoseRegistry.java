package wily.legacy.skins.pose;

import wily.legacy.skins.client.render.boxloader.BoxModelManager;

import java.util.*;
import java.util.regex.Pattern;

public final class SkinPoseRegistry {
    private static final Object LOCK = new Object();
    private static volatile Map<PoseTag, List<Selector>> ACTIVE = empty();
    private static volatile Map<PoseTag, List<Selector>> RUNTIME = empty();
    private static Map<PoseTag, List<Selector>> STAGING = empty();

    private SkinPoseRegistry() {
    }

    private static Map<PoseTag, List<Selector>> empty() {
        Map<PoseTag, List<Selector>> m = new EnumMap<>(PoseTag.class);
        for (PoseTag t : PoseTag.values()) m.put(t, List.of());
        return m;
    }

    public static void beginReload() {
        synchronized (LOCK) {
            Map<PoseTag, List<Selector>> m = new EnumMap<>(PoseTag.class);
            for (PoseTag t : PoseTag.values()) m.put(t, new ArrayList<>());
            STAGING = m;
        }
    }

    public static void addSelector(PoseTag tag, String selector, String defaultNamespace) {
        if (tag == null || selector == null) return;

        String s = normalize(selector, defaultNamespace);
        if (s == null || s.isBlank()) return;

        Selector compiled = compileSelector(s);
        synchronized (LOCK) {
            List<Selector> list = STAGING.get(tag);
            if (list instanceof ArrayList<Selector> al) {
                al.add(compiled);
            } else {
                ArrayList<Selector> al2 = new ArrayList<>(list);
                al2.add(compiled);
                STAGING.put(tag, al2);
            }
        }
    }

    public static void endReload() {
        synchronized (LOCK) {
            Map<PoseTag, List<Selector>> frozen = new EnumMap<>(PoseTag.class);
            for (PoseTag t : PoseTag.values()) {
                List<Selector> list = STAGING.get(t);
                if (list == null || list.isEmpty()) frozen.put(t, List.of());
                else frozen.put(t, Collections.unmodifiableList(new ArrayList<>(list)));
            }
            ACTIVE = frozen;
            STAGING = empty();
        }
    }

    public static void addRuntimeSelector(PoseTag tag, String selector) {
        if (tag == null || selector == null) return;
        String s = normalize(selector, null);
        if (s == null || s.isBlank()) return;

        Selector compiled = compileSelector(s);

        synchronized (LOCK) {
            Map<PoseTag, List<Selector>> cur = RUNTIME;
            List<Selector> list = cur.get(tag);
            ArrayList<Selector> al = (list == null || list.isEmpty()) ? new ArrayList<>() : new ArrayList<>(list);
            al.add(compiled);
            Map<PoseTag, List<Selector>> next = new EnumMap<>(cur);
            next.put(tag, Collections.unmodifiableList(al));
            RUNTIME = next;
        }
    }

    public static void clearRuntimeSelectors() {
        RUNTIME = empty();
    }

    public static boolean hasPose(PoseTag tag, String skinId) {
        if (tag == null || skinId == null || skinId.isEmpty()) return false;

        List<Selector> sels = ACTIVE.get(tag);
        List<Selector> rt = RUNTIME.get(tag);

        String id = skinId.indexOf(' ') >= 0 ? skinId.trim() : skinId;
        if (id.isEmpty()) return false;

        if (sels != null && !sels.isEmpty()) {
            for (Selector s : sels) {
                if (s.matches(id)) return true;
            }
        }

        if (rt != null && !rt.isEmpty()) {
            for (Selector s : rt) {
                if (s.matches(id)) return true;
            }
        }

        return BoxModelManager.hasPoseTag(id, tag);
    }

    private static String normalize(String in, String defaultNamespace) {
        String s = in.trim();
        if (s.isEmpty()) return null;

        if (!s.contains(":") && defaultNamespace != null && !defaultNamespace.isBlank()) {
            s = defaultNamespace + ":" + s;
        }
        return s;
    }

    private static Selector compileSelector(String selector) {
        boolean hasWildcard = selector.indexOf('*') >= 0 || selector.indexOf('?') >= 0;
        if (!hasWildcard) return new Selector(selector, null, true);

        StringBuilder regex = new StringBuilder();
        regex.append("^");
        for (int i = 0; i < selector.length(); i++) {
            char c = selector.charAt(i);
            switch (c) {
                case '*' -> regex.append(".*");
                case '?' -> regex.append(".");
                case '.' -> regex.append("\\.");
                case '\\' -> regex.append("\\\\");
                case '(', ')', '[', ']', '{', '}', '+', '^', '$', '|' -> regex.append('\\').append(c);
                default -> regex.append(c);
            }
        }
        regex.append("$");
        Pattern p = Pattern.compile(regex.toString());
        return new Selector(selector, p, false);
    }

    public enum PoseTag {
        ZOMBIE_ARMS("zombie_arms"),
        IDLE_SIT("idle_sit"),
        STIFF_ARMS("stiff_arms"),
        STIFF_LEGS("stiff_legs"),
        SYNC_LEGS("sync_legs"),
        SYNC_ARMS("sync_arms"),
        UPSIDE_DOWN("upside_down"),
        STATUE_OF_LIBERTY("statue_of_liberty"),
        DISABLE_VIEW_BOBBING("disable_view_bobbing"),
        HIDE_HAND("hide_hand");

        private final String key;

        PoseTag(String key) {
            this.key = key;
        }

        public static PoseTag fromKey(String key) {
            if (key == null) return null;
            String k = key.trim().toLowerCase(Locale.ROOT);
            String nk = k.replace("_", "");
            if (nk.equals("weepingstatue")) return STATUE_OF_LIBERTY;
            for (PoseTag t : values()) {
                if (t.key.equals(k)) return t;
                if (t.key.replace("_", "").equals(nk)) return t;
            }
            return null;
        }
    }

    private record Selector(String raw, Pattern pattern, boolean exact) {
        boolean matches(String skinId) {
            if (skinId == null) return false;
            return exact ? raw.equals(skinId) : pattern.matcher(skinId).matches();
        }
    }
}
