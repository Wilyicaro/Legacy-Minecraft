package wily.legacy.mixin.base.skins.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.legacy.Skins.client.render.boxloader.BoxAddonLayer;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererBoxAddonLayerMixin {

    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void consoleskins$addBoxAddonLayer(EntityRendererProvider.Context context, boolean slim, CallbackInfo ci) {
        RenderLayer layer = new BoxAddonLayer((RenderLayerParent) (Object) this);
        if (tryAddToLayerList(layer)) return;
        if (tryInvokeAddLayer(layer)) return;
    }
private boolean tryInvokeAddLayer(RenderLayer layer) {
        Class<?> c = this.getClass();
        while (c != null) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getParameterCount() != 1) continue;
                if (!m.getName().equals("addLayer") && !m.getName().equals("addFeature") && !m.getName().equals("addRenderLayer")) continue;
                if (!m.getParameterTypes()[0].isAssignableFrom(layer.getClass()) && !m.getParameterTypes()[0].isAssignableFrom(RenderLayer.class)) continue;
                try {
                    m.setAccessible(true);
                    m.invoke(this, layer);
                    return true;
                } catch (Throwable ignored) {
                }
            }
            c = c.getSuperclass();
        }
        return false;
    }

    private boolean tryAddToLayerList(RenderLayer layer) {
        Class<?> c = this.getClass();
        while (c != null) {
            for (Field f : c.getDeclaredFields()) {
                try {
                    if (!List.class.isAssignableFrom(f.getType())) continue;
                    f.setAccessible(true);
                    Object v = f.get(this);
                    if (!(v instanceof List list)) continue;
                    list.add(0, layer);
                    return true;
                } catch (Throwable ignored) {
                }
            }
            c = c.getSuperclass();
        }
        return false;
    }
}
