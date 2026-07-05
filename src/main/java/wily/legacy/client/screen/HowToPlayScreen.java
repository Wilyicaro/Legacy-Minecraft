package wily.legacy.client.screen;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.sounds.SoundEvents;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.client.UIDefinition;
import wily.factoryapi.util.DynamicUtil;
import wily.legacy.Legacy4J;
import wily.legacy.client.ControlType;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.util.JsonUtil;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.ScreenUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class HowToPlayScreen extends LegacyScreen {

    protected final Section section;
    protected final int sectionIndex;
    protected final ResourceLocation uiDefinitionID;

    protected final ScrollableRenderer scrollableRenderer = new ScrollableRenderer();

    public HowToPlayScreen(Screen parent, Section section) {
        super(parent, section.title());
        this.section = section;
        this.sectionIndex = Section.list.indexOf(section);
        uiDefinitionID = section.uiDefinitionLocation().withPrefix("ui_definitions/").withSuffix(".json");
    }

    @Override
    public String toString() {
        return uiDefinitionID.toString();
    }

    public ScrollableRenderer getScrollableRenderer(){
        return accessor.getElementValue("scrollable_renderer", scrollableRenderer, ScrollableRenderer.class);
    }

    @Override
    public void addControlTooltips(ControlTooltip.Renderer renderer) {
        super.addControlTooltips(renderer);
        renderer.replace(1, i->i, a-> hasNextPage() ? LegacyComponents.NEXT_PAGE : null).add(()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.getIcon(), ()-> hasPreviousPage() ? LegacyComponents.PREVIOUS_PAGE : null );
    }

    @Override
    protected void init() {
        super.init();
        accessor.putStaticElement("scrollable_renderer", scrollableRenderer);
    }

    protected boolean hasNextPage(){
        List<Section> sections = Section.getValid();
        int index = sections.indexOf(section);
        return index >= 0 && index < sections.size() - 1;
    }

    protected boolean hasPreviousPage(){
        return Section.getValid().indexOf(section) > 0;
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        boolean next;
        if ((next = i == InputConstants.KEY_RETURN) && hasNextPage() || i == InputConstants.KEY_X && hasPreviousPage()) {
            List<Section> sections = Section.getValid();
            int index = sections.indexOf(section);
            ScreenUtil.playSimpleUISound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f);
            minecraft.setScreen(sections.get(index + (next ? 1 : -1)).build(parent));
            return true;
        }
        if (getScrollableRenderer().keyPressed(i)) return true;
        return super.keyPressed(i, j, k);
    }

    @Override
    public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
        if (getScrollableRenderer().mouseScrolled(g)) return true;
        return super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g);
    }


    public static class Manager implements ResourceManagerReloadListener {
        public static final String HOW_TO_PLAY_SECTIONS = "how_to_play_sections.json";

        @Override
        public void onResourceManagerReload(ResourceManager resourceManager) {
            Section.list.clear();
            JsonUtil.getOrderedNamespaces(resourceManager).forEach(name->resourceManager.getResource(FactoryAPI.createLocation(name, HOW_TO_PLAY_SECTIONS)).ifPresent(((r) -> {
                try (BufferedReader bufferedReader = r.openAsReader()) {
                    Section.LIST_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(bufferedReader)).result().ifPresent(l-> l.stream()
                            .filter(Section::supportsCurrentVersion)
                            .forEach(s-> Section.list.add(s.index < 0 ? Section.list.size() : Math.min(s.index,Section.list.size()), s)));
                } catch (IOException exception) {
                    Legacy4J.LOGGER.warn(exception.getMessage());
                }
            })));
        }

        @Override
        public String getName() {
            return "legacy:how_to_play_sections";
        }
    }

    public record Section(Component title, ResourceLocation uiDefinitionLocation, boolean hasButton, int index, Optional<String> minVersion, Optional<String> maxVersion) implements ScreenSection<HowToPlayScreen>{
        private static final String CURRENT_VERSION = SharedConstants.getCurrentVersion().getName();
        public static final Codec<Section> CODEC = RecordCodecBuilder.create(i-> i.group(
                DynamicUtil.getComponentCodec().fieldOf("title").forGetter(Section::title),
                ResourceLocation.CODEC.fieldOf("ui_definition").forGetter(Section::uiDefinitionLocation),
                Codec.BOOL.fieldOf("hasButton").orElse(true).forGetter(Section::hasButton),
                Codec.INT.fieldOf("index").orElse(-1).forGetter(Section::index),
                Codec.STRING.optionalFieldOf("min_version").forGetter(Section::minVersion),
                Codec.STRING.optionalFieldOf("max_version").forGetter(Section::maxVersion)
        ).apply(i,Section::new));
        public static final Codec<List<Section>> LIST_CODEC = CODEC.listOf();
        public static final List<Section> list = new ArrayList<>();

        public static Stream<Section> getWithButton(){
            return list.stream().filter(Section::isValid).filter(Section::hasButton);
        }

        public static List<Section> getValid() {
            return list.stream().filter(Section::isValid).toList();
        }

        public UIDefinition getUIDefinition() {
            return FactoryAPIClient.uiDefinitionManager.map.get(uiDefinitionLocation.withPrefix("ui_definitions/").withSuffix(".json"));
        }

        public boolean isValid() {
            return getUIDefinition() != null;
        }

        public boolean supportsCurrentVersion() {
            return minVersion.map(v-> compareVersions(CURRENT_VERSION, v) >= 0).orElse(true) && maxVersion.map(v-> compareVersions(CURRENT_VERSION, v) <= 0).orElse(true);
        }

        private static int compareVersions(String left, String right) {
            List<Integer> a = Legacy4J.getParsedVersion(left);
            List<Integer> b = Legacy4J.getParsedVersion(right);
            int size = Math.max(a.size(), b.size());
            for (int i = 0; i < size; i++) {
                int l = i < a.size() ? a.get(i) : 0;
                int r = i < b.size() ? b.get(i) : 0;
                if (l != r) return Integer.compare(l, r);
            }
            return 0;
        }

        @Override
        public HowToPlayScreen build(Screen parent) {
            return new HowToPlayScreen(parent, this);
        }
    }
}
