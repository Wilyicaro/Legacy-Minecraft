package wily.legacy.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.worldselection.ConfirmExperimentalFeaturesScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Legacy4JClient;
import wily.legacy.util.LegacySprites;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.*;
import wily.legacy.init.LegacySoundEvents;
import wily.legacy.util.ScreenUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static wily.legacy.util.LegacySprites.UNSELECT_HIGHLIGHTED;
import static wily.legacy.util.LegacySprites.UNSELECT;

@Mixin(PackSelectionScreen.class)
public abstract class PackSelectionScreenMixin extends Screen {
    private static final Component INCOMPATIBLE_TITLE = Component.translatable("pack.incompatible").withStyle(ChatFormatting.RED);
    private static final Component INCOMPATIBLE_CONFIRM_TITLE = Component.translatable("pack.incompatible.confirm.title");
    private static final Component AVAILABLE_PACK = Component.translatable("pack.selected.title");
    private static final Component SELECTED_PACK = Component.translatable("pack.available.title");
    @Shadow @Final private PackSelectionModel model;
    @Shadow protected abstract void reload();

    @Shadow private Button doneButton;
    public ControlTooltip.Renderer controlTooltipRenderer = ControlTooltip.defaultScreen(this);

    private Panel panel = Panel.centered(this,410,240);
    private RenderableVList selectedPacksList = new RenderableVList().layoutSpacing(l->0);
    private RenderableVList unselectedPacksList = new RenderableVList().layoutSpacing(l->0);
    protected PackSelectionScreenMixin(Component component) {
        super(component);
    }
    private PackSelectionScreen self(){
        return(PackSelectionScreen)(Object) this;
    }

