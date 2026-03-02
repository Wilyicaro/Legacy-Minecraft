package wily.legacy.Skins.client.screen;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.InputConstants;

import wily.legacy.Skins.client.screen.changeskin.ChangeSkinActions;
import wily.legacy.Skins.client.screen.changeskin.ChangeSkinPackList;
import wily.legacy.Skins.client.screen.widget.PlayerSkinWidget;
import wily.legacy.Skins.client.screen.widget.PlayerSkinWidgetList;
import wily.legacy.Skins.client.render.boxloader.BoxModelManager;
import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinIds;
import wily.legacy.Skins.skin.SkinPack;
import wily.legacy.Skins.skin.SkinPackLoader;
import wily.legacy.Skins.skin.SkinSync;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.controller.ControllerManager;
import wily.legacy.Skins.client.compat.legacy4j.ControlsCompat;
import wily.legacy.Skins.skin.SkinIds;
import wily.legacy.Skins.skin.SkinIdUtil;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.PanelVListScreen;
import wily.legacy.client.screen.RenderableVList;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

public class TU3ChangeSkinScreen extends PanelVListScreen implements wily.legacy.client.controller.Controller.Event, ControlTooltip.Event {
    private static final ResourceLocation SKIN_PANEL = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/skin_panel"),
            PANEL_FILLER = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/panel_filler"),
            TU3_TOP_STRIP = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/tu3_top_strip"),
            TU3_BOTTOM_STRIP = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/tu3_bottom_strip"),
            TU3_TAB_PLATE = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/tu3_tab_plate"),
            TU3_NAME_PLATE = ResourceLocation.fromNamespaceAndPath("legacy", "tiles/tu3_nameplate"),
            TU3_SELECTED_BADGE = ResourceLocation.fromNamespaceAndPath("legacy", "tiles/tu3_selected"),
            PACK_NAME_BOX = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/pack_name_box"),
            SKIN_BOX = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/skin_box"),
            SIZEABLE_ICON_HOLDER = ResourceLocation.fromNamespaceAndPath("legacy", "textures/gui/sprites/container/sizeable_icon_holder.png"),
            BEACON_CHECK = ResourceLocation.fromNamespaceAndPath("legacy", "textures/gui/sprites/container/beacon_check.png"),
            HEART_CONTAINER = ResourceLocation.fromNamespaceAndPath("minecraft", "hud/heart/container"),
            HEART_FULL = ResourceLocation.fromNamespaceAndPath("minecraft", "hud/heart/full");

    private final Minecraft minecraft;
    private final Panel tooltipBox;
    private static final int BASE_TOOLTIP_WIDTH = 400;
    private static final int BASE_PANEL_WIDTH = 180;
    private static final int BASE_PANEL_HEIGHT = 290;
    private float uiScale = 1f;
    private int tooltipWidth = BASE_TOOLTIP_WIDTH;
    private final Map<ResourceLocation, int[]> packIconDims = new HashMap<>();
    private final ChangeSkinPackList packList;
    private final ChangeSkinActions actions;

    private boolean stickUpHeld, stickDownHeld, shiftHeld, pHeld, enterHeld, firstOpen = true;

    private boolean draggingCenterDoll;
    private boolean centerDragMoved;
    private double centerDragStartX;
    private double centerDragStartY;

    private float keptCenterRotX;
    private float keptCenterRotY;
    private int keptCenterPoseMode;
    private boolean keptCenterPunchLoop;

    private int queuedCarouselSteps;
    private int queuedCarouselDir;
    private boolean queuedCarouselSound;

    private boolean holdingOuterCarousel;
    private int holdingOuterDir;
    private long holdingOuterStartAt;
    private long holdingOuterNextAt;

    private PlayerSkinWidgetList playerSkinWidgetList;

    private boolean lastPendingSwap;

    private int tu3TabLeftX;
    private int tu3TabMidX;
    private int tu3TabRightX;
    private int tu3TabY;
    private int tu3TabH;
    private int tu3TabLeftW;
    private int tu3TabMidW;
    private int tu3TabRightW;

    private int layoutX;
    private int layoutY;
    private int layoutW;
    private int layoutH;

    private int tu3StripY;
    private int tu3StripH;

    private int tu3BottomStripY;
    private int tu3BottomStripH;

    private static final float TU3_CAROUSEL_SCALE = 1.5f;
    private static final float TU3_CAROUSEL_SPACING = 1.1f;

