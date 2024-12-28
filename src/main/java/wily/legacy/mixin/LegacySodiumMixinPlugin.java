package wily.legacy.mixin;

import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.mixin.SimpleMixinPlugin;

public class LegacySodiumMixinPlugin extends SimpleMixinPlugin {
    public LegacySodiumMixinPlugin() {
        super(()-> FactoryAPI.isLoadingMod("sodium"));
    }
}
