package wily.legacy.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import wily.factoryapi.FactoryAPI;
import wily.legacy.client.LegacyMixinOptions;
import wily.legacy.config.LegacyConfig;

import java.util.List;
import java.util.Set;

public class LegacyMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
        LegacyConfig.COMMON_STORAGE.load();
        if (FactoryAPI.isClient()) LegacyMixinOptions.CLIENT_MIXIN_STORAGE.load();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        LegacyConfig<?> config;
        if ((config = LegacyConfig.COMMON_STORAGE.configMap.get(formatToOption(mixinClassName))) != null && config.secureCast(Boolean.class).get() == Boolean.FALSE)
            return false;
        if (FactoryAPI.isClient() && (config = LegacyMixinOptions.CLIENT_MIXIN_STORAGE.configMap.get(formatToOption(mixinClassName))) != null && config.secureCast(Boolean.class).get() == Boolean.FALSE)
            return false;
        if (FactoryAPI.isLoadingMod("nostalgic_tweaks") && mixinClassName.endsWith("ItemInHandRendererSwayMixin")) return false;
        boolean hasVivecraft = FactoryAPI.isLoadingMod("vivecraft");
        if (hasVivecraft && mixinClassName.endsWith("GuiGameRendererMixin")) return false;
        return true;
    }

    public String formatToOption(String mixinClass){
        mixinClass = mixinClass.replace("wily.","");
        return mixinClass.substring(0, mixinClass.lastIndexOf("."));
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
