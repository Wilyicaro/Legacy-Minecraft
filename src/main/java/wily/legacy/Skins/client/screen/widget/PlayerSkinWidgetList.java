package wily.legacy.Skins.client.screen.widget;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.util.Mth;

public class PlayerSkinWidgetList {
    public int x;
    public int y;
    public final List<PlayerSkinWidget> widgets;
    private float centerRotationX;
    private float centerRotationY;
    private int centerPoseMode;
    private boolean centerPunchLoop;
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
    private static final float FACING_FROM_LEFT = -50f;
    private static final float FACING_FROM_RIGHT = 50f;
    private float uiScale = 1f;
    private float carouselScaleMultiplier = 1f;
    private float carouselSpacingMultiplier = 1f;
    private final ArrayList<PlayerSkinWidget> ring = new ArrayList<>();
    private boolean forceInstantNextLayout;
    private boolean alwaysVirtualCarousel;
    private boolean linearCarousel;
    private boolean avoidRepeatsWhenFew;
    private int avoidRepeatsThreshold;
    private int linearCenterX = Integer.MIN_VALUE;
    private int linearSlotSpacing;

    private int lastShiftDir;

    private static final class SlotValues {
        boolean active;
        float rotX;
        float rotY;
        int x;
        int y;
        float scale;
        int step;
    }

    private boolean customCarouselCenters;
    private int[] customCarouselCenterX = new int[9];
    private int customCarouselStep;

    private PlayerSkinWidgetList(int x, int y, List<PlayerSkinWidget> widgetPool) {
        this.x = x;
        this.y = y;
        this.widgets = widgetPool;
        this.ring.addAll(widgetPool);
        this.centerRotationX = 0;
        this.centerRotationY = 0;
        this.centerPoseMode = 0;
        this.centerPunchLoop = false;
    }

    public void setCenterRotation(float rotX, float rotY) {
        this.centerRotationX = Mth.clamp(rotX, -50.0F, 50.0F);
        float y = rotY;
        while (y < 0) y += 360;
        this.centerRotationY = (y + 180) % 360 - 180;
    }

    public float getCenterRotationX() {
        return centerRotationX;
    }

    public float getCenterRotationY() {
        return centerRotationY;
    }

    public void setCenterPose(int poseMode, boolean punchLoop) {
        this.centerPoseMode = poseMode;
        this.centerPunchLoop = punchLoop;
    }

    public int getCenterPoseMode() {
        return centerPoseMode;
    }

    public boolean isCenterPunchLoop() {
        return centerPunchLoop;
    }

    public static PlayerSkinWidgetList of(int x, int y, List<PlayerSkinWidget> widgetPool) {
        return new PlayerSkinWidgetList(x, y, widgetPool);
    }

