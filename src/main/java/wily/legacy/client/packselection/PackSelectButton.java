package wily.legacy.client.packselection;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.client.control.ControllerBinding;
import wily.legacy.client.screen.ConfirmationScreen;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

import static wily.legacy.util.LegacySprites.UNSELECT;
import static wily.legacy.util.LegacySprites.UNSELECT_HIGHLIGHTED;

public class PackSelectButton extends AbstractButton {
	private static final Component INCOMPATIBLE_CONFIRM_TITLE = Component.translatable("pack.incompatible.confirm.title");
	private final PackSelectionModel.Entry e;
	private final Minecraft minecraft;
	private final PackSelectionScreen screen;

	public PackSelectButton(PackSelectionScreen screen, Minecraft minecraft, Component title, PackSelectionModel.Entry e) {
		super(0, 0, 180, 30, title);
		this.screen = screen;
		this.minecraft = minecraft;
		this.e = e;
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor GuiGraphicsExtractor, int mouseX, int mouseY, float partialTicks) {
		extractDefaultSprite(GuiGraphicsExtractor);
		renderScrollingString(GuiGraphicsExtractor, Minecraft.getInstance().font, 2, e.getCompatibility().isCompatible() ? LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused()) : 0xFF0000FF);
		FactoryScreenUtil.enableBlend();
		FactoryGuiGraphics.of(GuiGraphicsExtractor).blit(e.getIconTexture(), getX() + 5, getY() + 5, 0.0f, 0.0f, 20, 20, 20, 20);
		FactoryScreenUtil.disableBlend();
		if ((minecraft.options.touchscreen().get().booleanValue() || isHovered) && showHoverOverlay()) {
			GuiGraphicsExtractor.fill(getX() + 5, getY() + 5, getX() + 25, getY() + 25, -1601138544);
			int p = mouseX - getX();
			int q = mouseY - getY();
			if (e.canSelect()) {
				if (p < 32) {
					FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.JOIN_HIGHLIGHTED, getX() + 5, getY() + 5, 20, 20);
				} else {
					FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.JOIN, getX() + 5, getY() + 5, 20, 20);
				}
			} else {
				if (e.canUnselect()) {
					if (p < 16) {
						FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(UNSELECT_HIGHLIGHTED, getX() + 5, getY() + 5, 20, 20);
					} else {
						FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(UNSELECT, getX() + 5, getY() + 5, 20, 20);
					}
				}
				if (e.canMoveUp()) {
					if (p < 32 && p > 16 && q < 16) {
						FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.TRANSFER_MOVE_UP_HIGHLIGHTED, getX(), getY(), 32, 32);
					} else {
						FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.TRANSFER_MOVE_UP, getX(), getY(), 32, 32);
					}
				}
				if (e.canMoveDown()) {
					if (p < 32 && p > 16 && q > 16) {
						FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.TRANSFER_MOVE_DOWN_HIGHLIGHTED, getX(), getY(), 32, 32);
					} else {
						FactoryGuiGraphics.of(GuiGraphicsExtractor).blitSprite(LegacySprites.TRANSFER_MOVE_DOWN, getX(), getY(), 32, 32);
					}
				}
			}
		}
	}

	protected void renderScrollingString(GuiGraphicsExtractor GuiGraphicsExtractor, Font font, int i, int j) {
		LegacyRenderUtil.renderScrollingString(GuiGraphicsExtractor, font, getMessage(), getX() + 30, getY(), getX() + width - 2, getY() + height, e.getCompatibility().isCompatible() ? LegacyRenderUtil.getDefaultTextColor(!isHoveredOrFocused()) : 0xFF0000FF, true);
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
			minecraft.setScreen(new ConfirmationScreen(screen, INCOMPATIBLE_CONFIRM_TITLE, e.getCompatibility().getConfirmation(), (b) -> {
				e.select();
				if (minecraft.screen != null)
					minecraft.screen.onClose();
			}));
	}

	@Override
	public boolean keyPressed(KeyEvent keyEvent) {
		if (keyEvent.hasShiftDown() || ControllerBinding.LEFT_BUTTON.state().pressed) {
			switch (keyEvent.key()) {
				case 265 -> {
					int oldFocused = screen.getFocused() == null ? -1 : screen.children().indexOf(screen.getFocused());
					if (e.canMoveUp()) e.moveUp();
					if (oldFocused >= 0 && oldFocused < screen.children.size())
						screen.setFocused(screen.children().get(oldFocused));
					return false;
				}
				case 264 -> {
					int oldFocused = screen.getFocused() == null ? -1 : screen.children().indexOf(screen.getFocused());
					if (e.canMoveDown()) e.moveDown();
					if (oldFocused >= 0 && oldFocused < screen.children.size())
						screen.setFocused(screen.children().get(oldFocused));
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

	public PackSelectionModel.Entry getEntry() {
		return e;
	}
}
