package wily.legacy.mixin.base.client;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import wily.legacy.entity.LegacyPlayerInfo;

@Mixin(PlayerInfo.class)
public abstract class PlayerInfoMixin implements LegacyPlayerInfo {
    @Unique
    private final LegacyPlayerInfo.Data legacyMinecraft$playerInfoData = new LegacyPlayerInfo.Data(new Object2IntOpenHashMap<>());

    @Shadow
    public abstract GameProfile getProfile();

    @Override
    public GameProfile legacyMinecraft$getProfile() {
        return getProfile();
    }

    @Override
    public LegacyPlayerInfo.Data legacyMinecraft$getPlayerInfoData() {
        return legacyMinecraft$playerInfoData;
    }
}
