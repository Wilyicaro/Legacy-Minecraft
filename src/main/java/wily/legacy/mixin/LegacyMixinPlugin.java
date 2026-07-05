package wily.legacy.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;
import wily.factoryapi.FactoryAPI;
import wily.legacy.client.LegacyMixinOptions;
import wily.legacy.config.LegacyMixinToggles;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class LegacyMixinPlugin implements IMixinConfigPlugin {
    private static final String OCCLUSION_CULLER = "net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller";
    private static final String SODIUM_08_DISTANCE_CHECK = "(Lnet/caffeinemc/mods/sodium/client/render/viewport/CameraTransform;Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;F)Z";
    private static final String SODIUM_09_VISIT_NODE = "(Lnet/caffeinemc/mods/sodium/client/util/collections/WriteQueue;Lnet/caffeinemc/mods/sodium/client/render/chunk/RenderSection;IZZZ)V";
    private static Boolean sodium08OcclusionCuller;
    private static Boolean sodium09OcclusionCuller;

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
        if (FactoryAPI.isClient() && !LegacyMixinOptions.CLIENT_MIXIN_STORAGE.getFormatted("wily.", mixinClassName))
            return false;
        if (FactoryAPI.isLoadingMod("nostalgic_tweaks")) {
            if (mixinClassName.endsWith("ItemInHandRendererSwayMixin")) return false;
        } else if (mixinClassName.contains("compat.nostalgic.")) return false;
        if (mixinClassName.contains("compat.sodium.")) {
            if (!FactoryAPI.isLoadingMod("sodium")) return false;
            if (mixinClassName.endsWith("OcclusionCullerMixin")) return hasSodium08OcclusionCuller();
            if (mixinClassName.endsWith("OcclusionCuller09Mixin")) return hasSodium09OcclusionCuller();
        }
        if (!FactoryAPI.isLoadingMod("jei") && mixinClassName.contains("compat.jei.")) return false;

        boolean hasVivecraft = FactoryAPI.isLoadingMod("vivecraft");
        return !hasVivecraft || !mixinClassName.endsWith("GuiGameRendererMixin");
    }

    private static boolean hasSodium08OcclusionCuller() {
        if (sodium08OcclusionCuller == null) sodium08OcclusionCuller = hasOcclusionCullerMethod("isWithinRenderDistance", SODIUM_08_DISTANCE_CHECK);
        return sodium08OcclusionCuller;
    }

    private static boolean hasSodium09OcclusionCuller() {
        if (sodium09OcclusionCuller == null) sodium09OcclusionCuller = hasOcclusionCullerMethod("visitNode", SODIUM_09_VISIT_NODE);
        return sodium09OcclusionCuller;
    }

    private static boolean hasOcclusionCullerMethod(String name, String desc) {
        try {
            ClassNode node = MixinService.getService().getBytecodeProvider().getClassNode(OCCLUSION_CULLER, false);
            for (MethodNode method : node.methods) {
                if (method.name.equals(name) && method.desc.equals(desc)) return true;
            }
        } catch (ClassNotFoundException | IOException e) {
            return false;
        }
        return false;
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
