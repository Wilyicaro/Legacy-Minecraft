package wily.legacy.Skins.skin;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
public record SkinPack(String id, String name, String author, String type, ResourceLocation icon, List<SkinEntry> skins, boolean editable, int sortIndex, boolean hasSort, int sortSubIndex) { }
