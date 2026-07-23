package wily.legacy.client;

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
import net.minecraft.client.gui.layouts.LayoutElement;
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
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.util.DynamicUtil;
import wily.factoryapi.util.FactoryScreenUtil;
import wily.factoryapi.util.ListMap;
import wily.legacy.Legacy4J;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.screen.*;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.skins.skin.DownloadedSkinPackStore;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static wily.legacy.client.screen.ControlTooltip.*;
import static wily.legacy.util.LegacySprites.PACK_HIGHLIGHTED;

public record PackAlbum(String id, int version, Component displayName, Component description, Optional<ResourceLocation> iconSprite, Optional<ResourceLocation> backgroundSprite, List<String> packs, Optional<String> displayPack) {
    public static final Codec<PackAlbum> CODEC = RecordCodecBuilder.create(i-> i.group(Codec.STRING.fieldOf("id").forGetter(PackAlbum::id),Codec.INT.optionalFieldOf("version",0).forGetter(PackAlbum::version), DynamicUtil.getComponentCodec().fieldOf("name").forGetter(PackAlbum::displayName),DynamicUtil.getComponentCodec().fieldOf("description").forGetter(PackAlbum::description),ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(PackAlbum::iconSprite),ResourceLocation.CODEC.optionalFieldOf("background").forGetter(PackAlbum::backgroundSprite),Codec.STRING.listOf().fieldOf("packs").forGetter(PackAlbum::packs),Codec.STRING.optionalFieldOf("displayPack").forGetter(PackAlbum::displayPack)).apply(i, PackAlbum::new));
    public static final Codec<List<PackAlbum>> LIST_CODEC = CODEC.listOf();
    public static final ListMap<String, PackAlbum> resourceAlbums = new ListMap<>();
    public static final List<PackAlbum> DEFAULT_RESOURCE_ALBUMS = new ArrayList<>();
    public static final String RESOURCE_ALBUMS = "resource_albums";
    public static final Path RESOURCE_ALBUMS_PATH = Minecraft.getInstance().gameDirectory.toPath().resolve(RESOURCE_ALBUMS);
    @Deprecated
    public static final Path RESOURCE_ASSORTS_PATH = Minecraft.getInstance().gameDirectory.toPath().resolve("resource_assorts");
    public static final Component ALBUM_OPTIONS = Component.translatable("legacy.menu.album_options");
    public static final Component ALBUM_OPTIONS_MESSAGE = Component.translatable("legacy.menu.album_options_message");
    public static final Component ADD_ALBUM = Component.translatable("legacy.menu.add_album");
    public static final Component EDIT_ALBUM = Component.translatable("legacy.menu.edit_album");
    public static final Component REMOVE_ALBUM = Component.translatable("legacy.menu.remove_album");

    public static List<String> getMinecraftResourcePacks(){
        return new ArrayList<>(List.of("vanilla", /*? if forge || neoforge {*/ /*"mod_resources"*//*?} else {*/"fabric"/*?}*/, "legacy:legacy_waters"));
    }

    public static List<String> getMinecraftClassicResourcePacks(){
        List<String> album = getMinecraftResourcePacks();
        album.add(album.size() - 1,"programmer_art");
        if (FactoryAPI.getLoader().isForgeLike())  album.add(album.size() - 1,"legacy:programmer_art");
        return album;
    }

    public static final PackAlbum MINECRAFT = registerDefaultResource("minecraft",2,Component.translatable("legacy.menu.albums.resource.minecraft"),Component.translatable("legacy.menu.albums.resource.minecraft.description"),null,Legacy4J.createModLocation("icon/background"), getMinecraftResourcePacks(),"vanilla");
    public static final PackAlbum MINECRAFT_CLASSIC_TEXTURES = registerDefaultResource("minecraft_classic",1,Component.translatable("legacy.menu.albums.resource.minecraft_classic"),Component.translatable("legacy.menu.albums.resource.minecraft_classic.description"), Legacy4J.createModLocation("icon/minecraft_classic"),Legacy4J.createModLocation("icon/minecraft_classic_background"), getMinecraftClassicResourcePacks(),null);

    public static final Stocker<String> defaultResourceAlbum = Stocker.of(MINECRAFT.id);

    public static PackAlbum getDefaultResourceAlbum(){
        return resourceById(defaultResourceAlbum.get());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PackAlbum a && a.id.equals(id);
    }
    public PackAlbum withPacks(List<String> packs){
        return new PackAlbum(id,version,displayName,description,iconSprite,backgroundSprite,packs,displayPack);
    }

