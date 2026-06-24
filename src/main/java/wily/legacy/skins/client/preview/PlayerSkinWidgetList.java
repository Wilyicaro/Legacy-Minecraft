package wily.legacy.skins.client.preview;

import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerSkinWidgetList {
    private static final int OFFSET = 80;
    private static final float FACING_FROM_LEFT = -45f;
    private static final float FACING_FROM_RIGHT = 45f;
    private static final float CENTER_SCALE = 0.935f;
    private static final int BASE_PADDING_X = 8;
    private static final int BASE_PADDING_Y = 20;
    private static final float BASE_WIDGET_W = 106f;
    private static final float BASE_WIDGET_H = 150f;
    private static final int VISIBLE_RADIUS = 4;
    private static final float[] SLOT_SCALE_MUL = new float[]{1f, 0.8f, 0.6375f, 0.508333f, 0.404167f};
    private static final float[] SLOT_DX_MUL = new float[]{0f, 1f, 1.808f, 2.460f, 2.989f};
    private static final float[] SLOT_DY_MUL = new float[]{0f, 30f / 294f, 53f / 294f, 72f / 294f, 87f / 294f};
    public final List<PlayerSkinWidget> widgets;
    private final PlayerSkinWidget[] visible = new PlayerSkinWidget[7];
    private final ArrayList<PlayerSkinWidget> ring = new ArrayList<>();
    private final int[] customCarouselCenterX = new int[9];
    public int x;
    public int y;
    public int index;
    private float centerRotationX;
    private float centerRotationY;
    private int centerPoseMode;
    private boolean centerPunchLoop;
    private List<String> skinIds = List.of();
    private float uiScale = 1f;
    private float carouselScaleMultiplier = 1f;
    private float carouselSpacingMultiplier = 1f;
    private int visibleRadius = VISIBLE_RADIUS;
    private int renderRadius = VISIBLE_RADIUS;
    private boolean forceInstantNextLayout;
    private boolean avoidRepeatsWhenFew;
    private int avoidRepeatsThreshold;
    private int lastShiftDir;
    private boolean customCarouselCenters;

    public PlayerSkinWidgetList(int x, int y, List<PlayerSkinWidget> widgetPool) {
        this.x = x;
        this.y = y;
        this.widgets = widgetPool;
        this.ring.addAll(widgetPool);
        this.centerRotationX = 0;
        this.centerRotationY = 0;
        this.centerPoseMode = 0;
        this.centerPunchLoop = false;
    }

    private static boolean isSparseCarousel(int size) {
        return size > 0 && size < 7;
    }

    private static int sparseStartOffset(int size) {
        return -Math.max(1, size) / 2;
    }

    private static int sparseEndOffset(int size) {
        return sparseStartOffset(size) + Math.max(1, size) - 1;
    }

    public void setCenterRotation(float rotX, float rotY) {
        this.centerRotationX = Mth.clamp(rotX, -50.0F, 50.0F);
        float y = rotY;
        while (y < 0) y += 360;
        this.centerRotationY = (y + 180) % 360 - 180;
    }

    public void setCenterPose(int poseMode, boolean punchLoop) {
        this.centerPoseMode = poseMode;
        this.centerPunchLoop = punchLoop;
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

    public void setVisibleRadius(int radius) {
        this.visibleRadius = Mth.clamp(radius, 1, VISIBLE_RADIUS);
    }

    public void setRenderRadius(int radius) {
        this.renderRadius = Mth.clamp(radius, 1, VISIBLE_RADIUS);
    }

    public void setAvoidRepeatsWhenFew(boolean enabled, int threshold) {
        this.avoidRepeatsWhenFew = enabled;
        this.avoidRepeatsThreshold = Math.max(1, threshold);
    }

    public void setCustomCarouselCenters(int[] centers) {
        if (centers == null || centers.length < 9) {
            this.customCarouselCenters = false;
            return;
        }
        System.arraycopy(centers, 0, this.customCarouselCenterX, 0, 9);
        this.customCarouselCenters = true;
    }

    public PlayerSkinWidget getVisible(int offset) {
        int index = offset + 3;
        return index < 0 || index >= visible.length ? null : visible[index];
    }

    public PlayerSkinWidget getCenter() {
        return getVisible(0);
    }

    public int getCenterAnchorX() {
        float centerScale = getCenterScale();
        return resolveSlotCenterX(0, getDefaultCenterX(centerScale), getSlotSpacing());
    }

    public void setSkinIds(List<String> skinIds, boolean instant) {
        this.skinIds = skinIds == null ? List.of() : skinIds;
        this.forceInstantNextLayout |= instant;
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
        int wrapped = Math.floorMod(requestedIndex, n);
        boolean doInstant = instant || forceInstantNextLayout;
        forceInstantNextLayout = false;
        sortVirtualized(requestedIndex, wrapped, n, doInstant);
    }

    private SlotLayout computeSlot(int offset) {
        int abs = Math.abs(offset);
        if (abs > visibleRadius) return null;

        float centerScale = getCenterScale();
        float scale = centerScale * SLOT_SCALE_MUL[abs];
        int defaultCenterX = getDefaultCenterX(centerScale);
        int slotSpacing = getSlotSpacing();
        int centerX = resolveSlotCenterX(offset, defaultCenterX, slotSpacing);
        int targetPosX = centerX - Math.round(BASE_WIDGET_W * scale / 2f);
        int targetPosY = getBaseTopY() + Math.round(BASE_WIDGET_H * centerScale * SLOT_DY_MUL[abs]);

        return new SlotLayout(
                offset == 0,
                offset == 0 ? centerRotationX : 0f,
                offset == 0 ? centerRotationY : offset < 0 ? FACING_FROM_LEFT : FACING_FROM_RIGHT,
                targetPosX,
                targetPosY,
                scale,
                slotSpacing
        );
    }

    private void sortVirtualized(int requestedIndex, int wrappedIndex, int n, boolean instant) {
        if (ring.isEmpty()) ring.addAll(widgets);
        if (instant) {
            for (PlayerSkinWidget w : ring) w.invisible();
        }
        int delta = requestedIndex - this.index;
        this.index = wrappedIndex;
        if (!instant) {
            boolean sparse = isSparseCarousel(n);
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
            } else if (delta == 1) {
                PlayerSkinWidget moved = ring.remove(0);
                ring.add(moved);
            } else if (delta == -1) {
                PlayerSkinWidget moved = ring.remove(ring.size() - 1);
                ring.add(0, moved);
            }
        }
        clearVisibleElements();
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
                int skinIndex = Math.floorMod(this.index + off, n);
                if (used[skinIndex]) continue;
                used[skinIndex] = true;
                avoidIds[idx] = skinIds.get(skinIndex);
                filled++;
                if (filled >= n) break;
            }
        }
        for (int pos = 0; pos < ring.size(); pos++) {
            int offset = pos - 4;
            boolean sparse = isSparseCarousel(n);
            int sparseStart = sparseStartOffset(n);
            int sparseEnd = sparseEndOffset(n);
            PlayerSkinWidget w = ring.get(pos);
            String id;
            if (sparse && (offset < sparseStart || offset > sparseEnd)) {
                id = null;
            } else if (avoid) {
                if (Math.abs(offset) > visibleRadius) id = null;
                else id = avoidIds[offset + 4];
            } else {
                int skinIndex = Math.floorMod(this.index + offset, n);
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
            assignVisibleElement(offset, w);
        }
    }

    private void clearVisibleElements() {
        Arrays.fill(visible, null);
    }

    private void assignVisibleElement(int offset, PlayerSkinWidget widget) {
        int index = offset + 3;
        if (index >= 0 && index < visible.length) visible[index] = widget;
    }

    private void hideAll() {
        clearVisibleElements();
        for (PlayerSkinWidget w : widgets) {
            w.setSkinId(null);
            w.invisible();
            w.resetPose();
        }
    }

    private void setupSlot(PlayerSkinWidget w, int offset) {
        int prevOffset = w.slotOffset;
        w.setSourceSlotOffset(prevOffset);
        w.slotOffset = offset;
        w.renderRadius = renderRadius;

        SlotLayout fin = computeSlot(offset);
        if (fin == null) {
            w.invisible();
            return;
        }
        w.active = fin.active();

        int currentX = w.getX();
        int warp = Math.max(1, Math.round(120 * uiScale));
        int wrapThreshold = Math.max(warp, fin.step() * visibleRadius);
        int n = skinIds == null ? 0 : skinIds.size();
        boolean sparse = isSparseCarousel(n);
        boolean wrapCross = sparse && w.visible && lastShiftDir != 0 && prevOffset != 0 && offset != 0
                && Integer.signum(prevOffset) != Integer.signum(offset);
        boolean wrap = w.visible && (Math.abs(currentX - fin.x()) > wrapThreshold || wrapCross);
        boolean doPreSnap = wrap && lastShiftDir != 0 && Math.abs(prevOffset) <= visibleRadius - 1;
        if (doPreSnap) {
            int midOffset = prevOffset + lastShiftDir;
            if (Math.abs(midOffset) <= visibleRadius) {
                SlotLayout mid = computeSlot(midOffset);
                if (mid != null) {
                    w.visible();
                    w.beginInterpolation(mid.rotX(), mid.rotY(), mid.x(), mid.y(), mid.scale());
                    w.snapTo(fin.x(), fin.y(), fin.rotX(), fin.rotY(), fin.scale());
                    if (offset == 0) w.setPoseMode(centerPoseMode, centerPunchLoop, true);
                    return;
                }
            }
        }

        if (wrap) {
            w.invisible();
            w.visible();
            w.beginInterpolation(fin.rotX(), fin.rotY(), fin.x(), fin.y(), fin.scale());
        } else {
            w.visible();
            w.beginInterpolation(fin.rotX(), fin.rotY(), fin.x(), fin.y(), fin.scale());
        }
        if (offset == 0) w.setPoseMode(centerPoseMode, centerPunchLoop, true);
    }

    private float getCenterScale() {
        return CENTER_SCALE * uiScale * carouselScaleMultiplier;
    }

    private int getBaseTopX() {
        return x + Math.round(BASE_PADDING_X * uiScale);
    }

    private int getBaseTopY() {
        return y + Math.round(BASE_PADDING_Y * uiScale);
    }

    private int getDefaultCenterX(float centerScale) {
        return getBaseTopX() + Math.round(BASE_WIDGET_W * centerScale / 2f);
    }

    private int getSlotSpacing() {
        return Math.round(OFFSET * uiScale * carouselSpacingMultiplier);
    }

    private int resolveSlotCenterX(int offset, int defaultCenterX, int slotSpacing) {
        if (customCarouselCenters) {
            int idx = offset + VISIBLE_RADIUS;
            if (idx >= 0 && idx < customCarouselCenterX.length) {
                int customCenter = customCarouselCenterX[idx];
                if (customCenter != Integer.MIN_VALUE) {
                    return customCenter;
                }
            }
        }

        int abs = Math.abs(offset);
        return defaultCenterX + (offset == 0 ? 0 : Integer.signum(offset) * Math.round(slotSpacing * SLOT_DX_MUL[abs]));
    }

    private record SlotLayout(boolean active, float rotX, float rotY, int x, int y, float scale, int step) {
    }
}
