package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIDefinition;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyWorldTemplate;
import wily.legacy.util.ScreenUtil;

import java.io.IOException;
import java.util.function.Consumer;

public class CreationList extends RenderableVList{
    protected final Minecraft minecraft;

    public CreationList(UIDefinition.Accessor accessor) {
        super(accessor);
        layoutSpacing(l->0);
        minecraft = Minecraft.getInstance();
        addIconButton(this, Legacy4J.createModLocation("creation_list/create_world"),Component.translatable("legacy.menu.create_world"), c-> CreateWorldScreen.openFresh(this.minecraft, getScreen()));
        LegacyWorldTemplate.list.forEach(t-> addIconButton(this,t.icon(),t.buttonMessage(), c-> {
            try (LevelStorageSource.LevelStorageAccess access = Legacy4JClient.currentWorldSource.createAccess(Legacy4JClient.importSaveFile(t.open(), minecraft.getLevelSource()::levelExists,Legacy4JClient.currentWorldSource,t.folderName()))) {
                LevelSummary summary = access.getSummary(/*? if >1.20.2 {*/access.getDataTag()/*?}*/);
                access.close();
                if (t.directJoin()) LoadSaveScreen.loadWorld(getScreen(),minecraft,Legacy4JClient.currentWorldSource,summary);
                else minecraft.setScreen(new LoadSaveScreen(getScreen(),summary,access,t.isLocked()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    public static void addIconButton(RenderableVList list, ResourceLocation iconSprite, Component message, Consumer<AbstractButton> onPress){
        addIconButton(list,iconSprite,message,onPress,null);
    }
    public static void addIconButton(RenderableVList list, ResourceLocation iconSprite, Component message, Consumer<AbstractButton> onPress, Tooltip tooltip){
        AbstractButton button;
        list.addRenderable(button = new AbstractButton(0,0,270,30,message) {
            @Override
            protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                super.renderWidget(guiGraphics, i, j, f);
                if (!list.accessor.getBoolean("allowButtonsWithIcons",true)) return;
                RenderSystem.enableBlend();
                FactoryGuiGraphics.of(guiGraphics).blitSprite(iconSprite, getX() + 5, getY() + 5, 20, 20);
                RenderSystem.disableBlend();
                if (Minecraft.getInstance().options.touchscreen().get().booleanValue() || isHovered) {
                    guiGraphics.fill(getX() + 5, getY() + 5, getX() + 25, getY() + 25, -1601138544);
                }
            }
            @Override
            protected void renderScrollingString(GuiGraphics guiGraphics, Font font, int i, int j) {
                int k = this.getX() + 35;
                int l = this.getX() + this.getWidth();
                ScreenUtil.renderScrollingString(guiGraphics, font, this.getMessage(), k, this.getY(), l, this.getY() + this.getHeight(), j, true);
            }
            @Override
            public void onPress() {
                onPress.accept(this);
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                defaultButtonNarrationText(narrationElementOutput);
            }
        });
        button.setTooltip(tooltip);
    }

}
