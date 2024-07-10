package wily.legacy.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.Legacy4J;
import wily.legacy.util.Offset;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.ScreenUtil;

import static wily.legacy.util.LegacySprites.SHIELD_SLOT;

@Mixin(InventoryMenu.class)
public class InventoryMenuMixin {
    @Shadow @Final private static EquipmentSlot[] SLOT_IDS;
    private static final Offset EQUIP_SLOT_OFFSET = new Offset(50,0,0);

    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/InventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 0))
    private Slot addSlotFirst(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot,180, 40,new LegacySlotDisplay(){
            public boolean isVisible() {
                return ScreenUtil.hasClassicCrafting();
            }
        });
    }
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/InventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1))
    private Slot addSlotSecond(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot,111 + originalSlot.getContainerSlot() % 2 * 21, 30 + originalSlot.getContainerSlot() / 2 * 21,new LegacySlotDisplay(){
            public boolean isVisible() {
                return ScreenUtil.hasClassicCrafting();
            }
        });
    }
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/InventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 2))
    private Slot addSlotThird(Slot originalSlot){
        int i = 39 - originalSlot.getContainerSlot();
        return LegacySlotDisplay.override(originalSlot, 14, 14 + i * 21,new LegacySlotDisplay(){
            public ResourceLocation getIconSprite() {
                return originalSlot.getItem().isEmpty() ? ResourceLocation.tryBuild(Legacy4J.MOD_ID,"container/"+ SLOT_IDS[i].getName()+ "_slot") : null;
            }
            public Offset getOffset() {
                return ScreenUtil.hasClassicCrafting() ? Offset.ZERO : EQUIP_SLOT_OFFSET;
            }
        });
    }
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/InventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 3))
    private Slot addInventorySlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 14 + (originalSlot.getContainerSlot() - 9) % 9 * 21,116 + (originalSlot.getContainerSlot() - 9) / 9 * 21);
    }
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/InventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 4))
    private Slot addHotbarSlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 14 + originalSlot.getContainerSlot() * 21,186);
    }
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/InventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 5))
    private Slot addSlotSixth(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot,111, 77,new LegacySlotDisplay(){
            public Offset getOffset() {
                return ScreenUtil.hasClassicCrafting() ? Offset.ZERO : EQUIP_SLOT_OFFSET;
            }
            public ResourceLocation getIconSprite() {
                return originalSlot.getItem().isEmpty() ? SHIELD_SLOT : null;
            }
        });
    }
}
