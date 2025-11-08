package wily.legacy.mixin.base.client.packselection;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.*;
import wily.legacy.util.client.LegacyRenderUtil;
import wily.legacy.util.client.LegacySoundUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static wily.legacy.util.LegacySprites.UNSELECT_HIGHLIGHTED;
import static wily.legacy.util.LegacySprites.UNSELECT;

@Mixin(PackSelectionScreen.class)
public abstract class PackSelectionScreenMixin extends Screen implements ControlTooltip.Event, RenderableVList.Access {
    private static final Component INCOMPATIBLE_TITLE = Component.translatable("pack.incompatible").withStyle(ChatFormatting.RED);
    private static final Component INCOMPATIBLE_CONFIRM_TITLE = Component.translatable("pack.incompatible.confirm.title");
    private static final Component AVAILABLE_PACK = Component.translatable("pack.selected.title");
    private static final Component SELECTED_PACK = Component.translatable("pack.available.title");
    @Shadow
    @Final
    private PackSelectionModel model;
    @Shadow
    private Button doneButton;
    @Shadow
    @Final
    private Path packDir;
    @Unique
    private final Panel panel = Panel.centered(this, 410, 240);
    @Unique
    private final RenderableVList selectedPacksList = new RenderableVList(this).layoutSpacing(l -> 0);
    @Unique
    private final RenderableVList unselectedPacksList = new RenderableVList(this).layoutSpacing(l -> 0);
    @Unique
    private final List<RenderableVList> renderableVLists = List.of(unselectedPacksList, selectedPacksList);

    protected PackSelectionScreenMixin(Component component) {
        super(component);
    }

    @Shadow
    protected abstract void reload();

