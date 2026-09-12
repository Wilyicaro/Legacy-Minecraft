package wily.legacy.client.control;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import wily.factoryapi.base.Bearer;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.base.config.FactoryConfigControl;
import wily.factoryapi.base.config.FactoryConfigDisplay;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;

import java.util.Optional;

public class LegacyControlsOptions {
    public static final FactoryConfig.StorageHandler STORAGE = new FactoryConfig.StorageHandler() {
        @Override
        public void load() {
            for (KeyMapping keyMapping : Minecraft.getInstance().options.keyMappings) {
                LegacyKeyMapping mapping = LegacyKeyMapping.of(keyMapping);
                register(FactoryConfig.create("component_" + keyMapping.getName(), null, Optional.ofNullable(((LegacyKeyMapping) keyMapping).getDefaultBinding()), Bearer.of(()->Optional.ofNullable(mapping.getBinding()), o->mapping.setBinding(o.filter(b -> b.isBindable).orElse(null))), FactoryConfigControl.of(ControllerBinding.OPTIONAL_CODEC), m -> {}, this));
            }
            super.load();
        }
    }.withFile("legacy_controls/client_options.json");

    //For now, just reusing the actual Legacy4J lang file
    public static Component optionsName(String key) {
        return Component.translatable("legacy.options." + key);
    }

    public static <T> FactoryConfigDisplay.Builder<T> optionsTooltip(String key, FactoryConfigDisplay.Builder<T> builder){
        Component tooltip = Component.translatable("legacy.options." + key + ".tooltip");
        return builder.tooltip(v -> tooltip);
    }

