package wily.legacy.client.screen;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.Tooltip;
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
import org.joml.Math;
import wily.legacy.LegacyMinecraft;
import wily.legacy.util.ScreenUtil;
import wily.legacy.util.Stocker;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static wily.legacy.client.LegacySprites.PACK_HIGHLIGHTED_SPRITE;
import static wily.legacy.client.LegacySprites.PACK_SELECTED_SPRITE;

public class PackSelector extends AbstractWidget {
    public static final ResourceLocation DEFAULT_ICON = new ResourceLocation("textures/misc/unknown_pack.png");
    public List<Pack> availablePacks;
    public List<Pack> selectedPacks;
    private final Map<String, ResourceLocation> packIcons = Maps.newHashMap();
    private final Map<String, ResourceLocation> packBackgrounds = Maps.newHashMap();
    public final Stocker.Sizeable scrolledList;
    private final Path packPath;
    private final Consumer<PackSelector> reloadChanges;
    private final boolean hasTooltip;
    public int selectedIndex = -1;
    public Pack selectedPack;
    public boolean hasChanged = false;
    private final PackRepository packRepository;
    private final Minecraft minecraft;
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    public final ScrollableRenderer scrollableRenderer  = new ScrollableRenderer(scrollRenderer);
    public LoadingCache<Component, MultiLineLabel> labelsCache = CacheBuilder.newBuilder().build(new CacheLoader<>() {
        @Override
        public MultiLineLabel load(Component key) {
            return MultiLineLabel.create(minecraft.font,key,145);
        }
    });;
    public static PackSelector resources(int i, int j, int k, int l, boolean hasTooltip) {
        return new PackSelector(i,j,k,l, Component.translatable("options.resourcepack"), Minecraft.getInstance().getResourcePackRepository(),Minecraft.getInstance().getResourcePackDirectory(), PackSelector::reloadResourcesChanges,hasTooltip);
    }
    public PackSelector(int i, int j, int k, int l, Component component, PackRepository packRepository, Path packPath, Consumer<PackSelector> reloadChanges, boolean hasTooltip) {
        super(i, j, k, l,component);
        this.packPath = packPath;
        this.reloadChanges = reloadChanges;
        this.hasTooltip = hasTooltip;
        minecraft = Minecraft.getInstance();
        this.packRepository = packRepository;
        updatePacks();
        scrolledList = new Stocker.Sizeable(0);
        List<Pack> displayPacks = getDisplayPacks();
        int s = displayPacks.size();
        if (s > getMaxPacks())
            scrolledList.max = displayPacks.size() - getMaxPacks();
        setSelectedPack(0);
        updateTooltip();
    }
    public void updatePacks(){
        selectedPacks = new ArrayList<>(packRepository.getSelectedPacks());
        Collections.reverse(selectedPacks);
        availablePacks = new ArrayList<>(packRepository.getAvailablePacks().stream().filter(p->!selectedPacks.contains(p)).toList());
    }
    public List<Pack> getDisplayPacks(){
        return Stream.concat(selectedPacks.stream(),availablePacks.stream()).toList();
    }
    public void updateTooltip(){
        if (hasTooltip) setTooltip(Tooltip.create(selectedPack.getDescription(), selectedPack.getTitle()));
    }
    public void renderTooltipBox(GuiGraphics guiGraphics, Panel panel){
        renderTooltipBox(guiGraphics,panel.x + panel.width - 2, panel.y + 5, 161, panel.height - 10);
    }
    public void renderTooltipBox(GuiGraphics graphics, int x, int y, int width, int height){
        if (!ScreenUtil.hasTooltipBoxes()) return;
        ScreenUtil.renderPointerPanel(graphics,x, y,width,height);
        if (selectedPack != null){
            graphics.blit(getPackIcon(selectedPack), x + 7,y + 10,0.0f, 0.0f, 28, 28, 28, 28);
            ScreenUtil.renderScrollingString(graphics,minecraft.font,selectedPack.getTitle(), x + 40, y + 13,x + 148, y + 21,0xFFFFFF, true);
            ResourceLocation background = getPackBackground(selectedPack);
            MultiLineLabel label = labelsCache.getUnchecked(selectedPack.getDescription());
            scrollableRenderer.render(graphics, x + 8,y + 38, 146, 12 * (background == null ? 20 : 7), ()->label.renderLeftAligned(graphics,x + 8, y + 42,12,0xFFFFFF));
            if (background != null) graphics.blit(background, x + 8,y + height - 78,0.0f, 0.0f, 145, 72, 145, 72);
        }
    }
    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (isHoveredOrFocused()) {
            if (CommonInputs.selected(i)) {
                tryChangePackState(selectedIndex);
                return true;
            }
            if (i == 263) {
                if (selectedIndex == scrolledList.get()) updateScroll(-1);
                setSelectedPack(selectedIndex - 1);
            } else if (i == 262) {
                if (selectedIndex == scrolledList.get() + getMaxPacks() - 1) updateScroll(1);
                setSelectedPack(selectedIndex + 1);
            }
        }
        return super.keyPressed(i, j, k);
    }

    public void setSelectedPack(int index) {
        if (selectedIndex == index) return;
        List<Pack> displayPacks = getDisplayPacks();
        this.selectedIndex = Stocker.cyclic(0,index,displayPacks.size());
        selectedPack = displayPacks.get(selectedIndex);
        scrollableRenderer.scrolled.set(0);
        ResourceLocation background = getPackBackground(selectedPack);
        scrollableRenderer.scrolled.max = Math.max(0,labelsCache.getUnchecked(selectedPack.getDescription()).getLineCount() - (background == null ? 20 : 7));
        updateTooltip();
    }


    public void tryChangePackState(int index){
        Pack p = getDisplayPacks().get(index);
        if (p.isRequired()) return;
        hasChanged = true;
        if (selectedPacks.contains(p)){
            selectedPacks.remove(p);
            availablePacks.add(p);
        }else{
            availablePacks.remove(p);
            selectedPacks.add(0,p);
        }
    }
    public void applyChanges(boolean reload){
        if (hasChanged) {
            List<String> list = selectedPacks.stream().map(Pack::getId).collect(Collectors.collectingAndThen(Collectors.toList(), l -> {
                Collections.reverse(l);
                return l;
            }));
            packRepository.setSelected(list);
            if (reload)
                reloadChanges.accept(this);
        }
    }
    public static void reloadResourcesChanges(PackSelector selector){
        selector.minecraft.options.updateResourcePacks(selector.packRepository);
    }
    @Override
    public void onClick(double d, double e) {
        if (Screen.hasShiftDown() && minecraft.screen != null) {
            Screen screen = minecraft.screen;
            applyChanges(false);
            minecraft.setScreen(new PackSelectionScreen(packRepository,p-> {
                reloadChanges.accept(this);
                updatePacks();
                minecraft.setScreen(screen);
            },packPath, getMessage()));
            return;
        }
        int visibleCount = 0;
        for (int index = 0; index < getDisplayPacks().size(); index++) {
            if (visibleCount>=getMaxPacks()) break;
            if (d >= getX() + 20 + 30 * index && e >= getY() +minecraft.font.lineHeight +  3 && d < getX()+minecraft.font.lineHeight + 49 + 30 * index && e < getY() + minecraft.font.lineHeight + 32) {
                if (selectedIndex == index + scrolledList.get()) tryChangePackState(index + scrolledList.get());
                setSelectedPack(index + scrolledList.get());
            }
            visibleCount++;
        }
        super.onClick(d, e);
    }


    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (updateScroll((int) Math.signum(g))) return true;
        return super.mouseScrolled(d, e, f, g);
    }
    public boolean updateScroll(int i){
        if (scrolledList.max > 0) {
            if ((scrolledList.get() <= scrolledList.max && i > 0) || (scrolledList.get() >= 0 && i < 0)) {
                scrolledList.set(scrolledList.get() + i,true);
                return true;
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
        ScreenUtil.renderPanelRecess(guiGraphics,getX() -1,getY()+ font.lineHeight -1 , width + 2,height + 2 - minecraft.font.lineHeight  ,2);
        List<Pack> displayPacks = getDisplayPacks();
        int visibleCount = 0;
        RenderSystem.enableBlend();
        for (int index = 0; index < displayPacks.size(); index++) {
            if (visibleCount>=getMaxPacks()) break;
            guiGraphics.blit(getPackIcon(displayPacks.get(scrolledList.get() + index)), getX() + 21 + 30 * index,getY() + font.lineHeight + 4,0.0f, 0.0f, 28, 28, 28, 28);
            if (selectedPacks.contains(displayPacks.get(scrolledList.get() + index)))  guiGraphics.blitSprite(PACK_SELECTED_SPRITE, getX() + 20 + 30 * index,getY() +font.lineHeight + 3,30,30);
            if (scrolledList.get() + index == selectedIndex)
                guiGraphics.blitSprite(PACK_HIGHLIGHTED_SPRITE, getX() + 20 + 30 * index,getY() +font.lineHeight + 3,30,30);
            visibleCount++;
        }
        RenderSystem.disableBlend();
        guiGraphics.drawString(font,getMessage(),getX() + 1,getY(),isHoveredOrFocused() ? ScreenUtil.getDefaultTextColor() : 0x404040,isHoveredOrFocused());
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
                ResourceLocation resourceLocation3 = new ResourceLocation("minecraft", "pack/" + Util.sanitizeName(string, ResourceLocation::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars(string) + "/icon");
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
            LegacyMinecraft.LOGGER.warn("Failed to load icon from pack {}", pack.getId(), exception);
            return fallback;
        }
    }

    public ResourceLocation getPackIcon(Pack pack) {
        return this.packIcons.computeIfAbsent(pack.getId(), string -> this.loadPackIcon(Minecraft.getInstance().getTextureManager(), pack, "pack.png",DEFAULT_ICON));
    }
    public ResourceLocation getPackBackground(Pack pack) {
        return this.packBackgrounds.computeIfAbsent(pack.getId(), string -> pack.getId().equals("vanilla") ? new ResourceLocation("background.png"): this.loadPackIcon(Minecraft.getInstance().getTextureManager(), pack, "background.png",null));
    }
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}