    public void setOrigin(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setUiScale(float uiScale) {
        this.uiScale = uiScale <= 0f ? 1f : uiScale;
    }

    public void setCarouselTuning(float scaleMultiplier, float spacingMultiplier) {
        this.carouselScaleMultiplier = scaleMultiplier <= 0f ? 1f : scaleMultiplier;
        this.carouselSpacingMultiplier = spacingMultiplier <= 0f ? 1f : spacingMultiplier;
    }

    public void setAlwaysVirtualCarousel(boolean enabled) {
        this.alwaysVirtualCarousel = enabled;
    }

    public void setAvoidRepeatsWhenFew(boolean enabled, int threshold) {
        this.avoidRepeatsWhenFew = enabled;
        this.avoidRepeatsThreshold = Math.max(1, threshold);
    }

    public void setLinearCarousel(int centerX, int slotSpacing) {
        this.linearCarousel = true;
        this.linearCenterX = centerX;
        this.linearSlotSpacing = slotSpacing;
    }

    public void clearLinearCarousel() {
        this.linearCarousel = false;
        this.linearCenterX = Integer.MIN_VALUE;
        this.linearSlotSpacing = 0;
    }

    public void setCustomCarouselCenters(int[] centers) {
        if (centers == null || centers.length < 9) {
            this.customCarouselCenters = false;
            return;
        }
        for (int i = 0; i < 9; i++) this.customCarouselCenterX[i] = centers[i];
        int step = 0;
        for (int i = 1; i < 7; i++) {
            int a = this.customCarouselCenterX[i];
            int b = this.customCarouselCenterX[i + 1];
            if (a != Integer.MIN_VALUE && b != Integer.MIN_VALUE) {
                step = Math.abs(b - a);
                break;
            }
        }
        if (step == 0) {
            for (int i = 0; i < 8; i++) {
                int a = this.customCarouselCenterX[i];
                int b = this.customCarouselCenterX[i + 1];
                if (a != Integer.MIN_VALUE && b != Integer.MIN_VALUE) {
                    step = Math.abs(b - a);
                    break;
                }
            }
        }
        this.customCarouselStep = step;
        this.customCarouselCenters = true;
    }

    public void clearCustomCarouselCenters() {
        this.customCarouselCenters = false;
        this.customCarouselStep = 0;
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
        if (!alwaysVirtualCarousel && this.skinIds.size() <= this.widgets.size()) {
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
        int deltaReq = requestedIndex - this.index;
        if (Math.abs(deltaReq) == 1) this.lastShiftDir = deltaReq > 0 ? -1 : 1;
        else this.lastShiftDir = 0;
        int wrapped = mod(requestedIndex, n);
        boolean virtualized = alwaysVirtualCarousel || n > widgets.size();
        boolean doInstant = instant || forceInstantNextLayout;
        forceInstantNextLayout = false;
        if (!virtualized) {
            sortNonVirtual(wrapped, doInstant);
            return;
        }
        sortVirtualized(requestedIndex, wrapped, n, doInstant);
    }

    private SlotValues computeSlot(int offset) {
        SlotValues v = new SlotValues();
        float rotX = 0;
        float rotY;
        int targetPosX;
        int targetPosY;
        float scale;
        int vo = Math.round(VERTICAL_OFFSET * uiScale * Math.min(1.2f, carouselSpacingMultiplier));
        int off = Math.round(OFFSET * uiScale * carouselSpacingMultiplier);
        int pad8 = Math.round(8 * uiScale);
        int centerOff = Math.round(45 * uiScale);
        int p8 = Math.round(8 * uiScale);
        int p17 = Math.round(17 * uiScale);
        int p18 = Math.round(18 * uiScale);
        int p20 = Math.round(20 * uiScale);
        int p25 = Math.round(25 * uiScale);
        int p33 = Math.round(33 * uiScale);
        int p35 = Math.round(35 * uiScale);
        int p45 = Math.round(45 * uiScale);

        switch (offset) {
            case 0 -> {
                v.active = true;
                rotX = centerRotationX;
                rotY = centerRotationY;
                targetPosX = x + p8;
                targetPosY = y + p20;
                scale = 0.935f * uiScale * carouselScaleMultiplier;
            }
            case -1 -> {
                v.active = false;
                rotY = FACING_FROM_LEFT;
                targetPosX = x - off + p18;
                targetPosY = y + vo + p17;
                scale = 0.77f * uiScale * carouselScaleMultiplier;
            }
            case 1 -> {
                v.active = false;
                rotY = FACING_FROM_RIGHT;
                targetPosX = x + off + p20;
                targetPosY = y + vo + p17;
                scale = 0.77f * uiScale * carouselScaleMultiplier;
            }
            case -2 -> {
                v.active = false;
                rotY = FACING_FROM_LEFT;
                targetPosX = x - off - p45;
                targetPosY = y + vo + p25;
                scale = 0.605f * uiScale * carouselScaleMultiplier;
            }
            case 2 -> {
                v.active = false;
                rotY = FACING_FROM_RIGHT;
                targetPosX = x + off * 2 + p18;
                targetPosY = y + vo + p25;
                scale = 0.605f * uiScale * carouselScaleMultiplier;
            }
            case -3 -> {
                v.active = false;
                rotY = FACING_FROM_LEFT;
                targetPosX = x - off * 3;
                targetPosY = y + vo + p33;
                scale = 0.44f * uiScale * carouselScaleMultiplier;
            }
            case 3 -> {
                v.active = false;
                rotY = FACING_FROM_RIGHT;
                targetPosX = x + off * 3 + p35;
                targetPosY = y + vo + p33;
                scale = 0.44f * uiScale * carouselScaleMultiplier;
            }
            case -4 -> {
                v.active = false;
                rotY = FACING_FROM_LEFT;
                targetPosX = x - off * 5 - p35;
                targetPosY = y + vo + p33;
                scale = 0.44f * uiScale * carouselScaleMultiplier;
            }
            case 4 -> {
                v.active = false;
                rotY = FACING_FROM_RIGHT;
                targetPosX = x + off * 5 + p35;
                targetPosY = y + vo + p33;
                scale = 0.44f * uiScale * carouselScaleMultiplier;
            }
            default -> {
                return null;
            }
        }

        int step = off;
        if (linearCarousel) {
            int slot = linearSlotSpacing > 0 ? linearSlotSpacing : off;
            int center = linearCenterX != Integer.MIN_VALUE ? linearCenterX : (x + pad8 + centerOff);
            int desiredCenter = center + offset * slot;
            targetPosX = desiredCenter - Math.round(106f * scale / 2f);
            step = slot;
        }
        if (customCarouselCenters) {
            int idx = offset + 4;
            if (idx >= 0 && idx < 9) {
                int desiredCenter = customCarouselCenterX[idx];
                if (desiredCenter != Integer.MIN_VALUE) {
                    targetPosX = desiredCenter - Math.round(106f * scale / 2f);
                    if (customCarouselStep > 0) step = customCarouselStep;
                }
            }
        }

        v.rotX = rotX;
        v.rotY = rotY;
        v.x = targetPosX;
        v.y = targetPosY;
        v.scale = scale;
        v.step = step;
        return v;
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
            if (offset != 0) w.resetPoseState();
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
            boolean sparse = n > 0 && n < 7;
            if (sparse && (delta == 1 || delta == -1)) {
                int visible = n;
                int startOff = -visible / 2;
                int endOff = startOff + visible - 1;
                int[] visIdx = new int[visible];
                for (int i = 0; i < visible; i++) visIdx[i] = (startOff + i) + 4;

                java.util.ArrayList<PlayerSkinWidget> vis = new java.util.ArrayList<>(visible);
                for (int idx : visIdx) {
                    if (idx < 0 || idx >= ring.size()) continue;
                    vis.add(ring.get(idx));
                }
                if (vis.size() == visible) {
                    if (delta == 1) {
                        PlayerSkinWidget moved = vis.remove(0);
                        vis.add(moved);
                    } else {
                        PlayerSkinWidget moved = vis.remove(vis.size() - 1);
                        vis.add(0, moved);
                    }
                    for (int i = 0; i < visible; i++) ring.set(visIdx[i], vis.get(i));
                }
            } else {
                if (delta == 1) {
                    PlayerSkinWidget moved = ring.remove(0);
                    ring.add(moved);
                } else if (delta == -1) {
                    PlayerSkinWidget moved = ring.remove(ring.size() - 1);
                    ring.add(0, moved);
                } else {
                }
            }
        }
        element0 = element1 = element2 = element4 = element5 = element6 = null;
        element3 = null;
        boolean avoid = avoidRepeatsWhenFew && n <= avoidRepeatsThreshold;
        String[] avoidIds = null;
        if (avoid) {
            avoidIds = new String[9];
            boolean[] used = new boolean[n];
            int dir = Integer.compare(delta, 0);
            int filled = 0;
            int[] order = new int[]{0,
                    dir < 0 ? -1 : 1, dir < 0 ? 1 : -1,
                    dir < 0 ? -2 : 2, dir < 0 ? 2 : -2,
                    dir < 0 ? -3 : 3, dir < 0 ? 3 : -3};
            for (int off : order) {
                if (Math.abs(off) >= 4) continue;
                int idx = off + 4;
                if (idx < 0 || idx >= 9) continue;
                int skinIndex = mod(this.index + off, n);
                if (used[skinIndex]) continue;
                used[skinIndex] = true;
                avoidIds[idx] = skinIds.get(skinIndex);
                filled++;
                if (filled >= n) break;
            }
        }
        for (int pos = 0; pos < ring.size(); pos++) {
            int offset = pos - 4;
            boolean sparse = n > 0 && n < 7;
            int sparseStart = -Math.max(1, n) / 2;
            int sparseEnd = sparseStart + Math.max(1, n) - 1;
            PlayerSkinWidget w = ring.get(pos);
            String id;
            if (sparse && (offset < sparseStart || offset > sparseEnd)) {
                id = null;
            } else if (avoid) {
                if (Math.abs(offset) >= 4) id = null;
                else id = avoidIds[offset + 4];
            } else {
                int skinIndex = mod(this.index + offset, n);
                id = skinIds.get(skinIndex);
            }
            if (id == null || id.isBlank()) {
                w.setSkinId(null);
                w.invisible();
                w.resetPose();
                continue;
            }
            w.setSkinId(id);
            if (offset != 0) w.resetPoseState();
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
        int prevOffset = w.slotOffset;
        w.slotOffset = offset;
        int pad8 = Math.round(8 * uiScale);
        int centerOff = Math.round(45 * uiScale);
        if (linearCarousel && linearCenterX != Integer.MIN_VALUE) w.setCarouselCenterX(linearCenterX);
        else w.setCarouselCenterX(this.x + pad8 + centerOff);

        SlotValues fin = computeSlot(offset);
        if (fin == null) {
            w.invisible();
            return;
        }
        w.active = fin.active;

        int currentX = w.getX();
        int warp = Math.max(1, Math.round(120 * uiScale));
        int wrapThreshold = Math.max(warp, fin.step * 4);
        int n = skinIds == null ? 0 : skinIds.size();
        boolean sparse = n > 0 && n < 7;
        boolean wrapCross = sparse && w.visible && lastShiftDir != 0 && prevOffset != 0 && offset != 0
                && Integer.signum(prevOffset) != Integer.signum(offset);
        boolean wrap = w.visible && (Math.abs(currentX - fin.x) > wrapThreshold || wrapCross);
        boolean doPreSnap = wrap && lastShiftDir != 0 && Math.abs(prevOffset) <= 3;
        if (doPreSnap) {
            int midOffset = prevOffset + lastShiftDir;
            if (Math.abs(midOffset) <= 4) {
                SlotValues mid = computeSlot(midOffset);
                if (mid != null) {
                    w.visible();
                    w.beginInterpolation(mid.rotX, mid.rotY, mid.x, mid.y, mid.scale);
                    w.snapTo(fin.x, fin.y, fin.rotX, fin.rotY, fin.scale);
                    if (offset == 0) w.setPoseMode(centerPoseMode, centerPunchLoop, true);
                    return;
                }
            }
        }

        if (wrap) {
            w.invisible();
            w.visible();
            w.beginInterpolation(fin.rotX, fin.rotY, fin.x, fin.y, fin.scale);
        } else {
            w.visible();
            w.beginInterpolation(fin.rotX, fin.rotY, fin.x, fin.y, fin.scale);
        }
        if (offset == 0) w.setPoseMode(centerPoseMode, centerPunchLoop, true);
    }

    private static int mod(int value, int n) {
        int m = value % n;
        return m < 0 ? m + n : m;
    }
}
