package wily.legacy.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShaderManager.class)
public abstract class ShaderManagerMixin {
    @ModifyExpressionValue(method = "loadShader", at = @At(value = "INVOKE", target = "Lorg/apache/commons/io/IOUtils;toString(Ljava/io/Reader;)Ljava/lang/String;"))
    private static String loadShader(String source, @Local(argsOnly = true) Identifier id, @Local(argsOnly = true) ShaderType type) {
        if (type != ShaderType.VERTEX || !id.getPath().startsWith("shaders/core/")) {
            return source;
        }
        String patched = patchSingleDistance(source, "pos");
        patched = patchSingleDistance(patched, "Position");
        patched = patchDistance(patched, "pos");
        return patchDistance(patched, "Position");
    }

    private static String patchDistance(String source, String value) {
        String distance = "fog_legacy_distance(ModelViewMat * vec4(" + value + ", 1.0))";
        return source
                .replace("sphericalVertexDistance = fog_spherical_distance(" + value + ");", "sphericalVertexDistance = " + distance + ";")
                .replace("cylindricalVertexDistance = fog_cylindrical_distance(" + value + ");", "cylindricalVertexDistance = " + distance + ";");
    }

    private static String patchSingleDistance(String source, String value) {
        return source.replace("vertexDistance = fog_spherical_distance(" + value + ");", "vertexDistance = fog_legacy_distance(ModelViewMat * vec4(" + value + ", 1.0));");
    }
}
