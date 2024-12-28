package wily.legacy.mixin.base;

//? if >=1.21.2 {
import net.minecraft.client.renderer.SkyRenderer;
//?} else {
/*import net.minecraft.client.renderer.LevelRenderer;
*///?}
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(/*? if <1.21.2 {*//*LevelRenderer*//*?} else {*/SkyRenderer/*?}*/.class)
public class LevelRendererMixin {
    @ModifyVariable(method = /*? if <1.21.4 {*//*"drawStars"*//*?} else {*/"buildStars"/*?}*/, at = @At(value = "STORE"), ordinal = 4)
    private /*? if <1.20.5 {*//*double*//*?} else {*/float/*?}*/ drawStars(/*? if <1.20.5 {*//*double*//*?} else {*/float/*?}*/ original) {
        return original - 0.05f;
    }
}
