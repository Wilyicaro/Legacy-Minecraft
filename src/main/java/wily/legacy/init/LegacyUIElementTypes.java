package wily.legacy.init;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.client.FactoryGuiGraphics;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.base.client.UIDefinition;
import wily.factoryapi.base.client.UIDefinitionManager;
import wily.factoryapi.util.DynamicUtil;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.LegacyIconHolder;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.client.screen.RenderableVList;
import wily.legacy.client.screen.ScrollableRenderer;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.ScreenUtil;

import java.util.ArrayList;
import java.util.List;

public class LegacyUIElementTypes {
    private static final Container emptyFakeContainer = new SimpleContainer();
    public static final ResourceLocation ENCHANTING_TABLE_BOOK = FactoryAPI.createVanillaLocation("textures/entity/enchanting_table_book.png");

    public static final UIDefinitionManager.ElementType PUT_LEGACY_SLOT = UIDefinitionManager.ElementType.registerConditional("put_legacy_slot", UIDefinitionManager.ElementType.createIndexable(slots->(uiDefinition, accessorFunction, elementName, element) -> {
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "fakeContainer", (s, d)->d.asListOpt(d1->DynamicUtil.getItemFromDynamic(d1, true)).result().map(l-> UIDefinition.createBeforeInit(elementName, (a)-> a.putStaticElement(s,new SimpleContainer(l.stream().map(ArbitrarySupplier::get).toArray(ItemStack[]::new))))).orElse(null));
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "fakeItem", (s, d)-> UIDefinitionManager.ElementType.parseItemStackElement(elementName, s, d));
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "spriteOverride", ResourceLocation.CODEC);
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "iconSprite", ResourceLocation.CODEC);
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "offset", DynamicUtil.VEC3_OBJECT_CODEC);
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, (s, d) -> UIDefinitionManager.ElementType.parseBooleanElement(elementName, s, d),"iconCondition", "iconHolderCondition", "isVisible", "isFake", "isWarning");
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, (s, d) -> UIDefinitionManager.ElementType.parseNumberElement(elementName, s, d), "x", "y", "width", "height");
        uiDefinition.getDefinitions().add(UIDefinition.createAfterInit(elementName,a->{
            Bearer<Integer> index = Bearer.of(0);
            a.getElements().put(elementName + ".index", index);
            slots.forEach(i->{
                Slot s = null;
                boolean isFake = a.getBoolean(elementName+".isFake", false);

                if (isFake)
                    s = new Slot(a.getElementValue(elementName+".fakeContainer", emptyFakeContainer, Container.class), a.getElements().containsKey(elementName+".fakeContainer") ? index.get() : 0, 0, 0);
                else if (a.getScreen() instanceof LegacyMenuAccess<?> access && access.getMenu().slots.size() > i && !access.getMenu().slots.isEmpty()) {
                    s = access.getMenu().slots.get(i);
                }
                if (s == null) return;
                LegacySlotDisplay.override(s, a.getInteger(elementName+".x", s.x), a.getInteger(elementName+".y", s.y), new LegacySlotDisplay() {
                    @Override
                    public ItemStack getItemOverride() {
                        return a.getElementValue(elementName+".fakeItem", LegacySlotDisplay.super.getItemOverride(), ItemStack.class);
                    }

                    @Override
                    public int getWidth() {
                        return a.getInteger(elementName+".width", LegacySlotDisplay.super.getWidth());
                    }
                    @Override
                    public int getHeight() {
                        return a.getInteger(elementName+".height", LegacySlotDisplay.super.getHeight());
                    }

                    @Override
                    public Vec3 getOffset() {
                        return a.getElementValue(elementName+".offset", LegacySlotDisplay.super.getOffset(), Vec3.class);
                    }

                    @Override
                    public boolean isVisible() {
                        return a.getBoolean(elementName+".isVisible", LegacySlotDisplay.super.isVisible());
                    }

                    @Override
                    public ResourceLocation getIconSprite() {
                        return a.getBoolean(elementName+".iconCondition",true) ? a.getElementValue(elementName+".iconSprite", LegacySlotDisplay.super.getIconSprite(), ResourceLocation.class) : null;
                    }

                    @Override
                    public ArbitrarySupplier<ResourceLocation> getIconHolderOverride() {
                        return a.getBoolean(elementName+".iconHolderCondition",true) ? a.getElement(elementName+".spriteOverride",ResourceLocation.class).or(LegacySlotDisplay.super.getIconHolderOverride()) : ArbitrarySupplier.empty();
                    }

                    @Override
                    public boolean isWarning() {
                        return a.getBoolean(elementName+".isWarning", LegacySlotDisplay.super.isWarning());
                    }
                });
                if (isFake)
                    a.addRenderable(LegacyIconHolder.fromSlot(s));
                index.set(index.get() + 1);
            });
        }));
    }));

    public static final UIDefinitionManager.ElementType PUT_SCROLLABLE_RENDERER = UIDefinitionManager.ElementType.registerConditional("put_scrollable_renderer", UIDefinitionManager.ElementType.createIndexable(slots->(uiDefinition, accessorFunction, elementName, element) -> {
        uiDefinition.getDefinitions().add(UIDefinition.createBeforeInit(a-> {
            a.putStaticElement(elementName+".renderables", UIAccessor.createRenderablesWrapper(a, new ArrayList<>()));
            a.putStaticElement(elementName, new ScrollableRenderer());
        }));
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "backgroundSprite", ResourceLocation.CODEC);
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "hasBackground", (s, d) -> UIDefinitionManager.ElementType.parseBooleanElement(elementName, s, d));
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, (s, d) -> UIDefinitionManager.ElementType.parseNumberElement(elementName, s, d), "x", "y", "width", "height");
        UIDefinitionManager.parseAllElements(uiDefinition, a-> a.getElementValue(elementName+".renderables", a, UIAccessor.class), element, s-> s);
        uiDefinition.getDefinitions().add(UIDefinition.createAfterInit(elementName, a-> a.addRenderable(elementName, ((guiGraphics, i, j, f) -> {
            int x = a.getInteger(elementName + ".x", 0);
            int y = a.getInteger(elementName + ".y", 0);
            int width = a.getInteger(elementName + ".width", 0);
            int height = a.getInteger(elementName + ".height", 0);
            if (a.getBoolean(elementName+".hasBackground", true)) ScreenUtil.blitTranslucentOverlaySprite(guiGraphics, a.getResourceLocation(elementName+".backgroundSprite", LegacySprites.POINTER_PANEL), x, y, width, height );
            a.getElement(elementName, ScrollableRenderer.class).ifPresent(s-> s.render(guiGraphics, x + 11, y + 11, width - 22, height - 28, ()-> {
                int yOffset = 0;
                for (Renderable r : a.getElementValue(elementName+".renderables", a, UIAccessor.class).getChildrenRenderables()) {
                    if (r instanceof LayoutElement e) {
                        e.setPosition(x + 11, y + 15 + yOffset);
                        r.render(guiGraphics, i, j + Math.round(s.getYOffset()), f);
                        yOffset += e.getHeight();
                    }
                }
                s.scrolled.max = Math.max(0, Mth.ceil((yOffset - (height - 28)) / 12f));
            }));
        }))));
    }));

    public static final UIDefinitionManager.ElementType PUT_RENDERABLE_VLIST = UIDefinitionManager.ElementType.registerConditional("put_renderable_vertical_list", UIDefinitionManager.ElementType.createIndexable(slots->(uiDefinition, accessorFunction, elementName, element) -> {
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, (s, d) -> UIDefinitionManager.ElementType.parseBooleanElement(elementName, s, d), "forceWidth", "cyclic");
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, (s, d) -> UIDefinitionManager.ElementType.parseNumberElement(elementName, s, d), "x", "y", "width", "height", "layoutSpacing");
        uiDefinition.getDefinitions().add(UIDefinition.createBeforeInit(a-> {
            RenderableVList renderableVList = new RenderableVList(a).layoutSpacing(l-> a.getInteger(elementName+".layoutSpacing", 5)).cyclic(a.getBoolean(elementName+".forceWidth", true)).cyclic(a.getBoolean(elementName+".cyclic", true));
            a.putStaticElement(elementName, renderableVList);
            a.putStaticElement(elementName+".renderables", UIAccessor.createRenderablesWrapper(a, renderableVList.renderables));
        })); UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, (s, d) -> UIDefinitionManager.ElementType.parseNumberElement(elementName, s, d), "x", "y", "width", "height");
        UIDefinitionManager.parseAllElements(uiDefinition, a-> a.getElementValue(elementName+".renderables", a, UIAccessor.class), element, s-> s);
        uiDefinition.getDefinitions().add(UIDefinition.createAfterInit(elementName, a-> a.getElementValue(elementName, null, RenderableVList.class).init(elementName, 0, 0, 225, 0)));
    }));

    public static final UIDefinitionManager.ElementType MODIFY_RENDERABLE_VLIST = UIDefinitionManager.ElementType.registerConditional("modify_renderable_vertical_list", UIDefinitionManager.ElementType.createIndexable(slots->(uiDefinition, accessorFunction, elementName, element) -> {
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, (s, d) -> UIDefinitionManager.ElementType.parseNumberElement(elementName, s, d), "index");
        UIDefinition renderableVListUIDefinition = new UIDefinition() {
            private final List<UIDefinition> definitions = new ArrayList<>();
            @Override
            public List<UIDefinition> getDefinitions() {
                return definitions;
            }

            @Override
            public void beforeInit(UIAccessor accessor) {
                if (!accessor.initialized() && accessor.getScreen() instanceof RenderableVList.Access access){
                    accessor.putStaticElement(elementName+".renderables", UIAccessor.createRenderablesWrapper(accessor, access.getRenderableVLists().get(accessor.getInteger(elementName + ".index", 0)).renderables));
                    UIDefinition.super.beforeInit(accessor);
                    UIDefinition.super.afterInit(accessor);
                }
            }

            @Override
            public void afterInit(UIAccessor accessor) {
            }
        };
        UIDefinitionManager.parseAllElements(renderableVListUIDefinition, a-> a.getElementValue(elementName+".renderables", a, UIAccessor.class), element, s-> s);
        uiDefinition.getDefinitions().add(renderableVListUIDefinition);
    }));

    public static final UIDefinitionManager.ElementType DRAW_OUTLINED_STRING = UIDefinitionManager.ElementType.registerConditional("draw_outlined_string", UIDefinitionManager.ElementType.createIndexable(slots->(uiDefinition, accessorFunction, elementName, element) -> {
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "component", (s, d) -> UIDefinitionManager.ElementType.parseComponentElement(elementName, s, d));
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, (s, d) -> UIDefinitionManager.ElementType.parseNumberElement(elementName, s, d), "outline");
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, (s, d) -> UIDefinitionManager.ElementType.parseNumberElement(elementName, s, d), "x", "y", "color", "outlineColor", "order");
        UIDefinitionManager.ElementType.parseTranslationElements(uiDefinition, elementName, element);
        uiDefinition.getDefinitions().add(UIDefinition.createAfterInit(elementName, (a) -> accessorFunction.apply(a).addRenderable(elementName, a.createModifiableRenderable(elementName, (guiGraphics, i, j, f) -> {
            a.getElement(elementName + ".component", Component.class).ifPresent((c) -> ScreenUtil.drawOutlinedString(guiGraphics ,Minecraft.getInstance().font, c, a.getInteger(elementName + ".x", 0), a.getInteger(elementName + ".y", 0), a.getInteger(elementName + ".color", 16777215), a.getInteger(elementName + ".outlineColor", 0), a.getFloat(elementName + ".outline", 0.5f)));
        }))));
    }));

    public static final UIDefinitionManager.ElementType RENDER_ENCHANTED_BOOK = UIDefinitionManager.ElementType.registerConditional("render_enchanted_book", UIDefinitionManager.ElementType.createIndexable(slots->(uiDefinition, accessorFunction, elementName, element) -> {
        Bearer<BookModel> bookModel = Bearer.of(null);
        Bearer<Float> flip = Bearer.of(0f);
        Bearer<Float> oFlip = Bearer.of(0f);
        Bearer<Float> flipT = Bearer.of(0f);
        Bearer<Float> flipA = Bearer.of(0f);
        Bearer<Float> open = Bearer.of(0f);
        Bearer<Float> oOpen = Bearer.of(0f);
        RandomSource random = RandomSource.create();
        Bearer<Boolean> canOpenBook = Bearer.of(false);
        uiDefinition.getDefinitions().add(UIDefinition.createBeforeInit(a-> {
            if (!a.initialized()) {
                bookModel.set(new BookModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.BOOK)));
                flip.set(0f);
                oFlip.set(0f);
                flipT.set(0f);
                flipA.set(0f);
                open.set(0f);
                oOpen.set(0f);
                canOpenBook.set(true);
            }
        }));
        uiDefinition.getDefinitions().add(UIDefinition.createBeforeTick(a-> {
            if (canOpenBook.get()) {
                canOpenBook.set(false);
                do {
                    flipT.set(flipT.get() + (float)(random.nextInt(4) - random.nextInt(4)));
                } while(flip.get() <= flipT.get() + 1.0F && flip.get() >= flipT.get() - 1.0F);
            }

            oFlip.set(flip.get());
            oOpen.set(open.get());
            open.set(open.get() + 0.2F);

            open.set(Mth.clamp(open.get(), 0.0F, 1.0F));
            float f1 = (flipT.get() - flip.get()) * 0.4F;
            f1 = Mth.clamp(f1, -0.2F, 0.2F);
            flipA.set(flipA.get() + (f1 - flipA.get()) * 0.9F);
            flip.set(flip.get() + flipA.get());
        }));
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, (s, d) -> UIDefinitionManager.ElementType.parseNumberElement(elementName, s, d), "x", "y");
        UIDefinitionManager.ElementType.parseTranslationElements(uiDefinition, elementName, element);
        uiDefinition.getDefinitions().add(UIDefinition.createAfterInit(elementName, (a) -> accessorFunction.apply(a).addRenderable(elementName, a.createModifiableRenderable(elementName, (guiGraphics, i, j, f) -> {
            float g = Mth.lerp(f, oOpen.get(), open.get());
            float f1 = Mth.lerp(f, oFlip.get(), flip.get());
            guiGraphics.flush();
            Lighting.setupForEntityInInventory();
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate( a.getInteger(elementName+".x", 0) + 33.0F, a.getInteger(elementName+".y", 0) + 31.0F, 100.0F);
            guiGraphics.pose().scale(-40.0F, 40.0F, 40.0F);
            guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(25.0F));
            guiGraphics.pose().translate((1.0F - g) * 0.2F, (1.0F - g) * 0.1F, (1.0F - g) * 0.25F);
            float f3 = -(1.0F - g) * 90.0F - 90.0F;
            guiGraphics.pose().mulPose(Axis.YP.rotationDegrees(f3));
            guiGraphics.pose().mulPose(Axis.XP.rotationDegrees(180.0F));
            float f4 = Mth.clamp(Mth.frac(f1 + 0.25F) * 1.6F - 0.3F, 0.0F, 1.0F);
            float f5 = Mth.clamp(Mth.frac(f1 + 0.75F) * 1.6F - 0.3F, 0.0F, 1.0F);
            bookModel.get().setupAnim(0.0F, f4, f5, g);
            VertexConsumer vertexconsumer = FactoryGuiGraphics.of(guiGraphics).getBufferSource().getBuffer(bookModel.get().renderType(ENCHANTING_TABLE_BOOK));
            bookModel.get().renderToBuffer(guiGraphics.pose(), vertexconsumer, 15728880, OverlayTexture.NO_OVERLAY/*? <1.20.5 {*//*, 1.0F, 1.0F, 1.0F, 1.0F*//*?}*/);
            FactoryGuiGraphics.of(guiGraphics).getBufferSource().endBatch();
            guiGraphics.flush();
            guiGraphics.pose().popPose();
            Lighting.setupFor3DItems();
        }))));
    }));

    public static void init(){
    }
}
