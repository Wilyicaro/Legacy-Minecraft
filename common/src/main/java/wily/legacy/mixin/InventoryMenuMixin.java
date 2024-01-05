package wily.legacy.mixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.legacy.LegacyMinecraft;
import wily.legacy.client.LegacyOptions;
import wily.legacy.inventory.LegacySlotWrapper;

import static wily.legacy.LegacyMinecraftClient.SHIELD_SLOT_SPRITE;

@Mixin(InventoryMenu.class)
public class InventoryMenuMixin {
    @Shadow @Final private static EquipmentSlot[] SLOT_IDS;
    private boolean hasClassicCrafting(){
        return ((LegacyOptions)Minecraft.getInstance().options).classicCrafting().get();
    }
    private static final Vec3 EQUIP_SLOT_TRANSLATION = new Vec3(50,0,0);

    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/InventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 0))
    private Slot addSlotFirst(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(), 180, 40){
            public boolean isActive() {
                return hasClassicCrafting();
            }
        };
    }
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/InventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 1))
    private Slot addSlotSecond(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(), 111 + originalSlot.getContainerSlot() % 2 * 21, 30 + originalSlot.getContainerSlot() / 2 * 21){
            public boolean isActive() {
                return hasClassicCrafting();
            }
        };
    }
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/InventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 2))
    private Slot addSlotThird(Slot originalSlot){
        int i = 39 - originalSlot.getContainerSlot();
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(),14, 14 + i * 21){
            public @Nullable Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return null;
            }
            public ResourceLocation getIconSprite() {
                return getItem().isEmpty() ? new ResourceLocation(LegacyMinecraft.MOD_ID,"container/"+ SLOT_IDS[i].getName()+ "_slot") : null;
            }
            public Vec3 getTranslation() {
                return hasClassicCrafting() ? null : EQUIP_SLOT_TRANSLATION;
            }
        };
    }
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/InventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 3))
    private Slot addInventorySlots(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(), 14 + (originalSlot.getContainerSlot() - 9) % 9 * 21,116 + (originalSlot.getContainerSlot() - 9) / 9 * 21);
    }
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/InventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 4))
    private Slot addHotbarSlots(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(), 14 + originalSlot.getContainerSlot() * 21,181);
    }
    @ModifyArg(method = "<init>",at = @At(value = "INVOKE",target = "Lnet/minecraft/world/inventory/InventoryMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;", ordinal = 5))
    private Slot addSlotSixth(Slot originalSlot){
        return new LegacySlotWrapper(originalSlot, originalSlot.container,originalSlot.getContainerSlot(),111, 77){
            public Vec3 getTranslation() {
                return hasClassicCrafting() ? null : EQUIP_SLOT_TRANSLATION;
            }
            public ResourceLocation getIconSprite() {
                return getItem().isEmpty() ? SHIELD_SLOT_SPRITE : null;
            }

            @Override
            public @Nullable Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
                return null;
            }
        };
    }
}
