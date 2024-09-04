package wily.legacy.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.util.ScreenUtil;

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
        ControlType type = create(ResourceLocation.fromNamespaceAndPath(Legacy4J.MOD_ID,name),null,kbm);
        defaultTypes.add(type);
        return type;
    }
    static ControlType create(ResourceLocation id, Component displayName, boolean isKbm){
        return create(id,new HashMap<>(),displayName == null ? Component.translatable((id.getNamespace().equals("minecraft") ? "" : id.getNamespace()) + ".controls.controller." + id.getPath()) : displayName,Style.EMPTY.withFont(id),isKbm);
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
        if (ScreenUtil.getLegacyOptions().selectedControlIcons().get().equals("auto")) {
            if (Legacy4JClient.controllerManager.connectedController != null){
                return Legacy4JClient.controllerManager.connectedController.getType();
            } else return x360;
        } else {
            ControlType type = typesMap.getOrDefault(ScreenUtil.getLegacyOptions().selectedControlIcons().get(), x360);
            if (type.isKbm()) return x360;
            return type;
        }
    }

    static ControlType getActiveType(){
        if (Legacy4JClient.controllerManager.connectedController != null && ScreenUtil.getLegacyOptions().selectedControlIcons().get().equals("auto") || !typesMap.getOrDefault(ScreenUtil.getLegacyOptions().selectedControlIcons().get(), KBM).isKbm()) return getActiveControllerType();
        return getKbmActiveType();
    }
    static ControlType getKbmActiveType(){
        if (ScreenUtil.getLegacyOptions().selectedControlIcons().get().equals("auto")) return KBM;
        else {
            ControlType type = typesMap.getOrDefault(ScreenUtil.getLegacyOptions().selectedControlIcons().get(), KBM);
            if (!type.isKbm()) return KBM;
            return type;
        }
    }
    ResourceLocation getId();
    Map<String, ControlTooltip.ComponentIcon> getIcons();
    boolean isKbm();
    Component getDisplayName();
    Style getStyle();
}
