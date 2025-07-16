package wily.legacy.client.screen;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
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
import wily.factoryapi.util.FactoryScreenUtil;
import wily.factoryapi.util.ListMap;
import wily.legacy.client.CommonColor;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class LegacyTabButton extends AbstractButton {
    public static final Vec3 DEFAULT_INACTIVE_OFFSET = new Vec3(0,22,0);
    public static final Vec3 DEFAULT_UNSELECTED_OFFSET = new Vec3(0,4,0);
    public final Render icon;
    public Render spriteRender = ToggleableTabSprites.DEFAULT;
    private final Consumer<LegacyTabButton> onPress;
    public boolean selected;
    public Type type;

    public Function<LegacyTabButton, Vec3> offset = (t)-> {
        if (!isActive()) return DEFAULT_INACTIVE_OFFSET;
        if (!t.selected) return DEFAULT_UNSELECTED_OFFSET;
        return Vec3.ZERO;
    };

    public LegacyTabButton(int i, int j, int width, int height, Type type, Render icon, Component text, Tooltip tooltip, Consumer<LegacyTabButton> onPress) {
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

    public static Render iconOf(Item item){
        return (t, guiGraphics, i, j, f) -> t.renderItemIcon(item.getDefaultInstance(),guiGraphics);
    }

    public static Render iconOf(ItemStack stack){
        return (t, guiGraphics, i, j, f) -> t.renderItemIcon(stack,guiGraphics);
    }

    public static Render iconOf(Supplier<ItemStack> stack){
        return (t, guiGraphics, i, j, f) -> t.renderItemIcon(stack.get(), guiGraphics);
    }

    public static Render iconOf(ResourceLocation sprite){
        return (t, guiGraphics, i, j, f) -> t.renderIconSprite(sprite,guiGraphics);
    }

    public static Render iconOf(ModsScreen.SizedLocation sized){
        return (t, guiGraphics, i, j, f) -> FactoryGuiGraphics.of(guiGraphics).blit(sized.location(), t.getX() + t.getWidth() / 2 - 12, t.getY() + t.getHeight() / 2 - 12, 0, 0, 24, sized.getScaledHeight(24), 24, sized.getScaledHeight(24));
    }

    public void renderString(GuiGraphics guiGraphics, Font font, int i, boolean shadow){
        renderString(guiGraphics,font,getX(),getY(),i,shadow);
    }

    public void renderString(GuiGraphics guiGraphics, Font font, int x, int y, int i, boolean shadow){
        LegacyRenderUtil.renderScrollingString(guiGraphics,font,getMessage(),x + Math.max(6, (getWidth() - font.width(getMessage())) / 2), y - 2, x + getWidth() - 6, y + getHeight() - 1, i,shadow);
    }

    public  void renderIconSprite(ResourceLocation icon, GuiGraphics guiGraphics){
        FactoryGuiGraphics.of(guiGraphics).blitSprite(icon, getX() + width / 2 - 12, getY() + height / 2 - 12, 24, 24);
    }

    public  void renderItemIcon(ItemStack itemIcon, GuiGraphics guiGraphics){
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(getX() + width / 2f - 12, getY() + height / 2f - 12);
        guiGraphics.pose().scale(1.5f, 1.5f);
        guiGraphics.renderItem(itemIcon, 0, 0);
        guiGraphics.pose().popMatrix();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        Minecraft minecraft = Minecraft.getInstance();
        FactoryGuiGraphics.of(guiGraphics).setBlitColor(1.0f, 1.0f, 1.0f, this.alpha);
        FactoryScreenUtil.enableBlend();
        FactoryScreenUtil.enableDepthTest();
        guiGraphics.pose().pushMatrix();
        Vec3 translate = offset.apply(this);
        if (!translate.equals(Vec3.ZERO)) {
            guiGraphics.pose().translate((float)translate.x, (float)translate.y);
            isHovered = isMouseOver(i,j);
        }
        spriteRender.render(this, guiGraphics, i, j, f);
        if (!selected) guiGraphics.pose().translate(0,-1);
        if (active) {
            if (icon == null) this.renderString(guiGraphics, minecraft.font, CommonColor.INVENTORY_GRAY_TEXT.get() | Mth.ceil(this.alpha * 255.0f) << 24);
            else icon.render(this, guiGraphics,i,j,f);
        }
        guiGraphics.pose().popMatrix();
    }

    public boolean isMouseOver(double d, double e) {
        Vec3 translate = offset.apply(this);
        double x = getX() + (translate.equals(Vec3.ZERO) ? 0 : translate.x());
        double y = getY() + (translate.equals(Vec3.ZERO) ? 0 : translate.y());
        return this.active && this.visible && d >= x && e >= y && d < (x + this.width) && e < (y + this.height);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.tab", this.getMessage().getString()));
    }

    @Override
    public void renderString(GuiGraphics guiGraphics, Font font, int i) {
        renderString(guiGraphics,font,i,false);
    }


    public interface Render {
        void render(LegacyTabButton button, GuiGraphics guiGraphics, int i, int j, float f);
    }

    public interface SpriteRender extends Render {
        ResourceLocation getSprite(LegacyTabButton button);
        @Override
        default void render(LegacyTabButton button, GuiGraphics guiGraphics, int i, int j, float f) {
            FactoryGuiGraphics.of(guiGraphics).blitSprite(getSprite(button), button.getX(), button.getY(), button.getWidth(), button.getHeight());
        }
    }

    public record TabSprite(ResourceLocation sprite) implements SpriteRender {
        public static final Codec<TabSprite> CODEC = ResourceLocation.CODEC.xmap(TabSprite::new, TabSprite::sprite);

        @Override
        public ResourceLocation getSprite(LegacyTabButton button) {
            return sprite();
        }
    }

    public enum Type {
        LEFT,MIDDLE,RIGHT;

        public static Type bySize(int actual, int size) {
            return actual == 0 ? LEFT : actual >= size ? RIGHT : MIDDLE;
        }
    }

    public record ToggleableTabSprites(TabSprites high, TabSprite down) implements SpriteRender {
        public static final ToggleableTabSprites DEFAULT = new ToggleableTabSprites(new TabSprites(LegacySprites.HIGH_TAB_LEFT, LegacySprites.HIGH_TAB_MIDDLE, LegacySprites.HIGH_TAB_RIGHT), new TabSprite(LegacySprites.LOW_TAB));
        public static final ToggleableTabSprites VERTICAL = new ToggleableTabSprites(new TabSprites(LegacySprites.HIGH_VERT_TAB_DOWN, LegacySprites.HIGH_VERT_TAB_MIDDLE, LegacySprites.HIGH_VERT_TAB_UP), new TabSprite(LegacySprites.LOW_VERT_TAB));
        public static final Codec<ToggleableTabSprites> CODEC = RecordCodecBuilder.create(i-> i.group(TabSprites.CODEC.fieldOf("high").forGetter(ToggleableTabSprites::high), TabSprite.CODEC.fieldOf("down").forGetter(ToggleableTabSprites::down)).apply(i, ToggleableTabSprites::new));

        @Override
        public ResourceLocation getSprite(LegacyTabButton button) {
            return button.selected ? high.getSprite(button) : down.getSprite(button);
        }
    }

    public record TabSprites(ResourceLocation left, ResourceLocation middle, ResourceLocation right) implements SpriteRender {
        public static final Codec<TabSprites> CODEC = RecordCodecBuilder.create(i-> i.group(ResourceLocation.CODEC.fieldOf("left").forGetter(TabSprites::left),ResourceLocation.CODEC.fieldOf("middle").forGetter(TabSprites::middle),ResourceLocation.CODEC.fieldOf("right").forGetter(TabSprites::right)).apply(i, TabSprites::new));

        public ResourceLocation byType(Type type){
            return switch (type){
                case LEFT -> left;
                case MIDDLE -> middle;
                case RIGHT -> right;
            };
        }

        @Override
        public ResourceLocation getSprite(LegacyTabButton button) {
            return byType(button.type);
        }
    }

    public record IconType<T>(Codec<T> contentCodec, Function<T,Render> createIcon){
        public DataResult<IconHolder<T>> parse(ResourceLocation typeId, Dynamic<?> dynamic){
            return contentCodec.parse(dynamic).map(c-> new IconHolder<>(typeId,this,c));
        }
    }
    public static final ResourceLocation DEFAULT_ICON_TYPE_ID = FactoryAPI.createVanillaLocation("sprite");
    public static final IconType<ResourceLocation> DEFAULT_ICON_TYPE = new IconType<>(ResourceLocation.CODEC,LegacyTabButton::iconOf);
    public static final ListMap<ResourceLocation, IconType<?>> ICON_TYPES = new ListMap.Builder<ResourceLocation, IconType<?>>().put(DEFAULT_ICON_TYPE_ID, DEFAULT_ICON_TYPE).put(FactoryAPI.createVanillaLocation("item"), new IconType<>(DynamicUtil.ITEM_SUPPLIER_CODEC, LegacyTabButton::iconOf)).build();
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

    public record IconHolder<T>(ResourceLocation typeId, IconType<T> type, T content, Render icon){
        public IconHolder(ResourceLocation typeId, IconType<T> type, T content){
            this(typeId,type,content,type.createIcon.apply(content));
        }
        public <C> DataResult<Dynamic<C>> encodeContent(DynamicOps<C> ops){
            return type.contentCodec.encodeStart(ops,content).map(c-> new Dynamic<>(ops, c));
        }
    }
}
