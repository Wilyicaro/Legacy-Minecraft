package wily.legacy.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.DynamicUtil;
import wily.factoryapi.util.ListMap;
import wily.legacy.Legacy4J;
import wily.legacy.client.CommonColor;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class LegacyTabButton extends AbstractButton {
    public static final ResourceLocation[][] SPRITES = new ResourceLocation[][]{new ResourceLocation[]{Legacy4J.createModLocation( "tiles/high_tab_left"),Legacy4J.createModLocation( "tiles/low_tab_left")}, new ResourceLocation[]{Legacy4J.createModLocation( "tiles/high_tab_middle"),Legacy4J.createModLocation( "tiles/low_tab_middle")}, new ResourceLocation[]{Legacy4J.createModLocation( "tiles/high_tab_right"),Legacy4J.createModLocation( "tiles/low_tab_right")},
            new ResourceLocation[]{Legacy4J.createModLocation( "tiles/high_vert_tab_up"),Legacy4J.createModLocation( "tiles/low_vert_tab_up")}, new ResourceLocation[]{Legacy4J.createModLocation( "tiles/high_vert_tab_middle"),Legacy4J.createModLocation( "tiles/low_vert_tab_middle")}, new ResourceLocation[]{Legacy4J.createModLocation( "tiles/high_vert_tab_down"),Legacy4J.createModLocation( "tiles/low_vert_tab_down")}};
    public static final Vec3 DEFAULT_DESACTIVE_OFFSET = new Vec3(0,22,0);
    public static final Vec3 DEFAULT_UNSELECTED_OFFSET = new Vec3(0,4,0);
    public final Function<LegacyTabButton,Renderable> icon;
    private final Consumer<LegacyTabButton> onPress;
    public boolean selected;
    protected int type;
    public Function<LegacyTabButton, Vec3> offset = (t)-> {
        if (!isActive()) return DEFAULT_DESACTIVE_OFFSET;
        if (!t.selected) return DEFAULT_UNSELECTED_OFFSET;
        return Vec3.ZERO;
    };

    public LegacyTabButton(int i, int j, int width, int height, int type, Function<LegacyTabButton,Renderable> icon, Component text, Tooltip tooltip, Consumer<LegacyTabButton> onPress) {
        super(i, j, width, height, text);
        setTooltip(tooltip);
        this.onPress = onPress;
        this.type = type;
        this.icon = icon;
    }

    @Override
    public void onPress() {
        selected = !selected;
        onPress.accept(this);
    }
    public static Function<LegacyTabButton,Renderable> iconOf(Item item){
        return t-> (guiGraphics, i, j, f) -> t.renderItemIcon(item.getDefaultInstance(),guiGraphics);
    }
    public static Function<LegacyTabButton,Renderable> iconOf(ItemStack stack){
        return t-> (guiGraphics, i, j, f) -> t.renderItemIcon(stack,guiGraphics);
    }
    public static Function<LegacyTabButton,Renderable> iconOf(Supplier<ItemStack> stack){
        return t-> (guiGraphics, i, j, f) -> t.renderItemIcon(stack.get(),guiGraphics);
    }
    public static Function<LegacyTabButton,Renderable> iconOf(ResourceLocation sprite){
        return t-> (guiGraphics, i, j, f) -> t.renderIconSprite(sprite,guiGraphics);
    }
    public  void renderString(GuiGraphics guiGraphics, Font font, int i, boolean shadow){
        guiGraphics.drawString(font,getMessage(),getX() + (width - font.width(getMessage())) / 2,getY() + (height - 7) / 2,i,shadow);
    }
    public  void renderIconSprite(ResourceLocation icon, GuiGraphics guiGraphics){
        FactoryGuiGraphics.of(guiGraphics).blitSprite(icon, getX() + width / 2 - 12, getY() + height / 2 - 12, 24, 24);
    }
    public  void renderItemIcon(ItemStack itemIcon, GuiGraphics guiGraphics){
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(getX() + width / 2f - 12, getY() + height / 2f - 12, 0);
        guiGraphics.pose().scale(1.5f, 1.5f, 1.5f);
        guiGraphics.renderItem(itemIcon, 0, 0);
        guiGraphics.pose().popPose();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        Minecraft minecraft = Minecraft.getInstance();
        FactoryGuiGraphics.of(guiGraphics).setColor(1.0f, 1.0f, 1.0f, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        guiGraphics.pose().pushPose();
        Vec3 translate = offset.apply(this);
        if (!translate.equals(Vec3.ZERO)) {
            guiGraphics.pose().translate(translate.x,translate.y,translate.z);
            isHovered = isMouseOver(i,j);
        }
        if (selected) guiGraphics.pose().translate(0F,0f,1F);
        FactoryGuiGraphics.of(guiGraphics).blitSprite(SPRITES[type][selected ? 0 : 1], getX(), getY(), getWidth(), this.getHeight());
        if (!selected) guiGraphics.pose().translate(0,-1,0);
        if (active) {
            if (icon == null) this.renderString(guiGraphics, minecraft.font, CommonColor.INVENTORY_GRAY_TEXT.get() | Mth.ceil(this.alpha * 255.0f) << 24);
            else icon.apply(this).render(guiGraphics,i,j,f);
        }
        guiGraphics.pose().popPose();
    }

    public boolean isMouseOver(double d, double e) {
        Vec3 translate = offset.apply(this);
        double x =  getX() + (translate.equals(Vec3.ZERO) ? 0 : translate.x());
        double y =  getY() + (translate.equals(Vec3.ZERO) ? 0 : translate.y());
        return this.active && this.visible && d >= x && e >= y && d < (x + this.width) && e < (y + this.height);
    }
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.tab", this.getMessage().getString()));
    }
    //? if <1.21.4 {
    /*@Override
    protected boolean clicked(double d, double e) {
        return isMouseOver(d,e);
    }
    *///?}
    @Override
    public void renderString(GuiGraphics guiGraphics, Font font, int i) {
        renderString(guiGraphics,font,i,false);
    }

    public record IconType<T>(Codec<T> contentCodec, Function<T,Function<LegacyTabButton,Renderable>> createIcon){
        public DataResult<IconHolder<T>> parse(ResourceLocation typeId, Dynamic<?> dynamic){
            return contentCodec.parse(dynamic).map(c-> new IconHolder<>(typeId,this,c));
        }
    }
    public static final ResourceLocation DEFAULT_ICON_TYPE_ID = FactoryAPI.createVanillaLocation("sprite");
    public static final IconType<ResourceLocation> DEFAULT_ICON_TYPE = new IconType<>(ResourceLocation.CODEC,LegacyTabButton::iconOf);
    public static final ListMap<ResourceLocation, IconType<?>> ICON_TYPES = new ListMap.Builder<ResourceLocation, IconType<?>>().put(DEFAULT_ICON_TYPE_ID, DEFAULT_ICON_TYPE).put(FactoryAPI.createVanillaLocation("item"), new IconType<>(DynamicUtil.ITEM_SUPPLIER_CODEC, LegacyTabButton::iconOf)).build();
    public static final Codec<IconHolder<?>> DEPRECATED_ICON_HOLDER_CODEC = createIconHolderCodec("iconType","icon");
    public static final Codec<IconHolder<?>> ICON_HOLDER_CODEC = Codec.either(ResourceLocation.CODEC.xmap(r->new IconHolder<>(DEFAULT_ICON_TYPE_ID, DEFAULT_ICON_TYPE,r), h-> h.content), createIconHolderCodec("type","value")).xmap(c-> c.right().orElseGet(c.left()::get), Either::right);

    public static Codec<IconHolder<?>> createIconHolderCodec(String typeField, String valueField){
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<IconHolder<?>, T>> decode(DynamicOps<T> ops, T input) {
                Dynamic<T> dynamic = new Dynamic<>(ops,input);
                DataResult<ResourceLocation> idResult = dynamic.get(typeField).flatMap(ResourceLocation.CODEC::parse);
                if (idResult.result().isEmpty()) idResult = DataResult.success(DEFAULT_ICON_TYPE_ID);
                return idResult.flatMap(f->dynamic.get(valueField).flatMap(d-> ICON_TYPES.get(f).parse(f,d).map(c-> Pair.of(c,input))));
            }

            @Override
            public <T> DataResult<T> encode(IconHolder<?> input, DynamicOps<T> ops, T prefix) {
                Dynamic<T> dynamic = new Dynamic<>(ops,prefix);
                dynamic.set(typeField, dynamic.createString(input.type.toString()));
                input.encodeContent(ops).result().ifPresent(d->dynamic.set(valueField,d));
                return DataResult.success(prefix);
            }
        };
    }

    public record IconHolder<T>(ResourceLocation typeId, IconType<T> type, T content, Function<LegacyTabButton,Renderable> icon){
        public IconHolder(ResourceLocation typeId, IconType<T> type, T content){
            this(typeId,type,content,type.createIcon.apply(content));
        }
        public <C> DataResult<Dynamic<C>> encodeContent(DynamicOps<C> ops){
            return type.contentCodec.encodeStart(ops,content).map(c-> new Dynamic<>(ops, c));
        }
    }
}
