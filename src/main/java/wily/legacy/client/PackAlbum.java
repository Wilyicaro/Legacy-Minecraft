package wily.legacy.client;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
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
import wily.legacy.client.screen.*;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

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

    public static void applyDefaultResourceAlbum(){
        List<String> oldSelection = getSelectableIds(Minecraft.getInstance().getResourcePackRepository());
        GlobalPacks.globalResources.get().applyPacks(Minecraft.getInstance().getResourcePackRepository(), getDefaultResourceAlbum().packs());
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

    public static ConfirmationScreen createAlbumEditScreen(Screen parent, Component title, Component defaultName, Component defaultDescription, BiConsumer<Component,Component> editAlbum){
        EditBox nameBox = new EditBox(Minecraft.getInstance().font, 0,0,200, 20, Component.translatable("legacy.menu.album_info"));
        MultiLineEditBox descriptionBox = new MultiLineEditBox(Minecraft.getInstance().font, 0,0,200, 60, defaultDescription, nameBox.getMessage());
        nameBox.setHint(defaultName);
        return new ConfirmationScreen(parent, 230, 184, title, nameBox.getMessage(), p -> editAlbum.accept(nameBox.getValue().isBlank() ? defaultName : Component.literal(nameBox.getValue()), descriptionBox.getValue().isBlank() ? defaultDescription : Component.literal(descriptionBox.getValue()))) {
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
        protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
        public final ScrollableRenderer scrollableRenderer  = new ScrollableRenderer(scrollRenderer);
        public final BiFunction<Component,Integer,MultiLineLabel> labelsCache = Util.memoize((c,i)->MultiLineLabel.create(Minecraft.getInstance().font,c,i));

        public static Selector resources(int i, int j, int k, int l, boolean hasTooltip) {
            return new Selector(i,j,k,l, LegacyComponents.RESOURCE_ALBUMS, LegacyComponents.SHOW_RESOURCE_PACKS, resourceAlbums, Minecraft.getInstance().hasSingleplayerServer() ? LegacyClientWorldSettings.of(Minecraft.getInstance().getSingleplayerServer().getWorldData()).getSelectedResourceAlbum() : resourceById(defaultResourceAlbum.get()), Minecraft.getInstance().getResourcePackRepository(),Minecraft.getInstance().getResourcePackDirectory(), Selector::reloadResourcesChanges, GlobalPacks.globalResources, hasTooltip){

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

        public static Selector resources(int i, int j, int k, int l, boolean hasTooltip, PackAlbum selectedAlbum) {
            return new Selector(i,j,k,l, LegacyComponents.RESOURCE_ALBUMS, LegacyComponents.SHOW_RESOURCE_PACKS, resourceAlbums, selectedAlbum, Minecraft.getInstance().getResourcePackRepository(),Minecraft.getInstance().getResourcePackDirectory(), Selector::reloadResourcesChanges, GlobalPacks.globalResources, hasTooltip);
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
            if (albums.size() > getMaxPacks())
                scrolledList.max = albums.size() - getMaxPacks();
            setSelectedIndex(savedAlbum == null ? 0 : ((List<PackAlbum>) albums.values()).indexOf(savedAlbum));
            while (selectedIndex >= scrolledList.get() + getMaxPacks()){
                if (scrolledList.add(1) == 0) break;
            }
            updateTooltip();
        }

        public void updateTooltip(){
            if (hasTooltip) setTooltip(Tooltip.create(getSelectedAlbum().description(), getSelectedAlbum().displayName()));
        }

        public void renderTooltipBox(GuiGraphics guiGraphics, LayoutElement panel){
            renderTooltipBox(guiGraphics, panel, 0);
        }

        public void renderTooltipBox(GuiGraphics guiGraphics, LayoutElement panel, int xOffset){
            renderTooltipBox(guiGraphics,panel.getX() + panel.getWidth() - 2 + xOffset, panel.getY() + 5, 161, panel.getHeight() - 10);
        }

        public void renderTooltipBox(GuiGraphics graphics, int x, int y, int width, int height){
            if (hasTooltip) return;
            LegacyRenderUtil.renderPointerPanel(graphics,x, y,width,height);
            if (getSelectedAlbum() != null){
                boolean p = getSelectedAlbum().isValidPackDisplay(packRepository);
                if (getSelectedAlbum().iconSprite().isPresent()) FactoryGuiGraphics.of(graphics).blitSprite(getSelectedAlbum().iconSprite().get(),x + 7,y + 5,32,32);
                else FactoryGuiGraphics.of(graphics).blit(p ? getPackIcon(packRepository.getPack(getSelectedAlbum().getDisplayPackId())) : DEFAULT_ICON, x + 7,y + 5,0.0f, 0.0f, 32, 32, 32, 32);
                FactoryGuiGraphics.of(graphics).enableScissor(x + 40, y + 4,x + 148, y + 44);
                labelsCache.apply(getSelectedAlbum().displayName(),108).renderLeftAligned(graphics,x + 43, y + 8,12,0xFFFFFF);
                graphics.disableScissor();
                ResourceLocation background = getSelectedAlbum().backgroundSprite.orElse(p ? getPackBackground(packRepository.getPack(getSelectedAlbum().getDisplayPackId())) : null);
                MultiLineLabel label = labelsCache.apply(getSelectedAlbum().description(),145);
                scrollableRenderer.render(graphics, x + 8,y + 40, 146, 12 * (background == null ? 14 : 7), ()->label.renderLeftAligned(graphics,x + 8, y + 40,12,0xFFFFFF));
                if (background != null) {
                    if (getSelectedAlbum().backgroundSprite().isPresent()) FactoryGuiGraphics.of(graphics).blitSprite(background, x + 8,y + height - 78,145, 72);
                    else FactoryGuiGraphics.of(graphics).blit(background, x + 8,y + height - 78,0.0f, 0.0f, 145, 72, 145, 72);
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
                    playDownSound(Minecraft.getInstance().getSoundManager());
                    return true;
                }
                if (i == 263 || i == 262) {
                    if (selectedIndex == scrolledList.get() + (i == 263 ? 0 : getMaxPacks() - 1)) updateScroll(i == 263 ? - 1 : 1,true);
                    setSelectedIndex(selectedIndex + (i == 263 ? - 1 : 1));
                    LegacyRenderUtil.playSimpleUISound(LegacyRegistries.SCROLL.get(),1.0f);
                    return true;
                }
                if (i == InputConstants.KEY_O){
                    Screen screen = Minecraft.getInstance().screen;
                    minecraft.setScreen(new ConfirmationScreen(minecraft.screen,230,133, ALBUM_OPTIONS, ALBUM_OPTIONS_MESSAGE, b->{}){
                        @Override
                        protected void addButtons() {
                            renderableVList.addRenderable(Button.builder(Component.translatable("gui.cancel"), b-> this.onClose()).build());
                            renderableVList.addRenderable(Button.builder(ADD_ALBUM, b-> {
                                int repeat = 0;
                                while (!resourceById(TEMPLATE_ALBUM +(repeat > 0 ? "_" + repeat : "")).equals(MINECRAFT))
                                    repeat++;
                                String id = TEMPLATE_ALBUM +(repeat > 0 ? "_" + repeat : "");
                                minecraft.setScreen(createAlbumEditScreen(parent, b.getMessage(), Component.translatable("legacy.menu.albums.resource.template",repeat), Component.translatable("legacy.menu.albums.resource.template.description"), (name,description)-> {
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
                            renderableVList.addRenderable(Button.builder(EDIT_ALBUM, b-> {
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
                            if (DEFAULT_RESOURCE_ALBUMS.stream().anyMatch(a->a.equals(getSelectedAlbum()))) removeButton.active = false;
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
            scrollableRenderer.scrolled.max = Math.max(0,labelsCache.apply(getSelectedAlbum().description(),145).getLineCount() - (getSelectedAlbum().backgroundSprite.orElse(getSelectedAlbum().isValidPackDisplay(packRepository) ? getPackBackground(packRepository.getPack(getSelectedAlbum().getDisplayPackId())) : null) == null ? 20 : 7));
            updateTooltip();
        }

        public void applyChanges(boolean reloadAndSave){
            globalPacks.get().applyPacks(packRepository, savedAlbum.packs());
            if (Minecraft.getInstance().hasSingleplayerServer()) LegacyClientWorldSettings.of(Minecraft.getInstance().getSingleplayerServer().getWorldData()).setSelectedResourceAlbum(savedAlbum);
            if (reloadAndSave) reloadChanges.accept(this);
        }

        public static void applyResourceChanges(Minecraft minecraft, List<String> oldSelection, List<String> newSelection, Runnable runnable){
            GlobalPacks.globalResources.get().applyPacks(minecraft.getResourcePackRepository(),newSelection);
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
                packRepository.setSelected(getSelectedAlbum().packs());
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
                if (LegacyRenderUtil.isMouseOver(d,e, getX() + 20 + 30 * index, getY() + minecraft.font.lineHeight +  3, 30,  30)) {
                    setSelectedIndex(index + scrolledList.get());
                    savedAlbum = getSelectedAlbum();
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
                if (visibleCount>=getMaxPacks()) break;
                PackAlbum album = albums.getByIndex(Math.min(albums.size() - 1, scrolledList.get() + index));
                if (album.iconSprite().isPresent()) FactoryGuiGraphics.of(guiGraphics).blitSprite(album.iconSprite().get(),getX() + 21 + 30 * index,getY() + font.lineHeight + 4,28,28);
                else FactoryGuiGraphics.of(guiGraphics).blit(album.isValidPackDisplay(packRepository) ? getPackIcon(packRepository.getPack(album.getDisplayPackId())) : DEFAULT_ICON, getX() + 21 + 30 * index,getY() + font.lineHeight + 4,0.0f, 0.0f, 28, 28, 28, 28);
                if (scrolledList.get() + index == selectedIndex)
                    FactoryGuiGraphics.of(guiGraphics).blitSprite(PACK_HIGHLIGHTED, getX() + 20 + 30 * index,getY() +font.lineHeight + 3,30,30);
                visibleCount++;
            }
            FactoryScreenUtil.disableBlend();
            guiGraphics.pose().pushMatrix();
            if (!isHoveredOrFocused()) guiGraphics.pose().translate(0.5f,0.5f,0f);
            guiGraphics.drawString(font,getMessage(),getX() + 2,getY(),isHoveredOrFocused() ? LegacyRenderUtil.getDefaultTextColor() : CommonColor.INVENTORY_GRAY_TEXT.get(),isHoveredOrFocused());
            guiGraphics.pose().popMatrix();
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
                        textureManager.register(resourceLocation3, new DynamicTexture(/*? if >1.21.4 {*/resourceLocation3::toString, /*?}*/nativeImage));
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

        public PackAlbum getSelectedAlbum() {
            return albums.isEmpty() || selectedIndex > albums.size() ? null : albums.getByIndex(selectedIndex);
        }

        @Override
        public @Nullable Component getAction(Context context) {
            return context.actionOfContext(KeyContext.class,k-> k.key() == InputConstants.KEY_O && isFocused() ? LegacyComponents.ALBUM_OPTIONS : k.key() == InputConstants.KEY_X && isFocused() || k.key() == InputConstants.MOUSE_BUTTON_LEFT && isHovered() ? screenComponent : ControlTooltip.getSelectAction(this,context));
        }
    }
}
