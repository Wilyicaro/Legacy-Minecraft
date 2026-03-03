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
import wily.legacy.Skins.client.compat.legacy4j.ControlsCompat;
import wily.legacy.Skins.skin.SkinIds;
import wily.legacy.Skins.skin.SkinIdUtil;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.PanelVListScreen;
import wily.legacy.client.screen.RenderableVList;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

public class ChangeSkinScreen extends PanelVListScreen implements wily.legacy.client.controller.Controller.Event, ControlTooltip.Event {
    private static final ResourceLocation SKIN_PANEL = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/skin_panel"),
            PANEL_FILLER = ResourceLocation.fromNamespaceAndPath(SkinSync.ASSET_NS, "tiles/panel_filler"),
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

    private boolean stickUpHeld, stickDownHeld, leftStickUpHeld, leftStickDownHeld, shiftHeld, pHeld, enterHeld, firstOpen = true;

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

    private static void blitSprite(GuiGraphics g, ResourceLocation id, int x, int y, int w, int h) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, w, h);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
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
        int tw = 128;
        int th = 128;
        try {
            Resource r = minecraft.getResourceManager().getResource(icon).orElse(null);
            if (r != null) {
                try (var in = r.open()) {
                    NativeImage img = NativeImage.read(in);
                    tw = img.getWidth();
                    th = img.getHeight();
                    img.close();
                }
            }
        } catch (Throwable ignored) {
        }

        int fw = tw;
        int fh = th;
        if (th > tw && tw > 0 && th % tw == 0) fh = tw;
        else if (tw > th && th > 0 && tw % th == 0) fw = th;

        int[] out = new int[]{Math.max(1, tw), Math.max(1, th), Math.max(1, fw), Math.max(1, fh)};
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

    private int resolveSelectedSkinIndex() {
        UUID self = minecraft.player != null ? minecraft.player.getUUID() : minecraft.getUser() != null ? minecraft.getUser().getProfileId() : null;
        String selectedId = self != null ? ClientSkinCache.get(self) : null;
        if (selectedId == null || selectedId.isBlank()) return 0;

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

    public ChangeSkinScreen(Screen parent) {
        super(parent, s -> Panel.centered(s,
                () -> Math.max(1, Math.round(BASE_PANEL_WIDTH * computeScale(s.width, s.height))),
                () -> Math.max(1, Math.round(BASE_PANEL_HEIGHT * computeScale(s.width, s.height))),
                0, 0), Component.empty());
        minecraft = Minecraft.getInstance();
        tooltipBox = new Panel(UIAccessor.of(this)) {
            @Override
            public void init(String name) {
                super.init(name);
                panel.x -= (tooltipWidth - 2) / 2;
                int groupWidth = panel.width + tooltipWidth - 2;
                int minX = sc(5);
                int maxX = ChangeSkinScreen.this.width - groupWidth - sc(5);
                if (maxX < minX) maxX = minX;
                if (panel.x < minX) panel.x = minX;
                if (panel.x > maxX) panel.x = maxX;

                int minY = sc(5);
                int maxY = ChangeSkinScreen.this.height - panel.height - sc(5);
                if (maxY < minY) maxY = minY;
                if (panel.y < minY) panel.y = minY;
                if (panel.y > maxY) panel.y = maxY;
                appearance(LegacySprites.POINTER_PANEL, tooltipWidth, panel.height - sc(10));
                pos(panel.x + panel.width - 2, panel.y + sc(18));
            }

            @Override
            public void init() {
                init("tooltipBox");
            }

            @Override
            public void render(GuiGraphics g, int i, int j, float f) {
                LegacyRenderUtil.renderPointerPanel(g, getX(), getY(), getWidth(), getHeight());
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
                return ChangeSkinScreen.this;
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
        addRenderableOnly((g, i, j, f) -> blitSprite(g, LegacySprites.SQUARE_RECESSED_PANEL, panel.x + sc(7), panel.y + sc(129), panel.width - sc(14), panel.height - sc(140)));
        addRenderableOnly((g, i, j, f) -> {
            int x = previewBoxX();
            int y = previewBoxY();
            int s = previewBoxSize();
            blitSprite(g, LegacySprites.SQUARE_RECESSED_PANEL, x, y, s, s);
        });
        addRenderableOnly((g, i, j, f) -> {
            ResourceLocation icon = getFocusedPackIcon();
            if (icon != null) {
                int x = previewBoxX();
                int y = previewBoxY();
                int s = previewBoxSize();
                int ix = x + 1;
                int iy = y + 1;
                int inner = Math.max(1, s - 2);
                int[] d = packIconDims(icon);
                int tw = d[0];
                int th = d[1];
                int fw = d[2];
                int fh = d[3];
                float scale = Math.min(inner / (float) fw, inner / (float) fh);
                float cx = ix + inner / 2f;
                float cy = iy + inner / 2f;
                var pose = g.pose();
                pose.pushMatrix();
                pose.translate(cx, cy);
                pose.scale(scale, scale);
                pose.translate(-fw / 2f, -fh / 2f);
                g.blit(RenderPipelines.GUI_TEXTURED, icon, 0, 0, 0, 0, fw, fh, tw, th);
                pose.popMatrix();
            }
        });

        packList.refreshPackIdsIfNeeded();
        packList.populateInto(getRenderableVList());

        int x = panel.x + sc(11);
        int w = Math.max(1, panel.width - sc(22));
        int desiredY = panel.y + sc(136);
        int py = previewBoxY();
        int ps = previewBoxSize();
        int minY = py + ps + sc(2);
        if (desiredY < minY) desiredY = minY;
        int bottom = panel.y + panel.height - sc(8);
        int y = desiredY;
        int h = Math.max(1, bottom - y);
        getRenderableVList().scrollArrowYOffset(-8 - sc(8));
        getRenderableVList().init("consoleskins.packList", x, y, w, h);
    }

    @Override
    protected void panelInit() {
        uiScale = computeScale(width, height);
        tooltipWidth = Math.max(1, Math.round(BASE_TOOLTIP_WIDTH * uiScale));
        renderableVList.layoutSpacing(l -> 0);
        packList.applyUiScale(uiScale);
        addRenderableOnly(panel);
        panel.init();
        tooltipBox.init("tooltipBox");

        if (firstOpen) {
            String openId = SkinPackLoader.getLastUsedCustomPackId();
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
        boolean kbm = ControlType.getActiveType().isKbm();
        boolean up = (kbm && key == InputConstants.KEY_W) || key == InputConstants.KEY_UP;
        boolean down = (kbm && key == InputConstants.KEY_S) || key == InputConstants.KEY_DOWN;
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

    private boolean insideCarouselPlane(double mx, double my) {
        if (tooltipBox == null || panel == null) return false;
        int x = tooltipBox.x;
        int y = panel.y + sc(45);
        int w = tooltipBox.getWidth() - sc(23);
        int h = tooltipBox.getHeight() - sc(90);
        int x1 = x + sc(2);
        int y1 = y;
        int x2 = x + w;
        int y2 = y + h - sc(24);
        return inside(mx, my, x1, y1, Math.max(1, x2 - x1), Math.max(1, y2 - y1));
    }

    private PlayerSkinWidget pickCarouselWidget(double mx, double my) {
        if (playerSkinWidgetList == null || playerSkinWidgetList.widgets == null) return null;
        if (!insideCarouselPlane(mx, my)) return null;
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

        int holder = Math.max(1, sc(24));
        int iconX = tooltipBox.x + tooltipBox.getWidth() - sc(50);
        int iconY = panel.y + tooltipBox.getHeight() - sc(60);

        if (inside(mx, my, iconX, iconY + sc(3), holder, holder)) {
            selectSkin();
            return true;
        }

        if (inside(mx, my, iconX, iconY + sc(30), holder, holder)) {
            favorite();
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
        if (!inside(d, e, tooltipBox.x, tooltipBox.y, tooltipBox.getWidth(), tooltipBox.getHeight()) && pickCarouselWidget(d, e) == null)
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

        if (!ControlType.getActiveType().isKbm() && state != null && state.is(ControllerBinding.LEFT_STICK) && state instanceof BindingState.Axis stick) {
            final double triggerY = 0.65d, sideLimit = 0.45d;
            double sx = stick.x, sy = stick.y;

            if (Math.abs(sx) <= sideLimit) {
                if (sy <= -triggerY) {
                    if (!leftStickUpHeld) {
                        leftStickUpHeld = true;
                        stepPack(true);
                    }
                    state.block();
                    return;
                }
                if (sy >= triggerY) {
                    if (!leftStickDownHeld) {
                        leftStickDownHeld = true;
                        stepPack(false);
                    }
                    state.block();
                    return;
                }
            }

            if (Math.abs(sy) < 0.25d) {
                leftStickUpHeld = false;
                leftStickDownHeld = false;
            }
        }

        super.bindingStateTick(state);
    }

    private void stepPack(boolean up) {
        int count = packList != null ? packList.getPackCount() : 0;
        if (count <= 1) return;

        int target = packList.getFocusedPackIndex() + (up ? -1 : 1);
        if (target < 0) target = count - 1;
        else if (target >= count) target = 0;

        ChangeSkinPackList.PackButton btn = packList.getButtonForIndex(target);
        if (btn == null) return;

        packList.setFocusedPackIndex(target, true);
        setFocused(btn);
        focusPackListItem(btn);
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
        ChangeSkinPackList.PackButton target = packList.getButtonForIndex(packList.getFocusedPackIndex());
        if (target == null) return;

        var f = getFocused();
        if (f == null || f == getRenderableVList() || f instanceof ChangeSkinPackList.PackButton)
            if (f != target) {
                setFocused(target);
                focusPackListItem(target);
            }
    }

    @Override
    public void tick() {
        super.tick();
        syncPackFocus();

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

        try {
            actions.tick();
        } catch (Throwable ignored) {
        }

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

            actions.skinPack(idx);
            return;
        }

        pumpQueuedCarousel();
        pumpHoldingOuterCarousel();
    }

    @Override
    public void renderDefaultBackground(GuiGraphics g, int mouseX, int mouseY, float pt) {
        LegacyRenderUtil.renderDefaultBackground(UIAccessor.of(this), g, false);

        blitSprite(g, SKIN_PANEL, tooltipBox.x - sc(10), panel.y + sc(7), Math.max(1, tooltipBox.getWidth()), Math.max(1, tooltipBox.getHeight() - sc(2)));
        blitSprite(g, PANEL_FILLER, tooltipBox.x - sc(5), panel.y + sc(16) + tooltipBox.getHeight() - sc(80), Math.max(1, tooltipBox.getWidth() - sc(14)), Math.max(1, sc(60)));
        blitSprite(g, LegacySprites.SQUARE_RECESSED_PANEL, tooltipBox.x - sc(1), panel.y + tooltipBox.getHeight() - sc(59), Math.max(1, tooltipBox.getWidth() - sc(55)), Math.max(1, sc(55)));
        blitSprite(g, PACK_NAME_BOX, tooltipBox.x - sc(5), panel.y + sc(20), Math.max(1, tooltipBox.getWidth() - sc(18)), Math.max(1, sc(40)));
        blitSprite(g, SKIN_BOX, tooltipBox.x - sc(5), panel.y + sc(16), Math.max(1, tooltipBox.getWidth() - sc(14)), Math.max(1, tooltipBox.getHeight() - sc(80)));

        int holder = Math.max(1, sc(24));
        int iconX = tooltipBox.x + tooltipBox.getWidth() - sc(50);
        int iconBaseY = panel.y + tooltipBox.getHeight() - sc(60);

        g.blit(RenderPipelines.GUI_TEXTURED, SIZEABLE_ICON_HOLDER, iconX, iconBaseY + sc(3), 0, 0, holder, holder, 24, 24);
        g.blit(RenderPipelines.GUI_TEXTURED, SIZEABLE_ICON_HOLDER, iconX, iconBaseY + sc(30), 0, 0, holder, holder, 24, 24);

        if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
            String selected = playerSkinWidgetList.element3.skinId.get();
            UUID self = minecraft.player != null ? minecraft.player.getUUID() : minecraft.getUser() != null ? minecraft.getUser().getProfileId() : null;
            String current = self == null ? null : ClientSkinCache.get(self);

            boolean isAuto = "auto_select".equals(selected);
            boolean isAutoActive = current == null || current.isBlank();

            if (selected != null && (selected.equals(current) || (isAuto && isAutoActive))) {
                var pose = g.pose();
                pose.pushMatrix();
                pose.translate(iconX + holder / 2f, iconBaseY + sc(3) + holder / 2f);
                float s = 0.5f * uiScale;
                pose.scale(s, s);
                pose.translate(-12, -12);
                g.blit(RenderPipelines.GUI_TEXTURED, BEACON_CHECK, 0, 0, 0, 0, 24, 24, 24, 24);
                pose.popMatrix();
            }

            if (selected != null && wily.legacy.Skins.skin.FavoritesStore.isFavorite(selected)) {
                int hs = Math.max(1, sc(14));
                int hx = iconX + (holder - hs) / 2;
                int hy = iconBaseY + sc(30) + (holder - hs) / 2;
                blitSprite(g, HEART_CONTAINER, hx, hy, hs, hs);
                blitSprite(g, HEART_FULL, hx, hy, hs, hs);
            }
        }

        int x = tooltipBox.x;
        int y = panel.y + sc(45);
        int w = tooltipBox.getWidth() - sc(23);
        int h = tooltipBox.getHeight() - sc(90);

        PlayerSkinWidget.setCarouselClip(x + sc(2), y, x + w, y + h - sc(24));
        PlayerSkinWidget.setCarouselYawDenom(Math.max(1f, 240f * uiScale));

        if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
            String skinId = playerSkinWidgetList.element3.skinId.get();
            SkinEntry entry = skinId == null ? null : SkinPackLoader.getSkin(skinId);
            String name = entry == null ? String.valueOf(skinId) : entry.name();

            int mid = tooltipBox.x - sc(5) + (tooltipBox.getWidth() - sc(18)) / 2;
            int skinNameY = panel.y + tooltipBox.getHeight() - sc(49);
            drawBigCentered(g, Component.literal(name), mid, skinNameY, 0xFFFFFFFF);

            ResourceLocation modelId = null;
            try {
                String ns = entry != null && entry.texture() != null ? entry.texture().getNamespace() : SkinSync.ASSET_NS;
                modelId = ResourceLocation.fromNamespaceAndPath(ns, skinId);
            } catch (Throwable ignored) {
            }

            String theme = modelId == null ? null : BoxModelManager.getThemeText(modelId);
            if (theme != null && !theme.isBlank() && !theme.equals(name)) {
                float scale = 1.485f * uiScale;
                if (scale < 0.65f) scale = 0.65f;

                int maxPx = Math.max(1, tooltipBox.getWidth() - sc(26));
                int maxUnscaled = (int) (maxPx / scale);
                String show = theme;
                if (minecraft.font.width(show) > maxUnscaled) {
                    int ellW = minecraft.font.width("…");
                    show = minecraft.font.plainSubstrByWidth(show, Math.max(0, maxUnscaled - ellW)) + "…";
                }

                int themeY = skinNameY + (int) (minecraft.font.lineHeight * scale) + sc(6);
                int bottom = panel.y + tooltipBox.getHeight() - sc(12);
                if (themeY > bottom) themeY = bottom;
                drawBigCentered(g, Component.literal(show), mid, themeY, 0xFFFFFFFF);
            }

        }

        SkinPack pack = getFocusedPack();
        if (pack != null) {
            int mid = tooltipBox.x - sc(5) + (tooltipBox.getWidth() - sc(18)) / 2;
            drawBigCentered(g, Component.literal(pack.name()), mid, panel.y + sc(27), 0xFFFFFFFF);
            String t = pack.type();
            if (t != null && !t.isBlank()) {
                String k = t.toLowerCase(Locale.ROOT);
                Component label = null;
                if (k.equals("skin")) label = Component.translatable("legacy.skinpack.type.skin");
                else if (k.equals("mashup")) label = Component.translatable("legacy.skinpack.type.mashup");

                if (label != null) {
                    drawSmallCentered(g, label, mid, panel.y + sc(27) + (int) (minecraft.font.lineHeight * (1.55f * uiScale)) + sc(8), 0xCCFFFFFF);
                }
            }
        }
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
        stickUpHeld = stickDownHeld = leftStickUpHeld = leftStickDownHeld = shiftHeld = pHeld = enterHeld = false;
        draggingCenterDoll = false;
        centerDragMoved = false;
        PlayerSkinWidget.clearCarouselClip();
        super.removed();
    }
}
