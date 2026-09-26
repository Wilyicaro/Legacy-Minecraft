package wily.legacy.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.client.UIDefinition;
import wily.factoryapi.util.FactoryGuiElement;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.control.ControlType;
import wily.legacy.client.control.LegacyKeyMapping;
import wily.legacy.client.control.tooltip.CommonIcon;
import wily.legacy.client.control.tooltip.ComponentIcon;
import wily.legacy.client.control.tooltip.ControlTooltip;
import wily.legacy.client.control.tooltip.ControlTooltipRenderer;

public class LegacyControls {
	public static void init() {
		final Minecraft minecraft = Minecraft.getInstance();
		FactoryGuiElement.HOTBAR.post().register(GuiGraphicsExtractor -> {
			if (minecraft.player != null)
				ControlTooltipRenderer.of(minecraft.gui).extractRenderState(GuiGraphicsExtractor, 0, 0, FactoryAPIClient.getPartialTick());
		});
		FactoryAPIClient.uiDefinitionManager.staticList.add(UIDefinition.createAfterInit(a -> {
			CommonIcon.commonIcons.forEach((s, i) -> {
				a.getElements().put("controlIcon." + s, i.map(ComponentIcon::getComponent));
			});
			for (KeyMapping keyMapping : minecraft.options.keyMappings) {
				a.getElements().put("controlIcon." + keyMapping.getName(), () -> ControlTooltip.getIconComponentFromKeyMapping(LegacyKeyMapping.of(keyMapping)));
			}
			Legacy4JClient.controlTypesManager.map().forEach((s, c) -> {
				a.getElements().put("activeControlType." + s, () -> ControlType.getActiveType().equals(c));
			});
		}));
	}
}
