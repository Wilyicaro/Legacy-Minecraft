package wily.legacy.fabric;

import dev.architectury.registry.menu.MenuRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import wily.legacy.Legacy4JClient;

public class Legacy4JFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Legacy4JClient.init();
        Legacy4JClient.enqueueInit();
        Legacy4JClient.registerScreen(new Legacy4JClient.MenuScreenRegister() {
            @Override
            public <H extends AbstractContainerMenu, S extends Screen & MenuAccess<H>> void register(MenuType<? extends H> type, MenuRegistry.ScreenFactory<H, S> factory) {
                MenuScreens.register(type,factory::create);
            }
        });
    }
}
