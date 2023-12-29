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
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.IoSupplier;
import wily.legacy.LegacyMinecraft;
import wily.legacy.util.ScreenUtil;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Stream;

public class ResourcePackSelector extends AbstractWidget {
    public static final ResourceLocation DEFAULT_ICON = new ResourceLocation("textures/misc/unknown_pack.png");
    public static final ResourceLocation PACK_HIGHLIGHTED = new ResourceLocation(LegacyMinecraft.MOD_ID, "widget/pack_highlighted");
    public static final ResourceLocation PACK_SELECTED = new ResourceLocation(LegacyMinecraft.MOD_ID, "widget/pack_selected");
    public final List<Pack> availablePacks;
    public final List<Pack> selectedPacks;
    private final PackRepository packRepository;
    private final Map<String, ResourceLocation> packIcons = Maps.newHashMap();
    public int selectedResourcePack = 0;
    public boolean hasChanged = false;

    public ResourcePackSelector(int i, int j, int k, int l) {
        super(i, j, k, l, Component.translatable("options.resourcepack"));
        this.packRepository = Minecraft.getInstance().getResourcePackRepository();
        selectedPacks =  new ArrayList<>(packRepository.getSelectedPacks());
        Collections.reverse(selectedPacks);
        availablePacks = new ArrayList<>(packRepository.getAvailablePacks().stream().filter(p->!selectedPacks.contains(p)).toList());
    }
    public List<Pack> getDisplayPacks(){
        return Stream.concat(selectedPacks.stream(),availablePacks.stream()).toList();
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (isHoveredOrFocused()) {
            if (CommonInputs.selected(i)) {
                tryChangePackState(selectedResourcePack);
                return true;
            }
            if (i == 263) selectedResourcePack = (selectedResourcePack == 0 ? getDisplayPacks().size() : selectedResourcePack) - 1;
            else if (i == 262)
                selectedResourcePack = (selectedResourcePack == getDisplayPacks().size() - 1 ? 0 : selectedResourcePack + 1);
        }
        return super.keyPressed(i, j, k);
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

    @Override
    public boolean mouseClicked(double d, double e, int i) {
        if (isHoveredOrFocused() && isValidClickButton(i)) {
            int visibleCount = 0;
            for (int index = 0; index < getDisplayPacks().size(); index++) {
                if (visibleCount>=6) break;
                if (d >= getX() + 20 + 30 * index && e >= getY() + 3 && d < getX() + 49 + 30 * index && e < getY() + 32) {
                    if (selectedResourcePack == index) tryChangePackState(index);
                    selectedResourcePack = index;
                }
                visibleCount++;
            }
        }
        return super.mouseClicked(d, e, i);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
        Font font = Minecraft.getInstance().font;
        ScreenUtil.renderPanelRecess(guiGraphics,getX() -1,getY() -1, width + 2,height + 2,2);
        List<Pack> displayPacks = getDisplayPacks();
        setTooltip(Tooltip.create(displayPacks.get(selectedResourcePack).getDescription(), displayPacks.get(selectedResourcePack).getTitle()));
        int visibleCount = 0;
        for (int index = 0; index < displayPacks.size(); index++) {
            if (visibleCount>=6) break;
            RenderSystem.enableBlend();
            guiGraphics.blit(getPackIcon(displayPacks.get(index)), getX() + 21 + 30 * index,getY() + 4,0.0f, 0.0f, 28, 28, 28, 28);
            if (selectedPacks.contains(displayPacks.get(index)))  guiGraphics.blitSprite(PACK_SELECTED, getX() + 20 + 30 * index,getY() + 3,30,30);
            if (index == selectedResourcePack)
                guiGraphics.blitSprite(PACK_HIGHLIGHTED, getX() + 20 + 30 * index,getY() + 3,30,30);
            visibleCount++;
        }
        guiGraphics.drawString(font,getMessage(),getX() + 1,getY() - font.lineHeight,isHoveredOrFocused() ? 0xFFFFFF : 0x404040,isHoveredOrFocused());
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
