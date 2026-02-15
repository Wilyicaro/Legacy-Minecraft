package wily.legacy.Skins.client.screen.widget;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PlayerSkinWidgetList {
    public int x;
    public int y;
    public final List<PlayerSkinWidget> widgets;
    private List<String> skinIds = List.of();
    public int index;
    public PlayerSkinWidget element0;
    public PlayerSkinWidget element1;
    public PlayerSkinWidget element2;
    public PlayerSkinWidget element3;
    public PlayerSkinWidget element4;
    public PlayerSkinWidget element5;
    public PlayerSkinWidget element6;
    private static final int VERTICAL_OFFSET = 10;
    private static final int OFFSET = 80;
    private static final float FACING_FROM_LEFT = -45f;
    private static final float FACING_FROM_RIGHT = 45f;
    private final ArrayList<PlayerSkinWidget> ring = new ArrayList<>();
    private boolean forceInstantNextLayout;

    private PlayerSkinWidgetList(int x, int y, List<PlayerSkinWidget> widgetPool) {
        this.x = x;
        this.y = y;
        this.widgets = widgetPool;
        this.ring.addAll(widgetPool);
    }

    public static PlayerSkinWidgetList of(int x, int y, List<PlayerSkinWidget> widgetPool) {
        return new PlayerSkinWidgetList(x, y, widgetPool);
    }

    public void setOrigin(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSkinIds(List<String> skinIds, boolean instant) {
        int oldSize = this.skinIds == null ? 0 : this.skinIds.size();
        List<String> newIds = skinIds == null ? List.of() : skinIds;
        boolean oldIncomplete = oldSize > 0 && oldSize <= this.widgets.size() && oldSize < this.widgets.size();
        boolean newVirtual = newIds.size() > this.widgets.size();
        if (oldIncomplete && newVirtual) {
            String donorId = null;
            net.minecraft.resources.ResourceLocation donorTex = null;
            for (PlayerSkinWidget w : this.widgets) {
                if (w.hasStablePreview()) {
                    donorId = w.getStablePreviewId();
                    donorTex = w.getStablePreviewTexture();
                    break;
                }
            }
            if (donorId != null && donorTex != null) {
                for (PlayerSkinWidget w : this.widgets) {
                    w.seedStablePreviewIfMissing(donorId, donorTex);
                }
            }
        }
        this.skinIds = newIds;
        this.forceInstantNextLayout |= instant;
        if (this.skinIds.size() <= this.widgets.size()) {
            int n = this.skinIds.size();
            for (int i = 0; i < n; i++) {
                PlayerSkinWidget w = this.widgets.get(i);
                w.setSkinId(this.skinIds.get(i));
            }
            for (int i = n; i < this.widgets.size(); i++) {
                PlayerSkinWidget w = this.widgets.get(i);
                w.setSkinId(null);
                w.invisible();
                w.resetPose();
            }
        }
    }

    public void sortForIndex(int index) {
        sortForIndex(index, false);
    }

    public void sortForIndex(int requestedIndex, boolean instant) {
        if (skinIds.isEmpty()) {
            this.index = 0;
            hideAll();
            return;
        }
        int n = skinIds.size();
        int wrapped = mod(requestedIndex, n);
        boolean virtualized = n > widgets.size();
        boolean doInstant = instant || forceInstantNextLayout;
        forceInstantNextLayout = false;
        if (!virtualized) {
            sortNonVirtual(wrapped, doInstant);
            return;
        }
        sortVirtualized(requestedIndex, wrapped, n, doInstant);
    }

    private void sortNonVirtual(int wrappedIndex, boolean instant) {
        int n = skinIds.size();
        for (int i = 0; i < n && i < widgets.size(); i++) {
            widgets.get(i).setSkinId(skinIds.get(i));
        }
        for (int i = n; i < widgets.size(); i++) {
            PlayerSkinWidget w = widgets.get(i);
            w.setSkinId(null);
            w.invisible();
            w.resetPose();
        }
        if (instant) {
            for (int i = 0; i < n && i < widgets.size(); i++) {
                widgets.get(i).invisible();
            }
        }
        this.index = wrappedIndex;
        element0 = element1 = element2 = element4 = element5 = element6 = null;
        element3 = getWrappedNonVirtual(this.index);
        Set<PlayerSkinWidget> used = new HashSet<>();
        int[] offsets = {0, -1, 1, -2, 2, -3, 3, -4, 4};
        for (int offset : offsets) {
            PlayerSkinWidget w = getWrappedNonVirtual(this.index + offset);
            if (w == null || used.contains(w)) continue;
            used.add(w);
            if (offset != 0) w.resetPose();
            setupSlot(w, offset);
            w.prewarm();
            if (offset == 0) element3 = w;
            else if (offset == -1) element2 = w;
            else if (offset == -2) element1 = w;
            else if (offset == -3) element0 = w;
            else if (offset == 1) element4 = w;
            else if (offset == 2) element5 = w;
            else if (offset == 3) element6 = w;
        }
        for (int i = 0; i < n && i < widgets.size(); i++) {
            PlayerSkinWidget w = widgets.get(i);
            if (!used.contains(w)) {
                w.invisible();
                w.resetPose();
            }
        }
    }

    private PlayerSkinWidget getWrappedNonVirtual(int i) {
        int n = Math.min(skinIds.size(), widgets.size());
        if (n == 0) return null;
        int wrapped = mod(i, n);
        return widgets.get(wrapped);
    }

    private void sortVirtualized(int requestedIndex, int wrappedIndex, int n, boolean instant) {
        if (ring.isEmpty()) ring.addAll(widgets);
        if (instant) {
            for (PlayerSkinWidget w : ring) w.invisible();
        }
        int delta = requestedIndex - this.index;
        this.index = wrappedIndex;
        if (!instant) {
            if (delta == 1) {
                PlayerSkinWidget moved = ring.remove(0);
                ring.add(moved);
            } else if (delta == -1) {
                PlayerSkinWidget moved = ring.remove(ring.size() - 1);
                ring.add(0, moved);
            } else {
            }
        }
        element0 = element1 = element2 = element4 = element5 = element6 = null;
        element3 = null;
        for (int pos = 0; pos < ring.size(); pos++) {
            int offset = pos - 4;
            PlayerSkinWidget w = ring.get(pos);
            int skinIndex = mod(this.index + offset, n);
            w.setSkinId(skinIds.get(skinIndex));
            if (offset != 0) w.resetPose();
            setupSlot(w, offset);
            w.prewarm();
            if (offset == 0) element3 = w;
            else if (offset == -1) element2 = w;
            else if (offset == -2) element1 = w;
            else if (offset == -3) element0 = w;
            else if (offset == 1) element4 = w;
            else if (offset == 2) element5 = w;
            else if (offset == 3) element6 = w;
        }
    }

    private void hideAll() {
        element0 = element1 = element2 = element3 = element4 = element5 = element6 = null;
        for (PlayerSkinWidget w : widgets) {
            w.setSkinId(null);
            w.invisible();
            w.resetPose();
        }
    }

    private void setupSlot(PlayerSkinWidget w, int offset) {
        w.slotOffset = offset;
        w.setCarouselCenterX(this.x + 8 + 45);
        float rotX = 0;
        float rotY;
        int targetPosX;
        int targetPosY;
        float scale;
        switch (offset) {
            case 0 -> {
                w.active = true;
                rotY = 0;
                targetPosX = x + 8;
                targetPosY = y + 20;
                scale = 0.85f;
            }
            case -1 -> {
                w.active = false;
                rotY = FACING_FROM_LEFT;
                targetPosX = x - OFFSET + 18;
                targetPosY = y + VERTICAL_OFFSET + 17;
                scale = 0.7f;
            }
            case 1 -> {
                w.active = false;
                rotY = FACING_FROM_RIGHT;
                targetPosX = x + OFFSET + 20;
                targetPosY = y + VERTICAL_OFFSET + 17;
                scale = 0.7f;
            }
            case -2 -> {
                w.active = false;
                rotY = FACING_FROM_LEFT;
                targetPosX = x - OFFSET - 45;
                targetPosY = y + VERTICAL_OFFSET + 25;
                scale = 0.55f;
            }
            case 2 -> {
                w.active = false;
                rotY = FACING_FROM_RIGHT;
                targetPosX = x + OFFSET * 2 + 18;
                targetPosY = y + VERTICAL_OFFSET + 25;
                scale = 0.55f;
            }
            case -3 -> {
                w.active = false;
                rotY = FACING_FROM_LEFT;
                targetPosX = x - OFFSET * 3;
                targetPosY = y + VERTICAL_OFFSET + 33;
                scale = 0.4f;
            }
            case 3 -> {
                w.active = false;
                rotY = FACING_FROM_RIGHT;
                targetPosX = x + OFFSET * 3 + 35;
                targetPosY = y + VERTICAL_OFFSET + 33;
                scale = 0.4f;
            }
            case -4 -> {
                w.active = false;
                rotY = FACING_FROM_LEFT;
                targetPosX = x - OFFSET * 4 + 80;
                targetPosY = y + VERTICAL_OFFSET + 10;
                scale = 0.4f;
            }
            case 4 -> {
                w.active = false;
                rotY = FACING_FROM_RIGHT;
                targetPosX = x + OFFSET * 4;
                targetPosY = y + VERTICAL_OFFSET * 4 + 20;
                scale = 0.4f;
            }
            default -> {
                w.invisible();
                return;
            }
        }
        int currentX = w.getX();
        if (w.visible && Math.abs(currentX - targetPosX) > 120) {
            int virtualTargetX = targetPosX > currentX ? currentX - OFFSET : currentX + OFFSET;
            w.visible();
            w.beginInterpolation(rotX, rotY, virtualTargetX, targetPosY, scale);
            w.snapTo(targetPosX, targetPosY);
        } else {
            w.visible();
            w.beginInterpolation(rotX, rotY, targetPosX, targetPosY, scale);
        }
    }

    private static int mod(int value, int n) {
        int m = value % n;
        return m < 0 ? m + n : m;
    }
}
