package wily.legacy.mixin.base;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import net.minecraft.ChatFormatting;
//? if >=1.20.5 {
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
//? if neoforge {
/*import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import net.neoforged.neoforge.common.util.AttributeUtil;
*///?}
//?}
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.factoryapi.base.Bearer;
import wily.legacy.Legacy4J;
import wily.legacy.config.LegacyConfig;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @ModifyArg(method = "getTooltipLines",at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;withStyle(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = /*? if neoforge {*/ /*0*//*?} else {*/ 1/*?}*/))
    public ChatFormatting getTooltipLines(ChatFormatting arg) {
        return ChatFormatting.GOLD;
    }


    //? if >=1.20.5 {
    @Shadow
    public abstract void forEachModifier(EquipmentSlotGroup par1, BiConsumer<Holder<Attribute>, AttributeModifier> par2);

    @Shadow protected abstract void addModifierTooltip(Consumer<Component> par1, Player par2, Holder<Attribute> par3, AttributeModifier par4);
    //? if neoforge {

    /*@Shadow protected abstract void addAttributeTooltips(Consumer<Component> par1, Player par2);

    // A temporary fix for not having the Legacy attributes format in NeoForge, as the way NeoForge did the Attributes event doesn't allow many changes, even with mixin
    @Redirect(method = "getTooltipLines", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/util/AttributeUtil;addAttributeTooltips(Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;Lnet/neoforged/neoforge/common/util/AttributeTooltipContext;)V", remap = false))
    private void replaceNeoForgeAttributesEvent(ItemStack stack, Consumer<Component> tooltip, AttributeTooltipContext ctx, Item.TooltipContext arg, Player arg2, TooltipFlag arg3) {
        if (LegacyConfig.legacyCombat.get()) addAttributeTooltips(tooltip, arg2);
        else AttributeUtil.addAttributeTooltips(stack, tooltip, ctx);
    }
    *///?}

    @Redirect(method = "addModifierTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;is(Lnet/minecraft/resources/ResourceLocation;)Z"))
    private boolean addModifierTooltip(AttributeModifier instance, ResourceLocation location) {
        return !LegacyConfig.legacyCombat.get() && instance.is(location);
    }

    @Inject(method = "addModifierTooltip", at = @At("HEAD"), cancellable = true)
    private void addModifierTooltip(Consumer<Component> consumer, Player player, Holder<Attribute> holder, AttributeModifier attributeModifier, CallbackInfo ci) {
        if (LegacyConfig.legacyCombat.get() && attributeModifier.is(Item.BASE_ATTACK_SPEED_ID)) ci.cancel();
    }

    @Redirect(method = "addModifierTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;amount()D"))
    private double addModifierTooltip(AttributeModifier instance) {
        return (instance.is(Item.BASE_ATTACK_DAMAGE_ID) ? Legacy4J.getItemDamageModifier((ItemStack) (Object)this) : 0) + instance.amount();
    }

    @Redirect(method = "addModifierTooltip", at = @At(value = "INVOKE", target = "Ljava/text/DecimalFormat;format(D)Ljava/lang/String;"))
    private String addModifierTooltip(DecimalFormat instance, double v) {
        instance.setMinimumFractionDigits(LegacyConfig.legacyCombat.get() ? 1 : 0);
        return instance.format(v);
    }

    @Redirect(method = "addAttributeTooltips", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V"))
    private void addAttributeTooltips(ItemStack instance, EquipmentSlotGroup equipmentSlotGroup, BiConsumer<Holder<Attribute>, AttributeModifier> biConsumer, Consumer<Component> consumer, @Nullable Player player) {
        Bearer<Boolean> noSpace = Bearer.of(true);
        forEachModifier(equipmentSlotGroup, (holder, attributeModifier)->{
            if (noSpace.get()){
                consumer.accept(CommonComponents.EMPTY);
                if (!LegacyConfig.legacyCombat.get()) consumer.accept(Component.translatable("item.modifiers." + equipmentSlotGroup.getSerializedName()).withStyle(ChatFormatting.GRAY));
                noSpace.set(false);
            }
            this.addModifierTooltip(consumer, player, holder, attributeModifier);
        });
    }
    //?} else {
    /*@Unique
    private static final UUID BASE_ATTACK_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    @Redirect(method = "getTooltipLines", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;getId()Ljava/util/UUID;"))
    private UUID getTooltipLines(AttributeModifier instance) {
        return LegacyConfig.legacyCombat.get() ? null : instance.getId();
    }
    @Redirect(method = "getTooltipLines", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false, ordinal = 6))
    private boolean getTooltipLines(List instance, Object e) {
        return LegacyConfig.legacyCombat.get() ? false : instance.add(e);
    }
    @Redirect(method = "getTooltipLines", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;getAmount()D"))
    private double addModifierTooltip(AttributeModifier instance) {
        return ((instance.getId().equals(BASE_ATTACK_DAMAGE_UUID)) ? Legacy4J.getItemDamageModifier((ItemStack) (Object)this) : 0) + instance.getAmount();
    }
    @Redirect(method = "getTooltipLines", at = @At(value = "INVOKE", target = "Ljava/text/DecimalFormat;format(D)Ljava/lang/String;"))
    private String addModifierTooltip(DecimalFormat instance, double v) {
        instance.setMinimumFractionDigits(LegacyConfig.legacyCombat.get() ? 1 : 0);
        return instance.format(v);
    }
    @Inject(method = "getAttributeModifiers", at = @At("RETURN"), cancellable = true)
    private void getAttributeModifiers(EquipmentSlot equipmentSlot, CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) {
        Multimap<Attribute, AttributeModifier> attributes = cir.getReturnValue();
        if (LegacyConfig.legacyCombat.get() && attributes.containsKey(Attributes.ATTACK_SPEED)) {
            Multimap<Attribute, AttributeModifier> newAttributes = Multimaps.newListMultimap(new LinkedHashMap<>(), ArrayList::new);
            newAttributes.putAll(attributes);
            newAttributes.removeAll(Attributes.ATTACK_SPEED);
            cir.setReturnValue(newAttributes);
        }
    }
    *///?}
}
