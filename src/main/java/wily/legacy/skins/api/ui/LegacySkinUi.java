package wily.legacy.Skins.api.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import wily.legacy.Skins.client.screen.ChangeSkinScreenSource;

import java.util.List;
import java.util.Objects;

public final class LegacySkinUi {
    private LegacySkinUi() {
    }

    public static Screen create(Screen parent, Adapter adapter) {
        return ChangeSkinScreenSource.from(Objects.requireNonNull(adapter, "adapter")).create(parent);
    }

    public interface Adapter {
        List<Pack> packs();

        default boolean supportsFavorites() {
            return false;
        }

        default @Nullable String selectedSkinId() {
            return null;
        }

        default boolean isFavorite(String skinId) {
            return false;
        }

        void selectSkin(String skinId);

        default void toggleFavorite(String skinId) {
        }

        default void prewarmPreview(String skinId) {
        }

        void renderPreview(PreviewContext context);

        default int version() {
            return 0;
        }
    }

    public record Pack(String id,
                       Component title,
                       @Nullable Component subtitle,
                       @Nullable ResourceLocation icon,
                       List<Skin> skins) {
        public Pack(String id, Component title, List<Skin> skins) {
            this(id, title, null, null, skins);
        }

        public Pack(String id, Component title, @Nullable Component subtitle, List<Skin> skins) {
            this(id, title, subtitle, null, skins);
        }

        public Pack {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(title, "title");
            skins = skins == null ? List.of() : List.copyOf(skins);
        }
    }

    public record Skin(String id, Component title) {
        public Skin {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(title, "title");
        }
    }

    public record PreviewContext(GuiGraphics graphics,
                                 String skinId,
                                 int left,
                                 int top,
                                 int right,
                                 int bottom,
                                 float yaw,
                                 boolean crouching,
                                 float attackTime,
                                 float partialTick) {
        public PreviewContext {
            Objects.requireNonNull(graphics, "graphics");
            Objects.requireNonNull(skinId, "skinId");
        }

        public int width() {
            return Math.max(0, right - left);
        }

        public int height() {
            return Math.max(0, bottom - top);
        }
    }
}
