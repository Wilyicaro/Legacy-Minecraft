package wily.legacy.mixin.base;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.apache.commons.lang3.function.TriConsumer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.util.LegacyItemAttributeDisplay;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements DataComponentHolder {


    private ItemStack self() {
        return (ItemStack) (Object) this;
    }

    @ModifyArg(method = "getStyledHoverName",at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;withStyle(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = /*? if neoforge {*/ /*0*//*?} else {*/1/*?}*/))
    public ChatFormatting getStyledHoverName(ChatFormatting arg) {
        return ChatFormatting.GOLD;
    }

    @Shadow public abstract void forEachModifier(EquipmentSlotGroup arg, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> triConsumer);

    @Redirect(method = "addAttributeTooltips", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V"))
    private void addAttributeTooltips(ItemStack instance, EquipmentSlotGroup equipmentSlotGroup, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> arg, Consumer<Component> consumer/*? if >=1.21.5 {*/, TooltipDisplay tooltipDisplay/*?}*/, @Nullable Player player) {
        Bearer<Boolean> noSpace = Bearer.of(true);
        forEachModifier(equipmentSlotGroup, (holder, attributeModifier, display)->{
            if (noSpace.get()){
                consumer.accept(CommonComponents.EMPTY);
                if (!FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat)) consumer.accept(Component.translatable("item.modifiers." + equipmentSlotGroup.getSerializedName()).withStyle(ChatFormatting.GRAY));
                noSpace.set(false);
            }
            display.apply(consumer, player, holder, attributeModifier);
        });
    }

    @Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Lorg/apache/commons/lang3/function/TriConsumer;)V", at = @At("HEAD"), cancellable = true)
    public void forEachModifier(EquipmentSlotGroup equipmentSlotGroup, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> triConsumer, CallbackInfo ci) {
        if (FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat)) {
            ci.cancel();
            LegacyItemAttributeDisplay display = new LegacyItemAttributeDisplay(self());
            ItemAttributeModifiers itemAttributeModifiers = this.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
            itemAttributeModifiers.forEach(equipmentSlotGroup, ((attributeHolder, attributeModifier, u) -> triConsumer.accept(attributeHolder, attributeModifier, display)));
            EnchantmentHelper.forEachModifier(self(), equipmentSlotGroup, (holder, attributeModifier) -> triConsumer.accept(holder, attributeModifier, new LegacyItemAttributeDisplay(self())));
        }
    }
}