    @Inject(method = "init",at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        super.init();
        panel.init();
        unselectedPacksList.init(this,panel.x + 15, panel.y + 30, 180, 210);
        selectedPacksList.init(this,panel.x + 215, panel.y + 30, 180, 210);
        this.doneButton = Button.builder(CommonComponents.GUI_DONE, (button) -> this.onClose()).build();
    }
    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        ScreenUtil.renderDefaultBackground(guiGraphics);
        panel.render(guiGraphics, i, j, f);
        RenderSystem.enableBlend();
        guiGraphics.setColor(1.0f,1.0f,1.0f,0.6f);
        ScreenUtil.renderPanelRecess(guiGraphics,panel.x + 10, panel.y + 10, 190, 220,2f);
        guiGraphics.setColor(1.0f,1.0f,1.0f,1.0f);
        RenderSystem.disableBlend();
        ScreenUtil.renderPanelRecess(guiGraphics,panel.x + 210, panel.y + 10, 190, 220,2f);
        guiGraphics.drawString(this.font, SELECTED_PACK, panel.x + 10 + (190 - font.width(SELECTED_PACK)) / 2, panel.y + 18, 0x383838,false);
        guiGraphics.drawString(this.font, AVAILABLE_PACK, panel.x + 210 + (190 - font.width(AVAILABLE_PACK)) / 2, panel.y + 18, 0x383838, false);
        controlTooltipRenderer.render(guiGraphics, i, j, f);
    }
    @Inject(method = "<init>", at = @At("RETURN"))
    public void initConstruct(CallbackInfo info){
        reload();
    }

    @Inject(method = "populateLists", at = @At("HEAD"), cancellable = true)
    private void populateLists(CallbackInfo ci) {
        ci.cancel();
        addPacks(unselectedPacksList,model.getUnselected());
        addPacks(selectedPacksList,model.getSelected());
        repositionElements();
    }
    @Inject(method = "onClose", at = @At("RETURN"))
    public void onClose(CallbackInfo info){
        ScreenUtil.playSimpleUISound(LegacySoundEvents.BACK.get(),1.0f);
    }
    private void addPacks(RenderableVList list,Stream<PackSelectionModel.Entry> stream){
        list.renderables.clear();
        stream.forEach(e-> {
            List<Component> description = new ArrayList<>();
            if (!e.getCompatibility().isCompatible()){
                description.add(INCOMPATIBLE_TITLE);
                description.add(e.getCompatibility().getDescription());
            }
            if (!e.getExtendedDescription().getString().isEmpty()) description.add(e.getExtendedDescription());
            AbstractButton button = new AbstractButton(0,0,180,30,e.getTitle()) {
                @Override
                protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
                    super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
                    RenderSystem.enableBlend();
                    guiGraphics.blit(e.getIconTexture(), getX() + 5, getY() + 5, 0.0f, 0.0f, 20, 20, 20, 20);
                    RenderSystem.disableBlend();
                    if ((minecraft.options.touchscreen().get().booleanValue() || isHovered) && showHoverOverlay()) {
                        guiGraphics.fill(getX() + 5, getY() + 5, getX() + 25, getY() + 25, -1601138544);
                        int p = mouseX - getX();
                        int q = mouseY - getY();
                        if (e.canSelect()) {
                            if (p < 32) {
                                guiGraphics.blitSprite(LegacySprites.JOIN_HIGHLIGHTED, getX(), getY(), 32, 32);
                            } else {
                                guiGraphics.blitSprite(LegacySprites.JOIN, getX(), getY(), 32, 32);
                            }
                        } else {
                            if (e.canUnselect()) {
                                if (p < 16) {
                                    guiGraphics.blitSprite(UNSELECT_HIGHLIGHTED, getX(), getY(), 32, 32);
                                } else {
                                    guiGraphics.blitSprite(UNSELECT, getX(), getY(), 32, 32);
                                }
                            }
                            if (e.canMoveUp()) {
                                if (p < 32 && p > 16 && q < 16) {
                                    guiGraphics.blitSprite(LegacySprites.TRANSFER_MOVE_UP_HIGHLIGHTED, getX(), getY(), 32, 32);
                                } else {
                                    guiGraphics.blitSprite(LegacySprites.TRANSFER_MOVE_UP, getX(), getY(), 32, 32);
                                }
                            }
                            if (e.canMoveDown()) {
                                if (p < 32 && p > 16 && q > 16) {
                                    guiGraphics.blitSprite(LegacySprites.TRANSFER_MOVE_DOWN_HIGHLIGHTED, getX(), getY(), 32, 32);
                                } else {
                                    guiGraphics.blitSprite(LegacySprites.TRANSFER_MOVE_DOWN, getX(), getY(), 32, 32);
                                }
                            }
                        }
                    }
                }
                protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                    ScreenUtil.renderScrollingString(guiGraphics,font,getMessage(),getX() + 30,getY(),getX() + width - 2,getY() + height,e.getCompatibility().isCompatible() ? ScreenUtil.getDefaultTextColor(!isHoveredOrFocused()): 0xFF0000,true);
                }

                @Override
                public void onClick(double i, double j) {
                    double f = i - getX();
                    double g = j - getY();
                    if (this.showHoverOverlay() && f <= 32.0) {
                        if (e.canSelect()) {
                            onPress();
                            return;
                        }
                        if (f < 16.0 && e.canUnselect()) {
                            e.unselect();
                            return;
                        }
                        if (f > 16.0 && g < 16.0 && e.canMoveUp()) {
                            e.moveUp();
                            return;
                        }
                        if (f > 16.0 && g > 16.0 && e.canMoveDown()) {
                            e.moveDown();
                            return;
                        }
                    }
                    if (isFocused()) onPress();
                }
                private boolean showHoverOverlay() {
                    return !e.isFixedPosition() || !e.isRequired();
                }
                @Override
                public void onPress() {
                    if (e.isSelected() && e.canUnselect()){
                        e.unselect();
                        return;
                    }
                    if (e.getCompatibility().isCompatible()) {
                        e.select();
                    } else minecraft.setScreen(new ConfirmationScreen(self(),INCOMPATIBLE_CONFIRM_TITLE, e.getCompatibility().getConfirmation(), (b) -> {
                        e.select();
                        if (minecraft.screen != null) minecraft.screen.onClose();
                    }));
                }
                public boolean keyPressed(int i, int j, int k) {
                    if (Screen.hasShiftDown() || ControllerBinding.LEFT_BUTTON.bindingState.pressed) {
                        switch (i) {
                            case 265 -> {
                                if (e.canMoveUp()) e.moveUp();
                                return false;
                            }
                            case 264 -> {
                                if (e.canMoveDown()) e.moveDown();
                                return false;
                            }
                        }
                    }
                    return super.keyPressed(i, j, k);
                }
                @Override
                protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                    defaultButtonNarrationText(narrationElementOutput);
                }
            };
            if (!description.isEmpty()) button.setTooltip(new MultilineTooltip(description,161));
            list.addRenderable(button);
        });
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (ScreenUtil.isMouseOver(d,e,panel.x + 10, panel.y + 10, 190, 220)) unselectedPacksList.mouseScrolled(g);
        else if (ScreenUtil.isMouseOver(d,e,panel.x + 210, panel.y + 10, 190, 220)) selectedPacksList.mouseScrolled(g);
        return super.mouseScrolled(d, e, f, g);
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (unselectedPacksList.keyPressed(i,true)) return true;
        if (selectedPacksList.keyPressed(i,true)) return true;
        return super.keyPressed(i, j, k);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
    }
}
