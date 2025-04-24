package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.base.client.AdvancedTextWidget;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.Legacy4J;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.controller.LegacyKeyMapping;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.function.Function;

import static wily.legacy.util.LegacyComponents.NONE;
import static wily.legacy.util.LegacyComponents.SELECTION;

public class LegacyKeyMappingScreen extends PanelVListScreen {
    protected AdvancedTextWidget mappingTooltipLines = new AdvancedTextWidget(accessor).withShadow(false);
    protected LegacyKeyMapping selectedMapping = null;
    protected ArbitrarySupplier<Component> mappingTooltip = ArbitrarySupplier.empty();

    public LegacyKeyMappingScreen(Screen parent) {
        this(parent, Component.translatable("controls.keybinds.title"));
    }

    public LegacyKeyMappingScreen(Screen parent, Component title) {
        this(parent, s-> Panel.centered(s, LegacySprites.PANEL,255, 293), title);
    }

    public LegacyKeyMappingScreen(Screen parent, Function<Screen, Panel> panelFunction, Component title) {
        super(parent, panelFunction, title);
        renderableVList.layoutSpacing(l->1);
        addButtons();
    }

    public void addButtons(){
        KeyMapping[] keyMappings = ArrayUtils.clone(Minecraft.getInstance().options.keyMappings);
        Arrays.sort(keyMappings);
        String lastCategory = null;
        renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.reset_defaults"),button -> minecraft.setScreen(new ConfirmationScreen(this, Component.translatable("legacy.menu.reset_keyBinds"),Component.translatable("legacy.menu.reset_keyBinds_message"), b-> {
            for (KeyMapping keyMapping : keyMappings)
                keyMapping.setKey(keyMapping.getDefaultKey());
            KeyMapping.resetMapping();
            Minecraft.getInstance().options.save();
            minecraft.setScreen(this);
        }))).size(240,20).build());
        renderableVList.addOptions(LegacyOptions.unbindConflictingKeys,LegacyOptions.of(Minecraft.getInstance().options.toggleCrouch()),LegacyOptions.of(Minecraft.getInstance().options.toggleSprint()));
        for (KeyMapping keyMapping : keyMappings) {
            String category = keyMapping.getCategory();
            if (!Objects.equals(lastCategory, category))
                renderableVList.addRenderables(SimpleLayoutRenderable.create(240, 13, (l -> ((graphics, i, j, f) -> {}))), SimpleLayoutRenderable.create(240, 13, (l -> ((graphics, i, j, f) -> graphics.drawString(font, Component.translatable(category), l.x + 1, l.y + 4, CommonColor.INVENTORY_GRAY_TEXT.get(), false)))));
            lastCategory = keyMapping.getCategory();
            renderableVList.addRenderable(new MappingButton(0,0,240,20, LegacyKeyMapping.of(keyMapping)) {
                @Override
                public ControlTooltip.ComponentIcon getIcon() {
                    return ControlTooltip.getKeyIcon(mapping.getKey().getValue());
                }

                @Override
                public boolean isNone() {
                    return mapping.getKey() == InputConstants.UNKNOWN;
                }

                @Override
                public void onPress() {
                    if (Screen.hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.state().pressed) {
                        setAndUpdateKey(keyMapping, keyMapping.getDefaultKey());
                        setAndUpdateMappingTooltip(ArbitrarySupplier.empty());
                    } else {
                        setSelectedMapping(mapping);
                        setAndUpdateMappingTooltip(LegacyKeyMappingScreen.this::getCancelTooltip);
                    }
                }
            });
        }
    }

    public abstract class MappingButton extends AbstractButton {
        public final LegacyKeyMapping mapping;

        public MappingButton(int i, int j, int k, int l, LegacyKeyMapping mapping) {
            super(i, j, k, l, mapping.getDisplayName());
            this.mapping = mapping;
        }

        public abstract ControlTooltip.ComponentIcon getIcon();

        public abstract boolean isNone();

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
            if (!isFocused() && isPressed()) setSelectedMapping(null);
            super.renderWidget(guiGraphics, i, j, f);
            Component c = isPressed() ? SELECTION : isNone() ? NONE : null;
            if (c != null){
                guiGraphics.drawString(font, c, getX() + width - 20 - Minecraft.getInstance().font.width(c) / 2, getY() + (height -  font.lineHeight) / 2 + 1, ScreenUtil.getDefaultTextColor(!isHoveredOrFocused()));
                return;
            }
            ControlTooltip.Icon icon = getIcon();
            FactoryScreenUtil.enableBlend();
            icon.render(guiGraphics, getX() + width - 20 - icon.render(guiGraphics,0,0,false,true) / 2, getY() + (height -  font.lineHeight) / 2 + 1,false,false);
            FactoryScreenUtil.disableBlend();
        }

        private boolean isPressed(){
            return selectedMapping != null && mapping == selectedMapping;
        }

        @Override
        protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
            ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), this.getX() + 8, this.getY(), getX() + getWidth(), this.getY() + this.getHeight(), j,true);
        }
        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }
    }

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        if (selectedMapping != null && allowsKey()) {
            setAndUpdateKey(selectedMapping.self(), InputConstants.Type.MOUSE.getOrCreate(i));
            setAndUpdateMappingTooltip(ArbitrarySupplier.empty());
            resolveConflictingMappings();
            setSelectedMapping(null);
            return true;
        }
        return super.mouseClicked(d, e, i);
    }

    public static void setAndUpdateKey(KeyMapping key, InputConstants.Key input){
        key.setKey(input);
        Minecraft.getInstance().options.save();
        KeyMapping.resetMapping();
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (selectedMapping != null && allowsKey()){
            setAndUpdateKey(selectedMapping.self(), i == 256 ? InputConstants.UNKNOWN : InputConstants.getKey(i, j));
            setAndUpdateMappingTooltip(ArbitrarySupplier.empty());
            resolveConflictingMappings();
            setSelectedMapping(null);
            return true;
        }
        return super.keyPressed(i, j, k);
    }

    public boolean allowsKey(){
        return true;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener guiEventListener) {
        super.setFocused(guiEventListener);
        setAndUpdateMappingTooltip(ArbitrarySupplier.empty());
    }

    @Override
    public void renderDefaultBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(accessor, guiGraphics, false);
    }

    public Component getCancelTooltip(){
        return Component.translatable("legacy.options.keyMappingTooltip", ControlTooltip.CANCEL_BINDING.get().getComponent());
    }

    public Component getConflictingTooltip(){
        return LegacyComponents.CONFLICTING_KEYS;
    }

    protected void setSelectedMapping(LegacyKeyMapping keyMapping){
        this.selectedMapping = keyMapping;
    }

    protected boolean areConflicting(LegacyKeyMapping keyMapping, LegacyKeyMapping comparison){
        return keyMapping.getKey() == comparison.getKey();
    }

    public boolean unbindConflictingBindings(){
        return LegacyOptions.unbindConflictingKeys.get();
    }

    protected void setNone(LegacyKeyMapping keyMapping){
        setAndUpdateKey(keyMapping.self(), InputConstants.UNKNOWN);
    }

    protected void resolveConflictingMappings(){
        for (Renderable renderable : getRenderableVList().renderables) {
            if (renderable instanceof MappingButton b && selectedMapping != b.mapping && !b.isNone() && areConflicting(selectedMapping, b.mapping)){
                getRenderableVList().focusRenderable(b);
                if (unbindConflictingBindings()) setNone(b.mapping);
                setAndUpdateMappingTooltip(this::getConflictingTooltip);
                break;
            }
        }
    }

    protected void setAndUpdateMappingTooltip(ArbitrarySupplier<Component> tooltip){
        mappingTooltip = tooltip;
        updateMappingTooltip();
    }

    protected void updateMappingTooltip(){
        mappingTooltipLines.withLines(Collections.emptyList());
        mappingTooltip.ifPresent(c-> mappingTooltipLines.withLines(c, 120));
    }

    @Override
    protected void init() {
        updateMappingTooltip();
        addRenderableOnly(((guiGraphics, i, j, f) -> {
            if (getFocused() instanceof MappingButton b && !mappingTooltipLines.getLines().isEmpty()) {
                int tooltipHeight = mappingTooltipLines.getHeight() + 18;
                int tooltipX = panel.getX() + panel.getWidth() - 2;
                int tooltipY = Math.max(panel.getY() + 2, Math.min(b.getY() + (b.getHeight() - tooltipHeight) / 2, panel.getY() + panel.getHeight() - tooltipHeight - 2));
                ScreenUtil.renderPointerPanel(guiGraphics, tooltipX, tooltipY, 129,  tooltipHeight);
                mappingTooltipLines.setPosition(tooltipX + 4, tooltipY + 9);
                mappingTooltipLines.render(guiGraphics, i, j, f);
            }
        }));
        super.init();
        addRenderableOnly(((guiGraphics, i, j, f) -> guiGraphics.drawString(font, FactoryAPIPlatform.getModInfo("minecraft").getVersion() + " " + Legacy4J.VERSION.get(),panel.getX() + panel.getWidth() + 81, panel.getY() + panel.getHeight() - 7,CommonColor.INVENTORY_GRAY_TEXT.get(),false)));
    }

    @Override
    public void renderableVListInit() {
        getRenderableVList().init(panel.x + 7,panel.y + 6,panel.width - 14,panel.height-20);
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.replace(0, i-> i, c-> selectedMapping == null ? c : null);
        renderer.replace(1, i-> i, c-> selectedMapping == null ? c : null);
        renderer.replace(2, i-> i, c-> selectedMapping == null ? c : null);
        renderer.replace(3, i-> i, c-> selectedMapping == null ? c : null);
        renderer.add(ControlTooltip.CANCEL_BINDING::get, ()-> selectedMapping == null ? null : LegacyComponents.CANCEL);
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if (selectedMapping != null) {
            state.block();
            if (state.is(ControllerBinding.BACK) && state.pressed){
                setAndUpdateKey(selectedMapping.self(), InputConstants.UNKNOWN);
                setAndUpdateMappingTooltip(ArbitrarySupplier.empty());
                setSelectedMapping(null);
            }
        }
    }
}
