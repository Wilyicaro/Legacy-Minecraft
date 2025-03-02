package wily.legacy.mixin.base;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.MixedCraftingScreen;
import wily.legacy.util.ScreenUtil;

import java.util.Map;

@Mixin(MenuScreens.class)
public class MenuScreensMixin {
    @Shadow @Final private static Map<MenuType<?>, MenuScreens.ScreenConstructor<?, ?>> SCREENS;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void init(CallbackInfo ci){
        SCREENS.put(MenuType.CRAFTING, new MenuScreens.ScreenConstructor<CraftingMenu, AbstractContainerScreen<CraftingMenu>>() {
            @Override
            public AbstractContainerScreen<CraftingMenu> create(CraftingMenu abstractContainerMenu, Inventory inventory, Component component) {
                return ScreenUtil.hasMixedCrafting() ? MixedCraftingScreen.craftingScreen(abstractContainerMenu, inventory, component) : new CraftingScreen(abstractContainerMenu, inventory, component);
            }
        });
    }
}