    private PackSelectionScreen self() {
        return (PackSelectionScreen) (Object) this;
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    public void init(CallbackInfo ci) {
        ci.cancel();
        super.init();
        panel.init();
        unselectedPacksList.init(panel.x + 15, panel.y + 30, 180, 192);
        selectedPacksList.init(panel.x + 215, panel.y + 30, 180, 192);
        this.doneButton = Button.builder(CommonComponents.GUI_DONE, (button) -> this.onClose()).build();
    }

    @Inject(method = "repositionElements", at = @At("HEAD"), cancellable = true)
    public void repositionElements(CallbackInfo ci) {
        super.repositionElements();
        ci.cancel();
    }

    @Override
    public void added() {
        super.added();
        ControlTooltip.Renderer.of(this).add(() -> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.getIcon(), () -> LegacyComponents.OPEN_DIRECTORY);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initConstruct(CallbackInfo info) {
        reload();
    }

    @Inject(method = "populateLists", at = @At("HEAD"), cancellable = true)
    private void populateLists(CallbackInfo ci) {
        ci.cancel();
        addPacks(unselectedPacksList, model.getUnselected());
        addPacks(selectedPacksList, model.getSelected());
        repositionElements();
    }

    @Inject(method = "onClose", at = @At("RETURN"))
    public void onClose(CallbackInfo info) {
        LegacySoundUtil.playBackSound();
    }

    private void addPacks(RenderableVList list, Stream<PackSelectionModel.Entry> stream) {
        list.renderables.clear();
        stream.forEach(e -> {
            List<Component> description = new ArrayList<>();
            if (!e.getCompatibility().isCompatible()) {
                description.add(INCOMPATIBLE_TITLE);
                description.add(e.getCompatibility().getDescription());
            }
            if (!e.getExtendedDescription().getString().isEmpty()) description.add(e.getExtendedDescription());
            AbstractButton button = new AbstractButton(0, 0, 180, 30, e.getTitle()) {
                @Override
                protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
                    super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);
                    FactoryScreenUtil.enableBlend();
                    FactoryGuiGraphics.of(guiGraphics).blit(e.getIconTexture(), getX() + 5, getY() + 5, 0.0f, 0.0f, 20, 20, 20, 20);
                    FactoryScreenUtil.disableBlend();
                    if ((minecraft.options.touchscreen().get().booleanValue() || isHovered) && showHoverOverlay()) {
                        guiGraphics.fill(getX() + 5, getY() + 5, getX() + 25, getY() + 25, -1601138544);
                        int p = mouseX - getX();
                        int q = mouseY - getY();
                        if (e.canSelect()) {
                            if (p < 32) {
                                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.JOIN_HIGHLIGHTED, getX() + 5, getY() + 5, 20, 20);
                            } else {
                                FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.JOIN, getX() + 5, getY() + 5, 20, 20);
                            }
                        } else {
                            if (e.canUnselect()) {
                                if (p < 16) {
                                    FactoryGuiGraphics.of(guiGraphics).blitSprite(UNSELECT_HIGHLIGHTED, getX() + 5, getY() + 5, 20, 20);
                                } else {
                                    FactoryGuiGraphics.of(guiGraphics).blitSprite(UNSELECT, getX() + 5, getY() + 5, 20, 20);
                                }
                            }
                            if (e.canMoveUp()) {
                                if (p < 32 && p > 16 && q < 16) {
                                    FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.TRANSFER_MOVE_UP_HIGHLIGHTED, getX(), getY(), 32, 32);
                                } else {
                                    FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.TRANSFER_MOVE_UP, getX(), getY(), 32, 32);
                                }
                            }
                            if (e.canMoveDown()) {
                                if (p < 32 && p > 16 && q > 16) {
                                    FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.TRANSFER_MOVE_DOWN_HIGHLIGHTED, getX(), getY(), 32, 32);
                                } else {
                                    FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.TRANSFER_MOVE_DOWN, getX(), getY(), 32, 32);
                                }
                            }
                        }
                    }
                }

                protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                    LegacyRenderUtil.renderScrollingString(guiGraphics, font, getMessage(), getX() + 30, getY(), getX() + width - 2, getY() + height, e.getCompatibility().isCompatible() ? LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused()) : 0xFF0000FF, true);
                }

                @Override
                public void onClick(MouseButtonEvent event, boolean bl) {
                    double f = event.x() - getX();
                    double g = event.y() - getY();
                    if (this.showHoverOverlay() && f <= 32.0) {
                        if (e.canSelect()) {
                            onPress(event);
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
                    if (isFocused()) onPress(event);
                }

                private boolean showHoverOverlay() {
                    return !e.isFixedPosition() || !e.isRequired();
                }

                @Override
                public void onPress(InputWithModifiers input) {
                    if (e.isSelected() && e.canUnselect()) {
                        e.unselect();
                        return;
                    }
                    if (e.getCompatibility().isCompatible()) {
                        e.select();
                    } else
                        minecraft.setScreen(new ConfirmationScreen(self(), INCOMPATIBLE_CONFIRM_TITLE, e.getCompatibility().getConfirmation(), (b) -> {
                            e.select();
                            if (minecraft.screen != null) minecraft.screen.onClose();
                        }));
                }

                @Override
                public boolean keyPressed(KeyEvent keyEvent) {
                    if (keyEvent.hasShiftDown() || ControllerBinding.LEFT_BUTTON.state().pressed) {
                        switch (keyEvent.key()) {
                            case 265 -> {
                                int oldFocused = getFocused() == null ? -1 : children().indexOf(getFocused());
                                if (e.canMoveUp()) e.moveUp();
                                if (oldFocused >= 0 && oldFocused < children.size())
                                    PackSelectionScreenMixin.this.setFocused(children().get(oldFocused));
                                return false;
                            }
                            case 264 -> {
                                int oldFocused = getFocused() == null ? -1 : children().indexOf(getFocused());
                                if (e.canMoveDown()) e.moveDown();
                                if (oldFocused >= 0 && oldFocused < children.size())
                                    PackSelectionScreenMixin.this.setFocused(children().get(oldFocused));
                                return false;
                            }
                        }
                    }
                    return super.keyPressed(keyEvent);
                }

                @Override
                protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                    defaultButtonNarrationText(narrationElementOutput);
                }
            };
            if (!description.isEmpty()) button.setTooltip(new MultilineTooltip(description, 161));
            list.addRenderable(button);
        });
    }


    @Override
    public RenderableVList getRenderableVList() {
        return unselectedPacksList;
    }

    @Override
    public List<RenderableVList> getRenderableVLists() {
        return renderableVLists;
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        RenderableVList vList = getRenderableVListAt(d, e);
        if (vList != null) vList.mouseScrolled(g);
        return super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g);
    }

    @Override
    public boolean keyPressed(KeyEvent keyEvent) {
        for (RenderableVList renderableVList : getRenderableVLists()) {
            if (renderableVList.keyPressed(keyEvent.key())) return true;
        }
        if (keyEvent.key() == InputConstants.KEY_O) {
            //? if <1.20.5 {
            /*Util.getPlatform().openUri(this.packDir.toUri());
             *///?} else {
            Util.getPlatform().openPath(this.packDir);
            //?}
            return true;
        }
        return super.keyPressed(keyEvent);
    }

    //? if <=1.20.1 {
    /*@Inject(method = "render", at = @At("HEAD"), cancellable = true)
     *///?} else {
    @Override
            //?}
    public void render(GuiGraphics guiGraphics, int i, int j, float f/*? if <=1.20.1 {*//*, CallbackInfo ci*//*?}*/) {
        //? if <=1.20.1
        /*ci.cancel();*/
        LegacyRenderUtil.renderDefaultBackground(UIAccessor.of(this), guiGraphics, false);
        panel.render(guiGraphics, i, j, f);
        FactoryScreenUtil.enableBlend();
        FactoryGuiGraphics.of(guiGraphics).setBlitColor(1.0f, 1.0f, 1.0f, 0.6f);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL_RECESS, panel.x + 10, panel.y + 10, 190, 220);
        FactoryGuiGraphics.of(guiGraphics).clearBlitColor();
        FactoryScreenUtil.disableBlend();
        FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL_RECESS, panel.x + 210, panel.y + 10, 190, 220);
        guiGraphics.drawString(this.font, SELECTED_PACK, panel.x + 10 + (190 - font.width(SELECTED_PACK)) / 2, panel.y + 18, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        guiGraphics.drawString(this.font, AVAILABLE_PACK, panel.x + 210 + (190 - font.width(AVAILABLE_PACK)) / 2, panel.y + 18, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        super.render(guiGraphics, i, j, f);
    }
}
