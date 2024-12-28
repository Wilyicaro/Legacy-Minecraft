package wily.legacy.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.Vec3;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.client.UIDefinition;
import wily.factoryapi.util.DynamicUtil;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.inventory.LegacySlotDisplay;

public class LegacyUIElementTypes {
    public static final UIDefinition.Manager.ElementType PUT_LEGACY_SLOT = UIDefinition.Manager.ElementType.registerConditional("put_legacy_slot", UIDefinition.Manager.ElementType.createIndexable(slots->(uiDefinition, elementName, element) -> {
        UIDefinition.Manager.ElementType.parseElement(uiDefinition, elementName, element, "spriteOverride", ResourceLocation.CODEC);
        UIDefinition.Manager.ElementType.parseElement(uiDefinition, elementName, element, "iconSprite", ResourceLocation.CODEC);
        UIDefinition.Manager.ElementType.parseElement(uiDefinition, elementName, element, "offset", DynamicUtil.VEC3_OBJECT_CODEC);
        UIDefinition.Manager.ElementType.parseElements(uiDefinition, elementName, element, (s, d) -> UIDefinition.createBeforeInit(elementName, (a) -> a.getElements().put(s, a.getBooleanFromDynamic(d))),"iconCondition", "isVisible");
        UIDefinition.Manager.ElementType.parseElements(uiDefinition, elementName, element, (s, d) -> UIDefinition.createBeforeInit(elementName, (a) -> a.getElements().put(s, a.getNumberFromDynamic(d))), "x", "y", "width", "height");
        uiDefinition.getDefinitions().add(UIDefinition.createAfterInit(elementName,a->{
            Bearer<Integer> count = Bearer.of(0);
            a.getElements().put(elementName + ".index", count);
            if (a.getScreen() instanceof LegacyMenuAccess<?> access) slots.forEach(i->{
                if (access.getMenu().slots.size() <= i || access.getMenu().slots.isEmpty()) return;
                Slot s = access.getMenu().slots.get(i);
                LegacySlotDisplay.override(s, a.getInteger(elementName + ".x", s.x), a.getInteger(elementName + ".y", s.y), new LegacySlotDisplay() {
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
                        return a.getElement(elementName+".spriteOverride",ResourceLocation.class).or(LegacySlotDisplay.super.getIconHolderOverride());
                    }
                });
                count.set(count.get() + 1);
            });
        }));
    }));

    public static void init(){
    }
}