    private static void blitSprite(GuiGraphics g, ResourceLocation id, int x, int y, int w, int h) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, w, h);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int wrapPackIndex(int idx) {
        int n = packList.getPackCount();
        if (n <= 0) return 0;
        int r = idx % n;
        if (r < 0) r += n;
        return r;
    }

    private void computeTu3Tabs() {
        int inset = Math.round((105f / 1280f) * width);
        if (inset < 0) inset = 0;
        int left = inset;
        int total = width - inset * 2;
        if (total < 1) {
            left = 0;
            total = Math.max(1, width);
        }

        int w = total / 3;
        int extra = total - (w * 3);

        tu3TabLeftX = left;
        tu3TabMidX = left + w;
        tu3TabRightX = left + (w * 2);

        tu3TabLeftW = w;
        tu3TabMidW = w;
        tu3TabRightW = w + extra;
    }

    private String tu3PackNameAt(int idx) {
        idx = wrapPackIndex(idx);
        ChangeSkinPackList.PackButton b = packList.getButtonForIndex(idx);
        if (b == null || b.getMessage() == null) return "";
        String s = b.getMessage().getString();
        return s == null ? "" : s;
    }

    private int tu3MidExtra() {
        return Math.max(1, sc(11));
    }

    private void renderTu3TabsBehindStrip(GuiGraphics g) {
        computeTu3Tabs();
        blitSprite(g, TU3_TAB_PLATE, tu3TabLeftX, tu3TabY, Math.max(1, tu3TabLeftW), tu3TabH);
        blitSprite(g, TU3_TAB_PLATE, tu3TabRightX, tu3TabY, Math.max(1, tu3TabRightW), tu3TabH);
    }

    private void renderTu3Tabs(GuiGraphics g) {
        computeTu3Tabs();
        int midExtra = tu3MidExtra();
        int midY = tu3TabY - midExtra;
        if (midY < tu3StripY) midY = tu3StripY;
        int midH = tu3TabH + (tu3TabY - midY);

        blitSprite(g, TU3_TAB_PLATE, tu3TabMidX, midY, Math.max(1, tu3TabMidW), midH);

        int baseLabelY = tu3TabY + (tu3TabH - minecraft.font.lineHeight) / 2;
        int midLabelY = midY + (midH - minecraft.font.lineHeight) / 2;
        int idx = packList.getFocusedPackIndex();

        renderTu3TabLabel(g, tu3TabLeftX, tu3TabLeftW, baseLabelY, tu3PackNameAt(idx - 1), 0xDDFFFFFF);
        renderTu3TabLabel(g, tu3TabMidX, tu3TabMidW, midLabelY, tu3PackNameAt(idx), 0xFFFFFFFF);
        renderTu3TabLabel(g, tu3TabRightX, tu3TabRightW, baseLabelY, tu3PackNameAt(idx + 1), 0xDDFFFFFF);
    }

    private void renderTu3TabLabel(GuiGraphics g, int x, int w, int y, String label, int color) {
        if (label == null) label = "";
        int maxPx = Math.max(1, w - sc(16));
        String show = label;
        if (minecraft.font.width(show) > maxPx) {
            int ellW = minecraft.font.width("…");
            show = minecraft.font.plainSubstrByWidth(show, Math.max(0, maxPx - ellW)) + "…";
        }
        int cx = x + Math.max(1, w) / 2;
        g.drawCenteredString(minecraft.font, Component.literal(show), cx, y, color);
    }

    private int sc(int v) {
        return Math.round(v * uiScale);
    }

    private static float computeScale(int w, int h) {
        float sw = (w - 20f) / (BASE_PANEL_WIDTH + BASE_TOOLTIP_WIDTH - 2f);
        float sh = (h - 20f) / BASE_PANEL_HEIGHT;
        float sc = Math.min(1f, Math.min(sw, sh));
        if (sc > 0.8f) sc *= 0.93f;
        sc *= 0.95f;
        if (sc <= 0f) sc = 1f;
        return sc;
    }

    private int previewBoxSize() {
        int min = Math.max(1, sc(24));
        int size = Math.max(1, sc(112));
        int max = panel.width - sc(20);
        if (max < size) size = Math.max(min, max);
        return Math.max(1, size);
    }

    private int[] packIconDims(ResourceLocation icon) {
        int[] d = packIconDims.get(icon);
        if (d != null) return d;
        int w = 128;
        int h = 128;
        try {
            Resource r = minecraft.getResourceManager().getResource(icon).orElse(null);
            if (r != null) {
                try (var in = r.open()) {
                    NativeImage img = NativeImage.read(in);
                    w = img.getWidth();
                    h = img.getHeight();
                    img.close();
                }
            }
        } catch (Throwable ignored) {
        }
        int[] out = new int[]{Math.max(1, w), Math.max(1, h)};
        packIconDims.put(icon, out);
        return out;
    }

    private int previewBoxX() {
        int s = previewBoxSize();
        int x = panel.x + sc(34);
        int right = panel.x + panel.width - sc(7);
        if (x + s > right) x = panel.x + Math.max(sc(7), (panel.width - s) / 2);
        return x;
    }

    private int previewBoxY() {
        int s = previewBoxSize();
        int y = panel.y + sc(10);
        int top = panel.y + sc(5);
        int bottom = panel.y + panel.height - s - sc(5);
        if (bottom < top) bottom = top;
        if (y > bottom) y = bottom;
        if (y < top) y = top;
        return y;
    }

    private void skinPack(int i) {
        if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
            keptCenterRotX = playerSkinWidgetList.element3.getRotationX();
            keptCenterRotY = playerSkinWidgetList.element3.getRotationY();
            playerSkinWidgetList.setCenterRotation(keptCenterRotX, keptCenterRotY);
            keptCenterPoseMode = playerSkinWidgetList.element3.getPoseMode();
            keptCenterPunchLoop = playerSkinWidgetList.element3.isPunchLoop();
            playerSkinWidgetList.setCenterPose(keptCenterPoseMode, keptCenterPunchLoop);
        }
        actions.skinPack(i);
        applyTu3CarouselTuning();
    }

    private void skinPack() {
        if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
            keptCenterRotX = playerSkinWidgetList.element3.getRotationX();
            keptCenterRotY = playerSkinWidgetList.element3.getRotationY();
            playerSkinWidgetList.setCenterRotation(keptCenterRotX, keptCenterRotY);
            keptCenterPoseMode = playerSkinWidgetList.element3.getPoseMode();
            keptCenterPunchLoop = playerSkinWidgetList.element3.isPunchLoop();
            playerSkinWidgetList.setCenterPose(keptCenterPoseMode, keptCenterPunchLoop);
        }
        actions.skinPack();
        applyTu3CarouselTuning();
    }

    private void applyTu3CarouselTuning() {
        if (playerSkinWidgetList == null) return;
        playerSkinWidgetList.setCarouselTuning(TU3_CAROUSEL_SCALE, TU3_CAROUSEL_SPACING);
        SkinPack p = actions.getFocusedPack();
        boolean fav = p != null && SkinIdUtil.isFavouritesPack(p.id());
        playerSkinWidgetList.setAvoidRepeatsWhenFew(fav, 7);
        playerSkinWidgetList.clearLinearCarousel();
        float mult = TU3_CAROUSEL_SCALE;
        float s0 = 0.935f * uiScale * mult;
        float s1 = 0.77f * uiScale * mult;
        float s2 = 0.605f * uiScale * mult;
        float s3 = 0.44f * uiScale * mult;
        float w0 = 106f * s0;
        float w1 = 106f * s1;
        float w2 = 106f * s2;
        float w3 = 106f * s3;
        int pad = Math.max(1, sc(6));
        float available = Math.max(1f, (float) layoutW - pad * 2f);
        float sum = w0 + (w1 + w2 + w3) * 2f;
        float gap = (available - sum) / 6f;
        float leftEdge = layoutX + pad;
        float cM3 = leftEdge + w3 / 2f;
        float cM2 = cM3 + w3 / 2f + gap + w2 / 2f;
        float cM1 = cM2 + w2 / 2f + gap + w1 / 2f;
        float c0 = cM1 + w1 / 2f + gap + w0 / 2f;
        float cP1 = c0 + w0 / 2f + gap + w1 / 2f;
        float cP2 = cP1 + w1 / 2f + gap + w2 / 2f;
        float cP3 = cP2 + w2 / 2f + gap + w3 / 2f;
        float cM4 = cM3 - (w3 / 2f + gap + w3 / 2f);
        float cP4 = cP3 + (w3 / 2f + gap + w3 / 2f);
        float spawnerExtra = Math.max(10f * uiScale, w3 * 0.35f);
        cM4 -= spawnerExtra;
        cP4 += spawnerExtra;
        int[] centers = new int[9];
        centers[0] = Math.round(cM4);
        centers[1] = Math.round(cM3);
        centers[2] = Math.round(cM2);
        centers[3] = Math.round(cM1);
        centers[4] = Math.round(c0);
        centers[5] = Math.round(cP1);
        centers[6] = Math.round(cP2);
        centers[7] = Math.round(cP3);
        centers[8] = Math.round(cP4);
        playerSkinWidgetList.setCustomCarouselCenters(centers);

        int baseCenterW = Math.round(106f * 0.935f * uiScale);
        int dx = Math.round(baseCenterW * (TU3_CAROUSEL_SCALE - 1f) / 2f);

        float areaTop = (float) layoutY;
        float areaBottom = (float) tu3BottomStripY;
        float areaCenter = (areaTop + areaBottom) / 2f;
        int p20 = Math.round(20f * uiScale);
        float centerH = 150f * s0;
        int desiredOriginY = Math.round(areaCenter - (centerH / 2f) - p20);

        playerSkinWidgetList.setOrigin(playerSkinWidgetList.x - dx, desiredOriginY);
        playerSkinWidgetList.sortForIndex(playerSkinWidgetList.index, true);
    }

    private void selectSkin() {
        actions.selectSkin();
    }

    private void favorite() {
        actions.favorite();
    }

    private void playClick() {
        actions.playClick();
    }

    private void openLegacyChangeSkinScreen() {
        actions.openLegacyChangeSkinScreen();
    }

    private boolean isUsingAutoSelectedSkin() {
        UUID self = minecraft.player != null ? minecraft.player.getUUID() : minecraft.getUser() != null ? minecraft.getUser().getProfileId() : null;
        if (self == null) return true;
        String selectedId = ClientSkinCache.get(self);
        if (selectedId == null) selectedId = SkinSync.getServerSkinId(self);
        return selectedId == null || selectedId.isBlank();
    }


