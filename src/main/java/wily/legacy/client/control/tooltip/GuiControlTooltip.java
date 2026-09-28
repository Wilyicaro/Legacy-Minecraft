package wily.legacy.client.control.tooltip;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import wily.legacy.Legacy4J;
import wily.legacy.client.control.LegacyKeyMapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public record GuiControlTooltip(KeyMapping keyMapping, Optional<Supplier<Component>> action) {
    public static final Codec<GuiControlTooltip> CODEC = RecordCodecBuilder.create(i -> i.group(Codec.STRING.validate(s -> KeyMapping.ALL.containsKey(s) ? DataResult.success(s) : DataResult.error(() -> "Can't find key mapping " + s)).xmap(KeyMapping.ALL::get, KeyMapping::getName).fieldOf("keyMapping").forGetter(GuiControlTooltip::keyMapping), CommonAction.CODEC.optionalFieldOf("action").forGetter(GuiControlTooltip::action)).apply(i, GuiControlTooltip::new));
    public static final Codec<List<GuiControlTooltip>> LIST_CODEC = CODEC.listOf();

    public ControlTooltip toControlTooltip() {
        return ControlTooltip.create(LegacyKeyMapping.of(keyMapping), action.orElseGet(() -> LegacyKeyMapping.of(keyMapping)::getDisplayName));
    }

    public record Manager(List<ControlTooltip> list) implements ResourceManagerReloadListener {
        public Manager() {
            this(new ArrayList<>());
        }

        @Override
        public void onResourceManagerReload(ResourceManager manager) {
            list.clear();
            manager.listResources("control_tooltips/gui", (string) -> string.getPath().endsWith(".json")).forEach((location, resource) -> {
                try (BufferedReader bufferedReader = resource.openAsReader()) {
                    LIST_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(bufferedReader)).resultOrPartial(s -> Legacy4J.LOGGER.error("Failed to parse {}: {}", location, s)).ifPresent(guiControlTooltips -> guiControlTooltips.forEach(guiControlTooltip -> list.add(guiControlTooltip.toControlTooltip())));
                } catch (IOException exception) {
                    Legacy4J.LOGGER.warn(exception.getMessage());
                }
            });
        }

        @Override
        public String getName() {
            return "legacy_controls:gui_control_tooltip";
        }
    }
}