    public static void init() {
        List<PackAlbum> albums = Files.exists(RESOURCE_ASSORTS_PATH) ? load(RESOURCE_ASSORTS_PATH, DEFAULT_RESOURCE_ALBUMS, defaultResourceAlbum, true) : Collections.emptyList();
        albums.forEach(PackAlbum::registerResource);
        if (!Files.exists(RESOURCE_ALBUMS_PATH) || !albums.isEmpty()) save(RESOURCE_ALBUMS_PATH, DEFAULT_RESOURCE_ALBUMS, defaultResourceAlbum);
        if (albums.isEmpty()) load();
        DownloadedResourceAlbums.syncAll();
    }

    public static PackAlbum resourceById(String s){
        return resourceAlbums.getOrDefault(s,MINECRAFT);
    }

    public static void load(){
        resourceAlbums.clear();
        load(RESOURCE_ALBUMS_PATH, DEFAULT_RESOURCE_ALBUMS, defaultResourceAlbum).forEach(PackAlbum::registerResource);
    }

    public static List<PackAlbum> load(Path path, List<PackAlbum> defaultAlbums, Stocker<String> selected){
        return load(path, defaultAlbums, selected, false);
    }

    public static List<PackAlbum> load(Path path, List<PackAlbum> defaultAlbums, Stocker<String> selected, boolean deprecated){
        List<String> order = new ArrayList<>();
        Path orderJson = path.resolveSibling(path.getFileName() +".json");
        String defaultAlbum = MINECRAFT.id;
        try (BufferedReader r = Files.newBufferedReader(orderJson, Charsets.UTF_8)){
            if (JsonParser.parseReader(r) instanceof JsonObject obj) {
                defaultAlbum = obj.getAsJsonPrimitive("default").getAsString();
                for (JsonElement e : obj.getAsJsonArray("order")) {
                    if (e instanceof JsonPrimitive) order.add(e.getAsString());
                }
            }
        } catch (IOException | RuntimeException e) {
            Legacy4J.LOGGER.warn("Failed to load albums definition in {}", orderJson, e);
        }
        for (int i = defaultAlbums.size() - 1; i >= 0; i--) {
            PackAlbum a = defaultAlbums.get(i);
            if (!order.contains(a.id)) order.add(0,a.id);
        }
        List<PackAlbum> list = new ArrayList<>();
        try (Stream<Path> s = Files.walk(path).sorted(Comparator.comparingInt(p-> {
            int i = order.indexOf(FilenameUtils.getBaseName(p.getFileName().toString()));
            return i < 0 ? order.size() : i;
        }))) {
            for (Path p : ((Iterable<Path>) s::iterator)) {
                if (!p.toString().endsWith(".json")) continue;
                try (BufferedReader r = Files.newBufferedReader(p, Charsets.UTF_8)){
                    CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(r)).result().ifPresent(list::add);
                } catch (IOException | RuntimeException e) {
                    Legacy4J.LOGGER.warn("Failed to load {}, this album won't be loaded", p, e);
                }
            }
        } catch (IOException | RuntimeException e) {
            Legacy4J.LOGGER.warn("Failed to read albums in {}", path, e);
        }
        if (deprecated) {
            FileUtils.deleteQuietly(path.toFile());
            FileUtils.deleteQuietly(orderJson.toFile());
            if (list.isEmpty()) {
                return Collections.emptyList();
            }
        }
        for (int i = defaultAlbums.size() - 1; i >= 0; i--) {
            PackAlbum a = defaultAlbums.get(i);
            int index = list.indexOf(a);
            if (index < 0) list.add(0, a);
            else if (a.version > list.get(index).version) list.set(index,a);
        }
        selected.set(defaultAlbum);
        return list;
    }

    public static void save(){
        save(RESOURCE_ALBUMS_PATH, resourceAlbums.values(), defaultResourceAlbum);
    }

    public static void save(Path path, Collection<PackAlbum> albums, Stocker<String> selected){
        if (!Files.exists(path)){
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                Legacy4J.LOGGER.warn("Failed to make albums directory {}",path,e);
            }
        }else FileUtils.listFiles(path.toFile(), new String[]{"json"},true).forEach(File::delete);

        List<String> order = new ArrayList<>();
        for (PackAlbum album : albums) {
            order.add(album.id);
            Path p = path.resolve(album.id + ".json");
            try (JsonWriter w = new JsonWriter(Files.newBufferedWriter(p, Charsets.UTF_8))){
                w.setSerializeNulls(false);
                w.setIndent("  ");
                GsonHelper.writeValue(w,CODEC.encodeStart(JsonOps.INSTANCE, album).result().orElseThrow(), null);
            } catch (IOException e) {
                Legacy4J.LOGGER.warn("Failed to write {}, this album won't be saved",p,e);
            }
        }
        Path orderJson = path.resolveSibling(path.getFileName() +".json");
        try (JsonWriter w = new JsonWriter(Files.newBufferedWriter(orderJson, Charsets.UTF_8))){
            w.setSerializeNulls(false);
            w.setIndent("  ");
            JsonArray a = new JsonArray();
            order.forEach(a::add);
            JsonObject obj = new JsonObject();
            obj.add("default", new JsonPrimitive(selected.get()));
            obj.add("order",a);
            GsonHelper.writeValue(w,obj,null);
        } catch (IOException e) {
            Legacy4J.LOGGER.warn("Failed to write {}, the albums definition won't be saved", orderJson, e);
        }
    }

    public static PackAlbum registerResource(PackAlbum a){
        resourceAlbums.put(a.id(), a);
        return a;
    }

    public static PackAlbum registerDefaultResource(String id, int version, Component displayName, Component description, ResourceLocation iconSprite, ResourceLocation backgroundSprite, List<String> packs, String displayPack){
        return registerDefaultResource(new PackAlbum(id,version,displayName,description,Optional.ofNullable(iconSprite),Optional.ofNullable(backgroundSprite),packs,Optional.ofNullable(displayPack)));
    }

    public static PackAlbum registerDefaultResource(PackAlbum a){;
        DEFAULT_RESOURCE_ALBUMS.add(a);
        return a;
    }

    public static PackAlbum resolveWorldResourceAlbum(@Nullable PackAlbum album) {
        if (album == null || MINECRAFT.id().equals(album.id())) return getDefaultResourceAlbum();
        return album;
    }

    public static void applyDefaultResourceAlbum(){
        List<String> oldSelection = getSelectedIds(Minecraft.getInstance().getResourcePackRepository());
        GlobalPacks.globalResources.get().applyPacks(Minecraft.getInstance().getResourcePackRepository(), getDefaultResourceAlbum().packs());
        if (!oldSelection.equals(getSelectedIds(Minecraft.getInstance().getResourcePackRepository()))) {
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
        return packRepository.getSelectedPacks().stream().filter(pack -> !FactoryAPIPlatform.isPackHidden(pack) && !DownloadedResourceAlbums.isManagedPack(pack.getId()) && !DownloadedSkinPackStore.isManagedResourcePackId(pack.getId())).map(Pack::getId).toList();
    }

    public boolean isValidPackDisplay(PackRepository packRepository){
        return getDisplayPack(packRepository) != null;
    }

    public String getDisplayPackId(){
        return displayPack.orElse(packs.isEmpty() ? null : packs.get(packs.size() - 1));
    }

    public Pack getDisplayPack(PackRepository packRepository) {
        String id = getDisplayPackId();
        if (id == null) return null;
        Pack pack = packRepository.getPack(id);
        if (pack != null) return pack;
        if (id.startsWith("file/")) return packRepository.getPack(id.substring(5));
        return packRepository.getPack("file/" + id);
    }

    public static ConfirmationScreen createAlbumEditScreen(Screen parent, Component title, Component defaultName, Component defaultDescription, BiConsumer<Component,Component> editAlbum){
        EditBox nameBox = new EditBox(Minecraft.getInstance().font, 0,0,200, 20, Component.translatable("legacy.menu.album_info"));
        MultiLineEditBox descriptionBox = new MultiLineEditBox(Minecraft.getInstance().font, 0,0,200, 60, defaultDescription, nameBox.getMessage());
        nameBox.setHint(defaultName);
        return new ConfirmationScreen(parent, ConfirmationScreen::getPanelWidth, () -> LegacyOptions.getUIMode().isSD() ? 140 : 184, title, nameBox.getMessage(), p -> editAlbum.accept(nameBox.getValue().isBlank() ? defaultName : Component.literal(nameBox.getValue()), descriptionBox.getValue().isBlank() ? defaultDescription : Component.literal(descriptionBox.getValue()))) {
            @Override
            protected void init() {
                super.init();
                nameBox.setPosition(panel.x + 15, panel.y + 45);
                addRenderableWidget(nameBox);
                descriptionBox.setPosition(panel.x + 15, panel.y + 69);
                addRenderableWidget(descriptionBox);
            }
        };
    }

    public static class Selector extends AbstractWidget implements ActionHolder {
        public static final String TEMPLATE_ALBUM = "template_album";
        public static final ResourceLocation DEFAULT_ICON = FactoryAPI.createVanillaLocation("textures/misc/unknown_pack.png");
        private static final Map<String, ResourceLocation> packIcons = Maps.newHashMap();
        private static final Map<String, ResourceLocation> packBackgrounds = Maps.newHashMap();
        private final ListMap<String, PackAlbum> albums;
        public final Stocker.Sizeable scrolledList;
        private final Component screenComponent;
        public PackAlbum savedAlbum;
        protected final PackAlbum initialAlbum;
        private final Path packPath;
        private final Consumer<Selector> reloadChanges;
        private final FactoryConfig<GlobalPacks> globalPacks;
        private final boolean hasTooltip;
        public int selectedIndex;
        private final PackRepository packRepository;
        private final Minecraft minecraft;
        protected final List<String> oldSelection;
        private boolean userSelectedAlbum;
        protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
        public final ScrollableRenderer scrollableRenderer  = new ScrollableRenderer(scrollRenderer);
        public final BiFunction<Component,Integer,MultiLineLabel> labelsCache = Util.memoize((c,i)->MultiLineLabel.create(Minecraft.getInstance().font,c,i));
        public final BiFunction<Component,Integer,MultiLineLabel> sdLabelsCache = Util.memoize((c,i)->MultiLineLabel.create(Minecraft.getInstance().font,c.copy().withStyle(c.getStyle().withFont(LegacyIconHolder.MOJANGLES_11_FONT)),i));

        public static Selector resources(int i, int j, int k, int l, boolean hasTooltip) {
            return new Selector(i,j,k,l, LegacyComponents.getResourceAlbums(), LegacyComponents.getShowResourcePacks(), resourceAlbums, Minecraft.getInstance().hasSingleplayerServer() ? LegacyClientWorldSettings.of(Minecraft.getInstance().getSingleplayerServer().getWorldData()).getSelectedResourceAlbum() : resourceById(defaultResourceAlbum.get()), Minecraft.getInstance().getResourcePackRepository(),Minecraft.getInstance().getResourcePackDirectory(), Selector::reloadResourcesChanges, GlobalPacks.globalResources, hasTooltip){

                @Override
                public void applyChanges(boolean reloadAndSave) {
                    super.applyChanges(reloadAndSave);
                    if (!Minecraft.getInstance().hasSingleplayerServer()) {
                        defaultResourceAlbum.set(savedAlbum.id());
                        save();
                    }
                }
            };
        }

        public static Selector globalResources(int i, int j, int k, int l, boolean hasTooltip) {
            PackRepository repository = Minecraft.getInstance().getResourcePackRepository();
            Path packPath = Minecraft.getInstance().getResourcePackDirectory();
            FactoryConfig<GlobalPacks> globalConfig = GlobalPacks.globalResources;
            ListMap<String, PackAlbum> albums = RemoteResourceAlbums.createAlbumMap();
            String selectedAlbumId = defaultResourceAlbum.get();
            Selector selector = new Selector(i, j, k, l, LegacyComponents.getResourceAlbums(), LegacyComponents.getShowResourcePacks(), albums, albums.getOrDefault(selectedAlbumId, MINECRAFT), repository, packPath, Selector::reloadResourcesChanges, globalConfig, hasTooltip) {
                @Override
                public void applyChanges(boolean reloadAndSave) {
                    if (RemoteResourceAlbums.isPlaceholder(this.savedAlbum)) {
                        PackAlbum appliedAlbum = RemoteResourceAlbums.isPlaceholder(this.initialAlbum) ? MINECRAFT : this.initialAlbum;
                        globalConfig.get().applyPacks(repository, appliedAlbum.packs());
                        defaultResourceAlbum.set(this.savedAlbum.id());
                        save();
                        if (reloadAndSave && !this.oldSelection.equals(getSelectedIds(repository))) {
                            updateSavedResourcePacks();
                            Minecraft.getInstance().reloadResourcePacks();
                        }
                        return;
                    }
                    globalConfig.get().applyPacks(repository, this.savedAlbum.packs());
                    defaultResourceAlbum.set(this.savedAlbum.id());
                    save();
                    if (reloadAndSave && !this.oldSelection.equals(getSelectedIds(repository))) {
                        updateSavedResourcePacks();
                        Minecraft.getInstance().reloadResourcePacks();
                    }
                }
            };
            selector.loadRemoteAlbums(selectedAlbumId);
            return selector;
        }

        public static Selector creationResources(int i, int j, int k, int l, boolean hasTooltip) {
            ListMap<String, PackAlbum> albums = RemoteResourceAlbums.createAlbumMap();
            String selectedAlbumId = defaultResourceAlbum.get();
            Selector selector = new Selector(i, j, k, l, LegacyComponents.getResourceAlbums(), LegacyComponents.getShowResourcePacks(), albums, albums.getOrDefault(selectedAlbumId, MINECRAFT), Minecraft.getInstance().getResourcePackRepository(), Minecraft.getInstance().getResourcePackDirectory(), Selector::reloadResourcesChanges, GlobalPacks.globalResources, hasTooltip) {
                @Override
                public void applyChanges(boolean reloadAndSave) {
                    super.applyChanges(reloadAndSave);
                    if (!Minecraft.getInstance().hasSingleplayerServer()) {
                        defaultResourceAlbum.set(savedAlbum.id());
                        save();
                    }
                }
            };
            selector.loadRemoteAlbums(selectedAlbumId);
            return selector;
        }

        public static Selector resources(int i, int j, int k, int l, boolean hasTooltip, PackAlbum selectedAlbum) {
            ListMap<String, PackAlbum> albums = RemoteResourceAlbums.createAlbumMap();
            PackAlbum initialAlbum = selectedAlbum == null ? MINECRAFT : selectedAlbum;
            if (albums.get(initialAlbum.id()) == null) albums.put(initialAlbum.id(), initialAlbum);
            initialAlbum = albums.get(initialAlbum.id());
            Selector selector = new Selector(i, j, k, l, LegacyComponents.getResourceAlbums(), LegacyComponents.getShowResourcePacks(), albums, initialAlbum, Minecraft.getInstance().getResourcePackRepository(), Minecraft.getInstance().getResourcePackDirectory(), Selector::reloadResourcesChanges, GlobalPacks.globalResources, hasTooltip);
            selector.loadRemoteAlbums(initialAlbum.id());
            return selector;
        }

        public Selector(int i, int j, int k, int l, Component component, Component screenComponent, ListMap<String, PackAlbum> albums, PackAlbum savedAlbum, PackRepository packRepository, Path packPath, Consumer<Selector> reloadChanges, FactoryConfig<GlobalPacks> globalPacks, boolean hasTooltip) {
            super(i, j, k, l,component);
            this.screenComponent = screenComponent;
            this.savedAlbum = initialAlbum = savedAlbum;
            this.packPath = packPath;
            this.reloadChanges = reloadChanges;
            this.globalPacks = globalPacks;
            this.hasTooltip = hasTooltip;
            this.albums = albums;
            minecraft = Minecraft.getInstance();
            this.packRepository = packRepository;
            oldSelection = getSelectedIds(packRepository);
            scrolledList = new Stocker.Sizeable(0);
            updateScrollBounds(albums.size());
            setSelectedIndex(savedAlbum == null ? 0 : ((List<PackAlbum>) albums.values()).indexOf(savedAlbum));
            while (selectedIndex >= scrolledList.get() + getMaxPacks()){
                if (scrolledList.add(1) == 0) break;
            }
            updateTooltip();
        }

        @Override
        public void setWidth(int i) {
            super.setWidth(i);
            if (scrolledList != null) updateScrollBounds(albums.size());
        }

        private void updateScrollBounds(int size) {
            scrolledList.max = Math.max(0, size - getMaxPacks());
            if (scrolledList.get() > scrolledList.max) scrolledList.set(scrolledList.max);
        }

        public void updateTooltip(){
            if (hasTooltip) setTooltip(Tooltip.create(getSelectedAlbum().description(), getSelectedAlbum().displayName()));
        }

        public static int getDefaultWidth() {
            return LegacyOptions.getUIMode().isSD() ? 105 : 161;
        }

        public void renderTooltipBox(GuiGraphics guiGraphics, LayoutElement panel){
            renderTooltipBox(guiGraphics, panel, 0);
        }

        public void renderTooltipBox(GuiGraphics guiGraphics, LayoutElement panel, int xOffset){
            renderTooltipBox(guiGraphics,panel.getX() + panel.getWidth() - 2 + xOffset, panel.getY() + 5, getDefaultWidth(), panel.getHeight() - 10);
        }

        public void renderTooltipBox(GuiGraphics graphics, int x, int y, int width, int height){
            if (hasTooltip) return;
            ScreenUtil.renderPointerPanel(graphics,x, y,width,height);
            if (getSelectedAlbum() != null){
                boolean sd = LegacyOptions.getUIMode().isSD();
                BiFunction<Component, Integer, MultiLineLabel> labelCache = sd ? sdLabelsCache : labelsCache;
                Pack displayPack = getSelectedAlbum().getDisplayPack(packRepository);
                renderAlbumIcon(graphics, getSelectedAlbum(), displayPack, x + 7, y + 5, 32, 32);
                int nameWidth = width - 53;
                int lineHeight = sd ? 8 : 12;
                graphics.enableScissor(x + 40, y + 4,x + 40 + nameWidth, y + 44);
                labelCache.apply(getSelectedAlbum().displayName(),nameWidth).renderLeftAligned(graphics,x + (sd ? 40 : 43), y + 8,lineHeight,CommonColor.ITEM_NAME_TEXT.get());
                graphics.disableScissor();
                ResourceLocation background = getSelectedAlbum().backgroundSprite.orElse(displayPack == null ? null : getPackBackground(displayPack));
                int descriptionWidth = width - 16;
                MultiLineLabel label = labelCache.apply(getSelectedAlbum().description(),descriptionWidth);
                int descriptionFromBottom = sd ? 52 : 78;
                int visibleLines = Math.max(0,(height - 50 - (background == null ? 0 : descriptionFromBottom)) / lineHeight);
                scrollableRenderer.scrolled.max = Math.max(0,label.getLineCount() - visibleLines);
                scrollableRenderer.lineHeight = lineHeight;
                int left = x + (sd ? 5 : 8);
                scrollableRenderer.render(graphics, left,y + 40, descriptionWidth, visibleLines * lineHeight, ()->label.renderLeftAligned(graphics,left, y + 40,lineHeight,CommonColor.TIP_TEXT.get()));
                if (background != null) {
                    if (getSelectedAlbum().backgroundSprite().isPresent()) FactoryGuiGraphics.of(graphics).blitSprite(background, left,y + height - descriptionFromBottom,sd ? 95 : 145, sd ? 47 : 72);
                    else FactoryGuiGraphics.of(graphics).blit(background, left,y + height - descriptionFromBottom,0.0f, 0.0f, sd ? 95 : 145, sd ? 47 : 72, sd ? 95 : 145, sd ? 47 : 72);
                }
            }
        }

        @Override
        public boolean keyPressed(int i, int j, int k) {
            if (isHoveredOrFocused() && active) {
                if (i == InputConstants.KEY_X){
                    openPackSelectionScreen();
                    return true;
                }
                if (CommonInputs.selected(i)) {
                    savedAlbum = getSelectedAlbum();
                    userSelectedAlbum = true;
                    playDownSound(Minecraft.getInstance().getSoundManager());
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
                    minecraft.setScreen(new ConfirmationScreen(minecraft.screen, ConfirmationScreen::getPanelWidth, () -> LegacyOptions.getUIMode().isSD() ? 108 : 133, ALBUM_OPTIONS, ALBUM_OPTIONS_MESSAGE, b->{}) {
                        @Override
                        protected void addButtons() {
                            renderableVList.addRenderable(Button.builder(Component.translatable("gui.cancel"), b-> this.onClose()).build());
                            renderableVList.addRenderable(Button.builder(ADD_ALBUM, b-> {
                                int repeat = 0;
                                while (!resourceById(TEMPLATE_ALBUM +(repeat > 0 ? "_" + repeat : "")).equals(MINECRAFT))
                                    repeat++;
                                String id = TEMPLATE_ALBUM +(repeat > 0 ? "_" + repeat : "");
                                minecraft.setScreen(createAlbumEditScreen(parent, b.getMessage(), LegacyComponents.getResourceAlbumTemplate(repeat), LegacyComponents.getResourceAlbumTemplateDescription(), (name,description)-> {
                                    minecraft.setScreen(new PackSelectionScreen(packRepository, r -> {
                                        PackAlbum.resourceAlbums.put(id,new PackAlbum(id, 0, name, description,Optional.empty(),Optional.empty(), getSelectableIds(packRepository), Optional.empty()));
                                        save();
                                        Minecraft.getInstance().setScreen(parent);
                                        packRepository.setSelected(PackAlbum.Selector.this.oldSelection);
                                        updateSavedAlbum();
                                        setSelectedIndex(albums.size());
                                    }, packPath, title));
                                }));
                            }).build());
                            AbstractButton editButton;
                            renderableVList.addRenderable(editButton = Button.builder(EDIT_ALBUM, b-> {
                                PackAlbum editAlbum = getSelectedAlbum();
                                minecraft.setScreen(createAlbumEditScreen(parent, b.getMessage(), editAlbum.displayName, editAlbum.description, (name,description)-> {
                                    PackAlbum.resourceAlbums.put(editAlbum.id(),new PackAlbum(editAlbum.id(), editAlbum.version(), name, description, editAlbum.iconSprite(), editAlbum.backgroundSprite(), editAlbum.packs(), editAlbum.displayPack()));
                                    save();
                                    Minecraft.getInstance().setScreen(parent);
                                    packRepository.setSelected(PackAlbum.Selector.this.oldSelection);
                                    updateSavedAlbum();
                                }));
                            }).build());
                            AbstractButton removeButton;
                            renderableVList.addRenderable(removeButton = Button.builder(REMOVE_ALBUM, b-> {
                                albums.remove(getSelectedAlbum().id());
                                save();
                                updateSavedAlbum();
                                setSelectedIndex(0);
                                minecraft.setScreen(screen);
                            }).build());
                            if (DownloadedResourceAlbums.isManagedAlbum(getSelectedAlbum().id())) {
                                editButton.active = false;
                                removeButton.active = false;
                            } else if (DEFAULT_RESOURCE_ALBUMS.stream().anyMatch(a->a.equals(getSelectedAlbum()))) removeButton.active = false;
                        }
                    });
                    return true;
                }
            }
            return super.keyPressed(i, j, k);
        }

        public void setSelectedIndex(int index) {
            if (selectedIndex == index) return;
            this.selectedIndex = Stocker.cyclic(0,index, albums.size());
            scrollableRenderer.scrolled.set(0);
            updateTooltip();
        }

        public void selectAlbum(PackAlbum album) {
            if (album == null) return;
            albums.put(album.id(), album);
            savedAlbum = album;
            updateScrollBounds(albums.size());
            setSelectedIndex(((List<PackAlbum>) albums.values()).indexOf(album));
            if (selectedIndex < scrolledList.get()) scrolledList.set(selectedIndex);
            else if (selectedIndex >= scrolledList.get() + getMaxPacks()) scrolledList.set(selectedIndex - getMaxPacks() + 1);
            updateTooltip();
        }

        private void loadRemoteAlbums(String selectedAlbumId) {
            RemoteResourceAlbums.load().whenComplete((unused, throwable) -> {
                if (throwable != null) {
                    Legacy4J.LOGGER.warn("Failed to load remote resource albums", throwable);
                    return;
                }
                minecraft.execute(() -> {
                    String activeAlbumId = userSelectedAlbum ? savedAlbum.id() : selectedAlbumId;
                    RemoteResourceAlbums.addTo(albums);
                    updateScrollBounds(albums.size());
                    PackAlbum selectedAlbum = albums.get(activeAlbumId);
                    if (selectedAlbum != null) selectAlbum(selectedAlbum);
                });
            });
        }

        public void applyChanges(boolean reloadAndSave){
            globalPacks.get().applyPacks(packRepository, savedAlbum.packs());
            if (Minecraft.getInstance().hasSingleplayerServer()) LegacyClientWorldSettings.of(Minecraft.getInstance().getSingleplayerServer().getWorldData()).setSelectedResourceAlbum(savedAlbum);
            if (reloadAndSave) reloadChanges.accept(this);
        }

        public static void applyResourceChanges(Minecraft minecraft, List<String> oldSelection, List<String> newSelection, boolean persistSelection, Runnable runnable){
            GlobalPacks.globalResources.get().applyPacks(minecraft.getResourcePackRepository(),newSelection);
            minecraft.setScreen(new LegacyLoadingScreen());
            if (!oldSelection.equals(getSelectedIds(minecraft.getResourcePackRepository()))) {
                if (persistSelection) updateSavedResourcePacks();
                Minecraft.getInstance().reloadResourcePacks().thenRun(runnable);
            }else runnable.run();
        }

        public static void applyResourceChanges(Minecraft minecraft, List<String> oldSelection, List<String> newSelection, Runnable runnable){
            applyResourceChanges(minecraft, oldSelection, newSelection, true, runnable);
        }

        public static void reloadResourcesChanges(Selector selector){
            if (!selector.oldSelection.equals(getSelectedIds(selector.packRepository))) {
                if (!Minecraft.getInstance().hasSingleplayerServer()) updateSavedResourcePacks();
                Minecraft.getInstance().reloadResourcePacks();
            }
        }

        public void openPackSelectionScreen(){
            if (DownloadedResourceAlbums.isManagedAlbum(getSelectedAlbum().id())) return;
            if (minecraft.screen != null) {
                Screen screen = minecraft.screen;
                packRepository.setSelected(DownloadedSkinPackStore.preserveSelection(packRepository, getSelectedAlbum().packs()));
                List<String> oldSelection = getSelectedIds(packRepository);
                minecraft.setScreen(new PackSelectionScreen(packRepository, p-> {
                    if (!oldSelection.equals(getSelectedIds(p))) {
                        albums.put(getSelectedAlbum().id(), getSelectedAlbum().withPacks(List.copyOf(getSelectableIds(p))));
                        updateSavedAlbum();
                        save();
                    }
                    minecraft.setScreen(screen);
                    packRepository.setSelected(this.oldSelection);
                }, packPath, getMessage()));
            }
        }

        public void updateSavedAlbum(){
            savedAlbum = albums.getOrDefault(savedAlbum.id(), initialAlbum);
        }

        @Override
        public void setFocused(boolean bl) {
            if (!bl && savedAlbum != null) setSelectedIndex(((List<PackAlbum>) albums.values()).indexOf(savedAlbum));
            super.setFocused(bl);
        }

        @Override
        public void onClick(double d, double e) {
            if ((Screen.hasShiftDown())) {
                openPackSelectionScreen();
                return;
            }
            int visibleCount = 0;
            for (int index = 0; index < albums.size(); index++) {
                if (visibleCount>=getMaxPacks()) break;
                visibleCount++;
                if (ScreenUtil.isMouseOver(d,e, getX() + 20 + 30 * index, getY() + minecraft.font.lineHeight +  3, 30,  30)) {
                    setSelectedIndex(index + scrolledList.get());
                    savedAlbum = getSelectedAlbum();
                    userSelectedAlbum = true;
                    playDownSound(Minecraft.getInstance().getSoundManager());
                    return;
                }
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
            FactoryScreenUtil.enableBlend();
            for (int index = 0; index < albums.size(); index++) {
                if (visibleCount>=getMaxPacks() || scrolledList.get() + index >= albums.size()) break;
                PackAlbum album = albums.getByIndex(scrolledList.get() + index);
                Pack displayPack = album.getDisplayPack(packRepository);
                renderAlbumIcon(guiGraphics, album, displayPack, getX() + 21 + 30 * index, getY() + font.lineHeight + 4, 28, 28);
                if (scrolledList.get() + index == selectedIndex)
                    FactoryGuiGraphics.of(guiGraphics).blitSprite(PACK_HIGHLIGHTED, getX() + 20 + 30 * index,getY() +font.lineHeight + 3,30,30);
                visibleCount++;
            }
            FactoryScreenUtil.disableBlend();
            guiGraphics.pose().pushPose();
            if (!isHoveredOrFocused()) guiGraphics.pose().translate(0.4f,0.4f,0f);
            ScreenUtil.applySDFont(ignored -> guiGraphics.drawString(font,getMessage(),getX() + 2,getY(),isHoveredOrFocused() ? ScreenUtil.getDefaultTextColor() : CommonColor.INVENTORY_GRAY_TEXT.get(),isHoveredOrFocused()));
            guiGraphics.pose().popPose();
            if (scrolledList.max > 0){
                if (scrolledList.get() < scrolledList.max) scrollRenderer.renderScroll(guiGraphics, ScreenDirection.RIGHT, getX() + width - 12, getY() + font.lineHeight + (height - font.lineHeight - 11) / 2);
                if (scrolledList.get() > 0) scrollRenderer.renderScroll(guiGraphics,ScreenDirection.LEFT,getX() + 8, getY() + font.lineHeight + (height - font.lineHeight - 11) / 2);
            }
        }

        private void renderAlbumIcon(GuiGraphics graphics, PackAlbum album, Pack displayPack, int x, int y, int width, int height) {
            if (album.iconSprite().isPresent()) {
                FactoryGuiGraphics.of(graphics).blitSprite(album.iconSprite().get(), x, y, width, height);
                return;
            }
            ResourceLocation remoteIcon = RemoteResourceAlbums.getIcon(album);
            if (remoteIcon != null) {
                FactoryGuiGraphics.of(graphics).blit(remoteIcon, x, y, 0.0f, 0.0f, width, height, width, height);
                return;
            }
            if (RemoteResourceAlbums.isIconPending(album)) {
                ScreenUtil.drawGenericLoading(graphics, x, y, (width - 2) / 3, 1);
                return;
            }
            FactoryGuiGraphics.of(graphics).blit(displayPack != null ? getPackIcon(displayPack) : DEFAULT_ICON, x, y, 0.0f, 0.0f, width, height, width, height);
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
                        textureManager.register(resourceLocation3, new DynamicTexture(/*? if >1.21.4 {*//*resourceLocation3::toString, *//*?}*/nativeImage));
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

        public static void invalidatePackAssets(String packId) {
            if (packId == null) return;
            String normalized = packId.startsWith("file/") ? packId.substring(5) : packId;
            List<String> ids = List.of(packId, normalized, "file/" + normalized);
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            ids.stream().map(packIcons::remove).filter(Objects::nonNull).forEach(textureManager::release);
            ids.stream().map(packBackgrounds::remove).filter(Objects::nonNull).forEach(textureManager::release);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            defaultButtonNarrationText(narrationElementOutput);
        }

        public PackAlbum getSelectedAlbum() {
            return albums.isEmpty() || selectedIndex > albums.size() ? null : albums.getByIndex(selectedIndex);
        }

        @Override
        public @Nullable Component getAction(Context context) {
            return context.actionOfContext(KeyContext.class, k -> {
                if (LegacyOptions.displayPackManagementTooltips.get() && k.key() == InputConstants.KEY_O && isFocused()) return LegacyComponents.ALBUM_OPTIONS;
                if (LegacyOptions.displayPackManagementTooltips.get() && (k.key() == InputConstants.KEY_X && isFocused() || k.key() == InputConstants.MOUSE_BUTTON_LEFT && isHovered())) return screenComponent;
                return ControlTooltip.getSelectAction(this, context);
            });
        }
    }
}
