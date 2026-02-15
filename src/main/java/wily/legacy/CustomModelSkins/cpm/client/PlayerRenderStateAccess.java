package wily.legacy.CustomModelSkins.cpm.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import wily.legacy.CustomModelSkins.cpm.shared.config.Player;

public interface PlayerRenderStateAccess {
    void cpm$setPlayer(Player<Avatar> player);

    Player<Avatar> cpm$getPlayer();

    void cpm$setModelStatus(Component status);

    Component cpm$getModelStatus();

    void cpm$storeState(PlayerModel model);

    void cpm$loadState(PlayerModel model);
}
