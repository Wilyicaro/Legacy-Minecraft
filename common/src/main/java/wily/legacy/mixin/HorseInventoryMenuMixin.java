package wily.legacy.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.util.LegacySprites;
import wily.legacy.inventory.LegacySlotDisplay;

@Mixin(HorseInventoryMenu.class)
public class HorseInventoryMenuMixin {
    @Shadow @Final private AbstractHorse horse;

    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/HorseInventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 0))
    private Slot addSlotFirst(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 14, 21,new LegacySlotDisplay(){
            @Override
            public ResourceLocation getIconSprite() {
                return originalSlot.getItem().isEmpty() ? LegacySprites.SADDLE_SLOT : null;
            }
        });
    }
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/HorseInventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1))
    private Slot addSlotSecond(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 14, 42,new LegacySlotDisplay(){
            @Override
            public ResourceLocation getIconSprite() {
                return originalSlot.getItem().isEmpty() ? horse instanceof Llama ? LegacySprites.LLAMA_ARMOR_SLOT : LegacySprites.ARMOR_SLOT : null;
            }
        });
    }
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/HorseInventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 2))
    private Slot addSlotThird(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 98 + (originalSlot.getContainerSlot() - 1) % ((AbstractChestedHorse)horse).getInventoryColumns()  * 21,21 + (originalSlot.getContainerSlot() - 1) / ((AbstractChestedHorse)horse).getInventoryColumns() * 21);
    }
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/HorseInventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 3))
    private Slot addInventorySlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 14 + (originalSlot.getContainerSlot() - 9) % 9 * 21,104 + (originalSlot.getContainerSlot() - 9) / 9 * 21);
    }
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/HorseInventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 4))
    private Slot addHotbarSlots(Slot originalSlot){
        return LegacySlotDisplay.override(originalSlot, 14 + originalSlot.getContainerSlot() * 21,174);
    }
}
