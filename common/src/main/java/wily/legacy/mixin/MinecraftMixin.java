package wily.legacy.mixin;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import wily.legacy.LegacyMinecraft;
import wily.legacy.client.screen.LegacyLoadingScreen;
import wily.legacy.network.ServerOpenClientMenu;
import wily.legacy.util.ScreenUtil;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow protected abstract void updateScreenAndTick(Screen screen);

    @Shadow @Nullable public ClientLevel level;

    @Shadow public Options options;

    @Shadow @Final private Tutorial tutorial;

    @Shadow public abstract void setScreen(@Nullable Screen screen);

    @Shadow @Nullable public LocalPlayer player;

    @Shadow @Nullable public MultiPlayerGameMode gameMode;

    @Redirect(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z", ordinal = 4))
    private boolean handleKeybinds(KeyMapping instance){
        while (instance.consumeClick()){
            if (ScreenUtil.hasClassicCrafting() || gameMode.hasInfiniteItems()) {
                if (!gameMode.hasInfiniteItems()) this.tutorial.onOpenInventory();
                this.setScreen(new InventoryScreen(this.player));
            }else LegacyMinecraft.NETWORK.sendToServer(new ServerOpenClientMenu(2));
        }
        return false;
    }
    @Redirect(method = "setLevel",at = @At(value = "INVOKE",target = "Lnet/minecraft/client/Minecraft;updateScreenAndTick(Lnet/minecraft/client/gui/screens/Screen;)V"))
    public void setLevelLoadingScreen(Minecraft instance, Screen screen, ClientLevel level) {
        boolean lastOd = isOtherDimension(this.level);
        boolean od = isOtherDimension(level);
        LegacyLoadingScreen s = new LegacyLoadingScreen(od || lastOd ? Component.translatable("legacy.menu." + (lastOd ? "leaving" : "entering"), getDimensionName((lastOd ? this.level : level).dimension())) : Component.empty(), Component.empty());
        if (od || lastOd) s.genericLoading = true;
        updateScreenAndTick(s);
    }
    private Component getDimensionName(ResourceKey<Level> dimension){
        String s = dimension.location().toLanguageKey("dimension");
        return Component.translatable(ScreenUtil.hasTip(s) ? s : "dimension.minecraft");

    }
    private boolean isOtherDimension(Level level){
        return level != null && level.dimension() !=Level.OVERWORLD;
    }

    @ModifyArg(method = "resizeDisplay",at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setGuiScale(D)V"))
    public double resizeDisplay(double d) {
        return d * 2/3;
    }
}
