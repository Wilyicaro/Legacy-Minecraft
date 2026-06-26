package wily.legacy.mixin.base;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.ChatFormatting;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.component.ItemAttributeModifiers;
//? if neoforge {
/*import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import net.neoforged.neoforge.common.util.AttributeUtil;
*///?}
//?}
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
//? if >=1.21.5 {
/*import net.minecraft.world.item.component.TooltipDisplay;
*///?}
//? if >=1.20.5 {
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
//?}
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
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
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.Legacy4J;
import wily.legacy.config.LegacyCommonOptions;

import java.text.DecimalFormat;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
    private void interactLivingEntity(Player player, LivingEntity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        DyeColor color = Legacy4J.getDyeColorOrNull(stack.getItem());
        if (color == null || !(entity instanceof Shulker shulker) || shulker.getColor() == color) return;
        shulker.level().playSound(player, shulker, SoundEvents.DYE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
        if (!player.level().isClientSide()) {
            ((ShulkerAccessor) shulker).callSetVariant(Optional.of(color));
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    //? if <1.20.5 {
    /*@Inject(method = "getHoverName", at = @At("HEAD"), cancellable = true)
    private void getHoverName(CallbackInfoReturnable<Component> cir) {
        if (Legacy4J.isMushroomPore((ItemStack)(Object)this)) cir.setReturnValue(Component.translatable(Legacy4J.MUSHROOM_PORE_NAME));
    }
    *///?}

    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void getMapHoverName(CallbackInfoReturnable<Component> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!stack.is(Items.FILLED_MAP) || /*? if <1.20.5 {*//*stack.hasCustomHoverName()*//*?} else {*/stack.has(DataComponents.CUSTOM_NAME) || stack.has(DataComponents.MAP_POST_PROCESSING)/*?}*/) return;
        //? if <1.20.5 {
        /*Integer mapId = MapItem.getMapId(stack);
        if (mapId != null) cir.setReturnValue(stack.getItem().getName(stack).copy().append(Component.translatable("legacy.map.id", mapId)));
        *///?} else {
        MapId mapId = stack.get(DataComponents.MAP_ID);
        if (mapId != null) cir.setReturnValue(stack.getItem().getName(stack).copy().append(Component.translatable("legacy.map.id", mapId.id())));
        //?}
    }

    //? if <1.20.5 {
    /*@Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void getLegacyMapTooltipLines(Player player, TooltipFlag tooltipFlag, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!stack.is(Items.FILLED_MAP)) return;
        Integer mapId = MapItem.getMapId(stack);
        cir.getReturnValue().removeIf(component -> {
            String line = component.getString();
            return line.equals("filled_map.id") || line.startsWith("Id #") || mapId != null && line.equals("#" + mapId);
        });
    }
    *///?}

    //? if >=1.20.5 && <1.21.5 {
    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void getTooltipLines(Item.TooltipContext tooltipContext, Player player, TooltipFlag tooltipFlag, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!stack.is(Items.FILLED_MAP)) return;
        MapId mapId = stack.get(DataComponents.MAP_ID);
        if (mapId == null) return;
        List<Component> lines = cir.getReturnValue();
        String idLine = Component.translatable("filled_map.id", mapId.id()).getString();
        lines.removeIf(component -> {
            String line = component.getString();
            return line.equals(idLine) || line.equals("filled_map.id") || line.startsWith("Id #") || line.equals("#" + mapId.id());
        });
        MapItemSavedData data = tooltipContext.mapData(mapId);
        if (data == null) {
            lines.add(Component.translatable("filled_map.unknown").withStyle(ChatFormatting.GRAY));
            return;
        }
        MapPostProcessing postProcessing = stack.get(DataComponents.MAP_POST_PROCESSING);
        if (data.locked || postProcessing == MapPostProcessing.LOCK) {
            lines.add(Component.translatable("filled_map.locked").withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("legacy.map.level", Math.min(data.scale + (postProcessing == MapPostProcessing.SCALE ? 1 : 0), 4), 4).withStyle(ChatFormatting.GRAY));
    }
    //?}

    //? if <1.21.2 {
    @ModifyArg(method = "getTooltipLines",at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;withStyle(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = /*? if neoforge || (forge && <1.20.5) {*/ /*0*//*?} else {*/ 1/*?}*/))
    public ChatFormatting getTooltipLines(ChatFormatting arg) {
        return ChatFormatting.GOLD;
    }
    //?} else {
    /*@ModifyArg(method = "getStyledHoverName",at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/MutableComponent;withStyle(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/MutableComponent;", ordinal = /^? if neoforge {^/ /^0^//^?} else {^/1/^?}^/))
    public ChatFormatting getStyledHoverName(ChatFormatting arg) {
        return ChatFormatting.GOLD;
    }
    *///?}

    @Unique
    private DecimalFormat getLegacyDecimalFormat(DecimalFormat instance) {
        instance.setMinimumFractionDigits(FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat) ? 1 : 0);
        return instance;
    }

    //? if >=1.20.5 {
    @Shadow
    public abstract void forEachModifier(EquipmentSlotGroup par1, BiConsumer<Holder<Attribute>, AttributeModifier> par2);

    @Shadow protected abstract void addModifierTooltip(Consumer<Component> par1, Player par2, Holder<Attribute> par3, AttributeModifier par4);
    @ModifyExpressionValue(method = "addModifierTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;is(Lnet/minecraft/resources/ResourceLocation;)Z"))
    private boolean addModifierTooltip(boolean original) {
        return !FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat) && original;
    }

    @Inject(method = "addModifierTooltip", at = @At("HEAD"), cancellable = true)
    private void addModifierTooltip(Consumer<Component> consumer, Player player, Holder<Attribute> holder, AttributeModifier attributeModifier, CallbackInfo ci) {
        if (FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat) && attributeModifier.is(Item.BASE_ATTACK_SPEED_ID)) ci.cancel();
    }

    @ModifyExpressionValue(method = "addModifierTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;amount()D"))
    private double addModifierTooltip(double original, @Local(argsOnly = true) AttributeModifier modifier) {
        return (modifier.is(Item.BASE_ATTACK_DAMAGE_ID) ? Legacy4J.getItemDamageModifier((ItemStack) (Object)this) : 0) + original;
    }

    @Inject(method = "addModifierTooltip", at = @At("RETURN"))
    private void addModifierTooltipNoDamage(Consumer<Component> consumer, Player player, Holder<Attribute> holder, AttributeModifier attributeModifier, CallbackInfo ci, @Local(ordinal = 1) double e) {
        if (e == 0 && FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat)){
            consumer.accept(Component.translatable("attribute.modifier.plus." + attributeModifier.operation().id(), getLegacyDecimalFormat(ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT).format(e), Component.translatable(holder.value().getDescriptionId())).withStyle(ChatFormatting.GRAY));
        }
    }

    @ModifyReceiver(method = "addModifierTooltip", at = @At(value = "INVOKE", target = "Ljava/text/DecimalFormat;format(D)Ljava/lang/String;"))
    private DecimalFormat addModifierTooltip(DecimalFormat instance, double v) {
        return getLegacyDecimalFormat(instance);
    }

    @Redirect(method = "addAttributeTooltips", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V"))
    private void addAttributeTooltips(ItemStack instance, EquipmentSlotGroup equipmentSlotGroup, BiConsumer<Holder<Attribute>, AttributeModifier> biConsumer, Consumer<Component> consumer/*? if >=1.21.5 {*//*, TooltipDisplay tooltipDisplay*//*?}*/, @Nullable Player player) {
        Bearer<Boolean> noSpace = Bearer.of(true);
        forEachModifier(equipmentSlotGroup, (holder, attributeModifier)->{
            if (noSpace.get()){
                consumer.accept(CommonComponents.EMPTY);
                if (!FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat)) consumer.accept(Component.translatable("item.modifiers." + equipmentSlotGroup.getSerializedName()).withStyle(ChatFormatting.GRAY));
                noSpace.set(false);
            }
            this.addModifierTooltip(consumer, player, holder, attributeModifier);
        });
    }
    //?} else {
    /*@Shadow @Final public static DecimalFormat ATTRIBUTE_MODIFIER_FORMAT;
    @Unique
    private static final UUID BASE_ATTACK_DAMAGE_UUID = UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    @Redirect(method = "getTooltipLines", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;getId()Ljava/util/UUID;"))
    private UUID getTooltipLines(AttributeModifier instance) {
        return FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat) ? null : instance.getId();
    }

    @Redirect(method = "getTooltipLines", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false, ordinal = 6))
    private boolean getTooltipLines(List instance, Object e) {
        return FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat) ? false : instance.add(e);
    }

    @ModifyExpressionValue(method = "getTooltipLines", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/attributes/AttributeModifier;getAmount()D"))
    private double addModifierTooltip(double original, @Local(ordinal = 0) Map.Entry<Attribute, AttributeModifier> entry, @Local(ordinal = 0) List<Component> list) {
        double d = (entry.getValue().getId().equals(BASE_ATTACK_DAMAGE_UUID) ? Legacy4J.getItemDamageModifier((ItemStack) (Object)this) : 0) + original;
        if (d == 0 && FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat)){
            list.add(Component.translatable("attribute.modifier.plus." + entry.getValue().getOperation().toValue(), getLegacyDecimalFormat(ATTRIBUTE_MODIFIER_FORMAT).format(d), Component.translatable(entry.getKey().getDescriptionId())).withStyle(ChatFormatting.GRAY));
        }
        return d;
    }
    @ModifyReceiver(method = "getTooltipLines", at = @At(value = "INVOKE", target = "Ljava/text/DecimalFormat;format(D)Ljava/lang/String;"))
    private DecimalFormat addModifierTooltip(DecimalFormat instance, double v) {
        return getLegacyDecimalFormat(instance);
    }
    @Inject(method = "getAttributeModifiers", at = @At("RETURN"), cancellable = true)
    private void getAttributeModifiers(EquipmentSlot equipmentSlot, CallbackInfoReturnable<Multimap<Attribute, AttributeModifier>> cir) {
        Multimap<Attribute, AttributeModifier> attributes = cir.getReturnValue();
        if (FactoryConfig.hasCommonConfigEnabled(LegacyCommonOptions.legacyCombat) && attributes.containsKey(Attributes.ATTACK_SPEED)) {
            Multimap<Attribute, AttributeModifier> newAttributes = Multimaps.newListMultimap(new LinkedHashMap<>(), ArrayList::new);
            newAttributes.putAll(attributes);
            newAttributes.removeAll(Attributes.ATTACK_SPEED);
            cir.setReturnValue(newAttributes);
        }
    }
    *///?}
}
