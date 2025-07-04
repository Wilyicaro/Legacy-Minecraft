package wily.legacy.util;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.function.Consumer;

public record LegacyItemAttributeDisplay(ItemStack context) implements ItemAttributeModifiers.Display {
    public static final Codec<LegacyItemAttributeDisplay> CODEC = ItemStack.CODEC.xmap(LegacyItemAttributeDisplay::new, LegacyItemAttributeDisplay::context);
    static final StreamCodec<RegistryFriendlyByteBuf, LegacyItemAttributeDisplay> STREAM_CODEC = ItemStack.STREAM_CODEC.map(LegacyItemAttributeDisplay::new, LegacyItemAttributeDisplay::context);
    public static final DecimalFormat ATTRIBUTE_MODIFIER_FORMAT = Util.make(new DecimalFormat("#.##"), (decimalFormat) -> {
        decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
        decimalFormat.setMinimumFractionDigits(1);
    });


    public Type type() {
        return ItemAttributeModifiers.Display.Type.DEFAULT;
    }

    public void apply(Consumer<Component> consumer, @Nullable Player arg, Holder<Attribute> arg2, AttributeModifier arg3) {
        if (arg3.is(Item.BASE_ATTACK_SPEED_ID)) return;

        double d = arg3.amount() + LegacyItemUtil.getItemDamageModifier(context());

        double e;
        if (arg3.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_BASE && arg3.operation() != AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
            if (arg2.is(Attributes.KNOCKBACK_RESISTANCE)) {
                e = d * (double)10.0F;
            } else {
                e = d;
            }
        } else {
            e = d * (double)100.0F;
        }

        if (d >= 0.0F) {
            consumer.accept(Component.translatable("attribute.modifier.plus." + arg3.operation().id(), ATTRIBUTE_MODIFIER_FORMAT.format(e), Component.translatable(arg2.value().getDescriptionId())).withStyle(d == 0.0 ? ChatFormatting.GRAY : arg2.value().getStyle(true)));
        } else if (d < 0.0F) {
            consumer.accept(Component.translatable("attribute.modifier.take." + arg3.operation().id(), ATTRIBUTE_MODIFIER_FORMAT.format(-e), Component.translatable(arg2.value().getDescriptionId())).withStyle(arg2.value().getStyle(false)));
        }
    }
}
