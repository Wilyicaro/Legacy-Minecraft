package wily.legacy.Skins.client.screen;

import java.lang.reflect.Method;
import java.util.UUID;

import com.mojang.blaze3d.platform.InputConstants;

import wily.legacy.Skins.client.screen.changeskin.ChangeSkinActions;
import wily.legacy.Skins.client.screen.changeskin.ChangeSkinPackList;
import wily.legacy.Skins.client.screen.widget.PlayerSkinWidget;
import wily.legacy.Skins.client.screen.widget.PlayerSkinWidgetList;
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
    private static final ResourceLocation SKIN_PANEL = ResourceLocation.fromNamespaceAndPath(SkinSync.MODID, "tiles/skin_panel"),
            PANEL_FILLER = ResourceLocation.fromNamespaceAndPath(SkinSync.MODID, "tiles/panel_filler"),
            PACK_NAME_BOX = ResourceLocation.fromNamespaceAndPath(SkinSync.MODID, "tiles/pack_name_box"),
            SKIN_BOX = ResourceLocation.fromNamespaceAndPath(SkinSync.MODID, "tiles/skin_box"),
            SIZEABLE_ICON_HOLDER = ResourceLocation.fromNamespaceAndPath("legacy", "textures/gui/sprites/container/sizeable_icon_holder.png"),
            BEACON_CHECK = ResourceLocation.fromNamespaceAndPath("legacy", "textures/gui/sprites/container/beacon_check.png"),
            HEART_CONTAINER = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/heart/container.png"),
            HEART_FULL = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/sprites/hud/heart/full.png");

    private final Minecraft minecraft;
    private final Panel tooltipBox;
    private static final int DESIRED_TOOLTIP_WIDTH = 400;
    private int tooltipWidth = DESIRED_TOOLTIP_WIDTH;
    private final ChangeSkinPackList packList;
    private final ChangeSkinActions actions;

    private boolean stickUpHeld, stickDownHeld, shiftHeld, pHeld, enterHeld, firstOpen = true;


    private boolean draggingCenterDoll;
    private boolean centerDragMoved;
    private double centerDragStartX;
    private double centerDragStartY;

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

    private int previewBoxSize() {
        int min = 24;
        int size = 112;
        int max = panel.width - 20;
        if (max < size) size = Math.max(min, max);
        return Math.max(1, size);
    }

    private int previewBoxX() {
        int s = previewBoxSize();
        int x = panel.x + 34;
        int right = panel.x + panel.width - 7;
        if (x + s > right) x = panel.x + Math.max(7, (panel.width - s) / 2);
        return x;
    }

    private int previewBoxY() {
        int s = previewBoxSize();
        int y = panel.y + 10;
        int top = panel.y + 5;
        int bottom = panel.y + panel.height - s - 5;
        if (bottom < top) bottom = top;
        if (y > bottom) y = bottom;
        if (y < top) y = top;
        return y;
    }

    private void skinPack(int i) {
        actions.skinPack(i);
    }

    private void skinPack() {
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
        super(parent, s -> Panel.centered(s, 180, 290, 0, -10), Component.empty());
        minecraft = Minecraft.getInstance();
        tooltipBox = new Panel(UIAccessor.of(this)) {
            @Override
            public void init(String name) {
                super.init(name);
                appearance(LegacySprites.POINTER_PANEL, tooltipWidth, panel.height - 10);

                pos(panel.x + panel.width - 2, panel.y + 5);
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
        });

        try {
            SkinPackLoader.loadPacks(minecraft.getResourceManager());
        } catch (Throwable ignored) {
        }
        packList.initFromLoader();
    }

    @Override
    public void renderableVListInit() {
        addRenderableOnly((g, i, j, f) -> blitSprite(g, LegacySprites.SQUARE_RECESSED_PANEL, panel.x + 7, panel.y + 129, panel.width - 14, panel.height - 140));
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
                int inner = Math.max(1, s - 2);
                g.blit(RenderPipelines.GUI_TEXTURED, icon, x + 1, y + 1, 0, 0, inner, inner, 128, 128);
            }
        });

        packList.refreshPackIdsIfNeeded();
        packList.populateInto(getRenderableVList());

        int x = panel.x + 11;
        int w = Math.max(1, panel.width - 22);
        int desiredY = panel.y + 136;
        int bottom = panel.y + panel.height - 12;
        int y = Math.min(desiredY, bottom - 44);
        int h = Math.max(1, bottom - y);
        getRenderableVList().init("consoleskins.packList", x, y, w, h);
    }

    @Override
    protected void panelInit() {
        addRenderableOnly(panel);
        panel.init();
        layoutPanels();

        if (firstOpen) {
            String openId = SkinPackLoader.getLastUsedCustomPackId();
            if (openId != null) packList.focusPackId(openId, false);
        }

        tooltipBox.init("tooltipBox");
    }

    private void layoutPanels() {
        int margin = 8;

        int availW = Math.max(1, this.width - margin * 2);
        int availH = Math.max(1, this.height - margin * 2);

        int baseW = 180;
        int baseH = 290;

        int minW = 150;
        int minH = 200;

        panel.width = Math.max(1, Math.min(Math.max(minW, baseW), availW));
        panel.height = Math.max(1, Math.min(Math.max(minH, baseH), availH));

        int tooltipMax = availW - (panel.width - 2);
        int tip = tooltipMax >= 220 ? Math.min(DESIRED_TOOLTIP_WIDTH, tooltipMax) : 0;
        tooltipWidth = Math.max(0, tip);

        int totalW = panel.width - 2 + tooltipWidth;

        int px = (this.width - totalW) / 2;
        int minX = margin;
        int maxX = this.width - totalW - margin;
        if (maxX < minX) maxX = minX;
        if (px < minX) px = minX;
        if (px > maxX) px = maxX;
        panel.x = px;

        int py = (this.height - panel.height) / 2 - 10;
        int minY = margin;
        int maxY = this.height - panel.height - margin;
        if (maxY < minY) maxY = minY;
        if (py < minY) py = minY;
        if (py > maxY) py = maxY;
        panel.y = py;

        tooltipBox.pos(panel.x + panel.width - 2, panel.y + 5);
        tooltipBox.appearance(LegacySprites.POINTER_PANEL, tooltipWidth, tooltipWidth == 0 ? 0 : Math.max(1, panel.height - 10));
    }

    @Override
    protected void init() {
        super.init();

        layoutPanels();

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

        int iconX = tooltipBox.x + tooltipBox.getWidth() - 50;
        int iconY = panel.y + tooltipBox.getHeight() - 60;

        if (inside(mx, my, iconX, iconY + 3, 24, 24)) {
            selectSkin();
            return true;
        }

        if (inside(mx, my, iconX, iconY + 30, 24, 24)) {
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
                            center.togglePunch();
                            playClick();
                        }
                        state.block();
                        return;
                    }
                    if (sy >= triggerY) {
                        if (!stickDownHeld) {
                            stickDownHeld = true;
                            center.togglePose();
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
                double dx = dz > Math.abs(sx) ? 0 : sx * 0.30;
                double dy = dz > Math.abs(sy) ? 0 : sy * 0.30;

                if (dx != 0 || dy != 0) {
                    center.applyDrag(dx, dy);
                    state.block();
                    return;
                }
            }
        }

        super.bindingStateTick(state);
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
            return id != null && wily.legacy.Skins.skin.FavoritesStore.isFavorite(id) ? Component.literal("Unfavourite") : Component.literal("Favourite");
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

        try {
            actions.tick();
        } catch (Throwable ignored) {
        }

        if (packList.consumeQueuedChangePack()) {
            stopHoldingOuterCarousel();
            cancelQueuedCarousel();

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

        blitSprite(g, SKIN_PANEL, tooltipBox.x - 10, panel.y + 7, tooltipBox.getWidth(), tooltipBox.getHeight() - 2);
        blitSprite(g, PANEL_FILLER, tooltipBox.x - 5, panel.y + 16 + tooltipBox.getHeight() - 80, tooltipBox.getWidth() - 14, 60);
        blitSprite(g, LegacySprites.SQUARE_RECESSED_PANEL, tooltipBox.x - 1, panel.y + tooltipBox.getHeight() - 59, tooltipBox.getWidth() - 55, 55);
        blitSprite(g, PACK_NAME_BOX, tooltipBox.x - 5, panel.y + 20, tooltipBox.getWidth() - 18, 40);
        blitSprite(g, SKIN_BOX, tooltipBox.x - 5, panel.y + 16, tooltipBox.getWidth() - 14, tooltipBox.getHeight() - 80);

        int iconX = tooltipBox.x + tooltipBox.getWidth() - 50;
        int iconBaseY = panel.y + tooltipBox.getHeight() - 60;

        g.blit(RenderPipelines.GUI_TEXTURED, SIZEABLE_ICON_HOLDER, iconX, iconBaseY + 3, 0, 0, 24, 24, 24, 24);
        g.blit(RenderPipelines.GUI_TEXTURED, SIZEABLE_ICON_HOLDER, iconX, iconBaseY + 30, 0, 0, 24, 24, 24, 24);

        if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
            String selected = playerSkinWidgetList.element3.skinId.get();
            UUID self = minecraft.player != null ? minecraft.player.getUUID() : minecraft.getUser() != null ? minecraft.getUser().getProfileId() : null;
            String current = self == null ? null : ClientSkinCache.get(self);

            boolean isAuto = "auto_select".equals(selected);
            boolean isAutoActive = current == null || current.isBlank();

            if (selected != null && (selected.equals(current) || (isAuto && isAutoActive))) {
                var pose = g.pose();
                pose.pushMatrix();
                pose.translate(iconX + 12, iconBaseY + 15);
                pose.scale(0.5f, 0.5f);
                pose.translate(-12, -12);
                g.blit(RenderPipelines.GUI_TEXTURED, BEACON_CHECK, 0, 0, 0, 0, 24, 24, 24, 24);
                pose.popMatrix();
            }

            if (selected != null && wily.legacy.Skins.skin.FavoritesStore.isFavorite(selected)) {
                g.blit(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER, iconX + 4, iconBaseY + 34, 0, 0, 16, 16, 16, 16);
                g.blit(RenderPipelines.GUI_TEXTURED, HEART_FULL, iconX + 4, iconBaseY + 34, 0, 0, 16, 16, 16, 16);
            }
        }

        int x = tooltipBox.x;
        int y = panel.y + 45;
        int w = tooltipBox.getWidth() - 23;
        int h = tooltipBox.getHeight() - 90;

        PlayerSkinWidget.setCarouselClip(x + 2, y, x + w, y + h - 24);

        if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
            String skinId = playerSkinWidgetList.element3.skinId.get();
            SkinEntry entry = skinId == null ? null : SkinPackLoader.getSkin(skinId);
            String name = entry == null ? String.valueOf(skinId) : entry.name();

            int mid = tooltipBox.x - 5 + (tooltipBox.getWidth() - 18) / 2;
            drawBigCentered(g, Component.literal(name), mid, panel.y + tooltipBox.getHeight() - 49, 0xFFFFFFFF);
        }

        SkinPack pack = getFocusedPack();
        if (pack != null) {
            int mid = tooltipBox.x - 5 + (tooltipBox.getWidth() - 18) / 2;
            drawBigCentered(g, Component.literal(pack.name()), mid, panel.y + 27, 0xFFFFFFFF);
        }
    }

    private void drawBigCentered(GuiGraphics g, Component text, int centerX, int y, int color) {
        float scale = 1.35f;
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
        super.removed();
    }
}
