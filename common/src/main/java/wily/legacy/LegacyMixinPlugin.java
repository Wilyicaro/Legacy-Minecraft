package wily.legacy;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class LegacyMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        boolean sodium = Legacy4JPlatform.isLoadingMod("rubidium") || Legacy4JPlatform.isLoadingMod("sodium");
        if ((mixinClassName.equals("LiquidRendererMixin") || mixinClassName.equals("ClientLevelMixin")) && sodium) return false;
        if (mixinClassName.equals("BlockColorsMixin")) return sodium;
        if (Legacy4JPlatform.isLoadingMod("nostalgic_tweaks") && mixinClassName.equals("ItemInHandRendererSwayMixin")) return false;
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
