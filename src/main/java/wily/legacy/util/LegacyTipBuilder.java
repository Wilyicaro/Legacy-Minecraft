package wily.legacy.util;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import wily.factoryapi.base.network.CommonNetwork;
import wily.factoryapi.util.DynamicUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class LegacyTipBuilder {
    public static final Codec<LegacyTipBuilder> COMPLETE_CODEC = RecordCodecBuilder.create(i-> i.group(DynamicUtil.getComponentCodec().optionalFieldOf("title").forGetter(LegacyTipBuilder::getTitle),DynamicUtil.getComponentCodec().optionalFieldOf("tip").forGetter(LegacyTipBuilder::getTip), DynamicUtil.ITEM_CODEC.fieldOf("itemIcon").orElse(ItemStack.EMPTY).forGetter(LegacyTipBuilder::getItem), Codec.INT.fieldOf("time").orElse(-1).forGetter(LegacyTipBuilder::getTime)).apply(i, LegacyTipBuilder::create));
    public static final Codec<LegacyTipBuilder> CODEC = Codec.either(DynamicUtil.getComponentCodec().xmap(c-> new LegacyTipBuilder().tip(c), b-> b.tip), COMPLETE_CODEC).xmap(e-> e.right().orElseGet(e.left()::get), Either::right);
    public static final Codec<Map<String, LegacyTipBuilder>> MAP_CODEC = Codec.unboundedMap(Codec.STRING, CODEC).xmap(HashMap::new, Function.identity());
    public static final Codec<List<LegacyTipBuilder>> LIST_CODEC = CODEC.listOf();

    private Component title = null;
    private Component tip = null;
    private ItemStack itemIcon = ItemStack.EMPTY;
    private int time = -1;

    public static LegacyTipBuilder create(Optional<Component> title, Optional<Component> tip, ItemStack itemIcon, int time) {
        return create(title.orElse(null), tip.orElse(null), itemIcon, time);
    }

    public static LegacyTipBuilder create(Component title, Component tip, ItemStack itemIcon, int time) {
        return new LegacyTipBuilder().title(title).tip(tip).itemIcon(itemIcon).disappearTime(time);
    }

    public static String getTipId(Item item){
        return item.getDescriptionId() + ".tip";
    }

    public static Component getTip(ItemStack stack){
        if (stack.getHoverName().getContents() instanceof TranslatableContents contents){
            return Component.translatable(contents.getKey() + ".tip");
        }
        return Component.translatable(getTipId(stack.getItem()));
    }

    public static String getTipId(EntityType<?> item){
        return item.getDescriptionId() + ".tip";
    }


    public LegacyTipBuilder title(Component title){
        this.title = title;
        return this;
    }

    public LegacyTipBuilder tip(Component tip){
        this.tip = tip;
        return this;
    }

    public LegacyTipBuilder itemIcon(ItemStack itemIcon){
        this.itemIcon = itemIcon;
        return this;
    }

    public LegacyTipBuilder item(ItemStack itemIcon){
        if (!itemIcon.isEmpty()) {
            return itemIcon(itemIcon).title(itemIcon.getHoverName()).tip(getTip(itemIcon));
        }
        return this;
    }

    public LegacyTipBuilder disappearTime(int time){
        this.time = time;
        return this;
    }

    public LegacyTipBuilder copyFrom(LegacyTipBuilder builder){
        return copyFrom(builder, false);
    }

    public LegacyTipBuilder copyFrom(LegacyTipBuilder builder, boolean modifier){
        if (!builder.getItem().isEmpty()) {
            if (modifier) itemIcon(builder.getItem());
            else item(builder.getItem());
        }
        if (builder.title != null) title(builder.title);
        if (builder.tip != null) tip(builder.tip);
        if (builder.getTime() >= 0) disappearTime(builder.getTime());
        return this;
    }

    public Optional<Component> getTitle(){
        return Optional.ofNullable(title);
    }

    public Optional<Component> getTip(){
        return Optional.ofNullable(tip);
    }

    public ItemStack getItem(){
        return itemIcon;
    }

    public int getTime(){
        return time;
    }

    public static LegacyTipBuilder decode(CommonNetwork.PlayBuf buf){
        return LegacyTipBuilder.create(buf.get().readOptional(b->CommonNetwork.decodeComponent(buf)),buf.get().readOptional(b->CommonNetwork.decodeComponent(buf)),CommonNetwork.decodeItemStack(buf),buf.get().readInt());
    }

    public void encode(CommonNetwork.PlayBuf buf) {
        buf.get().writeOptional(getTitle(), (b, t)-> CommonNetwork.encodeComponent(buf,t));
        buf.get().writeOptional(getTip(), (b, t)-> CommonNetwork.encodeComponent(buf,t));
        CommonNetwork.encodeItemStack(buf,itemIcon);
        buf.get().writeInt(time);
    }
}