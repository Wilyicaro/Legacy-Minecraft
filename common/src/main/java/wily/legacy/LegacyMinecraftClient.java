package wily.legacy;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.Level;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.client.screen.*;
import wily.legacy.client.LegacyOptions;

import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Consumer;

import static wily.legacy.LegacyMinecraft.MOD_ID;


public class LegacyMinecraftClient {
    public static final ResourceLocation SCROLL_DOWN = new ResourceLocation(LegacyMinecraft.MOD_ID,"widget/scroll_down");
    public static final ResourceLocation SCROLL_UP = new ResourceLocation(LegacyMinecraft.MOD_ID,"widget/scroll_up");
    public static final ResourceLocation SCROLL_RIGHT = new ResourceLocation(LegacyMinecraft.MOD_ID,"widget/scroll_right");
    public static final ResourceLocation SCROLL_LEFT = new ResourceLocation(LegacyMinecraft.MOD_ID,"widget/scroll_left");
    public static final ResourceLocation LOADING_BACKGROUND_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"widget/loading_background");
    public static final ResourceLocation LOADING_BAR_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"widget/loading_bar");
    public static final ResourceLocation SADDLE_SLOT_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/saddle_slot");
    public static final ResourceLocation LLAMA_ARMOR_SLOT_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/llama_armor_slot");
    public static final ResourceLocation ARMOR_SLOT_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/armor_slot");
    public static final ResourceLocation EXPERIENCE_BAR_BACKGROUND_SPRITE = new ResourceLocation("hud/experience_bar_background");
    public static final ResourceLocation EXPERIENCE_BAR_CURRENT_SPRITE = new ResourceLocation("hud/experience_bar_progress");
    public static final ResourceLocation EXPERIENCE_BAR_RESULT_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/experience_bar_result");
    public static final ResourceLocation PADLOCK_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/padlock");
    public static final ResourceLocation BEACON_1_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/beacon_1");
    public static final ResourceLocation BEACON_2_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/beacon_2");
    public static final ResourceLocation BEACON_3_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/beacon_3");
    public static final ResourceLocation BEACON_4_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/beacon_4");
    public static final ResourceLocation SHIELD_SLOT_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/shield_slot");
    public static final ResourceLocation FULL_ARROW_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/full_arrow");
    public static final ResourceLocation SMALL_ARROW_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/small_arrow");
    public static final ResourceLocation LIT = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/lit");
    public static final ResourceLocation LIT_PROGRESS = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/lit_progress");
    public static final ResourceLocation BREWING_SLOTS_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/brewing_slots");
    public static final ResourceLocation BREWING_COIL_FLAME_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/brewing_coil_flame");
    public static final ResourceLocation BREWING_FUEL_SLOT_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/brewing_fuel_slot");
    public static final ResourceLocation ERROR_CROSS_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/error_cross");
    public static final ResourceLocation ANVIL_HAMMER_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/anvil_hammer");
    public static final ResourceLocation SMITHING_HAMMER_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/smithing_hammer");
    public static final ResourceLocation COMBINER_PLUS_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/combiner_plus");
    public static final ResourceLocation ARROW_SPRITE = new ResourceLocation(LegacyMinecraft.MOD_ID,"container/arrow");
    public static float FONT_SHADOW_OFFSET = 1.0F;
    public static boolean canLoadVanillaOptions = true;
    public static final Map<Component, Component> OPTION_BOOLEAN_CAPTION = Map.of(Component.translatable("key.sprint"),Component.translatable("options.key.toggleSprint"),Component.translatable("key.sneak"),Component.translatable("options.key.toggleSneak"));
    public static LegacyLoadingScreen legacyLoadingScreen = new LegacyLoadingScreen();
    public static RenderType itemRenderTypeOverride = null;
    public static RenderType blockItemRenderTypeOverride = null;

    public static final LegacyTipManager legacyTipManager = new LegacyTipManager();
    public static void init() {
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES,legacyTipManager);
        KeyMappingRegistry.register(legacyKeyInventory);
    }
    public static void enqueueInit() {
        ClientGuiEvent.SET_SCREEN.register((screen) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (screen instanceof TitleScreen t)
                return CompoundEventResult.interruptTrue(new MainMenuScreen(false));
            if (screen instanceof DeathScreen d)
                return CompoundEventResult.interruptTrue(new LegacyDeathScreen(d.causeOfDeath,d.hardcore));
            if (((LegacyOptions)minecraft.options).legacyCreativeTab().get() && screen instanceof CreativeModeInventoryScreen c) {
                c.init(minecraft,0,0);
                return CompoundEventResult.interruptTrue(new CreativeModeScreen(Minecraft.getInstance().player));
            }
            return CompoundEventResult.interruptDefault(screen);
        });
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            while (legacyKeyInventory.consumeClick()) {
                if (minecraft.gameMode.isServerControlledInventory()) {
                    minecraft.player.sendOpenInventory();
                    continue;
                }
                minecraft.getTutorial().onOpenInventory();
                InventoryScreen inventoryScreen = new InventoryScreen(minecraft.player);
                ((ReplaceableScreen)inventoryScreen).setCanReplace(false);
                minecraft.setScreen(inventoryScreen);
            }
        });

    }
    public static final KeyMapping legacyKeyInventory = new KeyMapping( MOD_ID +".key.inventory", InputConstants.KEY_I, "key.categories.inventory");
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