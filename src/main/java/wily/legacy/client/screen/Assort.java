package wily.legacy.client.screen;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.GsonHelper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.FactoryAPIPlatform;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.util.DynamicUtil;
import wily.factoryapi.util.ListMap;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JPlatform;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyClientWorldSettings;
import wily.legacy.client.LegacyOption;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static wily.legacy.client.screen.ControlTooltip.*;
import static wily.legacy.util.LegacySprites.PACK_HIGHLIGHTED;

public record Assort(String id, int version, Component displayName, Component description, Optional<ResourceLocation> iconSprite, Optional<ResourceLocation> backgroundSprite, List<String> packs, Optional<String> displayPack) {
    public static final Codec<Assort> CODEC = RecordCodecBuilder.create(i-> i.group(Codec.STRING.fieldOf("id").forGetter(Assort::id),Codec.INT.optionalFieldOf("version",0).forGetter(Assort::version), DynamicUtil.getComponentCodec().fieldOf("name").forGetter(Assort::displayName),DynamicUtil.getComponentCodec().fieldOf("description").forGetter(Assort::description),ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(Assort::iconSprite),ResourceLocation.CODEC.optionalFieldOf("background").forGetter(Assort::backgroundSprite),Codec.STRING.listOf().fieldOf("packs").forGetter(Assort::packs),Codec.STRING.optionalFieldOf("displayPack").forGetter(Assort::displayPack)).apply(i,Assort::new));
    public static final Codec<List<Assort>> LIST_CODEC = CODEC.listOf();
    public static final ListMap<String,Assort> resourceAssorts = new ListMap<>();
    public static final List<Assort> defaultResourceAssorts = new ArrayList<>();
    public static final String RESOURCE_ASSORTS = "resource_assorts";
    public static final Path RESOURCE_ASSORTS_PATH = Minecraft.getInstance().gameDirectory.toPath().resolve(RESOURCE_ASSORTS);
    public static final Component ASSORT_OPTIONS = Component.translatable("legacy.menu.assort_options");
    public static final Component ASSORT_OPTIONS_MESSAGE = Component.translatable("legacy.menu.assort_options_message");
    public static final Component ADD_ASSORT = Component.translatable("legacy.menu.add_assort");
    public static final Component REMOVE_ASSORT = Component.translatable("legacy.menu.remove_assort");

    public static final Assort MINECRAFT = registerDefaultResource("minecraft",1,Component.translatable("legacy.menu.assorts.minecraft"),Component.translatable("legacy.menu.assorts.minecraft.description"),null,Legacy4J.createModLocation("icon/background"),Legacy4JPlatform.getMinecraftResourceAssort(),"vanilla");
    public static final Assort MINECRAFT_CLASSIC_TEXTURES = registerDefaultResource("minecraft_classic",0,Component.translatable("legacy.menu.assorts.minecraft_classic"),Component.translatable("legacy.menu.assorts.minecraft_classic.description"), Legacy4J.createModLocation("icon/minecraft_classic"),Legacy4J.createModLocation("icon/minecraft_classic_background"),Legacy4JPlatform.getMinecraftClassicResourceAssort(),null);

    public static final Stocker<String> defaultResourceAssort = Stocker.of(MINECRAFT.id);

    public static Assort getDefaultResourceAssort(){
        return resourceById(defaultResourceAssort.get());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Assort a && a.id.equals(id);
    }
    public Assort withPacks(List<String> packs){
        return new Assort(id,version,displayName,description,iconSprite,backgroundSprite,packs,displayPack);
    }

    public static void init() {
        if (!Files.exists(RESOURCE_ASSORTS_PATH)) save(RESOURCE_ASSORTS_PATH,defaultResourceAssorts,defaultResourceAssort);
        load();
    }
    public static Assort resourceById(String s){
        return resourceAssorts.getOrDefault(s,MINECRAFT);
    }

