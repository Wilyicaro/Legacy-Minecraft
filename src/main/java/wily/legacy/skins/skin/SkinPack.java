package wily.legacy.skins.skin;

import net.minecraft.resources.Identifier;

import java.util.List;

public record SkinPack(String id, String name, String author, String type, Identifier icon, List<SkinEntry> skins,
                       boolean editable, int sortIndex, boolean hasSort, int sortSubIndex) {
}
