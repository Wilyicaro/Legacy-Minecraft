package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyResourceManager;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class KeyboardScreen extends OverlayPanelScreen {
    public static final Component KEYBOARD = Component.translatable("legacy.menu.keyboard");
    private final Supplier<GuiEventListener> listenerSupplier;
    protected RenderableVList renderableVList = new RenderableVList(accessor).layoutSpacing(l->1);
    protected RenderableVList leftKeyBar = new RenderableVList(accessor).layoutSpacing(l->0);
    protected RenderableVList rightKeyBar = new RenderableVList(accessor).layoutSpacing(l->0);
    public boolean shift = false;
    protected boolean shiftLock = false;
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    protected final KeyButton shiftButton;
    protected final KeyButton upButton;
    protected final KeyButton downButton;
    protected final KeyButton rightButton;
    protected final KeyButton leftButton;
    protected final KeyButton backspaceButton;
    protected final KeyButton confirmButton;
    protected int lastX = 0;
    protected int lastY = 0;
    protected int xDiff = 0;
    protected int yDiff = 0;
    public KeyboardScreen(Supplier<GuiEventListener> listener, Screen parent){
        this(60, listener,parent);
    }
    public KeyboardScreen(int yOffset, Supplier<GuiEventListener> listener, Screen parent){
        this((s,p)->p.centeredLeftPos(s),(s,p)-> Math.min(p.centeredTopPos(s) + yOffset,s.height-p.height), listener,parent);
    }
    public KeyboardScreen(BiFunction<Screen,Panel,Integer> leftPosGetter, BiFunction<Screen,Panel,Integer> topPosGetter, Supplier<GuiEventListener> listener, Screen parent){
        this(s-> Panel.createPanel(s, p->p.appearance(LegacySprites.PANEL, 385, 154), p-> p.pos(leftPosGetter.apply(s,p), topPosGetter.apply(s,p))), listener,parent);
    }
    public KeyboardScreen(Function<Screen,Panel> panelConstructor, Supplier<GuiEventListener> listener, Screen parent) {
        super(parent, panelConstructor, CommonComponents.EMPTY);
        this.listenerSupplier = listener;
        renderableVList.forceWidth = false;
        transparentBackground = false;
        LegacyResourceManager.keyboardButtonBuilders.forEach(b-> renderableVList.addRenderable(b.build(this)));
        leftKeyBar.addRenderable(leftButton = new KeyButton(InputConstants.KEY_LEFT,listenerSupplier,ControllerBinding.LEFT_BUMPER,LegacySprites.SCROLL_LEFT));
        rightKeyBar.addRenderable(rightButton = new KeyButton(InputConstants.KEY_RIGHT,listenerSupplier,ControllerBinding.RIGHT_BUMPER,LegacySprites.SCROLL_RIGHT));
        leftKeyBar.addRenderable(upButton = new KeyButton(InputConstants.KEY_UP,20,listenerSupplier,null,LegacySprites.SCROLL_UP));
        leftKeyBar.addRenderable(downButton = new KeyButton(InputConstants.KEY_DOWN,20,listenerSupplier,null,LegacySprites.SCROLL_DOWN));
        leftKeyBar.addRenderable(shiftButton = new KeyButton(InputConstants.KEY_LSHIFT,listenerSupplier,LegacyResourceManager.shiftBinding,LegacySprites.SHIFT){
            long lastRelease;

            @Override
            public boolean playSoundOnClick() {
                return pressTime == 0 && !shiftLock;
            }

            @Override
            public void onRelease() {
                long millis = Util.getMillis();
                if (!shiftLock){
                    if (pressTime >= 6 || millis - lastRelease <= 300) {
                        ScreenUtil.playSimpleUISound(LegacyRegistries.SHIFT_LOCK.get(),1.0f);
                        shiftLock = true;
                    }
                    shift = !shift || shiftLock;
                }else {
                    shiftLock = false;
                    ScreenUtil.playSimpleUISound(LegacyRegistries.SHIFT_UNLOCK.get(),1.0f);
                }
                lastRelease = millis;
                super.onRelease();
            }

            @Override
            public ResourceLocation getSprite() {
                return shiftLock ? LegacySprites.BUTTON_SLOT_SELECTED : super.getSprite();
            }

        });
        rightKeyBar.addRenderable(backspaceButton = new KeyButton(InputConstants.KEY_BACKSPACE,listenerSupplier,ControllerBinding.LEFT_BUTTON,LegacySprites.BACK){
            @Override
            public SoundEvent getDownSoundEvent() {
                return LegacyRegistries.BACKSPACE.get();
            }
        });
        rightKeyBar.addRenderable(confirmButton = new KeyButton(InputConstants.KEY_RETURN,listenerSupplier,ControllerBinding.START,TickBox.TICK){
            @Override
            public void onPress() {
                onClose();
            }
        });
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.add(()-> ControlType.getActiveType().isKbm() ? null : ControllerBinding.RIGHT_STICK.bindingState.getIcon(), ()-> LegacyComponents.MOVE_KEYBOARD);
    }

    public static boolean isOpenKey(int i){
        return i == InputConstants.KEY_NUMPADENTER && !Legacy4JClient.controllerManager.isControllerTheLastInput || i == InputConstants.KEY_RETURN && Legacy4JClient.controllerManager.isControllerTheLastInput;
    }

    public static KeyboardScreen fromStaticListener(GuiEventListener listener, Screen parent){
        return listener instanceof LayoutElement e ? new KeyboardScreen((s,p)->Math.max(0,Math.min(e.getX() + (e.getWidth() - p.width) / 2,s.width - p.width)), (s,p)->Math.max(0,Math.min(s.height - (e.getY() + e.getHeight()) >= p.height ? e.getY() + e.getHeight() + 4 : e.getY() - p.getHeight() - 4, s.height - p.height)),()->listener,parent) : new KeyboardScreen(()->listener,parent);
    }

    @Override
    protected void init() {
        panel.init();
        panel.setPosition((lastX = panel.getX()) + xDiff,(lastY = panel.getY()) + yDiff);
        renderableVList.init(panel.getX() + (panel.getWidth() - 259) / 2,panel.getY() + 28,268,124);
        leftKeyBar.init(panel.getX() + 6,panel.getY() + 27,50,0);
        rightKeyBar.init(panel.getX() + panel.getWidth() - 56,panel.getY() + 27,50,0);
        if (getFocused() == null && !renderableVList.renderables.isEmpty() && renderableVList.renderables.get(0) instanceof GuiEventListener l) setFocused(l);
    }

    @Override
    public void resize(Minecraft minecraft, int i, int j) {
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        if (getFocused() instanceof CharButton c) c.renderTooltip(guiGraphics, i, j, f);
    }

    @Override
    public boolean charTyped(char c, int i) {
        for (Renderable renderable : renderableVList.renderables) {
            if (renderable instanceof CharButton b && b.matches(c)){
                setFocused(b);
                break;
            }
        }
        return super.charTyped(c, i);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (renderableVList.keyPressed(i)) return true;
        return super.keyPressed(i, j, k);
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        super.renderDefaultBackground(guiGraphics, i, j, f);
        RenderSystem.enableBlend();
        FactoryGuiGraphics.of(guiGraphics).setColor(1f,1f,1f,0.8f);
        panel.render(guiGraphics,i,j,f);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(panel.getX() + 4.5f,panel.getY() + 25.5,0);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,0,0,53, 123);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,panel.getWidth() - 62,0,53, 123);
        guiGraphics.pose().translate(-4.5f,0,0);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL_RECESS,(panel.getWidth() - 267) / 2,-1,267, 125);
        guiGraphics.pose().popPose();
        RenderSystem.disableBlend();
        FactoryGuiGraphics.of(guiGraphics).clearColor();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(panel.getX() + (panel.getWidth() - font.width(KEYBOARD) * 1.5f) / 2,panel.getY() + 8,0);
        guiGraphics.pose().scale(1.5f,1.5f,1.5f);
        guiGraphics.drawString(font,KEYBOARD,0,0, CommonColor.INVENTORY_GRAY_TEXT.get(),false);
        guiGraphics.pose().popPose();
    }

    public record CharButtonBuilder(int width, String chars, String shiftChars, ControllerBinding binding, ResourceLocation iconSprite, SoundEvent downSound){
        public CharButton build(KeyboardScreen screen){
            return screen.new CharButton(width,screen.listenerSupplier,chars,shiftChars, binding, iconSprite, downSound);
        }
    }
    public class CharButton extends ActionButton {
        private final Supplier<GuiEventListener> charListener;
        private final String chars;
        private final String shiftChars;
        private final SoundEvent downSound;
        private int selectedChar = 0;


        public CharButton(int width, Supplier<GuiEventListener> charListener,String chars, String shiftChars, ControllerBinding binding, ResourceLocation iconSprite, SoundEvent downSound) {
            super(width,20, CommonComponents.EMPTY, binding, iconSprite);
            this.charListener = charListener;
            this.chars = chars;
            this.shiftChars = shiftChars;
            this.downSound = downSound;
        }
        public boolean matches(char c){
            return chars.contains(String.valueOf(c)) || (shiftChars != null && shiftChars.contains(String.valueOf(c)));
        }

        public void renderTooltip(GuiGraphics guiGraphics, int i, int j, float f){
            if (pressTime >= 6 && getSelectedChars().length() > 1){
                int width = 18;
                char[] chars = getSelectedChars().toCharArray();
                for (int i1 = 0; i1 < chars.length; i1++) {
                    String s = String.valueOf(chars[i1]);
                    width += font.width(s) + (i1 == 0 ? 0 : 2);
                }
                int diffX = 0;
                ScreenUtil.renderPointerPanel(guiGraphics, getX() + (getWidth() - width) / 2, getY() - 17,width,15);
                for (char c : chars) {
                    String s = String.valueOf(c);
                    guiGraphics.drawString(font,s,getX() + (getWidth() - width) / 2 + diffX + 9, getY() - 14,c == getSelectedChar() ? 0xFFFF00 : 0xFFFFFF);
                    diffX += font.width(s) + 2;
                }
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.LEFT,getX() + (getWidth() - width) / 2 + 2, getY() - 15);
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.RIGHT,getX() + (getWidth() - width) / 2 + width -9, getY() - 15);
            }
        }

        @Override
        public SoundEvent getDownSoundEvent() {
            return downSound == null ? super.getDownSoundEvent() : downSound;
        }

        @Override
        public boolean keyPressed(int i, int j, int k) {
            if ((i == InputConstants.KEY_LEFT || i == InputConstants.KEY_RIGHT) && pressTime >= 6){
                selectedChar = Stocker.cyclic(0,selectedChar + (i == InputConstants.KEY_RIGHT ? 1 : -1),getSelectedChars().length());
                return true;
            }
            return super.keyPressed(i, j, k);
        }

        @Override
        public void setFocused(boolean bl) {
            super.setFocused(bl);
            if (!bl && pressTime > 0) onRelease();
        }

        @Override
        public void onRelease() {
            GuiEventListener l = charListener.get();
            if (l != null) {
                parent.setFocused(l);
                l.charTyped(getSelectedChar(), 0);
            }
            if (shiftChars != null && shift && !shiftLock) shift = false;
            selectedChar = 0;
            super.onRelease();
        }

        public char getSelectedChar(){
            String characters = getSelectedChars();
            return characters.charAt(characters.length() > selectedChar ? selectedChar : 0);
        }
        public String getSelectedChars(){
            return shiftChars != null && (hasShiftDown() || shift) ? shiftChars : chars;
        }

        @Override
        public Component getMessage() {
            return Component.literal(String.valueOf(getSelectedChar()));
        }
    }
    public static class KeyButton extends ActionButton {

        public final int key;

        private final Supplier<GuiEventListener> keyListener;

        public KeyButton(int key, Supplier<GuiEventListener> keyListener, ControllerBinding binding, ResourceLocation iconSprite){
            this(key,40,keyListener, binding, iconSprite);
        }
        public boolean playSoundOnClick() {
            return true;
        }
        public KeyButton(int key, int height, Supplier<GuiEventListener> keyListener, ControllerBinding binding, ResourceLocation iconSprite) {
            super(50, height, CommonComponents.EMPTY, binding, iconSprite);
            this.key = key;
            this.keyListener = keyListener;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
            FactoryGuiGraphics.of(guiGraphics).blitSprite(getSprite(),getX(),getY(),getWidth(),getHeight());
            RenderSystem.enableBlend();
            renderString(guiGraphics,Minecraft.getInstance().font, ScreenUtil.getDefaultTextColor(!isHoveredOrFocused()));
            RenderSystem.disableBlend();
        }
        public ResourceLocation getSprite(){
            return isHoveredOrFocused() ? LegacySprites.BUTTON_SLOT_HIGHLIGHTED : LegacySprites.BUTTON_SLOT;
        }
        @Override
        public void onPress() {
            super.onPress();
            GuiEventListener l = keyListener.get();
            if (l != null) {
                l.setFocused(true);
                l.keyPressed(key, 0, 0);
            }
        }
    }
    public static abstract class ActionButton extends AbstractButton{
        public final ControllerBinding binding;
        private final ResourceLocation iconSprite;
        public int pressTime = 0;

        public ActionButton(int k, int l, Component component, ControllerBinding binding, ResourceLocation iconSprite) {
            super(0, 0, k, l, component);
            this.binding = binding;
            this.iconSprite = iconSprite;
        }
        public void onRelease(){
            pressTime = 0;
        }

        @Override
        public void onPress() {
            pressTime++;
        }

        @Override
        public void playDownSound(SoundManager soundManager) {
            if (playSoundOnClick()) ScreenUtil.playSimpleUISound(getDownSoundEvent(),1.0f);
        }
        public boolean playSoundOnClick(){
            return pressTime == 0;
        }
        public SoundEvent getDownSoundEvent(){
            return LegacyRegistries.ACTION.get();
        }

        @Override
        public void onRelease(double d, double e) {
            if (pressTime > 0) onRelease();
        }


        @Nullable
        @Override
        public ComponentPath nextFocusPath(FocusNavigationEvent focusNavigationEvent) {
            return super.nextFocusPath(focusNavigationEvent);
        }

        @Override
        public boolean keyReleased(int i, int j, int k) {
            if (this.active && this.visible && CommonInputs.selected(i) && pressTime > 0) {
                this.onRelease();
                return true;
            } else {
                return false;
            }
        }

        protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
            int bindingOffset = 0;

            if (binding != null && Legacy4JClient.controllerManager.connectedController != null) bindingOffset = binding.bindingState.getIcon().render(guiGraphics,getX() + i, getY() + (getHeight() - 9) / 2 + 1,true,false);

            if (iconSprite == null) renderScrollingString(guiGraphics, font, this.getMessage(), this.getX() + i + bindingOffset, this.getY(), this.getX() + this.getWidth() - i, this.getY() + this.getHeight(), j);
            else {
                TextureAtlasSprite sprite = FactoryGuiGraphics.getSprites().textureAtlas.texturesByName.getOrDefault(iconSprite,null);
                if (sprite == null) return;
                try (SpriteContents contents = sprite.contents()){
                    RenderSystem.enableBlend();
                    FactoryGuiGraphics.of(guiGraphics).blitSprite(iconSprite, getX() + (getWidth() - contents.width()) / 2 + Math.max(0,i + bindingOffset -  (getWidth() - contents.width()) / 2), getY() + (getHeight() - contents.height()) / 2,contents.width(),contents.height());
                    RenderSystem.disableBlend();
                }
            }
        }
        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
    }

    @Override
    public void bindingStateTick(BindingState state) {
        children().forEach(r->  {
            if (r instanceof ActionButton a && a.binding == state.binding){
                if (state.canClick()) {
                    a.playDownSound(minecraft.getSoundManager());
                    a.onPress();
                }else if (state.released) a.onRelease();
            }
        });
        if (state.is(ControllerBinding.RIGHT_STICK) && state instanceof BindingState.Axis a && state.canClick(20)){
            if (state.canClick()) ScreenUtil.playSimpleUISound(LegacyRegistries.SCROLL.get(),1.0f);
            xDiff = Math.max(0, Math.min(panel.getX() + Math.round(a.x*4), width - panel.getWidth())) - lastX;
            yDiff = Math.max(0, Math.min(panel.getY() + Math.round(a.y*4), height - panel.getHeight())) - lastY;
            repositionElements();
        }
    }
    @Override
    public boolean onceClickBindings() {
        return false;
    }
}
