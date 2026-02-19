package wily.legacy.CustomModelSkins.cpm.client;


/**
 * Console skins / CPM glue.
 */

import net.minecraft.client.Minecraft;
import wily.factoryapi.FactoryAPIClient;
import wily.legacy.CustomModelSkins.cpm.CustomPlayerModels;
import wily.legacy.CustomModelSkins.cpm.shared.MinecraftCommonAccess;

public class CustomPlayerModelsClient extends ClientBase {
    public static CustomPlayerModelsClient INSTANCE;


    public void renderArmor(net.minecraft.client.renderer.entity.ArmorModelSet<net.minecraft.client.model.HumanoidModel<net.minecraft.client.renderer.entity.state.HumanoidRenderState>> modelSet,
                            net.minecraft.client.model.HumanoidModel<net.minecraft.client.renderer.entity.state.HumanoidRenderState> parentModel) {
        if (ClientBase.mc == null) return;
        if (this.manager == null) return;

        this.manager.bindArmor(parentModel, modelSet.head(), 1);
        this.manager.bindArmor(parentModel, modelSet.legs(), 2);
        this.manager.bindArmor(parentModel, modelSet.chest(), 3);
        this.manager.bindArmor(parentModel, modelSet.feet(), 4);
    }


    public static void initClient() {
        if (INSTANCE != null) return;
        if (MinecraftCommonAccess.get() == null) {
            CustomPlayerModels.initCommon();
        }

        INSTANCE = new CustomPlayerModelsClient();
        INSTANCE.init0();

        FactoryAPIClient.preTick((Minecraft mcIn) -> {
            try {
                if (mcIn != null && mcIn.isPaused()) return;
                if (ClientBase.mc == null) return;
                var prm = ClientBase.mc.getPlayerRenderManager();
                if (prm != null && prm.getAnimationEngine() != null) {
                    prm.getAnimationEngine().tick();
                }
            } catch (Throwable ignored) {
            }
        });
        INSTANCE.init1();
    }
}
