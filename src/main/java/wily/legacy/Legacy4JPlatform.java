package wily.legacy;

import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIPlatform;

import java.util.ArrayList;
import java.util.List;

public class Legacy4JPlatform {

    public static List<String> getMinecraftResourceAssort(){
        return new ArrayList<>(List.of("vanilla", FactoryAPI.getLoader().isForgeLike() ? "mod_resources" : "fabric","legacy:legacy_waters"));
    }
    public static List<String> getMinecraftClassicResourceAssort(){
        List<String> assort = getMinecraftResourceAssort();
        assort.add(assort.size() - 1,"programmer_art");
        if (FactoryAPI.getLoader().isForgeLike())  assort.add(assort.size() - 1,"legacy:programmer_art");
        return assort;
    }
}

