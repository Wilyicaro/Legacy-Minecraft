package wily.legacy.skins.skin;

import net.minecraft.resources.Identifier;

public record SkinEntry(String id, String sourceId, String name, Identifier texture, Identifier modelId,
                        Identifier cape, boolean slimArms, int order, boolean fair) {
}
