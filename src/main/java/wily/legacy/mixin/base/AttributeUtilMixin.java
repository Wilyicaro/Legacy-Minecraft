//? if >=1.20.5 && neoforge {
/*package wily.legacy.mixin.base;

import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IAttributeExtension;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import net.neoforged.neoforge.common.util.AttributeUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.Legacy4J;import wily.legacy.config.LegacyCommonOptions;

import java.util.function.Consumer;

@Mixin(value = AttributeUtil.class, remap = false)
public class AttributeUtilMixin {
    @WrapWithCondition(method = "applyModifierTooltips", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V", ordinal = 1))
    private static boolean applyModifierTooltips(Consumer instance, Object object) {
        return !FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat);
    }

    @ModifyExpressionValue(method = "applyTextFor", at = @At(value = "INVOKE", target = "Lnet/minecraft/resources/ResourceLocation;equals(Ljava/lang/Object;)Z"))
    private static boolean applyTextFor(boolean original) {
        return original && !FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat);
    }

    @ModifyExpressionValue(method = "applyModifierTooltips", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/util/AttributeUtil;getSortedModifiers(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlotGroup;)Lcom/google/common/collect/Multimap;"))
    private static Multimap<Holder<Attribute>, AttributeModifier> addModifierTooltip(Multimap<Holder<Attribute>, AttributeModifier> original) {
        if (FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat)) original.values().removeIf(m-> m.is(Item.BASE_ATTACK_SPEED_ID));
        return original;
    }

    @ModifyArg(method = "applyTextFor", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/Attribute;toComponent(Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;Lnet/minecraft/world/item/TooltipFlag;)Lnet/minecraft/network/chat/MutableComponent;"))
    private static AttributeModifier applyTextFor(AttributeModifier par1, @Local(argsOnly = true) ItemStack stack) {
        return par1.is(Item.BASE_ATTACK_DAMAGE_ID) && FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat) ? new AttributeModifier(par1.id(), Legacy4J.getItemDamageModifier(stack) + par1.amount(), par1.operation()) : par1;
    }

    @ModifyExpressionValue(method = "applyTextFor", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;amount()D", ordinal = 6))
    private static double applyTextFor(double original, ItemStack stack, Consumer<Component> tooltip, Multimap<Holder<Attribute>, AttributeModifier> modifierMap, AttributeTooltipContext ctx, @Local AttributeModifier modifier, @Local Holder<Attribute> attribute) {
        if (original == 0 && FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat)){
            tooltip.accept(Component.translatable("neoforge.modifier.plus", attribute.value().toValueComponent(modifier.operation(), 0, ctx.flag()), Component.translatable(attribute.value().getDescriptionId())).withStyle(ChatFormatting.GRAY).append(attribute.value().getDebugInfo(modifier, ctx.flag())));
        }
        return original;
    }
}
*///?}