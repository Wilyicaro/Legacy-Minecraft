package wily.legacy.client.screen;

import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
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

public class PackSelector extends AbstractWidget {
    public static final ResourceLocation DEFAULT_ICON = new ResourceLocation("textures/misc/unknown_pack.png");
    public static final ResourceLocation PACK_HIGHLIGHTED = new ResourceLocation(LegacyMinecraft.MOD_ID, "widget/pack_highlighted");
    public static final ResourceLocation PACK_SELECTED = new ResourceLocation(LegacyMinecraft.MOD_ID, "widget/pack_selected");
    public List<Pack> availablePacks;
    public List<Pack> selectedPacks;
    private final Map<String, ResourceLocation> packIcons = Maps.newHashMap();
    public final Stocker.Sizeable scrolledList;
    private final Path packPath;
    private final Consumer<PackSelector> reloadChanges;
    public int selectedPack = 0;
    public boolean hasChanged = false;
    private final PackRepository packRepository;
    private final Minecraft minecraft;

    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    public static PackSelector resources(int i, int j, int k, int l) {
        return new PackSelector(i,j,k,l, Component.translatable("options.resourcepack"), Minecraft.getInstance().getResourcePackRepository(),Minecraft.getInstance().getResourcePackDirectory(), PackSelector::reloadResourcesChanges);
    }
    public PackSelector(int i, int j, int k, int l, Component component, PackRepository packRepository, Path packPath, Consumer<PackSelector> reloadChanges) {
        super(i, j, k, l,component);
        this.packPath = packPath;
        this.reloadChanges = reloadChanges;
        minecraft = Minecraft.getInstance();
        this.packRepository = packRepository;
        updatePacks();
        scrolledList = new Stocker.Sizeable(0);
        List<Pack> displayPacks = getDisplayPacks();
        updateTooltip(displayPacks);
        int s = displayPacks.size();
        if (s > getMaxPacks())
            scrolledList.max = getDisplayPacks().size() - getMaxPacks();

    }
    public void updatePacks(){
        selectedPacks =  new ArrayList<>(packRepository.getSelectedPacks());
        Collections.reverse(selectedPacks);
        availablePacks = new ArrayList<>(packRepository.getAvailablePacks().stream().filter(p->!selectedPacks.contains(p)).toList());
    }
    public List<Pack> getDisplayPacks(){
        return Stream.concat(selectedPacks.stream(),availablePacks.stream()).toList();
    }
    public void updateTooltip(List<Pack> packs){
        setTooltip(Tooltip.create(packs.get(selectedPack).getDescription(), packs.get(selectedPack).getTitle()));
    }


    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (isHoveredOrFocused()) {
            if (CommonInputs.selected(i)) {
                tryChangePackState(selectedPack);
                return true;
            }
            if (i == 263) {
                if (selectedPack == scrolledList.get()) updateScroll(-1);
                setSelectedPack(selectedPack - 1);
            } else if (i == 262) {
                if (selectedPack == scrolledList.get() + getMaxPacks() - 1) updateScroll(1);
                setSelectedPack(selectedPack + 1);
            }
        }
        return super.keyPressed(i, j, k);
    }

    public void setSelectedPack(int index) {
        if (selectedPack == index) return;
        List<Pack> displayPacks = getDisplayPacks();
        this.selectedPack = Stocker.cyclic(0,index,displayPacks.size());
        updateTooltip(displayPacks);
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
                if (selectedPack == index + scrolledList.get()) tryChangePackState(index + scrolledList.get());
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
        for (int index = 0; index < displayPacks.size(); index++) {
            if (visibleCount>=getMaxPacks()) break;
            RenderSystem.enableBlend();
            guiGraphics.blit(getPackIcon(displayPacks.get(scrolledList.get() + index)), getX() + 21 + 30 * index,getY() + font.lineHeight + 4,0.0f, 0.0f, 28, 28, 28, 28);
            if (selectedPacks.contains(displayPacks.get(scrolledList.get() + index)))  guiGraphics.blitSprite(PACK_SELECTED, getX() + 20 + 30 * index,getY() +font.lineHeight + 3,30,30);
            if (scrolledList.get() + index == selectedPack)
                guiGraphics.blitSprite(PACK_HIGHLIGHTED, getX() + 20 + 30 * index,getY() +font.lineHeight + 3,30,30);
            visibleCount++;
        }
        guiGraphics.drawString(font,getMessage(),getX() + 1,getY(),isHoveredOrFocused() ? 0xFFFFFF : 0x404040,isHoveredOrFocused());
        if (scrolledList.max > 0){
            if (scrolledList.get() < scrolledList.max) scrollRenderer.renderScroll(guiGraphics, ScreenDirection.RIGHT, getX() + width - 12, getY() + font.lineHeight + (height - font.lineHeight - 11) / 2);
            if (scrolledList.get() > 0) scrollRenderer.renderScroll(guiGraphics,ScreenDirection.LEFT,getX() + 8, getY() + font.lineHeight + (height - font.lineHeight - 11) / 2);
        }
    }
    private ResourceLocation loadPackIcon(TextureManager textureManager, Pack pack) {
        try (PackResources packResources = pack.open();){
            ResourceLocation resourceLocation;
            block16: {
                IoSupplier<InputStream> ioSupplier = packResources.getRootResource("pack.png");
                if (ioSupplier == null) {
                    ResourceLocation resourceLocation2 = DEFAULT_ICON;
                    return resourceLocation2;
                }
                String string = pack.getId();
                ResourceLocation resourceLocation3 = new ResourceLocation("minecraft", "pack/" + Util.sanitizeName(string, ResourceLocation::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars(string) + "/icon");
                InputStream inputStream = ioSupplier.get();
                try {
                    NativeImage nativeImage = NativeImage.read(inputStream);
                    textureManager.register(resourceLocation3, new DynamicTexture(nativeImage));
                    resourceLocation = resourceLocation3;
                    if (inputStream == null) break block16;
                } catch (Throwable throwable) {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                inputStream.close();
            }
            return resourceLocation;
        } catch (Exception exception) {
            LegacyMinecraft.LOGGER.warn("Failed to load icon from pack {}", pack.getId(), exception);
            return DEFAULT_ICON;
        }
    }

    private ResourceLocation getPackIcon(Pack pack) {
        return this.packIcons.computeIfAbsent(pack.getId(), string -> this.loadPackIcon(Minecraft.getInstance().getTextureManager(), pack));
    }
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}
