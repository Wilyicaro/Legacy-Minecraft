package wily.legacy.init;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Dynamic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.network.chat.TextColor;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.MaterialAssetGroup;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.item.equipment.trim.TrimPatterns;
import net.minecraft.world.phys.Vec2;
import org.joml.Quaternionf;
import org.joml.Vector2i;
import org.joml.Vector3f;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.client.AdvancedTextWidget;
import wily.factoryapi.base.client.UIAccessor;
import wily.factoryapi.base.client.UIDefinition;
import wily.factoryapi.base.client.UIDefinitionManager;
import wily.factoryapi.util.DynamicUtil;
import wily.legacy.client.CommonColor;
import wily.legacy.client.screen.*;
import wily.legacy.inventory.LegacySlotDisplay;
import wily.legacy.util.LegacySprites;
import wily.legacy.util.client.LegacyRenderUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class LegacyUIElementTypes {
    public static final ResourceLocation ENCHANTING_TABLE_BOOK = FactoryAPI.createVanillaLocation("textures/entity/enchanting_table_book.png");
    private static final Equippable DIAMOND_CHESTPLATE_EQUIPPABLE = Equippable.builder(EquipmentSlot.CHEST)
            .setEquipSound(SoundEvents.ARMOR_EQUIP_DIAMOND)
            .setAsset(EquipmentAssets.DIAMOND)
            .build();
    private static final Holder<TrimMaterial> EMERALD_TRIM_MATERIAL = new KeyedHolder<>(TrimMaterials.EMERALD, new TrimMaterial(MaterialAssetGroup.EMERALD, Component.translatable("trim_material.minecraft.emerald").withStyle(Style.EMPTY.withColor(1155126))));
    private static final Holder<TrimPattern> SENTRY_TRIM_PATTERN = new KeyedHolder<>(TrimPatterns.SENTRY, new TrimPattern(ResourceLocation.withDefaultNamespace("sentry"), Component.translatable("trim_pattern.minecraft.sentry"), false));
    private static final ArmorTrim EMERALD_SENTRY_TRIM = new ArmorTrim(EMERALD_TRIM_MATERIAL, SENTRY_TRIM_PATTERN);
    private static final Container emptyFakeContainer = new SimpleContainer();

    public static final UIDefinitionManager.ElementType PUT_SCROLLABLE_RENDERER = UIDefinitionManager.ElementType.registerConditional("put_scrollable_renderer", UIDefinitionManager.ElementType.createIndexable(slots -> (uiDefinition, accessorFunction, elementName, element) -> {
        uiDefinition.addStatic(UIDefinition.createBeforeInit(a -> {
            a.putStaticElement(elementName + ".renderables", UIAccessor.createRenderablesWrapper(a, new ArrayList<>()));
            a.putStaticElement(elementName, new ScrollableRenderer());
        }));
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "backgroundSprite", ResourceLocation.CODEC);
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "hasBackground", UIDefinitionManager.ElementType::parseBoolean);
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, UIDefinitionManager.ElementType::parseNumber, "x", "y", "contentX", "contentY", "width", "height", "lineHeight");
        UIDefinitionManager.parseAllElements(uiDefinition, a -> a.getElementValue(elementName + ".renderables", a, UIAccessor.class), element, s -> s);
        uiDefinition.addStatic(UIDefinition.createAfterInit(a -> a.addRenderable(elementName, ((guiGraphics, i, j, f) -> {
            int x = a.getInteger(elementName + ".x", 0);
            int y = a.getInteger(elementName + ".y", 0);
            int xd = a.getInteger(elementName + ".contentX", 11);
            int yd = a.getInteger(elementName + ".contentY", 11);
            int width = a.getInteger(elementName + ".width", 0);
            int height = a.getInteger(elementName + ".height", 0);
            int lineHeight = a.getInteger(elementName + ".lineHeight", 12);
            if (a.getBoolean(elementName + ".hasBackground", true))
                LegacyRenderUtil.blitTranslucentOverlaySprite(guiGraphics, a.getResourceLocation(elementName + ".backgroundSprite", LegacySprites.POINTER_PANEL), x, y, width, height);
            a.getElement(elementName, ScrollableRenderer.class).ifPresent(s -> s.render(guiGraphics, x + xd, y + yd, width - 2 * xd, height - 2 * yd - 6, () -> {
                int yOffset = 0;
                for (Renderable r : a.getElementValue(elementName + ".renderables", a, UIAccessor.class).getChildrenRenderables()) {
                    if (r instanceof LayoutElement e) {
                        e.setPosition(x + xd, y + yd + 4 + yOffset);
                        r.render(guiGraphics, i, j + Math.round(s.getYOffset()), f);
                        yOffset += e.getHeight();
                    }
                }
                s.lineHeight = lineHeight;
                s.scrolled.max = Math.max(0, Mth.ceil((yOffset - (height - 2 * yd - 6.0f)) / lineHeight));
            }));
        }))));
    }));
    public static final UIDefinitionManager.ElementType DRAW_MULTILINE_STRING = replaceElementType("draw_multiline_string", UIDefinitionManager.ElementType.createIndexable(slots -> (uiDefinition, accessorFunction, elementName, element) -> {
        UIDefinitionManager.ElementType.parseTextElements(uiDefinition, elementName, element);
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, UIDefinitionManager.ElementType::parseNumber, "lineSpacing", "width");
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "centered", UIDefinitionManager.ElementType::parseBoolean);
        uiDefinition.addStatic(UIDefinition.createAfterInit(a -> a.getElement(elementName + ".component", Component.class).ifPresent(c -> {
            UIAccessor accessor = accessorFunction.apply(a);
            int width = a.getInteger(elementName + ".width", 0);
            boolean howToPlay = accessor.getScreen() instanceof HowToPlayScreen;
            List<FormattedCharSequence> lines = Minecraft.getInstance().font.split(c, width);
            if (howToPlay) lines = lines.stream().map(LegacyUIElementTypes::howToPlayLine).toList();
            AdvancedTextWidget widget = new AdvancedTextWidget(a)
                    .lineSpacing(a.getInteger(elementName + ".lineSpacing", 12))
                    .withWidth(width)
                    .withLines(lines)
                    .withColor(a.getInteger(elementName + ".color", howToPlay ? CommonColor.TIP_TEXT.get() : 0xFFFFFFFF))
                    .withShadow(a.getBoolean(elementName + ".shadow", true))
                    .centered(a.getBoolean(elementName + ".centered", false));
            a.putLayoutElement(elementName, accessor.addChild(elementName, widget), i -> {}, i -> {});
            a.putStaticElement(elementName + ".linesCount", widget.getLines().size());
        })));
    }));
    public static final UIDefinitionManager.ElementType PUT_RENDERABLE_VLIST = UIDefinitionManager.ElementType.registerConditional("put_renderable_vertical_list", UIDefinitionManager.ElementType.createIndexable(slots -> (uiDefinition, accessorFunction, elementName, element) -> {
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, UIDefinitionManager.ElementType::parseBoolean, "forceWidth", "cyclic");
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, UIDefinitionManager.ElementType::parseNumber, "x", "y", "width", "height", "layoutSpacing");
        uiDefinition.addStatic(UIDefinition.createBeforeInit(a -> {
            RenderableVList renderableVList = new RenderableVList(a).layoutSpacing(l -> a.getInteger(elementName + ".layoutSpacing", 5)).cyclic(a.getBoolean(elementName + ".forceWidth", true)).cyclic(a.getBoolean(elementName + ".cyclic", true));
            a.putStaticElement(elementName, renderableVList);
            a.putStaticElement(elementName + ".renderables", UIAccessor.createRenderablesWrapper(a, renderableVList.renderables));
        }));
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, UIDefinitionManager.ElementType::parseNumber, "x", "y", "width", "height");
        UIDefinitionManager.parseAllElements(uiDefinition, a -> a.getElementValue(elementName + ".renderables", a, UIAccessor.class), element, s -> s);
        uiDefinition.addStatic(UIDefinition.createAfterInit(a -> a.getElementValue(elementName, null, RenderableVList.class).init(elementName, 0, 0, 225, 0)));
    }));
    public static final UIDefinitionManager.ElementType MODIFY_RENDERABLE_VLIST = UIDefinitionManager.ElementType.registerConditional("modify_renderable_vertical_list", UIDefinitionManager.ElementType.createIndexable(slots -> (uiDefinition, accessorFunction, elementName, element) -> {
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, UIDefinitionManager.ElementType::parseNumber, "index");
        UIDefinition renderableVListUIDefinition = new UIDefinition() {
            private final List<UIDefinition> definitions = new ArrayList<>();

            @Override
            public List<UIDefinition> getDefinitions() {
                return definitions;
            }

            @Override
            public void beforeInit(UIAccessor accessor) {
                if (!accessor.initialized() && accessor.getScreen() instanceof RenderableVList.Access access) {
                    accessor.putStaticElement(elementName + ".renderables", UIAccessor.createRenderablesWrapper(accessor, access.getRenderableVLists().get(accessor.getInteger(elementName + ".index", 0)).renderables));
                    UIDefinition.super.beforeInit(accessor);
                    UIDefinition.super.afterInit(accessor);
                }
            }

            @Override
            public void afterInit(UIAccessor accessor) {
            }
        };
        UIDefinitionManager.parseAllElements(renderableVListUIDefinition, a -> a.getElementValue(elementName + ".renderables", a, UIAccessor.class), element, s -> s);
        uiDefinition.addStatic(renderableVListUIDefinition);
    }));
    public static final UIDefinitionManager.ElementType DRAW_OUTLINED_STRING = UIDefinitionManager.ElementType.registerConditional("draw_outlined_string", UIDefinitionManager.ElementType.createIndexable(slots -> (uiDefinition, accessorFunction, elementName, element) -> {
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "component", (s, d) -> UIDefinitionManager.ElementType.parseComponentElement(elementName, s, d));
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, UIDefinitionManager.ElementType::parseNumber, "outline");
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, UIDefinitionManager.ElementType::parseNumber, "x", "y", "color", "outlineColor", "order");
        UIDefinitionManager.ElementType.parseTranslationElements(uiDefinition, elementName, element);
        uiDefinition.addStatic(UIDefinition.createAfterInit(a -> accessorFunction.apply(a).addRenderable(elementName, a.createModifiableRenderable(elementName, (guiGraphics, i, j, f) -> {
            a.getElement(elementName + ".component", Component.class).ifPresent((c) -> LegacyRenderUtil.drawOutlinedString(guiGraphics, Minecraft.getInstance().font, c, a.getInteger(elementName + ".x", 0), a.getInteger(elementName + ".y", 0), a.getInteger(elementName + ".color", 16777215), a.getInteger(elementName + ".outlineColor", 0xFF000000), a.getFloat(elementName + ".outline", 0.5f)));
        }))));
    }));
    public static final UIDefinitionManager.ElementType RENDER_ITEM_TOOLTIP = UIDefinitionManager.ElementType.registerConditional("render_item_tooltip", UIDefinitionManager.ElementType.createIndexable(slots -> (uiDefinition, accessorFunction, elementName, element) -> {
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "fakeItem", LegacyUIElementTypes::parseFakeItem);
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "fakeBundleItems", LegacyUIElementTypes::parseFakeContainer);
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, UIDefinitionManager.ElementType::parseNumber, "x", "y", "scale");
        uiDefinition.addStatic(UIDefinition.createAfterInit(a -> accessorFunction.apply(a).addRenderable(elementName, a.createModifiableRenderable(elementName, (guiGraphics, i, j, f) -> {
            ItemStack stack = a.getElements().containsKey(elementName + ".fakeBundleItems") ? createBundleStack(a.getElementValue(elementName + ".fakeBundleItems", emptyFakeContainer, Container.class)) : a.getElementValue(elementName + ".fakeItem", ItemStack.EMPTY, ItemStack.class);
            if (stack.isEmpty()) return;
            int x = a.getInteger(elementName + ".x", 0);
            int y = a.getInteger(elementName + ".y", 0);
            float scale = a.getElements().containsKey(elementName + ".scale") ? a.getFloat(elementName + ".scale", 1.0f) : 0.0f;
            renderTooltip(guiGraphics, Minecraft.getInstance().font, stack, x, y, scale);
        }))));
    }));
    public static final UIDefinitionManager.ElementType RENDER_ARMOR_STAND = UIDefinitionManager.ElementType.registerConditional("render_armor_stand", UIDefinitionManager.ElementType.createIndexable(slots -> (uiDefinition, accessorFunction, elementName, element) -> {
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "fakeItem", LegacyUIElementTypes::parseFakeItem);
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, UIDefinitionManager.ElementType::parseNumber, "x", "y", "width", "height", "scale");
        uiDefinition.addStatic(UIDefinition.createAfterInit(a -> accessorFunction.apply(a).addRenderable(elementName, a.createModifiableRenderable(elementName, (guiGraphics, i, j, f) -> {
            ArmorStandRenderState state = createArmorStandState(a.getElementValue(elementName + ".fakeItem", ItemStack.EMPTY, ItemStack.class));
            Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ENTITY_IN_UI);
            int x = a.getInteger(elementName + ".x", 0);
            int y = a.getInteger(elementName + ".y", 0);
            int width = a.getInteger(elementName + ".width", 0);
            int height = a.getInteger(elementName + ".height", 0);
            guiGraphics.submitEntityRenderState(state, a.getFloat(elementName + ".scale", 35.0f), new Vector3f(0.0F, 1.0F, 0.0F), new Quaternionf().rotationXYZ(0.43633232F, 0.0F, (float) Math.PI), null, x, y, x + width, y + height);
        }))));
    }));
    public static final UIDefinitionManager.ElementType RENDER_ENCHANTED_BOOK = UIDefinitionManager.ElementType.registerConditional("render_enchanted_book", UIDefinitionManager.ElementType.createIndexable(slots -> (uiDefinition, accessorFunction, elementName, element) -> {
        Bearer<BookModel> bookModel = Bearer.of(null);
        Bearer<Float> flip = Bearer.of(0f);
        Bearer<Float> oFlip = Bearer.of(0f);
        Bearer<Float> flipT = Bearer.of(0f);
        Bearer<Float> flipA = Bearer.of(0f);
        Bearer<Float> open = Bearer.of(0f);
        Bearer<Float> oOpen = Bearer.of(0f);
        RandomSource random = RandomSource.create();
        Bearer<Boolean> canOpenBook = Bearer.of(false);
        uiDefinition.addStatic(UIDefinition.createBeforeInit(a -> {
            if (!a.initialized() || bookModel.isEmpty()) {
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
        uiDefinition.addStatic(UIDefinition.createBeforeTick(a -> {
            if (canOpenBook.get()) {
                canOpenBook.set(false);
                do {
                    flipT.set(flipT.get() + (float) (random.nextInt(4) - random.nextInt(4)));
                } while (flip.get() <= flipT.get() + 1.0F && flip.get() >= flipT.get() - 1.0F);
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
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, UIDefinitionManager.ElementType::parseNumber, "x", "y", "width", "height", "scale");
        UIDefinitionManager.ElementType.parseTranslationElements(uiDefinition, elementName, element);
        uiDefinition.addStatic(UIDefinition.createAfterInit(a -> accessorFunction.apply(a).addRenderable(elementName, a.createModifiableRenderable(elementName, (guiGraphics, i, j, f) -> {
            float g = Mth.lerp(f, oOpen.get(), open.get());
            float f1 = Mth.lerp(f, oFlip.get(), flip.get());
            int x = a.getInteger(elementName + ".x", 0);
            int y = a.getInteger(elementName + ".y", 0);
            if (bookModel.isPresent())
                guiGraphics.submitBookModelRenderState(bookModel.get(), ENCHANTING_TABLE_BOOK, a.getFloat(elementName + ".scale", 40.0f), g, f1, x, y, x + a.getInteger(elementName + ".width", 38), y + a.getInteger(elementName + ".height", 31));
        }))));
    }));
    public static final UIDefinitionManager.ElementType PUT_TOGGLEABLE_TAB_SPRITES = UIDefinitionManager.ElementType.registerCodec("put_toggleable_tab_sprites", LegacyTabButton.ToggleableTabSprites.CODEC);
    public static final UIDefinitionManager.ElementType PUT_TAB_STATE_OFFSET = UIDefinitionManager.ElementType.registerCodec("put_tab_state_offset", LegacyTabButton.StateOffset.CODEC);
    public static final UIDefinitionManager.ElementType PUT_LEGACY_SLOT = UIDefinitionManager.ElementType.registerConditional("put_legacy_slot", UIDefinitionManager.ElementType.createIndexable(slots -> (uiDefinition, accessorFunction, elementName, element) -> {
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "fakeContainer", LegacyUIElementTypes::parseFakeContainer);
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "fakeItem", LegacyUIElementTypes::parseFakeItem);
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "fakeBundleItems", LegacyUIElementTypes::parseFakeContainer);
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "spriteOverride", ResourceLocation.CODEC);
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "iconSprite", ResourceLocation.CODEC);
        UIDefinitionManager.ElementType.parseElement(uiDefinition, elementName, element, "offset", DynamicUtil.VEC3_OBJECT_CODEC);
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, UIDefinitionManager.ElementType::parseBoolean, "iconCondition", "iconHolderCondition", "isVisible", "isFake", "isWarning");
        UIDefinitionManager.ElementType.parseElements(uiDefinition, elementName, element, UIDefinitionManager.ElementType::parseNumber, "x", "y", "width", "height");
        uiDefinition.addStatic(UIDefinition.createAfterInit(a -> {
            Bearer<Integer> index = Bearer.of(0);
            a.getElements().put(elementName + ".index", index);
            slots.forEach(i -> {
                Slot s = null;
                boolean isFake = a.getBoolean(elementName + ".isFake", false);

                if (isFake)
                    s = new Slot(a.getElementValue(elementName + ".fakeContainer", emptyFakeContainer, Container.class), a.getElements().containsKey(elementName + ".fakeContainer") ? index.get() : 0, 0, 0);
                else if (a.getScreen() instanceof LegacyMenuAccess<?> access && access.getMenu().slots.size() > i && !access.getMenu().slots.isEmpty()) {
                    s = access.getMenu().slots.get(i);
                }
                if (s == null) return;
                LegacySlotDisplay.override(s, a.getInteger(elementName + ".x", s.x), a.getInteger(elementName + ".y", s.y), new LegacySlotDisplay() {
                    @Override
                    public ItemStack getItemOverride() {
                        if (a.getElements().containsKey(elementName + ".fakeBundleItems"))
                            return createBundleStack(a.getElementValue(elementName + ".fakeBundleItems", emptyFakeContainer, Container.class));
                        return a.getElementValue(elementName + ".fakeItem", LegacySlotDisplay.super.getItemOverride(), ItemStack.class);
                    }

                    @Override
                    public int getWidth() {
                        return a.getInteger(elementName + ".width", LegacySlotDisplay.super.getWidth());
                    }

                    @Override
                    public int getHeight() {
                        return a.getInteger(elementName + ".height", LegacySlotDisplay.super.getHeight());
                    }

                    @Override
                    public Vec2 getOffset() {
                        return a.getElementValue(elementName + ".offset", LegacySlotDisplay.super.getOffset(), Vec2.class);
                    }

                    @Override
                    public boolean isVisible() {
                        return a.getBoolean(elementName + ".isVisible", LegacySlotDisplay.super.isVisible());
                    }

                    @Override
                    public ResourceLocation getIconSprite() {
                        return a.getBoolean(elementName + ".iconCondition", true) ? a.getElementValue(elementName + ".iconSprite", LegacySlotDisplay.super.getIconSprite(), ResourceLocation.class) : null;
                    }

                    @Override
                    public ArbitrarySupplier<ResourceLocation> getIconHolderOverride() {
                        return a.getBoolean(elementName + ".iconHolderCondition", true) ? a.getElement(elementName + ".spriteOverride", ResourceLocation.class).or(LegacySlotDisplay.super.getIconHolderOverride()) : ArbitrarySupplier.empty();
                    }

                    @Override
                    public boolean isWarning() {
                        return a.getBoolean(elementName + ".isWarning", LegacySlotDisplay.super.isWarning());
                    }
                });
                if (isFake)
                    a.addRenderable(LegacyIconHolder.fromSlot(s));
                index.set(index.get() + 1);
            });
        }));
    }));

    private static FormattedCharSequence howToPlayLine(FormattedCharSequence line) {
        return sink -> line.accept((i, style, codePoint) -> sink.accept(i, howToPlayStyle(style), codePoint));
    }

    private static Style howToPlayStyle(Style style) {
        TextColor color = style.getColor();
        if (color == null) return style;
        int value = color.getValue() & 0x00FFFFFF;
        return style.withColor(matchesColor(value, CommonColor.WHITE, 0xFFFFFF) ? colorValue(CommonColor.TIP_TEXT) : colorValue(CommonColor.TIP_TITLE_TEXT));
    }

    private static int colorValue(CommonColor color) {
        return color.get() & 0x00FFFFFF;
    }

    private static boolean matchesColor(int value, CommonColor color, int fallback) {
        return value == (color.get() & 0x00FFFFFF) || value == fallback;
    }

    private static UIDefinitionManager.ElementType replaceElementType(String path, UIDefinitionManager.ElementType type) {
        UIDefinitionManager.ElementType.map.remove(FactoryAPI.createVanillaLocation(path));
        return UIDefinitionManager.ElementType.registerConditional(path, type);
    }

    private static UIDefinition parseFakeContainer(String field, Dynamic<?> dynamic) {
        return dynamic.asListOpt(d -> d).result().map(items -> UIDefinition.createBeforeInit(a -> {
            ItemStack[] stacks = items.stream().map(LegacyUIElementTypes::parseStack).toArray(ItemStack[]::new);
            a.putStaticElement(field, new SimpleContainer(stacks));
        })).orElse(null);
    }

    private static UIDefinition parseFakeItem(String field, Dynamic<?> dynamic) {
        return UIDefinition.createBeforeInit(a -> a.putStaticElement(field, parseStack(dynamic)));
    }

    private static ItemStack parseStack(Dynamic<?> dynamic) {
        ItemStack stack = DynamicUtil.getItemFromDynamic(dynamic, true).get();
        applyItemComponents(stack);
        setFakeTrim(stack, dynamic);
        return stack;
    }

    private static void setFakeTrim(ItemStack stack, Dynamic<?> dynamic) {
        if (!stack.is(Items.DIAMOND_CHESTPLATE) || dynamic.get("components").result().isEmpty()) return;
        stack.set(DataComponents.EQUIPPABLE, DIAMOND_CHESTPLATE_EQUIPPABLE);
        stack.set(DataComponents.TRIM, EMERALD_SENTRY_TRIM);
    }

    private static ItemStack createBundleStack(Container items) {
        ItemStack stack = new ItemStack(Items.BUNDLE);
        List<ItemStack> contents = new ArrayList<>();
        for (int i = 0; i < items.getContainerSize(); i++) {
            ItemStack item = items.getItem(i);
            if (!item.isEmpty()) contents.add(item.copy());
        }
        stack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(contents));
        return stack;
    }

    private static void renderTooltip(GuiGraphics guiGraphics, Font font, ItemStack stack, int x, int y, float scale) {
        List<ClientTooltipComponent> components = new ArrayList<>();
        Screen.getTooltipFromItem(Minecraft.getInstance(), stack).stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .forEach(components::add);
        stack.getTooltipImage().ifPresent(image -> components.add(Math.min(1, components.size()), ClientTooltipComponent.create(image)));
        ClientTooltipPositioner positioner = scale > 0.0f ? new LegacyRenderUtil.ScaledTooltipPositioner() {
            @Override
            public float scale() {
                return scale;
            }

            @Override
            public Vector2i positionTooltip(int screenWidth, int screenHeight, int mouseX, int mouseY, int tooltipWidth, int tooltipHeight) {
                return new Vector2i(x, y);
            }
        } : (screenWidth, screenHeight, mouseX, mouseY, tooltipWidth, tooltipHeight) -> new Vector2i(x, y);
        guiGraphics.renderTooltip(font, components, 0, 0, positioner, stack.get(DataComponents.TOOLTIP_STYLE));
    }

    private static ArmorStandRenderState createArmorStandState(ItemStack armor) {
        ArmorStandRenderState state = new ArmorStandRenderState();
        state.entityType = EntityType.ARMOR_STAND;
        state.boundingBoxWidth = 0.5F;
        state.boundingBoxHeight = 1.975F;
        state.eyeHeight = 1.7775F;
        state.lightCoords = 15728880;
        state.showBasePlate = false;
        state.showArms = true;
        state.xRot = 25.0F;
        state.bodyRot = 210.0F;
        state.scale = 1.0F;
        state.ageScale = 1.0F;
        state.chestEquipment = createArmorPreviewStack(armor);
        return state;
    }

    private static ItemStack createArmorPreviewStack(ItemStack stack) {
        ItemStack armor = stack.isEmpty() ? new ItemStack(Items.DIAMOND_CHESTPLATE) : stack.copy();
        applyItemComponents(armor);
        if (armor.is(Items.DIAMOND_CHESTPLATE)) {
            armor.set(DataComponents.EQUIPPABLE, DIAMOND_CHESTPLATE_EQUIPPABLE);
            if (!armor.has(DataComponents.TRIM)) armor.set(DataComponents.TRIM, EMERALD_SENTRY_TRIM);
        }
        return armor;
    }

    private static void applyItemComponents(ItemStack stack) {
        stack.getItem().components().forEach(component -> applyItemComponent(stack, component));
    }

    private static <T> void applyItemComponent(ItemStack stack, TypedDataComponent<T> component) {
        if (!stack.has(component.type())) stack.set(component);
    }

    private record KeyedHolder<T>(ResourceKey<T> key, T value) implements Holder<T> {
        @Override public boolean isBound() { return true; }
        @Override public boolean is(ResourceLocation location) { return key.location().equals(location); }
        @Override public boolean is(ResourceKey<T> key) { return this.key.equals(key); }
        @Override public boolean is(Predicate<ResourceKey<T>> predicate) { return predicate.test(key); }
        @Override public boolean is(TagKey<T> tagKey) { return false; }
        @Override public boolean is(Holder<T> holder) { return holder.is(key); }
        @Override public Stream<TagKey<T>> tags() { return Stream.empty(); }
        @Override public Either<ResourceKey<T>, T> unwrap() { return Either.left(key); }
        @Override public Optional<ResourceKey<T>> unwrapKey() { return Optional.of(key); }
        @Override public Kind kind() { return Kind.REFERENCE; }
        @Override public boolean canSerializeIn(HolderOwner<T> owner) { return true; }
    }

    public static void init() {
    }
}
