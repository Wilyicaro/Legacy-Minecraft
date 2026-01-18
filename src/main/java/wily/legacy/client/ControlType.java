package wily.legacy.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.FactoryEvent;
import wily.factoryapi.util.DynamicUtil;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.util.IOUtil;

import java.util.*;

public record ControlType(ResourceLocation id, Optional<Component> name, boolean isKbm,
                          Optional<SizeableAsset<ControlFont>> font, Optional<ResourceLocation> minecraftLogo,
                          Optional<SizeableAsset<Style>> style,
                          Map<String, ControlTooltip.LegacyIcon> icons) implements IdValueInfo<ControlType> {
    public static final SizeableAsset<Style> EMPTY_STYLE_ASSET = new SizeableAsset<>(Style.EMPTY);
    public static final Codec<ControlType> EXTENDED_CODEC = RecordCodecBuilder.create(i -> i.group(ResourceLocation.CODEC.fieldOf("id").forGetter(ControlType::id), DynamicUtil.getComponentCodec().optionalFieldOf("name").forGetter(ControlType::name), Codec.BOOL.optionalFieldOf("isKbm", false).forGetter(ControlType::isKbm), SizeableAsset.createWithFallback(ControlFont.CODEC).optionalFieldOf("font").forGetter(ControlType::font), ResourceLocation.CODEC.optionalFieldOf("minecraftLogo").forGetter(ControlType::minecraftLogo)).apply(i, ControlType::new));
    public static final Codec<ControlType> CODEC = IOUtil.createFallbackCodec(EXTENDED_CODEC, ResourceLocation.CODEC.xmap(ControlType::new, ControlType::id));
    public static final Codec<OptionHolder<ControlType>> OPTION_CODEC = OptionHolder.createCodecWithAuto(ControlType::get);
    public static final ResourceLocation KBM = Legacy4J.createModLocation("java");
    public static final ResourceLocation x360 = Legacy4J.createModLocation("xbox_360");
    public static final ResourceLocation xONE = Legacy4J.createModLocation("xbox_one");
    public static final ResourceLocation PS3 = Legacy4J.createModLocation("playstation_3");
    public static final ResourceLocation PS4 = Legacy4J.createModLocation("playstation_4");
    public static final ResourceLocation WII_U = Legacy4J.createModLocation("wii_u");
    public static final ResourceLocation SWITCH = Legacy4J.createModLocation("switch");
    public static final ResourceLocation STEAM = Legacy4J.createModLocation("steam");
    public static final ResourceLocation STADIA = Legacy4J.createModLocation("stadia");
    public static final ResourceLocation PSVITA = Legacy4J.createModLocation("playstation_vita");
    public static final ResourceLocation PS5 = Legacy4J.createModLocation("playstation_5");
    public ControlType(ResourceLocation id, Optional<Component> name, boolean isKbm, Optional<SizeableAsset<ControlFont>> font, Optional<ResourceLocation> minecraftLogo) {
        this(id, name, isKbm, font, minecraftLogo, font.map(asset -> asset.map(f -> Style.EMPTY.withFont(f.font()))), new HashMap<>());
    }
    public ControlType(ResourceLocation id) {
        this(id, Optional.of(Component.translatable("legacy.options.controlType." + id.getPath())), false, Optional.of(new SizeableAsset<>(new ControlFont(new FontDescription.Resource(id)))), Optional.of(id.withPath("textures/gui/title/minecraft/%s.png".formatted(id.getPath()))));
    }

    public static ControlType getActiveControllerType() {
        if (LegacyOptions.selectedControlType.get().isAuto()) {
            if (Legacy4JClient.controllerManager.connectedController != null) {
                return Legacy4JClient.controllerManager.connectedController.getType();
            } else return get(x360);
        } else {
            ControlType type = LegacyOptions.selectedControlType.get().get();
            return type.isKbm() ? get(x360) : type;
        }
    }

    public static ControlType getActiveType() {
        return !LegacyOptions.lockControlTypeChange.get() && Legacy4JClient.controllerManager.isControllerTheLastInput() || LegacyOptions.lockControlTypeChange.get() && (Legacy4JClient.controllerManager.connectedController != null && LegacyOptions.selectedControlType.get().isAuto() || !LegacyOptions.selectedControlType.get().orElse(get(KBM)).isKbm()) ? getActiveControllerType() : getKbmActiveType();
    }

    public static ControlType getKbmActiveType() {
        if (LegacyOptions.selectedControlType.get().isAuto()) return get(KBM);
        else {
            ControlType type = LegacyOptions.selectedControlType.get().get();
            return !type.isKbm() ? get(KBM) : type;
        }
    }

    public record ControlFont(FontDescription font, int iconHeight) {
        public static final Codec<ControlFont> CODEC = IOUtil.createFallbackCodec(RecordCodecBuilder.create(i -> i.group(FontDescription.CODEC.fieldOf("id").forGetter(ControlType.ControlFont::font), Codec.INT.fieldOf("iconHeight").forGetter(ControlType.ControlFont::iconHeight)).apply(i, ControlFont::new)), FontDescription.CODEC.xmap(ControlFont::new, ControlFont::font));

        public ControlFont(FontDescription font) {
            this(font, 15);
        }
    }

    public static ControlType get(ResourceLocation id) {
        return Legacy4JClient.controlTypesManager.map().get(id);
    }

    public Style styleOrEmpty() {
        return style.orElse(EMPTY_STYLE_ASSET).get();
    }

    public int iconHeight() {
        return font.isPresent() ? font.get().get().iconHeight() : 0;
    }

    @Override
    public ControlType copyFrom(ControlType other) {
        return new ControlType(id, other.name.or(this::name), isKbm, other.font.or(this::font), other.minecraftLogo.or(this::minecraftLogo), other.style.or(this::style), icons);
    }

    @Override
    public boolean isValid() {
        return name.isPresent() && font.isPresent() && style.isPresent();
    }

    public interface UpdateEvent {
        FactoryEvent<UpdateEvent> EVENT = new FactoryEvent<>(e -> (last, actual) -> e.invokeAll(l -> l.change(last, actual)));

        void change(ControlType last, ControlType actual);
    }

}
