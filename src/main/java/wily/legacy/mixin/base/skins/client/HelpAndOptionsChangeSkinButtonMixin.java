package wily.legacy.mixin.base.skins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.SkinsClientBootstrap;
import wily.legacy.client.screen.HelpAndOptionsScreen;
import wily.legacy.client.screen.RenderableVList;
import wily.legacy.client.screen.RenderableVListScreen;

import java.util.function.Consumer;

@Mixin(value = HelpAndOptionsScreen.class, remap = false)
public abstract class HelpAndOptionsChangeSkinButtonMixin extends RenderableVListScreen {
    protected HelpAndOptionsChangeSkinButtonMixin(Screen parent, Component component, Consumer<RenderableVList> vListBuild) {
        super(parent, component, vListBuild);
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false, require = 0)
    private void consoleskins$restoreIntegratedChangeSkinButton(Screen parent, CallbackInfo ci) {
        Renderable replacement = Button.builder(
                Component.translatable("legacy.menu.change_skin"),
                b -> SkinsClientBootstrap.requestOpenChangeSkinScreen(Minecraft.getInstance(), (Screen) (Object) this)
        ).build();

        if (renderableVList.renderables.isEmpty()) renderableVList.renderables.add(replacement);
        else renderableVList.renderables.set(0, replacement);
    }
}
