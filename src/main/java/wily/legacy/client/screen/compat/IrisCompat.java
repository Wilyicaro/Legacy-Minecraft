//? if fabric || >=1.21 && neoforge {
package wily.legacy.client.screen.compat;

import net.irisshaders.iris.gui.screen.ShaderPackScreen;
import net.minecraft.network.chat.Component;
import wily.legacy.client.screen.RenderableVListScreen;

public class IrisCompat {
    public static void init() {
        SodiumCompat.SODIUM.elements().add(o -> o.getRenderableVList().addRenderables(0, RenderableVListScreen.openScreenButton(Component.translatable("options.iris.shaderPackSelection"), () -> new ShaderPackScreen(o)).build()));
    }
}
//?}
