package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.material.Fluids;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.ItemContainerPlatform;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.legacy.client.CommonColor;
import wily.legacy.client.LegacyCreativeTabListing;
import wily.legacy.client.LegacyTip;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class HelpAndOptionsScreen extends RenderableVListScreen {
    public static final List<Function<Screen, AbstractButton>> HOW_TO_PLAY_BUTTONS = new ArrayList<>(List.of(s->RenderableVListScreen.openScreenButton(Component.literal("Minecraft Wiki"),()->ConfirmationScreen.createLinkScreen(s,"https://minecraft.wiki/")).build(), s->RenderableVListScreen.openScreenButton(Component.literal("Legacy4J Wiki"),()->ConfirmationScreen.createLinkScreen(s,"https://github.com/Wilyicaro/Legacy-Minecraft/wiki")).build(),s->RenderableVListScreen.openScreenButton(Component.translatable("legacy.options.hints"),()->new ItemViewerScreen(s,s1->Panel.centered(s1,325,180), CommonComponents.EMPTY)).build()));

    public static List<AbstractWidget> createPlayerSkinWidgets(){
        List<AbstractWidget> list = new ArrayList<>();
        for (PlayerModelPart p : PlayerModelPart.values()) {
            list.add(new TickBox(0,0,Minecraft.getInstance().options.isModelPartEnabled(p),b->p.getName(),b->null,t->Minecraft.getInstance().options./*? if <1.21.2 {*/toggleModelPart/*?} else {*//*setModelPart*//*?}*/(p,t.selected)));
        }
        list.add(Minecraft.getInstance().options.mainHand().createButton(Minecraft.getInstance().options, 0 , 0, 0));
        return list;
    }

    public static final OptionsScreen.Section CHANGE_SKIN = new OptionsScreen.Section(Component.translatable("legacy.menu.change_skin"), s->Panel.centered(s, 250,144), new ArrayList<>(List.of(o-> o.renderableVList.renderables.addAll(createPlayerSkinWidgets()))));
    public static final OptionsScreen.Section HOW_TO_PLAY = new OptionsScreen.Section(Component.translatable("legacy.menu.how_to_play"), s->Panel.centered(s, 220, HOW_TO_PLAY_BUTTONS.size()*24+16), new ArrayList<>(List.of(o-> HOW_TO_PLAY_BUTTONS.forEach(s-> o.getRenderableVList().addRenderable(s.apply(o))))));

    public HelpAndOptionsScreen(Screen parent) {
        super(parent,Component.translatable("options.title"), r-> {});
        renderableVList.addRenderable(RenderableVListScreen.openScreenButton(CHANGE_SKIN.title(),()-> CHANGE_SKIN.build(this)).build());
        renderableVList.addRenderable(RenderableVListScreen.openScreenButton(HOW_TO_PLAY.title(),()-> HOW_TO_PLAY.build(this)).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("controls.title"),()-> new RenderableVListScreen(this,Component.translatable("controls.title"),r->r.addRenderables(Button.builder(Component.translatable("options.mouse_settings.title"), button -> this.minecraft.setScreen(new OptionsScreen(r.getScreen(), s->Panel.centered(s, 250,102),Component.translatable("options.mouse_settings.title"), minecraft.options.invertYMouse(), Minecraft.getInstance().options.sensitivity(), minecraft.options.mouseWheelSensitivity(), minecraft.options.discreteMouseScroll(), minecraft.options.touchscreen()))).build(),Button.builder(Component.translatable("controls.keybinds.title"), button -> this.minecraft.setScreen(new LegacyKeyBindsScreen(r.getScreen(),minecraft.options))).build(),Button.builder(Component.translatable("legacy.options.selectedController"), button -> this.minecraft.setScreen(new ControllerMappingScreen(r.getScreen(),minecraft.options))).build()))).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("legacy.menu.settings"),()->new SettingsScreen(this)).build());
        renderableVList.addRenderable(openScreenButton(Component.translatable("credits_and_attribution.button.credits"),()->new RenderableVListScreen(this,Component.translatable("credits_and_attribution.screen.title"),r-> r.addRenderables(openScreenButton(Component.translatable("credits_and_attribution.button.credits"),()->new WinScreen(false, () -> this.minecraft.setScreen(r.getScreen()))).build(),Button.builder(Component.translatable("credits_and_attribution.button.attribution"), b-> Minecraft.getInstance().setScreen(ConfirmationScreen.createLinkScreen(r.getScreen(), "https://aka.ms/MinecraftJavaAttribution"))).build(),Button.builder(Component.translatable("credits_and_attribution.button.licenses"), b-> Minecraft.getInstance().setScreen(ConfirmationScreen.createLinkScreen(r.getScreen(), "https://aka.ms/MinecraftJavaLicenses"))).build()))).build());
    }

}
