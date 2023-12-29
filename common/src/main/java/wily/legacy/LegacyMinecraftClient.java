package wily.legacy;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import wily.legacy.client.screen.*;
import wily.legacy.init.LegacyOptions;
import wily.legacy.network.ServerOpenClientMenu;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Consumer;

import static wily.legacy.LegacyMinecraft.MOD_ID;
import static wily.legacy.init.LegacyMenuTypes.*;


public class LegacyMinecraftClient {

    public static float FONT_SHADOW_OFFSET = 1.0F;
    public static boolean canLoadVanillaOptions = true;
    public static final Map<Component, Component> OPTION_BOOLEAN_CAPTION = Map.of(Component.translatable("key.sprint"),Component.translatable("options.key.toggleSprint"),Component.translatable("key.sneak"),Component.translatable("options.key.toggleSneak"));
    public static LegacyLoadingScreen legacyLoadingScreen = new LegacyLoadingScreen();
    public static void init() {
        KeyMappingRegistry.register(playerInventoryKey);
    }
    public static void enqueueInit() {
        MenuRegistry.registerScreenFactory(STORAGE_5X1.get(), LegacyChestScreen::new);
        MenuRegistry.registerScreenFactory(STORAGE_3X3.get(), LegacyChestScreen::new);
        MenuRegistry.registerScreenFactory(BAG_MENU.get(), LegacyChestScreen::new);
        MenuRegistry.registerScreenFactory(CHEST_MENU.get(), LegacyChestScreen::new);
        MenuRegistry.registerScreenFactory(LARGE_CHEST_MENU.get(), LegacyChestScreen::new);
        MenuRegistry.registerScreenFactory(LEGACY_INVENTORY_MENU.get(), LegacyInventoryScreen::new);
        MenuRegistry.registerScreenFactory(LEGACY_INVENTORY_MENU_CRAFTING.get(), LegacyInventoryScreen::new);
        MenuRegistry.registerScreenFactory(CLASSIC_CRAFTING_MENU.get(), ClassicCraftingScreen::new);
        MenuRegistry.registerScreenFactory(LEGACY_FURNACE_MENU.get(), LegacyFurnaceScreen::new);
        MenuRegistry.registerScreenFactory(LEGACY_BLAST_FURNACE_MENU.get(), LegacyFurnaceScreen::new);
        MenuRegistry.registerScreenFactory(LEGACY_SMOKER_MENU.get(), LegacyFurnaceScreen::new);
        ClientGuiEvent.SET_SCREEN.register((screen) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (screen instanceof TitleScreen t)
                return CompoundEventResult.interruptTrue(new MainMenuScreen(false));
            if (screen instanceof PauseScreen p)
                return CompoundEventResult.interruptTrue(new LegacyPauseScreen(p.showsPauseMenu()));
            if (screen instanceof DeathScreen d)
                return CompoundEventResult.interruptTrue(new LegacyDeathScreen(d.causeOfDeath,d.hardcore));
            if (((LegacyOptions)minecraft.options).legacyCreativeTab().get() && screen instanceof CreativeModeInventoryScreen c) {
                c.init(minecraft,0,0);
                return CompoundEventResult.interruptTrue(new CreativeModeScreen(Minecraft.getInstance().player));
            }
            return CompoundEventResult.interruptDefault(screen);
        });
        ClientGuiEvent.RENDER_POST.register((screen,graphics,i,j,f) -> {
            if (screen instanceof LevelLoadingScreen || screen instanceof  ProgressScreen || screen instanceof GenericDirtMessageScreen || screen instanceof ReceivingLevelScreen) {
                Minecraft minecraft = Minecraft.getInstance();
                Component lastLoadingHeader = null;
                Component lastLoadingStage = null;
                int progress = 0;
                if (screen instanceof LevelLoadingScreen loading) {
                    lastLoadingHeader = Component.translatable("connect.connecting");
                    lastLoadingStage = Component.translatable("legacy.loading_spawn_area");
                    progress = loading.progressListener.getProgress();
                }
                if (screen instanceof GenericDirtMessageScreen p)
                    lastLoadingHeader = p.getTitle();
                if (screen instanceof ProgressScreen p) {
                    lastLoadingHeader = p.header;
                    lastLoadingStage = p.stage;
                    progress = lastLoadingHeader == Component.translatable("connect.joining") ?  100 : p.progress;
                }
                legacyLoadingScreen.prepareRender(minecraft,screen.width, screen.height,lastLoadingHeader,lastLoadingStage,progress);
                legacyLoadingScreen.render(graphics,i,j,f);
            }
        });
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            while (playerInventoryKey.consumeClick()) {
                if (minecraft.gameMode.isServerControlledInventory()) {
                    minecraft.player.sendOpenInventory();
                    continue;
                }
                minecraft.getTutorial().onOpenInventory();
                LegacyMinecraft.NETWORK.sendToServer(new ServerOpenClientMenu(((LegacyOptions)minecraft.options).classicCrafting().get() ? 1 : 0));
            }
        });

    }
    public static final KeyMapping playerInventoryKey = new KeyMapping( MOD_ID +".key.inventory", InputConstants.KEY_I, "key.categories.inventory");
    public static void resetVanillaOptions(Minecraft minecraft){
        canLoadVanillaOptions = false;
        minecraft.options = new Options(minecraft,minecraft.gameDirectory);
        minecraft.options.save();
        canLoadVanillaOptions = true;
    }
    public static String importSaveFile(Minecraft minecraft, InputStream saveInputStream, String saveDirName){
        StringBuilder builder = new StringBuilder(saveDirName);
        int levelRepeat = 0;
        while (minecraft.getLevelSource().levelExists(builder +(levelRepeat > 0 ? String.format(" (%s)",levelRepeat) : "")))
            levelRepeat++;
        if (levelRepeat > 0)
            builder.append(String.format(" (%s)",levelRepeat));
        LegacyMinecraft.copySaveToDirectory(saveInputStream,new File(minecraft.gameDirectory, "saves/" + builder));
        return builder.toString();
    }
    public static void registerExtraModels(Consumer<ResourceLocation> register){

    }


}