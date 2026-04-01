package wily.legacy.Skins.skin;

import net.minecraft.client.Minecraft;
import wily.factoryapi.base.network.CommonNetwork;

import java.util.UUID;

final class SkinAssetTransfer {
    private SkinAssetTransfer() {
    }

    static void sendAssets(Minecraft client, ClientSkinSyncState state, String skinId) {
        if (!SkinIdUtil.hasSkin(skinId) || state.sentAssets.putIfAbsent(skinId, Boolean.TRUE) != null) return;
        ClientSkinAssets.AssetData assets = ClientSkinAssets.resolveAssetData(client, skinId);
        sendChunks(skinId, SkinSync.ASSET_TEXTURE, assets.texture());
        sendChunks(skinId, SkinSync.ASSET_MODEL, assets.model());
    }

    static void acceptAssetChunk(ClientSkinSyncState state, UUID uuid, String skinId, int assetType, int index, int total, byte[] data) {
        if (!SkinIdUtil.hasSkin(skinId) || total <= 0) return;
        String key = uuid + "|" + skinId + "|" + assetType;
        SkinChunkAccumulator accumulator = state.assetChunks.computeIfAbsent(key, ignored -> new SkinChunkAccumulator(total));
        accumulator.put(index, data);
        if (!accumulator.isComplete()) return;
        state.assetChunks.remove(key);
        applyAsset(skinId, assetType, accumulator.assemble());
        ClientSkinCache.set(uuid, skinId);
    }

    private static void sendChunks(String skinId, int assetType, byte[] bytes) {
        if (!SkinIdUtil.hasSkin(skinId)) return;
        SkinSync.forEachChunk(bytes, SkinSync.UploadAssetChunkC2S.MAX_CHUNK, (index, total, chunk) ->
                CommonNetwork.sendToServer(new SkinSync.UploadAssetChunkC2S(skinId, assetType, index, total, chunk))
        );
    }

    private static void applyAsset(String skinId, int assetType, byte[] data) {
        if (assetType == SkinSync.ASSET_TEXTURE) {
            ClientSkinAssets.putTexture(skinId, data);
        } else if (assetType == SkinSync.ASSET_MODEL) {
            ClientSkinAssets.putModel(skinId, data);
        }
    }
}
