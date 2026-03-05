package wily.legacy.Skins.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.InputConstants;
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
import wily.legacy.Skins.client.compat.legacy4j.ControlsCompat;
import wily.legacy.Skins.client.screen.changeskin.ChangeSkinActions;
import wily.legacy.Skins.client.screen.changeskin.ChangeSkinPackList;
import wily.legacy.Skins.client.screen.widget.PlayerSkinWidget;
import wily.legacy.Skins.client.screen.widget.PlayerSkinWidgetList;
import wily.legacy.Skins.client.util.SkinPreviewWarmup;
import wily.legacy.Skins.skin.ClientSkinCache;
import wily.legacy.Skins.skin.SkinEntry;
import wily.legacy.Skins.skin.SkinPack;
import wily.legacy.Skins.skin.SkinPackLoader;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.client.screen.Panel;
import wily.legacy.client.screen.PanelVListScreen;
import wily.legacy.client.screen.RenderableVList;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public abstract class AbstractChangeSkinScreen extends PanelVListScreen
        implements wily.legacy.client.controller.Controller.Event, ControlTooltip.Event {

    @Override
    public void tick() {
        super.tick();
        int budget = 2;
        try {
            if (actions != null && actions.isPendingSwap()) budget = 6;
            else if (queuedCarouselSteps > 0 || holdingOuterCarousel || carouselAnimating()) budget = 3;
        } catch (Throwable ignored) {
        }
        try {
            SkinPreviewWarmup.pump(minecraft, budget);
        } catch (Throwable ignored) {
        }
    }

    protected static final int BASE_TOOLTIP_WIDTH = 400;
    protected static final int BASE_PANEL_WIDTH   = 180;
    protected static final int BASE_PANEL_HEIGHT  = 290;

    protected final Minecraft minecraft;

    
    protected final Panel tooltipBox;

    protected final ChangeSkinPackList packList;
    protected final ChangeSkinActions  actions;

    protected float uiScale   = 1f;
    protected int tooltipWidth = BASE_TOOLTIP_WIDTH;

    // Cache icon sizes; avoid image reads
    protected final Map<ResourceLocation, int[]> packIconDims = new HashMap<>();

    
    protected boolean stickUpHeld, stickDownHeld;

    
    protected boolean leftStickUpHeld, leftStickDownHeld;

    
    protected boolean shiftHeld, pHeld, enterHeld;

    
    protected boolean firstOpen = true;



    protected boolean draggingCenterDoll;
    protected boolean centerDragMoved;
    protected double  centerDragStartX, centerDragStartY;

    

    protected float   keptCenterRotX, keptCenterRotY;
    protected int     keptCenterPoseMode;
    protected boolean keptCenterPunchLoop;

    

    protected int     queuedCarouselSteps;
    protected int     queuedCarouselDir;
    protected boolean queuedCarouselSound;

    

    protected boolean holdingOuterCarousel;
    protected int     holdingOuterDir;
    protected long    holdingOuterStartAt, holdingOuterNextAt;



    protected PlayerSkinWidgetList playerSkinWidgetList;



    protected AbstractChangeSkinScreen(Screen parent) {
        super(parent, s -> Panel.centered(s,
                () -> Math.max(1, Math.round(BASE_PANEL_WIDTH  * computeScale(s.width, s.height))),
                () -> Math.max(1, Math.round(BASE_PANEL_HEIGHT * computeScale(s.width, s.height))),
                0, 0), Component.empty());

        minecraft  = Minecraft.getInstance();
        tooltipBox = createTooltipBox();

        renderableVList.layoutSpacing(l -> 2);

        packList = new ChangeSkinPackList(this::playClick, () -> {});

        actions = new ChangeSkinActions(minecraft, packList, new ChangeSkinActions.Host() {
            @Override public PlayerSkinWidgetList getPlayerSkinWidgetList()              { return playerSkinWidgetList; }
            @Override public void setPlayerSkinWidgetList(PlayerSkinWidgetList list) {
                playerSkinWidgetList = list;
                onWidgetListCreated(list);
            }
            @Override public PlayerSkinWidget addSkinWidget(PlayerSkinWidget w)         { return addRenderableWidget(w); }
            @Override public Panel             getTooltipBox()                          { return tooltipBox; }
            @Override public Panel             getPanel()                               { return panel; }
            @Override public Screen            getScreen()                              { return AbstractChangeSkinScreen.this; }
            @Override public float             getUiScale()                             { return uiScale; }
        });

        try {
            SkinPackLoader.ensureLoaded();
        } catch (Throwable ignored) {}

        packList.initFromLoader();
    }



    
    protected abstract Panel createTooltipBox();

    
    protected void onWidgetListCreated(PlayerSkinWidgetList list) {}

    
    protected void onAfterSkinPackChanged() {}

    
    protected abstract boolean insideScrollRegion(double mx, double my);

    
    protected boolean isInCarouselBounds(double mx, double my) {
        return true;
    }

    
    protected abstract boolean handlePackListStepNavigation(int key);

    
    protected void focusPackListItem(Object item) {
        RenderableVList vList = getRenderableVList();
        if (vList == null || item == null) return;
        try {
            String[] names = {"setFocused", "focus", "focusRenderable", "ensureVisible", "scrollTo"};
            for (String n : names)
                for (java.lang.reflect.Method m : vList.getClass().getMethods()) {
                    if (!m.getName().equals(n)) continue;
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 1 && p[0].isAssignableFrom(item.getClass())) {
                        m.invoke(vList, item);
                        return;
                    }
                }
            int idx = item instanceof ChangeSkinPackList.PackButton pb ? pb.getPackIndex() : packList.getFocusedPackIndex();
            String[] idxNames = {"scrollToIndex", "focusIndex", "setFocusedIndex", "scrollTo"};
            for (String n : idxNames)
                for (java.lang.reflect.Method m : vList.getClass().getMethods()) {
                    if (!m.getName().equals(n)) continue;
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 1 && (p[0] == int.class || p[0] == Integer.class)) {
                        m.invoke(vList, idx);
                        return;
                    }
                }
        } catch (Throwable ignored) {}
    }



    
    protected int sc(int v) {
        return Math.round(v * uiScale);
    }

    
    // Scale UI to fit screen
    protected static float computeScale(int w, int h) {
        float sw = (w - 20f) / (BASE_PANEL_WIDTH + BASE_TOOLTIP_WIDTH - 2f);
        float sh = (h - 20f) / BASE_PANEL_HEIGHT;
        float sc = Math.min(1f, Math.min(sw, sh));
        if (sc > 0.8f) sc *= 0.93f;
        sc *= 0.95f;
        if (sc <= 0f) sc = 1f;
        return sc;
    }

    
    // Read dimensions once per icon
    protected int[] packIconDims(ResourceLocation icon) {
        int[] d = packIconDims.get(icon);
        if (d != null) return d;
        int w = 128, h = 128;
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
        } catch (Throwable ignored) {}
        int[] out = {Math.max(1, w), Math.max(1, h)};
        packIconDims.put(icon, out);
        return out;
    }

    protected int previewBoxSize() {
        int min  = Math.max(1, sc(24));
        int size = Math.max(1, sc(112));
        int max  = panel.width - sc(20);
        if (max < size) size = Math.max(min, max);
        return Math.max(1, size);
    }

    protected int previewBoxX() {
        int s     = previewBoxSize();
        int x     = panel.x + sc(34);
        int right = panel.x + panel.width - sc(7);
        if (x + s > right) x = panel.x + Math.max(sc(7), (panel.width - s) / 2);
        return x;
    }

    protected int previewBoxY() {
        int s      = previewBoxSize();
        int y      = panel.y + sc(10);
        int top    = panel.y + sc(5);
        int bottom = panel.y + panel.height - s - sc(5);
        if (bottom < top) bottom = top;
        return Math.max(top, Math.min(y, bottom));
    }



    
    protected void skinPack(int i) {
        if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
            keptCenterRotX    = playerSkinWidgetList.element3.getRotationX();
            keptCenterRotY    = playerSkinWidgetList.element3.getRotationY();
            playerSkinWidgetList.setCenterRotation(keptCenterRotX, keptCenterRotY);
            keptCenterPoseMode   = playerSkinWidgetList.element3.getPoseMode();
            keptCenterPunchLoop  = playerSkinWidgetList.element3.isPunchLoop();
            playerSkinWidgetList.setCenterPose(keptCenterPoseMode, keptCenterPunchLoop);
        }
        actions.skinPack(i);
        onAfterSkinPackChanged();
    }

    
    protected void skinPack() {
        if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
            keptCenterRotX    = playerSkinWidgetList.element3.getRotationX();
            keptCenterRotY    = playerSkinWidgetList.element3.getRotationY();
            playerSkinWidgetList.setCenterRotation(keptCenterRotX, keptCenterRotY);
            keptCenterPoseMode   = playerSkinWidgetList.element3.getPoseMode();
            keptCenterPunchLoop  = playerSkinWidgetList.element3.isPunchLoop();
            playerSkinWidgetList.setCenterPose(keptCenterPoseMode, keptCenterPunchLoop);
        }
        actions.skinPack();
        onAfterSkinPackChanged();
    }



    protected void selectSkin()              { actions.selectSkin(); }
    protected void favorite()                { actions.favorite(); }
    protected void playClick()               { actions.playClick(); }
    protected void openLegacyChangeSkinScreen() { actions.openLegacyChangeSkinScreen(); }



    
    protected int resolveSelectedSkinIndex() {
        UUID self = minecraft.player != null ? minecraft.player.getUUID()
                  : minecraft.getUser() != null ? minecraft.getUser().getProfileId() : null;
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

    protected ResourceLocation getFocusedPackIcon() { return actions.getFocusedPackIcon(); }
    protected SkinPack         getFocusedPack()     { return actions.getFocusedPack(); }



    
    protected boolean control(boolean left, boolean right) {
        if (!(left || right) || playerSkinWidgetList == null) return false;
        if (playerSkinWidgetList.widgets.stream().anyMatch(w -> w.progress <= 1f)) return true;
        playerSkinWidgetList.setCenterRotation(0, 0);
        playerSkinWidgetList.sortForIndex(playerSkinWidgetList.index + (left ? -1 : 0) + (right ? 1 : 0));
        playClick();
        return true;
    }

    
    protected boolean carouselAnimating() {
        if (playerSkinWidgetList == null || playerSkinWidgetList.widgets == null) return false;
        for (PlayerSkinWidget w : playerSkinWidgetList.widgets)
            if (w != null && w.visible && w.progress <= 1f) return true;
        return false;
    }

    
    protected PlayerSkinWidget pickCarouselWidget(double mx, double my) {
        if (playerSkinWidgetList == null || playerSkinWidgetList.widgets == null) return null;
        if (!isInCarouselBounds(mx, my)) return null;
        PlayerSkinWidget best  = null;
        double           bestD = Double.MAX_VALUE;
        for (PlayerSkinWidget w : playerSkinWidgetList.widgets) {
            if (w == null || !w.visible) continue;
            int wx = w.getX(), wy = w.getY(), ww = w.getWidth(), wh = w.getHeight();
            if (!inside(mx, my, wx, wy, ww, wh)) continue;
            double cx = wx + ww * 0.5, cy = wy + wh * 0.5;
            double dx = mx - cx,       dy = my - cy;
            double d  = dx * dx + dy * dy;
            if (d < bestD) { bestD = d; best = w; }
        }
        return best;
    }

    

    // Clear queued carousel steps
    protected void cancelQueuedCarousel() {
        queuedCarouselSteps = 0;
        queuedCarouselDir   = 0;
        queuedCarouselSound = false;
    }

    // Run queued steps when safe
    protected void pumpQueuedCarousel() {
        if (queuedCarouselSteps <= 0) return;
        if (actions != null && actions.isPendingSwap()) return;
        if (carouselAnimating()) return;
        if (playerSkinWidgetList == null) { cancelQueuedCarousel(); return; }
        if (queuedCarouselSound) { queuedCarouselSound = false; playClick(); }
        playerSkinWidgetList.setCenterRotation(0, 0);
        playerSkinWidgetList.sortForIndex(playerSkinWidgetList.index + queuedCarouselDir);
        if (--queuedCarouselSteps <= 0) cancelQueuedCarousel();
    }

    // Batch multi-step carousel moves
    protected void startQueuedCarousel(int offset) {
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
        queuedCarouselDir   = offset > 0 ? 1 : -1;
        queuedCarouselSteps = abs;
        queuedCarouselSound = true;
        pumpQueuedCarousel();
    }

    

    // Hold input: repeat carousel stepping
    protected void startHoldingOuterCarousel(int dir) {
        holdingOuterCarousel = true;
        holdingOuterDir      = dir < 0 ? -1 : 1;
        long now             = net.minecraft.Util.getMillis();
        holdingOuterStartAt  = now;
        holdingOuterNextAt   = now + 220L;
    }

    protected void stopHoldingOuterCarousel() {
        holdingOuterCarousel = false;
        holdingOuterDir      = 0;
        holdingOuterStartAt  = holdingOuterNextAt = 0L;
    }

    protected void pumpHoldingOuterCarousel() {
        if (!holdingOuterCarousel) return;
        if (actions != null && actions.isPendingSwap()) return;
        if (queuedCarouselSteps > 0 || carouselAnimating()) return;
        long now = net.minecraft.Util.getMillis();
        if (now < holdingOuterNextAt) return;
        startQueuedCarousel(holdingOuterDir);
        long held  = now - holdingOuterStartAt;
        long delay = held < 700L ? 120L : 80L;
        holdingOuterNextAt = now + delay;
    }



    @Override
    // Restore focus and selected pack
    protected void init() {
        super.init();

        
        ChangeSkinPackList.PackButton b = packList.getButtonForIndex(packList.getFocusedPackIndex());
        if (b != null && children().contains(b))
            setFocused(b);
        else
            for (var c : children())
                if (c instanceof ChangeSkinPackList.PackButton pb) { setFocused(pb); break; }

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
    // Centralized keyboard navigation handling
    public boolean keyPressed(KeyEvent e) {
        int key = InputConstants.getKey(e).getValue();

        if (key == InputConstants.KEY_RETURN) {
            if (!enterHeld) enterHeld = true;
            return true;
        }
        if (key == InputConstants.KEY_F) { favorite(); return true; }

        if (key == InputConstants.KEY_LSHIFT || key == InputConstants.KEY_RSHIFT) {
            if (!shiftHeld) {
                shiftHeld = true;
                if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null
                        && !playerSkinWidgetList.element3.isInterpolating()) {
                    playerSkinWidgetList.element3.togglePose();
                    keptCenterPoseMode  = playerSkinWidgetList.element3.getPoseMode();
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
                if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null
                        && !playerSkinWidgetList.element3.isInterpolating()) {
                    playerSkinWidgetList.element3.togglePunch();
                    keptCenterPoseMode  = playerSkinWidgetList.element3.getPoseMode();
                    keptCenterPunchLoop = playerSkinWidgetList.element3.isPunchLoop();
                    playerSkinWidgetList.setCenterPose(keptCenterPoseMode, keptCenterPunchLoop);
                    playClick();
                }
            }
            return true;
        }
        if (key == InputConstants.KEY_O) { openLegacyChangeSkinScreen(); return true; }

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
            if (enterHeld) { enterHeld = false; selectSkin(); }
            return true;
        }
        if (key == InputConstants.KEY_LSHIFT || key == InputConstants.KEY_RSHIFT) { shiftHeld = false; return true; }
        if (key == InputConstants.KEY_P)                                           { pHeld    = false; return true; }
        return super.keyReleased(e);
    }



    
    protected boolean handleCarouselMouseClicked(MouseButtonEvent e, boolean bl) {
        double mx = e.x(), my = e.y();
        PlayerSkinWidget hit = pickCarouselWidget(mx, my);
        if (hit == null) return false;

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
            draggingCenterDoll  = true;
            centerDragMoved     = false;
            centerDragStartX    = mx;
            centerDragStartY    = my;
            return true;
        }
        
        
        stopHoldingOuterCarousel();
        startQueuedCarousel(off);
        if (Math.abs(off) == 2) startHoldingOuterCarousel(off);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingCenterDoll && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            PlayerSkinWidget center = playerSkinWidgetList != null ? playerSkinWidgetList.element3 : null;
            if (center != null && center.visible && !center.isInterpolating()) {
                if (!centerDragMoved) {
                    if (Math.abs(event.x() - centerDragStartX) > 2.0 || Math.abs(event.y() - centerDragStartY) > 2.0)
                        centerDragMoved = true;
                }
                if (centerDragMoved) {
                    center.applyDrag(-dragX, 0);
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
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && holdingOuterCarousel)
            stopHoldingOuterCarousel();

        if (draggingCenterDoll && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            draggingCenterDoll = false;
            
            
            
            if (!centerDragMoved) selectSkin();
            centerDragMoved = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (g == 0) return false;

        
        
        
        RenderableVList vList = getRenderableVListAt(d, e);
        if (vList != null) { vList.mouseScrolled(g); return true; }

        if (playerSkinWidgetList == null) return false;
        if (!insideScrollRegion(d, e) && pickCarouselWidget(d, e) == null) return false;

        if (actions != null && actions.isPendingSwap()) return true;
        if (draggingCenterDoll) return true;
        if (carouselAnimating()) return true;

        startQueuedCarousel(g > 0 ? -1 : 1);
        return true;
    }



    
    protected boolean handleSharedBindingState(BindingState state) {
        if (ControlsCompat.squareOnce(state)) { favorite(); return true; }

        if (!ControlType.getActiveType().isKbm() && ControlsCompat.triangleOnce(state)) {
            openLegacyChangeSkinScreen();
            return true;
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
            return true;
        }

        
        
        
        
        if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null
                && state != null && state.is(ControllerBinding.RIGHT_STICK)
                && state instanceof BindingState.Axis stick) {
            PlayerSkinWidget center = playerSkinWidgetList.element3;
            if (!center.isInterpolating()) {
                final double triggerY = 0.85d, sideLimit = 0.35d;
                double sx = stick.x, sy = stick.y;

                if (Math.abs(sx) <= sideLimit) {
                    if (sy <= -triggerY) {
                        if (!stickUpHeld) {
                            stickUpHeld = true;
                            if (center.isPunchLoop()) center.setPoseMode(1, false, false);
                            else center.togglePunch();
                            keptCenterPoseMode  = center.getPoseMode();
                            keptCenterPunchLoop = center.isPunchLoop();
                            playerSkinWidgetList.setCenterPose(keptCenterPoseMode, keptCenterPunchLoop);
                            playClick();
                        }
                        state.block();
                        return true;
                    }
                    if (sy >= triggerY) {
                        if (!stickDownHeld) {
                            stickDownHeld = true;
                            if (center.getPoseMode() == 1) center.setPoseMode(0, true, false);
                            else center.togglePose();
                            keptCenterPoseMode  = center.getPoseMode();
                            keptCenterPunchLoop = center.isPunchLoop();
                            playerSkinWidgetList.setCenterPose(keptCenterPoseMode, keptCenterPunchLoop);
                            playClick();
                        }
                        state.block();
                        return true;
                    }
                }
                if (Math.abs(sy) < 0.25d) { stickUpHeld = false; stickDownHeld = false; }

                double dz = stick.getDeadZone();
                double dx = dz > Math.abs(sx) ? 0 : -sx * 0.12;
                double dy = dz > Math.abs(sy) ? 0 : -sy * 0.12;
                if (dx != 0 || dy != 0) {
                    center.applyDrag(dx, dy);
                    keptCenterRotX = center.getRotationX();
                    keptCenterRotY = center.getRotationY();
                    state.block();
                    return true;
                }
            }
        }

        return false;
    }



    
    protected boolean sharedTick() {
        
        
        if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
            PlayerSkinWidget center = playerSkinWidgetList.element3;
            if (!center.isInterpolating()) {
                keptCenterRotX    = center.getRotationX();
                keptCenterRotY    = center.getRotationY();
                playerSkinWidgetList.setCenterRotation(keptCenterRotX, keptCenterRotY);
                keptCenterPoseMode   = center.getPoseMode();
                keptCenterPunchLoop  = center.isPunchLoop();
                playerSkinWidgetList.setCenterPose(keptCenterPoseMode, keptCenterPunchLoop);
            }
        }

        try { actions.tick(); } catch (Throwable ignored) {}

        
        
        if (packList.consumeQueuedChangePack()) {
            stopHoldingOuterCarousel();
            cancelQueuedCarousel();

            
            
            if (playerSkinWidgetList != null && playerSkinWidgetList.element3 != null) {
                keptCenterRotX    = playerSkinWidgetList.element3.getRotationX();
                keptCenterRotY    = playerSkinWidgetList.element3.getRotationY();
                playerSkinWidgetList.setCenterRotation(keptCenterRotX, keptCenterRotY);
                keptCenterPoseMode   = playerSkinWidgetList.element3.getPoseMode();
                keptCenterPunchLoop  = playerSkinWidgetList.element3.isPunchLoop();
                playerSkinWidgetList.setCenterPose(keptCenterPoseMode, keptCenterPunchLoop);
            }

            
            
            int idx = 0;
            UUID self = minecraft.player != null ? minecraft.player.getUUID()
                      : minecraft.getUser() != null ? minecraft.getUser().getProfileId() : null;
            String selectedId = self != null ? ClientSkinCache.get(self) : null;
            if (selectedId != null && !selectedId.isBlank()) {
                SkinPack focused = actions.getFocusedPack();
                if (focused != null && focused.skins() != null) {
                    int limit = Math.min(100, focused.skins().size());
                    for (int i = 0; i < limit; i++) {
                        SkinEntry se = focused.skins().get(i);
                        if (se != null && selectedId.equals(se.id())) { idx = i; break; }
                    }
                }
            }

            skinPack(idx);
            return true; 
        }

        return false;
    }



    @Override
    public void removed() {
        stickUpHeld = stickDownHeld = leftStickUpHeld = leftStickDownHeld = false;
        shiftHeld = pHeld = enterHeld = false;
        stopHoldingOuterCarousel();
        draggingCenterDoll = centerDragMoved = false;
        PlayerSkinWidget.clearCarouselClip();
        SkinPreviewWarmup.clear();
        super.removed();
    }



    protected static void blitSprite(GuiGraphics g, ResourceLocation id, int x, int y, int w, int h) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, w, h);
    }

    protected static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

        // Match Minecraft font pipeline ordering
    protected void drawBigCentered(GuiGraphics g, Component text, int centerX, int y, int color) {
        float scale = Math.max(0.65f, 1.485f * uiScale);
        int   yAdj  = y - (int) ((scale - 1f) * minecraft.font.lineHeight / 2f);
        var   pose  = g.pose();
        pose.pushMatrix();
        pose.translate((float) centerX, (float) yAdj);
        pose.scale(scale, scale);
        g.drawCenteredString(minecraft.font, text, 0, 0, color);
        pose.popMatrix();
    }

    
    protected void drawSmallCentered(GuiGraphics g, Component text, int centerX, int y, int color) {
        float scale = Math.max(0.6f, 1.045f * uiScale);
        int   yAdj  = y - (int) ((scale - 1f) * minecraft.font.lineHeight / 2f);
        var   pose  = g.pose();
        pose.pushMatrix();
        pose.translate((float) centerX, (float) yAdj);
        pose.scale(scale, scale);
        g.drawCenteredString(minecraft.font, text, 0, 0, color);
        pose.popMatrix();
    }
}
