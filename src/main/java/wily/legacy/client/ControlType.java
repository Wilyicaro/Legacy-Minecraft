package wily.legacy.client;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryEvent;
import wily.factoryapi.base.RegisterListing;
import wily.factoryapi.util.ListMap;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.screen.ControlTooltip;

import java.util.*;

public interface ControlType  {
    ListMap<String,ControlType> types = new ListMap<>();
    List<ControlType> defaultTypes = new ArrayList<>();

    ControlType KBM = createDefault("java",true);
    ControlType x360 = createDefault("xbox_360");
    ControlType xONE = createDefault("xbox_one");
    ControlType PS3 = createDefault("playstation_3");
    ControlType PS4 = createDefault("playstation_4");
    ControlType WII_U = createDefault("wii_u");
    ControlType SWITCH = createDefault("switch");
    ControlType STEAM = createDefault("steam");
    ControlType STADIA = createDefault("stadia");
    ControlType PSVITA = createDefault("playstation_vita");
    ControlType PS5 = createDefault("playstation_5");

    static ControlType createDefault(String name){
        return createDefault(name,false);
    }
    static ControlType createDefault(String name, boolean kbm){
        ControlType type = create(Legacy4J.createModLocation(name),null,kbm);
        defaultTypes.add(type);
        return type;
    }
    static ControlType create(ResourceLocation id, Component displayName, boolean isKbm){
        return create(id,new HashMap<>(),displayName == null ? Component.translatable("legacy.options.controlType." + id.getPath()) : displayName,Style.EMPTY.withFont(id),isKbm);
    }
    static ControlType create(ResourceLocation id, Map<String, ControlTooltip.LegacyIcon> map, Component displayName, Style style, boolean isKbm){
        return new ControlType() {
            @Override
            public Map<String, ControlTooltip.LegacyIcon> getIcons() {
                return map;
            }

            @Override
            public boolean isKbm() {
                return isKbm;
            }

            @Override
            public ResourceLocation getId() {
                return id;
            }
            @Override
            public Component getDisplayName() {
                return displayName;
            }

            @Override
            public Style getStyle() {
                return style;
            }
        };
    }

    static ControlType getActiveControllerType(){
        if (LegacyOptions.selectedControlType.get().isAuto()) {
            if (Legacy4JClient.controllerManager.connectedController != null){
                return Legacy4JClient.controllerManager.connectedController.getType();
            } else return x360;
        } else {
            ControlType type = LegacyOptions.selectedControlType.get().get();
            return type.isKbm() ? x360 : type;
        }
    }

    static ControlType getActiveType(){
        return !LegacyOptions.lockControlTypeChange.get() && Legacy4JClient.controllerManager.isControllerTheLastInput || LegacyOptions.lockControlTypeChange.get() && (Legacy4JClient.controllerManager.connectedController != null && LegacyOptions.selectedControlType.get().isAuto() || !LegacyOptions.selectedControlType.get().orElse(KBM).isKbm()) ? getActiveControllerType() : getKbmActiveType();
    }

    static ControlType getKbmActiveType(){
        if (LegacyOptions.selectedControlType.get().isAuto()) return KBM;
        else {
            ControlType type = LegacyOptions.selectedControlType.get().get();
            return !type.isKbm() ? KBM : type;
        }
    }

    ResourceLocation getId();

    Map<String, ControlTooltip.LegacyIcon> getIcons();

    boolean isKbm();

    Component getDisplayName();

    Style getStyle();

    class Holder implements RegisterListing.Holder<ControlType> {
        public static final Holder AUTO = new Holder(null){
            @Override
            public ControlType get() {
                return null;
            }

            @Override
            public String toString() {
                return "auto";
            }
        };
        public static final Codec<Holder> CODEC = Codec.STRING.xmap(Holder::parse,Holder::toString);
        private final ResourceLocation id;
        private ControlType controlType;

        public static Holder parse(String stringId){
            if (stringId.equals("auto")) return AUTO;
            ResourceLocation id = FactoryAPI.createLocation(stringId);
            return id == null ? AUTO : new Holder(id);
        }

        public static Holder of(ControlType controlType){
            return new Holder(controlType.getId()){
                @Override
                public ControlType get() {
                    return controlType;
                }
            };
        }

        public Holder(ResourceLocation id){
            this.id = id;
        }

        public boolean isAuto(){
            return get() == null;
        }

        public ControlType orElse(ControlType alternative){
            ControlType controlType = get();
            return controlType == null ? alternative : controlType;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Holder h && h.get() == get();
        }

        @Override
        public ResourceLocation getId() {
            return id;
        }

        @Override
        public String toString() {
            return id.toString();
        }

        @Override
        public ControlType get() {
            return controlType == null ? controlType = types.get(id.toString()) : controlType;
        }
    }

    interface UpdateEvent {
        FactoryEvent<UpdateEvent> EVENT = new FactoryEvent<>(e-> (last, actual)-> e.invokeAll(l->l.change(last, actual)));
        void change(ControlType last, ControlType actual);
    }
}
