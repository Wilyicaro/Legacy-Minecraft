package wily.legacy.client;

//? if >=1.21.2 {
import wily.factoryapi.base.client.FactoryRenderStateExtension;
import net.minecraft.client.renderer.entity.state.ThrownTridentRenderState;
//?}
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.ThrownTrident;

public class LoyaltyLinesRenderState /*? if >=1.21.2 {*/implements FactoryRenderStateExtension<ThrownTrident>/*?}*/ {
    public boolean canRender;
    public double x;
    public double y;
    public double z;
    public float uniqueAge;
    public int clientSideReturnTridentTickCount;
    public double horizontalMovementFactor;

    public static LoyaltyLinesRenderState of(/*? if <1.21.2 {*//*ThrownTrident thrownTrident, float partialTicks*//*?} else {*/ThrownTridentRenderState renderState/*?}*/){
        //? if <1.21.2 {
        /*LoyaltyLinesRenderState renderState = new LoyaltyLinesRenderState();
        renderState.extractToRenderState(thrownTrident, partialTicks);
        return renderState;
        *///?} else {
        return FactoryRenderStateExtension.Accessor.of(renderState).getExtension(LoyaltyLinesRenderState.class);
        //?}
    }

    public Class<ThrownTrident> getEntityClass() {
        return ThrownTrident.class;
    }

    public void extractToRenderState(ThrownTrident trident, float partialTicks){
        canRender = trident.getOwner() != null && trident.isNoPhysics();
        if (!canRender) return;
        horizontalMovementFactor = Mth.lerp(partialTicks * 0.5F, trident.getOwner().yRotO, trident.getOwner().getYRot()) * (Math.PI / 180);
        x = Mth.lerp(partialTicks, trident.getOwner().xOld, trident.getOwner().getX()) - Mth.lerp(partialTicks, trident.xOld, trident.getX());
        y = Mth.lerp(partialTicks, trident.getOwner().yOld + trident.getOwner().getEyeHeight() * 0.8D, trident.getOwner().getY() + trident.getOwner().getEyeHeight() * 0.8D) - Mth.lerp(partialTicks, trident.yOld, trident.getY());;
        z = Mth.lerp(partialTicks, trident.getOwner().zOld, trident.getOwner().getZ()) - Mth.lerp(partialTicks, trident.zOld, trident.getZ());
        uniqueAge = trident.getId() + trident.tickCount + partialTicks;
        clientSideReturnTridentTickCount = trident.clientSideReturnTridentTickCount;
    }
}