    public static final FactoryConfig<Boolean> unfocusedInputs = FactoryConfig.toggleBuilder().key("unfocusedInputs").displayFromKey(v -> optionsTooltip(v, FactoryConfigDisplay.toggleBuilder()).build(optionsName(v))).defaultValue(false).buildAndRegister(STORAGE);
    public static final FactoryConfig<Double> leftStickDeadZone = FactoryConfig.<Double>builder().key("leftStickDeadZone").displayFromKey(v -> FactoryConfigDisplay.percentBuilder().build(optionsName(v))).control(FactoryConfigControl.createDouble()).defaultValue(0.25).buildAndRegister(STORAGE);
    public static final FactoryConfig<Double> rightStickDeadZone = FactoryConfig.<Double>builder().key("rightStickDeadZone").displayFromKey(v -> FactoryConfigDisplay.percentBuilder().build(optionsName(v))).control(FactoryConfigControl.createDouble()).defaultValue(0.34).buildAndRegister(STORAGE);
    public static final FactoryConfig<Double> leftTriggerDeadZone = FactoryConfig.<Double>builder().key("leftTriggerDeadZone").displayFromKey(v -> FactoryConfigDisplay.percentBuilder().build(optionsName(v))).control(FactoryConfigControl.createDouble()).defaultValue(0.2).buildAndRegister(STORAGE);
    public static final FactoryConfig<Double> rightTriggerDeadZone = FactoryConfig.<Double>builder().key("rightTriggerDeadZone").displayFromKey(v -> FactoryConfigDisplay.percentBuilder().build(optionsName(v))).control(FactoryConfigControl.createDouble()).defaultValue(0.2).buildAndRegister(STORAGE);
    public static final FactoryConfig<Boolean> controllerToggleCrouch = FactoryConfig.toggleBuilder().key("controllerToggleCrouch").display(FactoryConfigDisplay.createToggle(Component.translatable("options.key.toggleSneak"))).defaultValue(false).buildAndRegister(STORAGE);
    public static final FactoryConfig<Boolean> controllerToggleSprint =  FactoryConfig.toggleBuilder().key("controllerToggleSprint").display(FactoryConfigDisplay.createToggle(Component.translatable("options.key.toggleSprint"))).defaultValue(false).buildAndRegister(STORAGE);
    public static final FactoryConfig<Boolean> controllerToggleUse = FactoryConfig.toggleBuilder().key("controllerToggleUse").display(FactoryConfigDisplay.createToggle(Component.translatable("options.key.toggleUse"))).defaultValue(false).buildAndRegister(STORAGE);
    public static final FactoryConfig<Boolean> controllerToggleAttack = FactoryConfig.toggleBuilder().key("controllerToggleAttack").display(FactoryConfigDisplay.createToggle(Component.translatable("options.key.toggleAttack"))).defaultValue(false).buildAndRegister(STORAGE);
    public static final FactoryConfig<Boolean> lockControlTypeChange = FactoryConfig.toggleBuilder().key("lockControlTypeChange").displayFromKey(v -> FactoryConfigDisplay.toggleBuilder().build(optionsName(v))).defaultValue(false).buildAndRegister(STORAGE);
    public static final FactoryConfig<Boolean> systemCursor = FactoryConfig.toggleBuilder().key("systemCursor").displayFromKey(v -> FactoryConfigDisplay.toggleBuilder().build(optionsName(v))).defaultValue(false).afterSet(b -> Legacy4JClient.controllerManager.updateCursorInputMode()).buildAndRegister(STORAGE);
    public static final FactoryConfig<Boolean> invertYController = FactoryConfig.toggleBuilder().key("invertYController").displayFromKey(v -> FactoryConfigDisplay.toggleBuilder().build(optionsName(v))).defaultValue(false).buildAndRegister(STORAGE);
    public static final FactoryConfig<Boolean> invertControllerButtons = FactoryConfig.toggleBuilder().key("invertControllerButtons").displayFromKey(v -> FactoryConfigDisplay.toggleBuilder().build(optionsName(v))).defaultValue(false).buildAndRegister(STORAGE);
    public static final FactoryConfig<Integer> controllerLedRed = FactoryConfig.<Integer>builder().key("controllerLedRed").displayFromKey(v -> FactoryConfigDisplay.intBuilder().valueToComponent(i -> Component.literal(String.valueOf(i)).withStyle(s -> s.withColor(0xFF0000 | (i << 16)))).build(optionsName(v))).control(new FactoryConfigControl.Int(0, () -> 255, 255)).defaultValue(255).buildAndRegister(STORAGE);
    public static final FactoryConfig<Integer> controllerLedGreen = FactoryConfig.<Integer>builder().key("controllerLedGreen").displayFromKey(v -> FactoryConfigDisplay.intBuilder().valueToComponent(i -> Component.literal(String.valueOf(i)).withStyle(s -> s.withColor(0x00FF00 | (i << 8)))).build(optionsName(v))).control(new FactoryConfigControl.Int(0, () -> 255, 255)).defaultValue(255).buildAndRegister(STORAGE);
    public static final FactoryConfig<Integer> controllerLedBlue = FactoryConfig.<Integer>builder().key("controllerLedBlue").displayFromKey(v -> FactoryConfigDisplay.intBuilder().valueToComponent(i -> Component.literal(String.valueOf(i)).withStyle(s -> s.withColor(0x0000FF | i))).build(optionsName(v))).control(new FactoryConfigControl.Int(0, () -> 255, 255)).defaultValue(255).buildAndRegister(STORAGE);
    public static final FactoryConfig<Integer> selectedController = FactoryConfig.<Integer>builder().key("selectedController").displayFromKey(v -> FactoryConfigDisplay.intBuilder().valueToComponent(Legacy4JClient.controllerManager::getControllerDisplayName).build(optionsName(v))).control(new FactoryConfigControl.Int(0, () -> 15, 0)).afterSet(Legacy4JClient.controllerManager::connectTo).defaultValue(0).buildAndRegister(STORAGE);
    public static final FactoryConfig<Controller.Handler> selectedControllerHandler = FactoryConfig.<Controller.Handler>builder().key("selectedControllerHandler").displayFromKey(v -> FactoryConfigDisplay.<Controller.Handler>builder().valueToComponent(Controller.Handler::getName).build(optionsName(v))).control(new FactoryConfigControl.FromInt<>(ControllerManager.handlers::getByIndex, ControllerManager.handlers::indexOf, ControllerManager.handlers::size)).defaultValue(SDLControllerHandler.getInstance()).afterSet(Legacy4JClient.controllerManager::updateHandler).buildAndRegister(STORAGE);
    public static final FactoryConfig<Integer> controllerPollingRate = FactoryConfig.<Integer>builder().key("controllerPollingRate").displayFromKey(v -> optionsTooltip(v, FactoryConfigDisplay.intBuilder()).messageFunction((display, value) -> CommonComponents.optionNameValue(display.name(), Component.literal(value + " ms"))).build(optionsName(v))).control(new FactoryConfigControl.Int(1, () -> 16, 16)).defaultValue(8).afterSet(v -> Legacy4JClient.controllerManager.restartPoller()).buildAndRegister(STORAGE);
    public static final FactoryConfig<Boolean> controllerVirtualCursor = FactoryConfig.toggleBuilder().key("controllerVirtualCursor").displayFromKey(v -> FactoryConfigDisplay.toggleBuilder().build(optionsName(v))).defaultValue(false).buildAndRegister(STORAGE);
    public static final FactoryConfig<CursorMode> cursorMode = FactoryConfig.<CursorMode>builder().key("cursorMode").displayFromKey(v -> FactoryConfigDisplay.<CursorMode>builder().valueToComponent(mode -> mode.displayName).build(optionsName(v))).control(new FactoryConfigControl.FromInt<>(CursorMode.CODEC, i -> CursorMode.values()[i], CursorMode::ordinal, ()-> CursorMode.values().length)).defaultValue(CursorMode.AUTO).afterSet(v -> Legacy4JClient.controllerManager.updateCursorMode()).buildAndRegister(STORAGE);
    public static final FactoryConfig<Double> controllerSensitivity = FactoryConfig.<Double>builder().key("controllerSensitivity").displayFromKey(v -> FactoryConfigDisplay.percentBuilder().valueToComponent(d -> Component.literal(String.valueOf((int)(d * 200)))).build(Component.translatable("options.sensitivity"))).control(FactoryConfigControl.createDouble()).defaultValue(0.5).buildAndRegister(STORAGE);
    public static final FactoryConfig<Boolean> controllerCursorAtFirstInventorySlot = FactoryConfig.toggleBuilder().key("controllerCursorAtFirstInventorySlot").displayFromKey(v -> FactoryConfigDisplay.toggleBuilder().build(optionsName("legacy.options.cursorAtFirstInventorySlot"))).defaultValue(true).buildAndRegister(STORAGE);
    public static final FactoryConfig<Boolean> controllerToasts = FactoryConfig.toggleBuilder().key("controllerToasts").displayFromKey(v -> FactoryConfigDisplay.toggleBuilder().build(optionsName(v))).defaultValue(true).buildAndRegister(STORAGE);
    public static final FactoryConfig<Boolean> controllerDoubleClick = FactoryConfig.toggleBuilder().key("controllerDoubleClick").displayFromKey(v -> FactoryConfigDisplay.toggleBuilder().build(optionsName(v))).defaultValue(false).buildAndRegister(STORAGE);

    public enum CursorMode implements StringRepresentable {
        AUTO("auto"),ALWAYS("always"),NEVER("never");
        public static final EnumCodec<CursorMode> CODEC = StringRepresentable.fromEnum(CursorMode::values);
        private final String name;
        public final Component displayName;

        CursorMode(String name, Component displayName) {
            this.name = name;
            this.displayName = displayName;
        }

        CursorMode(String name) {
            this(name, Component.translatable("legacy.options.cursorMode."+name));
        }

        public boolean isAuto() {
            return this == AUTO;
        }

        public boolean isAlways() {
            return this == ALWAYS;
        }

        public boolean isNever() {
            return this == NEVER;
        }
        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
