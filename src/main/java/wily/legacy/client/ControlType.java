package wily.legacy.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import wily.factoryapi.FactoryAPI;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.screen.ControlTooltip;

import java.util.*;

public interface ControlType  {
    List<ControlType> types = new ArrayList<>();
    Map<String,ControlType> typesMap = new HashMap<>();
    List<ControlType> defaultTypes = new ArrayList<>();

    ControlType KBM = createDefault("java",true);
    ControlType x360 = createDefault("xbox_360");
    ControlType xONE = createDefault("xbox_one");
    ControlType PS3 = createDefault("playstation_3");
    ControlType PS4 = createDefault("playstation_4");
    ControlType WII_U = createDefault("wii_u");
    ControlType SWITCH = createDefault("switch");
    ControlType STEAM = createDefault("steam");
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
    static ControlType create(ResourceLocation id, Map<String, ControlTooltip.ComponentIcon> map, Component displayName, Style style, boolean isKbm){
        return new ControlType() {
            @Override
            public Map<String, ControlTooltip.ComponentIcon> getIcons() {
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
        if (LegacyOption.selectedControlType.get().equals("auto")) {
            if (Legacy4JClient.controllerManager.connectedController != null){
                return Legacy4JClient.controllerManager.connectedController.getType();
            } else return x360;
        } else {
            ControlType type = typesMap.getOrDefault(LegacyOption.selectedControlType.get(), x360);
            return type.isKbm() ? x360 : type;
        }
    }

    static ControlType getActiveType(){
        return !LegacyOption.lockControlTypeChange.get() && Legacy4JClient.controllerManager.isControllerTheLastInput || LegacyOption.lockControlTypeChange.get() && (Legacy4JClient.controllerManager.connectedController != null && LegacyOption.selectedControlType.get().equals("auto") || !typesMap.getOrDefault(LegacyOption.selectedControlType.get(), KBM).isKbm()) ? getActiveControllerType() : getKbmActiveType();
    }

    static ControlType getKbmActiveType(){
        if (LegacyOption.selectedControlType.get().equals("auto")) return KBM;
        else {
            ControlType type = typesMap.getOrDefault(LegacyOption.selectedControlType.get(), KBM);
            return !type.isKbm() ? KBM : type;
        }
    }
    ResourceLocation getId();
    Map<String, ControlTooltip.ComponentIcon> getIcons();
    boolean isKbm();
    Component getDisplayName();
    Style getStyle();
}
