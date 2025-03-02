package wily.legacy.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.config.FactoryConfig;
import wily.legacy.client.LegacyMixinOptions;
import wily.legacy.config.LegacyCommonOptions;
import wily.legacy.config.LegacyMixinToggles;

import java.util.List;
import java.util.Set;

public class LegacyMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
        LegacyMixinToggles.COMMON_STORAGE.load();
        if (FactoryAPI.isClient()) LegacyMixinOptions.CLIENT_MIXIN_STORAGE.load();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!LegacyMixinToggles.COMMON_STORAGE.getFormatted("wily.", mixinClassName)) return false;
        if (FactoryAPI.isClient() && !LegacyMixinOptions.CLIENT_MIXIN_STORAGE.getFormatted("wily.", mixinClassName)) return false;
        if (FactoryAPI.isLoadingMod("nostalgic_tweaks")) {
            if (mixinClassName.endsWith("ItemInHandRendererSwayMixin")) return false;
        } else if (mixinClassName.contains("compat.nostalgic.")) return false;
        if (!FactoryAPI.isLoadingMod("sodium") && mixinClassName.contains("compat.sodium.")) return false;
        if (!FactoryAPI.isLoadingMod("jei") && mixinClassName.contains("compat.jei.")) return false;

        boolean hasVivecraft = FactoryAPI.isLoadingMod("vivecraft");
        if (hasVivecraft && mixinClassName.endsWith("GuiGameRendererMixin")) return false;
        return true;
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