    public static void load(){
        resourceAssorts.clear();
        load(RESOURCE_ASSORTS_PATH, defaultResourceAssorts,defaultResourceAssort).forEach(Assort::registerResource);
    }
    public static List<Assort> load(Path path,List<Assort> defaultAssorts, Stocker<String> selected){
        List<String> order = new ArrayList<>();
        Path orderJson = path.resolveSibling(path.getFileName() +".json");
        String defaultAssort = MINECRAFT.id;
        try (BufferedReader r = Files.newBufferedReader(orderJson, Charsets.UTF_8)){
            JsonObject obj = LegacyOption.GSON.getAdapter(JsonObject.class).read(new JsonReader(r));
            defaultAssort = obj.getAsJsonPrimitive("default").getAsString();
            for (JsonElement e : obj.getAsJsonArray("order")) {
                if (e instanceof JsonPrimitive) order.add(e.getAsString());
            }
        } catch (IOException e) {
        }
        for (int i = defaultAssorts.size() - 1; i >= 0; i--) {
            Assort a = defaultAssorts.get(i);
            if (!order.contains(a.id)) order.add(0,a.id);
        }
        List<Assort> list = new ArrayList<>();
        try (Stream<Path> s = Files.walk(path).sorted(Comparator.comparingInt(p-> {
            int i = order.indexOf(FilenameUtils.getBaseName(p.getFileName().toString()));
            return i < 0 ? order.size() : i;
        }))) {
            for (Path p : ((Iterable<Path>) s::iterator)) {
                if (!p.toString().endsWith(".json")) continue;
                try (BufferedReader r = Files.newBufferedReader(p, Charsets.UTF_8)){
                    CODEC.parse(JsonOps.INSTANCE,GsonHelper.parse(r)).result().ifPresent(list::add);
                } catch (IOException e) {
                    Legacy4J.LOGGER.warn("Failed to load {}, this assort won't be loaded",p,e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (int i = defaultAssorts.size() - 1; i >= 0; i--) {
            Assort a = defaultAssorts.get(i);
            int index = list.indexOf(a);
            if (index < 0) list.add(0,a);
            else if (a.version > list.get(index).version) list.set(index,a);
        }
        selected.set(defaultAssort);
        return list;
    }
    public static void save(){
        save(RESOURCE_ASSORTS_PATH,resourceAssorts.values(),defaultResourceAssort);
    }
    public static void save(Path path, Collection<Assort> assorts, Stocker<String> selected){
        if (!Files.exists(path)){
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                Legacy4J.LOGGER.warn("Failed to make assorts directory {}",path,e);
            }
        }else FileUtils.listFiles(path.toFile(), new String[]{"json"},true).forEach(File::delete);

        List<String> order = new ArrayList<>();
        for (Assort assort : assorts) {
            order.add(assort.id);
            Path p = path.resolve(assort.id + ".json");
            try (JsonWriter w = new JsonWriter(Files.newBufferedWriter(p, Charsets.UTF_8))){
                w.setSerializeNulls(false);
                w.setIndent("  ");
                GsonHelper.writeValue(w,CODEC.encodeStart(JsonOps.INSTANCE,assort).result().orElseThrow(), null);
            } catch (IOException e) {
                Legacy4J.LOGGER.warn("Failed to write {}, this assort won't be saved",p,e);
            }
        }
        Path orderJson = path.resolveSibling(path.getFileName() +".json");
        try (JsonWriter w = new JsonWriter(Files.newBufferedWriter(orderJson, Charsets.UTF_8))){
            w.setSerializeNulls(false);
            w.setIndent("  ");
            JsonArray a = new JsonArray();
            order.forEach(a::add);
            JsonObject obj = new JsonObject();
            obj.add("default",new JsonPrimitive(selected.get()));
            obj.add("order",a);
            GsonHelper.writeValue(w,obj,null);
        } catch (IOException e) {
        }
    }
    public static Assort registerResource(Assort a){
        resourceAssorts.put(a.id(), a);
        return a;
    }
    public static Assort registerDefaultResource(String id,int version, Component displayName, Component description, ResourceLocation iconSprite, ResourceLocation backgroundSprite, List<String> packs,String displayPack){
        return registerDefaultResource(new Assort(id,version,displayName,description,Optional.ofNullable(iconSprite),Optional.ofNullable(backgroundSprite),packs,Optional.ofNullable(displayPack)));
    }
    public static Assort registerDefaultResource(Assort a){;
        defaultResourceAssorts.add(a);
        return a;
    }
    public static void applyDefaultResourceAssort(){
        List<String> oldSelection = getSelectableIds(Minecraft.getInstance().getResourcePackRepository());
        Minecraft.getInstance().getResourcePackRepository().setSelected(getDefaultResourceAssort().packs());
        if (!oldSelection.equals(getSelectableIds(Minecraft.getInstance().getResourcePackRepository()))) {
            Minecraft.getInstance().reloadResourcePacks();
            updateSavedResourcePacks();
        }
    }
    public static void updateSavedResourcePacks(){
        Minecraft.getInstance().options.resourcePacks.clear();
        Minecraft.getInstance().options.incompatibleResourcePacks.clear();
        Minecraft.getInstance().getResourcePackRepository().getSelectedPacks().forEach(p->{
            if (p.getCompatibility().isCompatible()) Minecraft.getInstance().options.resourcePacks.add(p.getId());
            else Minecraft.getInstance().options.incompatibleResourcePacks.add(p.getId());
        });
    }
    public static List<String> getSelectedIds(PackRepository packRepository){
        return packRepository.getSelectedPacks().stream().map(Pack::getId).toList();
    }
    public static List<String> getSelectableIds(PackRepository packRepository){
        return packRepository.getSelectedPacks().stream().filter(pack -> !FactoryAPIPlatform.isPackHidden(pack)).map(Pack::getId).toList();
    }
    public boolean isValidPackDisplay(PackRepository packRepository){
        String id = getDisplayPackId();
        if (id == null) return false;
        return packRepository.getPack(id) != null;
    }
    public String getDisplayPackId(){
        return displayPack.orElse(packs.isEmpty() ? null : packs.get(packs.size() - 1));
    }
    public static class Selector extends AbstractWidget implements ActionHolder {
        public static final String TEMPLATE_ASSORT = "template_assort";
        public static final ResourceLocation DEFAULT_ICON = FactoryAPI.createVanillaLocation("textures/misc/unknown_pack.png");
        private static final Map<String, ResourceLocation> packIcons = Maps.newHashMap();
        private static final Map<String, ResourceLocation> packBackgrounds = Maps.newHashMap();
        private final ListMap<String,Assort> assorts;
        public final Stocker.Sizeable scrolledList;
        private final Component screenComponent;
        public Assort savedAssort;
        protected final Assort initialAssort;
        private final Path packPath;
        private final Consumer<Selector> reloadChanges;
        private final boolean hasTooltip;
        public int selectedIndex;
        private final PackRepository packRepository;
        private final Minecraft minecraft;
        protected final List<String> oldSelection;
        protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
        public final ScrollableRenderer scrollableRenderer  = new ScrollableRenderer(scrollRenderer);
        public final BiFunction<Component,Integer,MultiLineLabel> labelsCache = Util.memoize((c,i)->MultiLineLabel.create(Minecraft.getInstance().font,c,i));

        public static Selector resources(int i, int j, int k, int l, boolean hasTooltip) {
            return new Selector(i,j,k,l, LegacyComponents.RESOURCE_ASSORTS, LegacyComponents.SHOW_RESOURCE_PACKS, resourceAssorts, Minecraft.getInstance().hasSingleplayerServer() ? LegacyClientWorldSettings.of(Minecraft.getInstance().getSingleplayerServer().getWorldData()).getSelectedResourceAssort() : resourceById(defaultResourceAssort.get()), Minecraft.getInstance().getResourcePackRepository(),Minecraft.getInstance().getResourcePackDirectory(), Selector::reloadResourcesChanges,hasTooltip){

                @Override
                public void applyChanges(boolean reloadAndSave) {
                    super.applyChanges(reloadAndSave);
                    if (!Minecraft.getInstance().hasSingleplayerServer()) {
                        defaultResourceAssort.set(savedAssort.id());
                        save();
                    }
                }
            };
        }
        public static Selector resources(int i, int j, int k, int l, boolean hasTooltip, Assort selectedAssort) {
            return new Selector(i,j,k,l, LegacyComponents.RESOURCE_ASSORTS, LegacyComponents.SHOW_RESOURCE_PACKS,resourceAssorts, selectedAssort, Minecraft.getInstance().getResourcePackRepository(),Minecraft.getInstance().getResourcePackDirectory(), Selector::reloadResourcesChanges,hasTooltip);
        }
        public Selector(int i, int j, int k, int l, Component component, Component screenComponent, ListMap<String,Assort> assorts, Assort savedAssort, PackRepository packRepository, Path packPath, Consumer<Selector> reloadChanges, boolean hasTooltip) {
            super(i, j, k, l,component);
            this.screenComponent = screenComponent;
            this.savedAssort = initialAssort = savedAssort;
            this.packPath = packPath;
            this.reloadChanges = reloadChanges;
            this.hasTooltip = hasTooltip;
            this.assorts = assorts;
            minecraft = Minecraft.getInstance();
            this.packRepository = packRepository;
            oldSelection = getSelectedIds(packRepository);
            scrolledList = new Stocker.Sizeable(0);
            if (assorts.size() > getMaxPacks())
                scrolledList.max = assorts.size() - getMaxPacks();
            setSelectedIndex(savedAssort == null ? 0 : ((List<Assort>)assorts.values()).indexOf(savedAssort));
            while (selectedIndex >= scrolledList.get() + getMaxPacks()){
                if (scrolledList.add(1) == 0) break;
            }
            updateTooltip();
        }

        public void updateTooltip(){
            if (hasTooltip) setTooltip(Tooltip.create(getSelectedAssort().description(), getSelectedAssort().displayName()));
        }
        public void renderTooltipBox(GuiGraphics guiGraphics, Panel panel){
            renderTooltipBox(guiGraphics,panel.x + panel.width - 2, panel.y + 5, 161, panel.height - 10);
        }
        public void renderTooltipBox(GuiGraphics graphics, int x, int y, int width, int height){
            if (hasTooltip) return;
            ScreenUtil.renderPointerPanel(graphics,x, y,width,height);
            if (getSelectedAssort() != null){
                boolean p = getSelectedAssort().isValidPackDisplay(packRepository);
                if (getSelectedAssort().iconSprite().isPresent()) FactoryGuiGraphics.of(graphics).blitSprite(getSelectedAssort().iconSprite().get(),x + 7,y + 5,32,32);
                else FactoryGuiGraphics.of(graphics).blit(p ? getPackIcon(packRepository.getPack(getSelectedAssort().getDisplayPackId())) : DEFAULT_ICON, x + 7,y + 5,0.0f, 0.0f, 32, 32, 32, 32);
                graphics.enableScissor(x + 40, y + 4,x + 148, y + 44);
                labelsCache.apply(getSelectedAssort().displayName(),108).renderLeftAligned(graphics,x + 43, y + 8,12,0xFFFFFF);
                graphics.disableScissor();
                ResourceLocation background = getSelectedAssort().backgroundSprite.orElse(p ? getPackBackground(packRepository.getPack(getSelectedAssort().getDisplayPackId())) : null);
                MultiLineLabel label = labelsCache.apply(getSelectedAssort().description(),145);
                scrollableRenderer.render(graphics, x + 8,y + 40, 146, 12 * (background == null ? 20 : 7), ()->label.renderLeftAligned(graphics,x + 8, y + 40,12,0xFFFFFF));
                if (background != null) {
                    if (getSelectedAssort().backgroundSprite().isPresent()) FactoryGuiGraphics.of(graphics).blitSprite(background, x + 8,y + height - 78,145, 72);
                    else FactoryGuiGraphics.of(graphics).blit(background, x + 8,y + height - 78,0.0f, 0.0f, 145, 72, 145, 72);
                }
            }
        }

        public void addControlTooltips(Screen screen, ControlTooltip.Renderer renderer) {
            renderer.add(()-> ControlType.getActiveType().isKbm() ? COMPOUND_ICON_FUNCTION.apply(new ControlTooltip.Icon[]{getKeyIcon(InputConstants.KEY_LSHIFT), PLUS_ICON,getKeyIcon(InputConstants.MOUSE_BUTTON_LEFT)}) : null, ()-> ControlTooltip.getKeyMessage(InputConstants.MOUSE_BUTTON_LEFT,screen));
            renderer.add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(), ()-> screen.getFocused() instanceof Assort.Selector ? screenComponent : null);
            renderer.add(()-> ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.bindingState.getIcon(), ()-> ControlTooltip.getKeyMessage(InputConstants.KEY_O,screen));
        }

        @Override
        public boolean keyPressed(int i, int j, int k) {
            if (isHoveredOrFocused() && active) {
                if (i == InputConstants.KEY_X){
                    openPackSelectionScreen();
                    return true;
                }
                if (CommonInputs.selected(i)) {
                    savedAssort = getSelectedAssort();
                    return true;
                }
                if (i == 263 || i == 262) {
                    if (selectedIndex == scrolledList.get() + (i == 263 ? 0 : getMaxPacks() - 1)) updateScroll(i == 263 ? - 1 : 1,true);
                    setSelectedIndex(selectedIndex + (i == 263 ? - 1 : 1));
                    ScreenUtil.playSimpleUISound(LegacyRegistries.SCROLL.get(),1.0f);
                    return true;
                }
                if (i == InputConstants.KEY_O){
                    Screen screen = Minecraft.getInstance().screen;
                    minecraft.setScreen(new ConfirmationScreen(minecraft.screen,230,155,ASSORT_OPTIONS,ASSORT_OPTIONS_MESSAGE,b->{}){
                        @Override
                        protected void addButtons() {
                            renderableVList.addRenderable(Button.builder(Component.translatable("gui.cancel"), b-> this.onClose()).build());
                            renderableVList.addRenderable(Button.builder(ADD_ASSORT,b-> {
                                int repeat = 0;
                                while (!resourceById(TEMPLATE_ASSORT +(repeat > 0 ? "_" + repeat : "")).equals(MINECRAFT))
                                    repeat++;
                                String id = TEMPLATE_ASSORT +(repeat > 0 ? "_" + repeat : "");
                                setSelectedIndex(assorts.size());
                                EditBox nameBox = new EditBox(minecraft.font, width / 2 - 100,0,200, 20, Component.translatable("legacy.menu.assort_name"));
                                Component name = Component.translatable("legacy.menu.assorts.template",repeat);
                                nameBox.setHint(name);
                                minecraft.setScreen(new ConfirmationScreen(parent, ADD_ASSORT, Component.translatable("legacy.menu.assort_name"), p -> {
                                    minecraft.setScreen(new PackSelectionScreen(packRepository, r -> {
                                        Assort.resourceAssorts.put(id,new Assort(id, 0,nameBox.getValue().isBlank() ? name : Component.literal(nameBox.getValue()),Component.translatable("legacy.menu.assorts.template.description"),Optional.empty(),Optional.empty(), getSelectableIds(packRepository), Optional.empty()));
                                        save();
                                        updateSavedAssort();
                                        minecraft.setScreen(screen);
                                        packRepository.setSelected(Assort.Selector.this.oldSelection);
                                    }, packPath, getMessage()));
                                }) {
                                    @Override
                                    protected void init() {
                                        super.init();
                                        nameBox.setPosition(panel.x + 15, panel.y + 45);
                                        addRenderableWidget(nameBox);
                                    }
                                });
                            }).build());
                            AbstractButton removeButton;
                            renderableVList.addRenderable(removeButton= Button.builder(REMOVE_ASSORT,b-> {
                                assorts.remove(getSelectedAssort());
                                save();
                                updateSavedAssort();
                                setSelectedIndex(0);
                                minecraft.setScreen(screen);
                            }).build());
                            if (defaultResourceAssorts.stream().anyMatch(a->a.equals(getSelectedAssort()))) removeButton.active = false;
                        }
                    });
                    return true;
                }
            }
            return super.keyPressed(i, j, k);
        }

        public void setSelectedIndex(int index) {
            if (selectedIndex == index) return;
            this.selectedIndex = Stocker.cyclic(0,index,assorts.size());
            scrollableRenderer.scrolled.set(0);
            scrollableRenderer.scrolled.max = Math.max(0,labelsCache.apply(getSelectedAssort().description(),145).getLineCount() - (getSelectedAssort().backgroundSprite.orElse(getSelectedAssort().isValidPackDisplay(packRepository) ? getPackBackground(packRepository.getPack(getSelectedAssort().getDisplayPackId())) : null) == null ? 20 : 7));
            updateTooltip();
        }

        public void applyChanges(boolean reloadAndSave){
            packRepository.setSelected(savedAssort.packs());
            if (Minecraft.getInstance().hasSingleplayerServer()) LegacyClientWorldSettings.of(Minecraft.getInstance().getSingleplayerServer().getWorldData()).setSelectedResourceAssort(savedAssort);
            if (reloadAndSave) reloadChanges.accept(this);
        }

        public static void applyResourceChanges(Minecraft minecraft, Collection<String> oldSelection, Collection<String> newSelection, Runnable runnable){
            minecraft.getResourcePackRepository().setSelected(newSelection);
            minecraft.setScreen(new LegacyLoadingScreen());
            if (!oldSelection.equals(getSelectedIds(minecraft.getResourcePackRepository()))) {
                updateSavedResourcePacks();
                Minecraft.getInstance().reloadResourcePacks().thenRun(runnable);
            }else runnable.run();
        }

        public static void reloadResourcesChanges(Selector selector){
            if (!selector.oldSelection.equals(getSelectedIds(selector.packRepository))) {
                updateSavedResourcePacks();
                Minecraft.getInstance().reloadResourcePacks();
            }
        }
        public void openPackSelectionScreen(){
            if (minecraft.screen != null) {
                Screen screen = minecraft.screen;
                packRepository.setSelected(getSelectedAssort().packs());
                List<String> oldSelection = getSelectedIds(packRepository);
                minecraft.setScreen(new PackSelectionScreen(packRepository, p-> {
                    if (!oldSelection.equals(getSelectedIds(p))) {
                        assorts.put(getSelectedAssort().id(), getSelectedAssort().withPacks(List.copyOf(getSelectableIds(p))));
                        updateSavedAssort();
                        save();
                    }
                    minecraft.setScreen(screen);
                    packRepository.setSelected(this.oldSelection);
                }, packPath, getMessage()));
            }
        }
        public void updateSavedAssort(){
            savedAssort = assorts.getOrDefault(savedAssort.id(),initialAssort);
        }

        @Override
        public void setFocused(boolean bl) {
            if (!bl && savedAssort != null) setSelectedIndex(((List<Assort>)assorts.values()).indexOf(savedAssort));
            super.setFocused(bl);
        }

        @Override
        public void onClick(double d, double e) {
            if ((Screen.hasShiftDown())) {
                openPackSelectionScreen();
                return;
            }
            int visibleCount = 0;
            for (int index = 0; index < assorts.size(); index++) {
                if (visibleCount>=getMaxPacks()) break;
                if (ScreenUtil.isMouseOver(d,e, getX() + 20 + 30 * index, getY() + minecraft.font.lineHeight +  3, 30,  30)) {
                    setSelectedIndex(index + scrolledList.get());
                    savedAssort = getSelectedAssort();
                }
                visibleCount++;
            }
            super.onClick(d, e);
        }


        @Override
        public boolean mouseScrolled(double d, double e/*? if >1.20.1 {*/, double f/*?}*/, double g) {
            if (updateScroll((int) Math.signum(g),false)) return true;
            return super.mouseScrolled(d, e/*? if >1.20.1 {*/, f/*?}*/, g);
        }
        public boolean updateScroll(int i, boolean cyclic){
            if (scrolledList.max > 0) {
                if ((scrolledList.get() <= scrolledList.max && i > 0) || (scrolledList.get() >= 0 && i < 0)) {;
                    return scrolledList.add(i,cyclic) != 0;
                }
            }
            return false;
        }
        protected int getMaxPacks(){
            return (width - 40) / 30;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
            Font font = minecraft.font;
            FactoryGuiGraphics.of(guiGraphics).blitSprite(LegacySprites.PANEL_RECESS,getX() -1,getY()+ font.lineHeight -1 , width + 2,height + 2 - minecraft.font.lineHeight);
            int visibleCount = 0;
            RenderSystem.enableBlend();
            for (int index = 0; index < assorts.size(); index++) {
                if (visibleCount>=getMaxPacks()) break;
                Assort assort = ((List<Assort>)assorts.values()).get(Math.min(assorts.size() - 1, scrolledList.get() + index));
                if (assort.iconSprite().isPresent()) FactoryGuiGraphics.of(guiGraphics).blitSprite(assort.iconSprite().get(),getX() + 21 + 30 * index,getY() + font.lineHeight + 4,28,28);
                else FactoryGuiGraphics.of(guiGraphics).blit(assort.isValidPackDisplay(packRepository) ? getPackIcon(packRepository.getPack(assort.getDisplayPackId())) : DEFAULT_ICON, getX() + 21 + 30 * index,getY() + font.lineHeight + 4,0.0f, 0.0f, 28, 28, 28, 28);
                if (scrolledList.get() + index == selectedIndex)
                    FactoryGuiGraphics.of(guiGraphics).blitSprite(PACK_HIGHLIGHTED, getX() + 20 + 30 * index,getY() +font.lineHeight + 3,30,30);
                visibleCount++;
            }
            RenderSystem.disableBlend();
            guiGraphics.pose().pushPose();
            if (!isHoveredOrFocused()) guiGraphics.pose().translate(0.5f,0.5f,0f);
            guiGraphics.drawString(font,getMessage(),getX() + 2,getY(),isHoveredOrFocused() ? ScreenUtil.getDefaultTextColor() : CommonColor.INVENTORY_GRAY_TEXT.get(),isHoveredOrFocused());
            guiGraphics.pose().popPose();
            if (scrolledList.max > 0){
                if (scrolledList.get() < scrolledList.max) scrollRenderer.renderScroll(guiGraphics, ScreenDirection.RIGHT, getX() + width - 12, getY() + font.lineHeight + (height - font.lineHeight - 11) / 2);
                if (scrolledList.get() > 0) scrollRenderer.renderScroll(guiGraphics,ScreenDirection.LEFT,getX() + 8, getY() + font.lineHeight + (height - font.lineHeight - 11) / 2);
            }
        }
        public static ResourceLocation loadPackIcon(TextureManager textureManager, Pack pack, String icon, ResourceLocation fallback) {
            try (PackResources packResources = pack.open();){
                ResourceLocation resourceLocation;
                {
                    IoSupplier<InputStream> ioSupplier = packResources.getRootResource(icon);
                    if (ioSupplier == null)
                        return fallback;
                    String string = pack.getId();
                    ResourceLocation resourceLocation3 = FactoryAPI.createLocation("minecraft", icon + "/" + Util.sanitizeName(string, ResourceLocation::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars(string) + "/icon");
                    InputStream inputStream = ioSupplier.get();
                    try {
                        NativeImage nativeImage = NativeImage.read(inputStream);
                        textureManager.register(resourceLocation3, new DynamicTexture(nativeImage));
                        resourceLocation = resourceLocation3;
                    } catch (Throwable throwable) {
                        try {
                            inputStream.close();
                        } catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                        throw throwable;
                    }
                    inputStream.close();
                }
                return resourceLocation;
            } catch (Exception exception) {
                Legacy4J.LOGGER.warn("Failed to load icon from pack {}", pack.getId(), exception);
                return fallback;
            }
        }

        public static ResourceLocation getPackIcon(Pack pack) {
            return packIcons.computeIfAbsent(pack.getId(), string -> loadPackIcon(Minecraft.getInstance().getTextureManager(), pack, "pack.png",DEFAULT_ICON));
        }
        public static ResourceLocation getPackBackground(Pack pack) {
            return packBackgrounds.computeIfAbsent(pack.getId(), string -> loadPackIcon(Minecraft.getInstance().getTextureManager(), pack, "background.png",null));
        }
        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }

        public Assort getSelectedAssort() {
            return assorts.isEmpty() || selectedIndex > assorts.size() ? null : ((List<Assort>)assorts.values()).get(selectedIndex);
        }

        @Override
        public @Nullable Component getAction(Context context) {
            return context.actionOfContext(KeyContext.class,k-> k.key() == InputConstants.KEY_O && isFocused() ? LegacyComponents.ASSORT_OPTIONS : k.key() == InputConstants.KEY_X && isFocused() || k.key() == InputConstants.MOUSE_BUTTON_LEFT && isHovered() ? screenComponent : ControlTooltip.getSelectAction(this,context));
        }
    }
}
