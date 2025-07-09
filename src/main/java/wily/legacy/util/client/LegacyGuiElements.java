package wily.legacy.util.client;

import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.base.client.*;
import wily.factoryapi.util.ColorUtil;
import wily.factoryapi.util.FactoryGuiElement;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.*;
import wily.legacy.client.controller.LegacyKeyMapping;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.network.TopMessage;

public class LegacyGuiElements {
    public static long lastHotbarSelectionChange = -1;
    public static int lastHotbarSelection = -1;
    public static long lastGui = -1;

    public static void setup(Minecraft minecraft) {
        FactoryGuiElement[] nonScaledElements = new FactoryGuiElement[]{FactoryGuiElement.SELECTED_ITEM_NAME, FactoryGuiElement.OVERLAY_MESSAGE, FactoryGuiElement.SPECTATOR_TOOLTIP};
        ArbitrarySupplier<Float> hudScale = ()-> 3 / LegacyRenderUtil.getHUDScale();
        ArbitrarySupplier<Float> crosshairScale = ()-> LegacyRenderUtil.getStandardHeight() >= 1080 ? switch (LegacyOptions.hudScale.get()){
            case 1,2 -> 0.9f;
            default -> 1f;
        } : 1f;
        UIAccessor accessor = FactoryScreenUtil.getGuiAccessor();
        FactoryGuiElement.HOTBAR.pre().register(guiGraphics -> {
            AnimatedCharacterRenderer.render(guiGraphics);
            int newSelection = minecraft.player != null ? minecraft.player.getInventory().getSelectedSlot() : -1;
            if (lastHotbarSelection >= 0 && lastHotbarSelection != newSelection) lastHotbarSelectionChange = Util.getMillis();
            lastHotbarSelection = newSelection;
            LegacyGuiItemRenderer.pushSubmitSize(Math.round(48 / LegacyRenderUtil.getHUDScale()));
        });
        FactoryGuiElement.HOTBAR.post().register(guiGraphics -> {
            LegacyGuiItemRenderer.popSubmitSize();
            if (minecraft.player != null) ControlTooltip.Renderer.of(minecraft.gui).render(guiGraphics, 0,0, FactoryAPIClient.getPartialTick());
            LegacyRenderUtil.renderTopText(guiGraphics, TopMessage.small,21,1.0f, TopMessage.smallTicks);
            LegacyRenderUtil.renderTopText(guiGraphics, TopMessage.medium,37,1.5f, TopMessage.mediumTicks);
        });
        FactoryGuiElement.SPECTATOR_HOTBAR.pre().register(guiGraphics -> {
            Legacy4JClient.legacyFont = false;
            AnimatedCharacterRenderer.render(guiGraphics);
        });
        FactoryGuiElement.SPECTATOR_HOTBAR.post().register(guiGraphics -> Legacy4JClient.legacyFont = true);
        accessor.getStaticDefinitions().add(UIDefinition.createBeforeInit(a->{
            if (!LegacyMixinOptions.legacyGui.get()) return;
            a.getElements().put(FactoryGuiElement.VIGNETTE.name()+".isVisible", ()-> false);
            a.getElements().put("isGuiVisible", LegacyRenderUtil::canDisplayHUD);
            a.getElements().put("hud.scaleX", hudScale);
            a.getElements().put("hud.scaleY", hudScale);
            a.getElements().put("hud.scaleZ", hudScale);
            a.getElements().put("hud.translateX", ()-> minecraft.getWindow().getGuiScaledWidth() / 2);
            a.getElements().put("hud.scaledTranslateX", ()-> -minecraft.getWindow().getGuiScaledWidth() / 2);
            a.getElements().put("hud.translateY", ()-> minecraft.getWindow().getGuiScaledHeight() + LegacyRenderUtil.getHUDDistance());
            a.getElements().put("hud.scaledTranslateY", ()-> -minecraft.getWindow().getGuiScaledHeight());
            a.getElements().put("hud.renderColor", ()-> ColorUtil.colorFromFloat(1.0f,1.0f,1.0f, LegacyRenderUtil.getHUDOpacity()));
            a.getElements().put(FactoryGuiElement.BOSSHEALTH.name()+".renderColor", ()-> ColorUtil.colorFromFloat(1.0f,1.0f,1.0f, LegacyRenderUtil.getInterfaceOpacity()));

            a.getElements().put(FactoryGuiElement.CROSSHAIR.name()+".scaleX", crosshairScale);
            a.getElements().put(FactoryGuiElement.CROSSHAIR.name()+".scaleY", crosshairScale);
            a.getElements().put(FactoryGuiElement.CROSSHAIR.name()+".scaleZ", crosshairScale);
            a.putStaticElement(FactoryGuiElement.CROSSHAIR.name()+".hud.translateY", false);
            a.putStaticElement(FactoryGuiElement.CROSSHAIR.name()+".hud.scaledTranslateY", false);
            a.getElements().put(FactoryGuiElement.SPECTATOR_HOTBAR.name()+".translateY", ()-> (22- LegacyRenderUtil.getHUDDistance()) * (1- SpectatorGuiAccessor.getInstance().getVisibility()));
            a.getElements().put(FactoryGuiElement.SPECTATOR_TOOLTIP.name()+".translateY", ()-> (22- LegacyRenderUtil.getHUDDistance()) * (1- SpectatorGuiAccessor.getInstance().getVisibility()) + 35 - LegacyRenderUtil.getHUDSize() + LegacyRenderUtil.getHUDDistance());

            for (FactoryGuiElement element : nonScaledElements) {
                a.putStaticElement(element.name()+".hud.translateX", false);
                a.putStaticElement(element.name()+".hud.translateY", false);
                a.putStaticElement(element.name()+".hud.scaledTranslateX", false);
                a.putStaticElement(element.name()+".hud.scaledTranslateY", false);
                a.putStaticElement(element.name()+".hud.scale", false);
            }
            a.getElements().put(FactoryGuiElement.OVERLAY_MESSAGE.name()+".translateY", ()-> LegacyRenderUtil.getHUDDistance() + 72 - LegacyOptions.selectedItemTooltipSpacing.get() - LegacyRenderUtil.getHUDSize() - (GuiAccessor.getInstance().getLastToolHighlight().isEmpty() || GuiAccessor.getInstance().getToolHighlightTimer() <= 0 || LegacyRenderUtil.getSelectedItemTooltipLines() == 0 ? 0 : (Math.min(LegacyRenderUtil.getSelectedItemTooltipLines() + 1, LegacyRenderUtil.getTooltip(GuiAccessor.getInstance().getLastToolHighlight()).stream().filter(c->!c.getString().isEmpty()).mapToInt(c->1).sum()) - 1) * LegacyOptions.selectedItemTooltipSpacing.get()));

            a.getElements().put(FactoryGuiElement.CROSSHAIR.name()+".translateY", ()-> minecraft.getWindow().getGuiScaledHeight() / 2);
            a.getElements().put(FactoryGuiElement.CROSSHAIR.name()+".scaledTranslateY", ()-> -minecraft.getWindow().getGuiScaledHeight() / 2);
        }));

        FactoryAPIClient.uiDefinitionManager.staticList.add(UIDefinition.createBeforeInit(a-> {
            CommonValue.COMMON_VALUES.forEach((s, c)-> a.getElements().put("commonValue."+(s.getNamespace().equals("minecraft") ? "" : s.getNamespace() + ".")+s.getPath(),c));
            CommonColor.COMMON_COLORS.forEach((s, c)-> a.getElements().put("commonColor."+(s.getNamespace().equals("minecraft") ? "" : s.getNamespace() + ".")+s.getPath(),c));
            ControlTooltip.commonIcons.forEach((s,i)-> {
                a.getElements().put("controlIcon."+s, i.map(ControlTooltip.ComponentIcon::getComponent));
            });
            for (KeyMapping keyMapping : minecraft.options.keyMappings) {
                a.getElements().put("controlIcon."+keyMapping.getName(), ()-> ControlTooltip.getIconComponentFromKeyMapping(LegacyKeyMapping.of(keyMapping)));
            }
            a.getElements().put("interfaceResolution", LegacyRenderUtil::getInterfaceResolution);
            ControlType.types.forEach((s,c)->{
                a.getElements().put("activeControlType."+s, ()-> ControlType.getActiveType().equals(c));
            });

        }));
    }
}
