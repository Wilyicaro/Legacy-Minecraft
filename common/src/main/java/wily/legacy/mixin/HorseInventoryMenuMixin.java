package wily.legacy.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.inventory.LegacySlotWrapper;

import static wily.legacy.LegacyMinecraftClient.*;

@Mixin(HorseInventoryMenu.class)
public class HorseInventoryMenuMixin {
    @Shadow @Final private AbstractHorse horse;

    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/HorseInventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 0))
    private Slot addSlotFirst(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(), 14, 21){
            @Override
            public ResourceLocation getIconSprite() {
                return getItem().isEmpty() ? SADDLE_SLOT_SPRITE : null;
            }

            public @Nullable Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return null;
            }
        };
    }
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/HorseInventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1))
    private Slot addSlotSecond(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(), 14, 42){
            @Override
            public ResourceLocation getIconSprite() {
                return getItem().isEmpty() ? horse instanceof Llama ? LLAMA_ARMOR_SLOT_SPRITE : ARMOR_SLOT_SPRITE : null;
            }
        };
    }
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/HorseInventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 2))
    private Slot addSlotThird(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(), 98 + (originalSlot.getContainerSlot() - 2) % ((AbstractChestedHorse)horse).getInventoryColumns()  * 21,21 + (originalSlot.getContainerSlot() - 2) / ((AbstractChestedHorse)horse).getInventoryColumns() * 21);
    }
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/HorseInventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 3))
    private Slot addInventorySlots(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(), 14 + (originalSlot.getContainerSlot() - 9) % 9 * 21,104 + (originalSlot.getContainerSlot() - 9) / 9 * 21);
    }
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/HorseInventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 4))
    private Slot addHotbarSlots(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(), 14 + originalSlot.getContainerSlot() * 21,174);
    }
}