private String getCurrentSelectedSkinId() {
    UUID self = minecraft.player != null ? minecraft.player.getUUID()
            : (minecraft.getUser() != null ? minecraft.getUser().getProfileId() : null);
    if (self == null) return null;
    String selectedId = ClientSkinCache.get(self);
    if (selectedId == null) selectedId = SkinSync.getServerSkinId(self);
    return selectedId;
}

private String resolvePackIdToOpenOnFirstOpen() {
    try {
        String def = SkinPackLoader.getPreferredDefaultPackId();
        var packs = SkinPackLoader.getPacks();

        String selectedId = getCurrentSelectedSkinId();
        String candidate = null;

        if (selectedId != null && !selectedId.isBlank()) {
            String last = SkinPackLoader.getLastUsedCustomPackId();
            if (last != null && packs.containsKey(last) && SkinPackLoader.packContainsSkinId(last, selectedId)) candidate = last;

            if (candidate == null) {
                String src = SkinPackLoader.getSourcePackId(selectedId);
                if (src != null && !src.isBlank() && packs.containsKey(src)) candidate = src;
            }
        }

        if (candidate == null) {
            String last = SkinPackLoader.getLastUsedCustomPackId();
            if (last != null && packs.containsKey(last)) candidate = last;
        }

        if (candidate != null && packs.containsKey(candidate)) return candidate;
        if (def != null && packs.containsKey(def)) return def;
        return SkinIds.PACK_DEFAULT;
    } catch (Throwable ignored) {
        return SkinIds.PACK_DEFAULT;
    }
}

    private int resolveSelectedSkinIndex() {
        UUID self = minecraft.player != null ? minecraft.player.getUUID() : minecraft.getUser() != null ? minecraft.getUser().getProfileId() : null;
        String selectedId = self != null ? ClientSkinCache.get(self) : null;
        if (selectedId == null || selectedId.isBlank()) {
            SkinPack focused = actions.getFocusedPack();
            if (focused != null && focused.skins() != null) {
                int limit = Math.min(100, focused.skins().size());
                for (int i = 0; i < limit; i++) {
                    SkinEntry se = focused.skins().get(i);
                    if (se != null && SkinIds.AUTO_SELECT.equals(se.id())) return i;
                }
            }
            return 0;
        }

        SkinPack focused = actions.getFocusedPack();
        if (focused != null && focused.skins() != null) {
            int limit = Math.min(100, focused.skins().size());
            for (int i = 0; i < limit; i++) {
                SkinEntry se = focused.skins().get(i);
                if (se != null && selectedId.equals(se.id())) return i;
            }
        }
        return 0;
    }

    private ResourceLocation getFocusedPackIcon() {
        return actions.getFocusedPackIcon();
    }

    private SkinPack getFocusedPack() {
        return actions.getFocusedPack();
    }

    public TU3ChangeSkinScreen(Screen parent) {
        super(parent, s -> Panel.centered(s,
                () -> Math.max(1, Math.round(BASE_PANEL_WIDTH * computeScale(s.width, s.height))),
                () -> Math.max(1, Math.round(BASE_PANEL_HEIGHT * computeScale(s.width, s.height))),
                0, 0), Component.empty());
        minecraft = Minecraft.getInstance();
        tooltipBox = new Panel(UIAccessor.of(this)) {
            @Override
            public void init(String name) {
                super.init(name);
            }

            @Override
            public void init() {
                init("tooltipBox");
            }

            @Override
            public void render(GuiGraphics g, int i, int j, float f) {
            }
        };

        renderableVList.layoutSpacing(l -> 2);

        packList = new ChangeSkinPackList(this::playClick, () -> {
        });

        actions = new ChangeSkinActions(minecraft, packList, new ChangeSkinActions.Host() {
            @Override
            public PlayerSkinWidgetList getPlayerSkinWidgetList() {
                return playerSkinWidgetList;
            }

            @Override
            public void setPlayerSkinWidgetList(PlayerSkinWidgetList list) {
                playerSkinWidgetList = list;
                if (playerSkinWidgetList != null) {
                    playerSkinWidgetList.setAlwaysVirtualCarousel(true);
                }
            }

            @Override
            public PlayerSkinWidget addSkinWidget(PlayerSkinWidget w) {
                return addRenderableWidget(w);
            }

            @Override
            public Panel getTooltipBox() {
                return tooltipBox;
            }

            @Override
            public Panel getPanel() {
                return panel;
            }

            @Override
            public Screen getScreen() {
                return TU3ChangeSkinScreen.this;
            }

            @Override
            public float getUiScale() {
                return uiScale;
            }
        });

        try {
            SkinPackLoader.ensureLoaded();
        } catch (Throwable ignored) {
        }
        packList.initFromLoader();
    }

    @Override
    public void renderableVListInit() {
        packList.refreshPackIdsIfNeeded();
        packList.populateInto(getRenderableVList());
        getRenderableVList().scrollArrowYOffset(0);
        getRenderableVList().init("consoleskins.packList.tu3.hidden", -100000, -100000, 1, 1);
    }

    @Override
    protected void panelInit() {
        uiScale = computeScale(width, height);
        tooltipWidth = Math.max(1, Math.round(BASE_TOOLTIP_WIDTH * uiScale));
        renderableVList.layoutSpacing(l -> 0);
        packList.applyUiScale(uiScale);

        panel.init();
        tooltipBox.init("tooltipBox");

        int margin = sc(10);
        layoutX = 0;
        layoutW = Math.max(1, width);

        float topScale = 0.6f;
        tu3StripH = Math.max(1, Math.round(62f * uiScale * topScale));
        tu3BottomStripH = Math.max(1, Math.round(20f * uiScale));

        int maxGrey = height - tu3StripH - tu3BottomStripH;
        if (maxGrey < 1) maxGrey = 1;
        int shrunkGrey = Math.max(1, Math.round(maxGrey * 0.67f));
        int blockH = tu3StripH + shrunkGrey + tu3BottomStripH;
        tu3StripY = Math.max(0, (height - blockH) / 2);

        tu3TabH = Math.max(1, Math.round((50f / 62f) * tu3StripH));
        tu3TabY = tu3StripY + Math.round((2f / 62f) * tu3StripH);
        int tabMaxH = (tu3StripY + tu3StripH) - tu3TabY;
        if (tabMaxH < 1) tabMaxH = 1;
        if (tu3TabH > tabMaxH) tu3TabH = tabMaxH;

        layoutY = tu3StripY + tu3StripH;

        tu3BottomStripY = Math.min(height - tu3BottomStripH, layoutY + shrunkGrey);
        tu3BottomStripY = Math.max(layoutY + 1, tu3BottomStripY);
        layoutH = Math.max(1, tu3BottomStripY - layoutY);

        int off45 = Math.round(45 * uiScale);
        int off23 = Math.round(23 * uiScale);
        int off90 = Math.round(90 * uiScale);

        panel.pos(layoutX, layoutY - off45);
        panel.size(1, layoutH);

        tooltipBox.pos(layoutX, layoutY);
        tooltipBox.size(layoutW + off23, layoutH + off90);

        if (firstOpen) {
            String openId = resolvePackIdToOpenOnFirstOpen();
            if (openId != null) packList.focusPackId(openId, false);
        }
    }

    @Override
    protected void init() {
        super.init();

        ChangeSkinPackList.PackButton b = packList.getButtonForIndex(packList.getFocusedPackIndex());
        if (b != null && children().contains(b))
            setFocused(b);
        else
            for (var c : children())
                if (c instanceof ChangeSkinPackList.PackButton pb) {
                    setFocused(pb);
                    break;
                }

        if (firstOpen) {
            firstOpen = false;
            String openId = resolvePackIdToOpenOnFirstOpen();
            if (openId != null) packList.focusPackId(openId, false);
            packList.consumeQueuedChangePack();
            skinPack(resolveSelectedSkinIndex());
            actions.warmupFavouritesPack();
        } else if (playerSkinWidgetList != null) {
            skinPack(playerSkinWidgetList.index);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        int key = InputConstants.getKey(e).getValue();

        if (key == InputConstants.KEY_W || key == InputConstants.KEY_S) {
            int dir = key == InputConstants.KEY_S ? 1 : -1;
            int count = packList.getPackCount();
            if (count > 1) {
                int target = wrapPackIndex(packList.getFocusedPackIndex() + dir);
                ChangeSkinPackList.PackButton btn = packList.getButtonForIndex(target);
                packList.setFocusedPackIndex(target, true);
                if (btn != null) {
                    setFocused(btn);
                    focusPackListItem(btn);
                }
                if (packList.consumeQueuedChangePack()) {
                    stopHoldingOuterCarousel();
                    cancelQueuedCarousel();
                    skinPack(resolveSelectedSkinIndex());
                }
            }
            return true;
        }

        if (key == InputConstants.KEY_RETURN) {
            if (!enterHeld) enterHeld = true;
            return true;
        }

        if (key == InputConstants.KEY_F) {
            favorite();
            return true;
        }

        if (key == InputConstants.KEY_LSHIFT || key == InputConstants.KEY_RSHIFT) {
            if (!shiftHeld) {
                shiftHeld = true;
                if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null && !playerSkinWidgetList.element3.isInterpolating()) {
                    playerSkinWidgetList.element3.togglePose();
                    keptCenterPoseMode = playerSkinWidgetList.element3.getPoseMode();
                    keptCenterPunchLoop = playerSkinWidgetList.element3.isPunchLoop();
                    playerSkinWidgetList.setCenterPose(keptCenterPoseMode, keptCenterPunchLoop);
                    playClick();
                }
            }
            return true;
        }

        if (key == InputConstants.KEY_P) {
            if (!pHeld) {
                pHeld = true;
                if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null && !playerSkinWidgetList.element3.isInterpolating()) {
                    playerSkinWidgetList.element3.togglePunch();
                    keptCenterPoseMode = playerSkinWidgetList.element3.getPoseMode();
                    keptCenterPunchLoop = playerSkinWidgetList.element3.isPunchLoop();
                    playerSkinWidgetList.setCenterPose(keptCenterPoseMode, keptCenterPunchLoop);
                    playClick();
                }
            }
            return true;
        }

        if (key == InputConstants.KEY_O) {
            openLegacyChangeSkinScreen();
            return true;
        }

        if (handlePackListStepNavigation(key)) return true;

        if (control(
                key == InputConstants.KEY_LEFT || key == InputConstants.KEY_LBRACKET || key == InputConstants.KEY_A,
                key == InputConstants.KEY_RIGHT || key == InputConstants.KEY_RBRACKET || key == InputConstants.KEY_D))
            return true;

        return super.keyPressed(e);
    }

    @Override
    public boolean keyReleased(KeyEvent e) {
        int key = InputConstants.getKey(e).getValue();

        if (key == InputConstants.KEY_RETURN) {
            if (enterHeld) {
                enterHeld = false;
                selectSkin();
            }
            return true;
        }

        if (key == InputConstants.KEY_LSHIFT || key == InputConstants.KEY_RSHIFT) {
            shiftHeld = false;
            return true;
        }

        if (key == InputConstants.KEY_P) {
            pHeld = false;
            return true;
        }

        return super.keyReleased(e);
    }

    private boolean handlePackListStepNavigation(int key) {
        boolean up = key == InputConstants.KEY_W || key == InputConstants.KEY_UP;
        boolean down = key == InputConstants.KEY_S || key == InputConstants.KEY_DOWN;
        if (!(up || down)) return false;

        if (key == InputConstants.KEY_UP || key == InputConstants.KEY_DOWN) {
            var f = getFocused();
            if (!(f == null || f == getRenderableVList() || f instanceof ChangeSkinPackList.PackButton)) return false;
        }

        int count = packList.getPackCount();
        if (count <= 1) return true;

        int target = packList.getFocusedPackIndex() + (up ? -1 : 1);
        if (target < 0) target = count - 1;
        else if (target >= count) target = 0;

        ChangeSkinPackList.PackButton btn = packList.getButtonForIndex(target);
        if (btn == null) return true;

        packList.setFocusedPackIndex(target, true);
        setFocused(btn);
        focusPackListItem(btn);

        if (packList.consumeQueuedChangePack()) {
            stopHoldingOuterCarousel();
            cancelQueuedCarousel();
            skinPack(resolveSelectedSkinIndex());
        }
        return true;
    }

    private void focusPackListItem(Object item) {
        Object v = getRenderableVList();
        if (v == null || item == null) return;

        try {
            String[] names = {"setFocused", "focus", "focusRenderable", "setFocusedRenderable", "ensureVisible", "ensureRenderableVisible", "scrollTo", "scrollToIndex"};
            for (String n : names)
                for (Method m : v.getClass().getMethods()) {
                    if (!m.getName().equals(n)) continue;
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 1 && p[0].isAssignableFrom(item.getClass())) {
                        m.invoke(v, item);
                        return;
                    }
                }

            int idx = item instanceof ChangeSkinPackList.PackButton pb ? pb.getPackIndex() : packList.getFocusedPackIndex();
            String[] idxNames = {"scrollToIndex", "setScrollToIndex", "focusIndex", "setFocusedIndex", "setIndex", "scrollTo"};
            for (String n : idxNames)
                for (Method m : v.getClass().getMethods()) {
                    if (!m.getName().equals(n)) continue;
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 1 && (p[0] == int.class || p[0] == Integer.class)) {
                        m.invoke(v, idx);
                        return;
                    }
                }
        } catch (Throwable ignored) {
        }
    }

    private boolean control(boolean left, boolean right) {
        if (!(left || right) || playerSkinWidgetList == null) return false;
        if (playerSkinWidgetList.widgets.stream().anyMatch(w -> w.progress <= 1f)) return true;
        playerSkinWidgetList.setCenterRotation(0, 0);
        playerSkinWidgetList.sortForIndex(playerSkinWidgetList.index + (left ? -1 : 0) + (right ? 1 : 0));
        playClick();
        return true;
    }

    private boolean carouselAnimating() {
        if (playerSkinWidgetList == null || playerSkinWidgetList.widgets == null) return false;
        for (PlayerSkinWidget w : playerSkinWidgetList.widgets) {
            if (w != null && w.visible && w.progress <= 1f) return true;
        }
        return false;
    }

    private PlayerSkinWidget pickCarouselWidget(double mx, double my) {
        if (playerSkinWidgetList == null || playerSkinWidgetList.widgets == null) return null;
        PlayerSkinWidget best = null;
        double bestD = Double.MAX_VALUE;
        for (PlayerSkinWidget w : playerSkinWidgetList.widgets) {
            if (w == null || !w.visible) continue;
            int x = w.getX();
            int y = w.getY();
            int ww = w.getWidth();
            int hh = w.getHeight();
            if (!inside(mx, my, x, y, ww, hh)) continue;
            double cx = x + ww * 0.5;
            double cy = y + hh * 0.5;
            double dx = mx - cx;
            double dy = my - cy;
            double d = dx * dx + dy * dy;
            if (d < bestD) {
                bestD = d;
                best = w;
            }
        }
        return best;
    }

    private void cancelQueuedCarousel() {
        queuedCarouselSteps = 0;
        queuedCarouselDir = 0;
        queuedCarouselSound = false;
    }

    private void pumpQueuedCarousel() {
        if (queuedCarouselSteps <= 0) return;
        if (actions != null && actions.isPendingSwap()) return;
        if (carouselAnimating()) return;
        if (playerSkinWidgetList == null) {
            cancelQueuedCarousel();
            return;
        }
        if (queuedCarouselSound) {
            queuedCarouselSound = false;
            playClick();
        }
        playerSkinWidgetList.setCenterRotation(0, 0);
        playerSkinWidgetList.sortForIndex(playerSkinWidgetList.index + queuedCarouselDir);
        queuedCarouselSteps--;
        if (queuedCarouselSteps <= 0) cancelQueuedCarousel();
    }

    private void startQueuedCarousel(int offset) {
        if (playerSkinWidgetList == null) return;
        if (actions != null && actions.isPendingSwap()) return;
        if (carouselAnimating()) return;
        int abs = Math.abs(offset);
        if (abs == 0) return;
        if (abs == 1) {
            playerSkinWidgetList.setCenterRotation(0, 0);
            playerSkinWidgetList.sortForIndex(playerSkinWidgetList.index + offset);
            playClick();
            return;
        }
        queuedCarouselDir = offset > 0 ? 1 : -1;
        queuedCarouselSteps = abs;
        queuedCarouselSound = true;
        pumpQueuedCarousel();
    }

    private void startHoldingOuterCarousel(int dir) {
        holdingOuterCarousel = true;
        holdingOuterDir = dir < 0 ? -1 : 1;
        long now = net.minecraft.Util.getMillis();
        holdingOuterStartAt = now;
        holdingOuterNextAt = now + 220L;
    }

    private void stopHoldingOuterCarousel() {
        holdingOuterCarousel = false;
        holdingOuterDir = 0;
        holdingOuterStartAt = 0L;
        holdingOuterNextAt = 0L;
    }

    private void pumpHoldingOuterCarousel() {
        if (!holdingOuterCarousel) return;
        if (actions != null && actions.isPendingSwap()) return;
        if (queuedCarouselSteps > 0) return;
        if (carouselAnimating()) return;
        long now = net.minecraft.Util.getMillis();
        if (now < holdingOuterNextAt) return;
        startQueuedCarousel(holdingOuterDir);
        long held = now - holdingOuterStartAt;
        long delay = held < 700L ? 120L : 80L;
        holdingOuterNextAt = now + delay;
    }
    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean bl) {
        double mx = e.x(), my = e.y();

        if (e.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            computeTu3Tabs();
            int midExtra = tu3MidExtra();
            int midY = tu3TabY - midExtra;
            if (midY < tu3StripY) midY = tu3StripY;
            int midH = tu3TabH + (tu3TabY - midY);
            if (inside(mx, my, tu3TabLeftX, tu3TabY, Math.max(1, tu3TabLeftW), tu3TabH)) {
                packList.setFocusedPackIndex(packList.getFocusedPackIndex() - 1, true);
                return true;
            }
            if (inside(mx, my, tu3TabRightX, tu3TabY, Math.max(1, tu3TabRightW), tu3TabH)) {
                packList.setFocusedPackIndex(packList.getFocusedPackIndex() + 1, true);
                return true;
            }
            if (inside(mx, my, tu3TabMidX, midY, Math.max(1, tu3TabMidW), midH)) {
                return true;
            }
        }

        PlayerSkinWidget hit = pickCarouselWidget(mx, my);
        if (hit != null) {
            if (actions != null && actions.isPendingSwap()) return true;
            if (carouselAnimating()) return true;

            if (e.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
                if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
                    PlayerSkinWidget center = playerSkinWidgetList.element3;
                    if (!center.isInterpolating()) {
                        center.recenterView();
                        keptCenterRotX = center.getRotationX();
                        keptCenterRotY = center.getRotationY();
                        playClick();
                    }
                }
                return true;
            }

            if (e.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;

            int off = hit.slotOffset;
            if (off == 0) {
                stopHoldingOuterCarousel();
                draggingCenterDoll = true;
                centerDragMoved = false;
                centerDragStartX = mx;
                centerDragStartY = my;
                return true;
            }
            stopHoldingOuterCarousel();
            startQueuedCarousel(off);
            if (Math.abs(off) == 2) startHoldingOuterCarousel(off);
            return true;
        }

        return super.mouseClicked(e, bl);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingCenterDoll && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            PlayerSkinWidget center = playerSkinWidgetList != null ? playerSkinWidgetList.element3 : null;
            if (center != null && center.visible && !center.isInterpolating()) {
                if (!centerDragMoved) {
                    if (Math.abs(event.x() - centerDragStartX) > 2.0 || Math.abs(event.y() - centerDragStartY) > 2.0) {
                        centerDragMoved = true;
                    }
                }

                if (centerDragMoved) center.applyDrag(-dragX, 0);
                if (centerDragMoved) {
                    keptCenterRotX = center.getRotationX();
                    keptCenterRotY = center.getRotationY();
                }
                return true;
            }
        }

        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && holdingOuterCarousel) stopHoldingOuterCarousel();
        if (draggingCenterDoll && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            draggingCenterDoll = false;
            if (!centerDragMoved) {
                selectSkin();
            }
            centerDragMoved = false;
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (g == 0) return false;

        RenderableVList vList = getRenderableVListAt(d, e);
        if (vList != null) {
            vList.mouseScrolled(g);
            return true;
        }

        if (playerSkinWidgetList == null) return false;
        if (!inside(d, e, layoutX, layoutY, layoutW, layoutH) && pickCarouselWidget(d, e) == null)
            return false;

        if (actions != null && actions.isPendingSwap()) return true;
        if (draggingCenterDoll) return true;
        if (carouselAnimating()) return true;

        int dir = g > 0 ? -1 : 1;
        startQueuedCarousel(dir);
        return true;
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if (state != null && (state.is(ControllerBinding.LEFT_BUMPER) || state.is(ControllerBinding.RIGHT_BUMPER))) {
            if (state.pressed && state.canClick()) {
                int dir = state.is(ControllerBinding.RIGHT_BUMPER) ? 1 : -1;
                int count = packList.getPackCount();
                if (count > 1) {
                    int target = wrapPackIndex(packList.getFocusedPackIndex() + dir);
                    ChangeSkinPackList.PackButton btn = packList.getButtonForIndex(target);
                    packList.setFocusedPackIndex(target, true);
                    if (btn != null) setFocused(btn);
                    if (packList.consumeQueuedChangePack()) {
                        stopHoldingOuterCarousel();
                        cancelQueuedCarousel();
                        skinPack(resolveSelectedSkinIndex());
                    }
                }
            }
            state.block();
            return;
        }

        if (ControlsCompat.squareOnce(state)) {
            favorite();
            return;
        }

        if (!ControlType.getActiveType().isKbm() && ControlsCompat.triangleOnce(state)) {
            openLegacyChangeSkinScreen();
            return;
        }

        if (!ControlType.getActiveType().isKbm() && ControlsCompat.r3Once(state)) {
            if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
                PlayerSkinWidget center = playerSkinWidgetList.element3;
                if (!center.isInterpolating()) {
                    center.recenterView();
                    keptCenterRotX = center.getRotationX();
                    keptCenterRotY = center.getRotationY();
                    playClick();
                }
            }
            return;
        }

        if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null && state != null && state.is(ControllerBinding.RIGHT_STICK)
                && state instanceof BindingState.Axis stick) {
            PlayerSkinWidget center = playerSkinWidgetList.element3;

            if (!center.isInterpolating()) {
                final double triggerY = 0.85d, sideLimit = 0.35d;
                double sx = stick.x, sy = stick.y;

                if (Math.abs(sx) <= sideLimit) {
                    if (sy <= -triggerY) {
                        if (!stickUpHeld) {
                            stickUpHeld = true;
                            if (center.isPunchLoop()) {
                                center.setPoseMode(1, false, false);
                            } else {
                                center.togglePunch();
                            }
                            keptCenterPoseMode = center.getPoseMode();
                            keptCenterPunchLoop = center.isPunchLoop();
                            playerSkinWidgetList.setCenterPose(keptCenterPoseMode, keptCenterPunchLoop);
                            playClick();
                        }
                        state.block();
                        return;
                    }
                    if (sy >= triggerY) {
                        if (!stickDownHeld) {
                            stickDownHeld = true;
                            if (center.getPoseMode() == 1) {
                                center.setPoseMode(0, true, false);
                            } else {
                                center.togglePose();
                            }
                            keptCenterPoseMode = center.getPoseMode();
                            keptCenterPunchLoop = center.isPunchLoop();
                            playerSkinWidgetList.setCenterPose(keptCenterPoseMode, keptCenterPunchLoop);
                            playClick();
                        }
                        state.block();
                        return;
                    }
                }

                if (Math.abs(sy) < 0.25d) {
                    stickUpHeld = false;
                    stickDownHeld = false;
                }

                double dz = stick.getDeadZone();
                double dx = dz > Math.abs(sx) ? 0 : -sx * 0.12;
                double dy = dz > Math.abs(sy) ? 0 : -sy * 0.12;

                if (dx != 0 || dy != 0) {
                    center.applyDrag(dx, dy);
                    keptCenterRotX = center.getRotationX();
                    keptCenterRotY = center.getRotationY();
                    state.block();
                    return;
                }
            }
        }

        super.bindingStateTick(state);
    }

    @Override
    public void simulateKeyAction(ControllerManager manager, BindingState state) {
        if (manager.isCursorDisabled)
            manager.simulateKeyAction(s -> s.is(ControllerBinding.DOWN_BUTTON), InputConstants.KEY_RETURN, state);
        manager.simulateKeyAction(s -> s.is(ControllerBinding.RIGHT_BUTTON), InputConstants.KEY_ESCAPE, state, true);
        manager.simulateKeyAction(s -> s.is(ControllerBinding.LEFT_BUTTON), InputConstants.KEY_X, state);
        manager.simulateKeyAction(s -> s.is(ControllerBinding.UP_BUTTON), InputConstants.KEY_O, state);
        if (manager.isCursorDisabled) {
            manager.simulateKeyAction(s -> s.is(ControllerBinding.LEFT_TRIGGER), InputConstants.KEY_PAGEUP, state);
            manager.simulateKeyAction(s -> s.is(ControllerBinding.RIGHT_TRIGGER), InputConstants.KEY_PAGEDOWN, state);
        } else manager.simulateKeyAction(s -> s.is(ControllerBinding.RIGHT_TRIGGER), InputConstants.KEY_W, state);
        manager.simulateKeyAction(s -> s.is(ControllerBinding.TOUCHPAD_BUTTON), InputConstants.KEY_T, state);
        manager.simulateKeyAction(s -> s.is(ControllerBinding.CAPTURE), InputConstants.KEY_F2, state);
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer r) {
        r.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RETURN)
                : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(), () -> Component.literal("Select"));

        r.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_ESCAPE)
                : ControllerBinding.RIGHT_BUTTON.bindingState.getIcon(), () -> Component.translatable("gui.cancel"));

        r.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_F)
                : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(), () -> {
            String id = playerSkinWidgetList != null && playerSkinWidgetList.element3 != null ? playerSkinWidgetList.element3.skinId.get() : null;
            return id != null && wily.legacy.Skins.skin.FavoritesStore.isFavorite(id) ? Component.literal("Remove Favorite") : Component.literal("Add Favorite");
        });

        r.add(() -> ControlType.getActiveType().isKbm()
                ? ControlTooltip.COMPOUND_ICON_FUNCTION.apply(new ControlTooltip.Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_W), ControlTooltip.SPACE_ICON,
                ControlTooltip.getKeyIcon(InputConstants.KEY_A), ControlTooltip.SPACE_ICON, ControlTooltip.getKeyIcon(InputConstants.KEY_S), ControlTooltip.SPACE_ICON,
                ControlTooltip.getKeyIcon(InputConstants.KEY_D)})
                : ControllerBinding.LEFT_STICK.bindingState.getIcon(), () -> Component.literal("Navigate"));

        r.add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O)
                : ControllerBinding.UP_BUTTON.bindingState.getIcon(), () -> Component.literal("Advanced Options"));
    }

    private void syncPackFocus() {
    }

    @Override
    public void tick() {
        super.tick();

        if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
            PlayerSkinWidget center = playerSkinWidgetList.element3;
            if (!center.isInterpolating()) {
                keptCenterRotX = center.getRotationX();
                keptCenterRotY = center.getRotationY();
                playerSkinWidgetList.setCenterRotation(keptCenterRotX, keptCenterRotY);

                keptCenterPoseMode = center.getPoseMode();
                keptCenterPunchLoop = center.isPunchLoop();
                playerSkinWidgetList.setCenterPose(keptCenterPoseMode, keptCenterPunchLoop);
            }
        }

        boolean wasPending = false;
        try {
            wasPending = actions.isPendingSwap();
        } catch (Throwable ignored) {
        }

        try {
            actions.tick();
        } catch (Throwable ignored) {
        }

        boolean isPending = false;
        try {
            isPending = actions.isPendingSwap();
        } catch (Throwable ignored) {
        }

        if ((wasPending || lastPendingSwap) && !isPending) {
            applyTu3CarouselTuning();
        }
        lastPendingSwap = isPending;

        if (packList.consumeQueuedChangePack()) {
            stopHoldingOuterCarousel();
            cancelQueuedCarousel();

            if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
                keptCenterRotX = playerSkinWidgetList.element3.getRotationX();
                keptCenterRotY = playerSkinWidgetList.element3.getRotationY();
                playerSkinWidgetList.setCenterRotation(keptCenterRotX, keptCenterRotY);

                keptCenterPoseMode = playerSkinWidgetList.element3.getPoseMode();
                keptCenterPunchLoop = playerSkinWidgetList.element3.isPunchLoop();
                playerSkinWidgetList.setCenterPose(keptCenterPoseMode, keptCenterPunchLoop);
            }

            int idx = 0;
            UUID self = minecraft.player != null ? minecraft.player.getUUID() : minecraft.getUser() != null ? minecraft.getUser().getProfileId() : null;
            String selectedId = self != null ? ClientSkinCache.get(self) : null;

            if (selectedId != null && !selectedId.isBlank()) {
                SkinPack focused = actions.getFocusedPack();
                if (focused != null && focused.skins() != null) {
                    int limit = Math.min(100, focused.skins().size());
                    for (int i = 0; i < limit; i++) {
                        SkinEntry se = focused.skins().get(i);
                        if (se != null && selectedId.equals(se.id())) {
                            idx = i;
                            break;
                        }
                    }
                }
            }

            skinPack(idx);
            return;
        }

        pumpQueuedCarousel();
        pumpHoldingOuterCarousel();
    }

    @Override
    public void renderDefaultBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
        LegacyRenderUtil.renderDefaultBackground(UIAccessor.of(this), g, false);

        renderTu3TabsBehindStrip(g);

        if (tu3StripH > 0) {
            blitSprite(g, TU3_TOP_STRIP, 0, tu3StripY, Math.max(1, width), Math.max(1, tu3StripH));
        }

        int bgX1 = 0;
        int bgY1 = tu3StripY + tu3StripH;
        int bgX2 = Math.max(1, width);
        int bgY2 = Math.max(bgY1 + 1, tu3BottomStripY);
        g.fill(bgX1, bgY1, bgX2, bgY2, 0x880D0D0D);

        if (tu3BottomStripH > 0) {
            blitSprite(g, TU3_BOTTOM_STRIP, 0, tu3BottomStripY, Math.max(1, width), Math.max(1, tu3BottomStripH));
        }

        int pad = sc(4);
        int clipX1 = layoutX + pad;
        int clipX2 = layoutX + layoutW - pad;
        PlayerSkinWidget.setCarouselClip(clipX1, layoutY + pad, clipX2, tu3BottomStripY - pad);
        PlayerSkinWidget.setCarouselYawDenom(Math.max(1f, 240f * uiScale));

        renderTu3Tabs(g);

        int plateH = Math.max(1, Math.round(16f * uiScale));
        plateH = Math.max(1, plateH * 2);
        int plateY = tu3BottomStripY - Math.max(1, sc(8)) - plateH;
        int minPlateY = tu3StripY + tu3StripH + Math.max(1, sc(4));
        if (plateY < minPlateY) plateY = minPlateY;
        PlayerSkinWidget.setCenterNamePlate(true, tu3TabMidW, plateH, 0, plateY);
        PlayerSkinWidget.setCenterNamePlateCenterX(width / 2);
        PlayerSkinWidget.setCenterNamePlateSprite(TU3_NAME_PLATE);
        int badgeW = Math.max(1, Math.round(tu3TabMidW * 0.52f));
        int badgeH = Math.max(1, Math.round(12f * uiScale));
        int gap = Math.max(0, sc(4));
        PlayerSkinWidget.setCenterSelectedBadge(true, badgeW, badgeH, gap, 0.75f, TU3_SELECTED_BADGE);
    }

    private void drawBigCentered(GuiGraphics g, Component text, int centerX, int y, int color) {
        float scale = 1.485f * uiScale;
        if (scale < 0.65f) scale = 0.65f;
        int yAdj = y - (int) ((scale - 1f) * minecraft.font.lineHeight / 2f);
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) centerX, (float) yAdj);
        pose.scale(scale, scale);
        g.drawCenteredString(minecraft.font, text, 0, 0, color);
        pose.popMatrix();
    }

    private void drawSmallCentered(GuiGraphics g, Component text, int centerX, int y, int color) {
        float scale = 1.045f * uiScale;
        if (scale < 0.6f) scale = 0.6f;
        int yAdj = y - (int) ((scale - 1f) * minecraft.font.lineHeight / 2f);
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) centerX, (float) yAdj);
        pose.scale(scale, scale);
        g.drawCenteredString(minecraft.font, text, 0, 0, color);
        pose.popMatrix();
    }

    @Override
    public void removed() {
        stickUpHeld = stickDownHeld = shiftHeld = pHeld = enterHeld = false;
        draggingCenterDoll = false;
        centerDragMoved = false;
        PlayerSkinWidget.clearCarouselClip();
        PlayerSkinWidget.setCenterNamePlate(false, 1, 1, 0, -1);
        PlayerSkinWidget.setCenterNamePlateCenterX(-1);
        PlayerSkinWidget.setCenterSelectedBadge(false, 1, 1, 0, 1f, TU3_SELECTED_BADGE);
        super.removed();
    }
}
