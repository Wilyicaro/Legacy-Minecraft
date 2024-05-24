package wily.legacy.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.impl.recipe.ingredient.builtin.ComponentsIngredient;
import net.fabricmc.fabric.impl.resource.loader.ResourceManagerHelperImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import wily.legacy.Legacy4J;


public class Legacy4JFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Legacy4J.registerBuiltInPacks((path, name, displayName, position, enabledByDefault) -> ResourceManagerHelperImpl.registerBuiltinResourcePack(new ResourceLocation(Legacy4J.MOD_ID, name),path, FabricLoader.getInstance().getModContainer(Legacy4J.MOD_ID).orElseThrow(), displayName, enabledByDefault ? ResourcePackActivationType.DEFAULT_ENABLED : ResourcePackActivationType.NORMAL));
        Legacy4J.init();
        CustomIngredientSerializer.register(StrictComponentsIngredient.SERIALIZER);
    }
}
