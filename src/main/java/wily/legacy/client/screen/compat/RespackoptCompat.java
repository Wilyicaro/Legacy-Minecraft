package wily.legacy.client.screen.compat;

import dev.jfronny.libjf.config.api.v2.ConfigInstance;
import dev.jfronny.libjf.config.api.v2.dsl.DSL;
import dev.jfronny.respackopts.gui.BranchNaming;
import dev.jfronny.respackopts.gui.GuiEntryBuilder;
import dev.jfronny.respackopts.model.cache.CacheKey;
import dev.jfronny.respackopts.model.tree.ConfigBranch;
import dev.jfronny.respackopts.util.MetaCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import wily.factoryapi.FactoryAPI;
import wily.legacy.client.packselection.PackSelectButton;

public class RespackoptCompat {
	public static boolean isAvailable() {
		return FactoryAPI.isLoadingMod("respackopts") && FactoryAPI.isLoadingMod("libjf-config-ui-tiny");
	}
	public static boolean isPackConfigurable(PackSelectionModel.Entry entry) {
		return isAvailable() && Direct.isPackConfigurable(entry);
	}
	public static boolean openConfigScreen(PackSelectButton focused) {
		return isAvailable() && Direct.openConfigScreen(focused);
	}
	private static final class Direct {
		private static boolean isPackConfigurable(PackSelectionModel.Entry entry) {
			return MetaCache.getKeyByDisplayName(entry.getTitle().getString()) != null;
		}


		private static boolean openConfigScreen(PackSelectButton focused) {
			CacheKey dataLocation = MetaCache.getKeyByDisplayName(focused.getEntry().getTitle().getString());
			if (dataLocation != null) {
				Minecraft c = Minecraft.getInstance();
				String id = MetaCache.getId(dataLocation);
				ConfigBranch cb = MetaCache.getBranch(dataLocation);
				ConfigInstance ci = DSL.create(id).config(builder -> GuiEntryBuilder.buildConfig(cb, builder, dataLocation.dataLocation()));
				LegacyTripleTConfigScreen screen = new LegacyTripleTConfigScreen(ci, new BranchNaming(id, cb), c.screen);
				c.setScreen(screen);
				return true;
			}
			return false;
		}
	}
}
