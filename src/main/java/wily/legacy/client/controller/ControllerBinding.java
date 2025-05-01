package wily.legacy.client.controller;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.util.ListMap;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.ControlTooltip;

import java.util.*;
import java.util.function.Function;


public class ControllerBinding<T extends BindingState> {
    public static final Map<String,ControllerBinding<?>> map = new Object2ObjectLinkedOpenHashMap<>();
    public static final Codec<ControllerBinding<?>> CODEC = Codec.STRING.xmap(ControllerBinding::getOrCreate, ControllerBinding::getKey);
    public static final Codec<Optional<ControllerBinding<?>>> OPTIONAL_CODEC = Codec.STRING.xmap(s->s.equals("none") ? Optional.empty() : Optional.ofNullable(getOrCreate(s)), b-> b.map(ControllerBinding::getKey).orElse("none"));
    private static final Map<ControllerBinding<?>, Function<Options, List<KeyMapping>>> defaultKeyMappingByBinding = new HashMap<>();
    public final Function<ControllerBinding<T>, T> stateConstructor;
    public final T bindingState;
    public final boolean isBindable;
    private final String key;

    public ControllerBinding(String key, Function<ControllerBinding<T>,T> stateConstructor, boolean isBindable){
        this.key = key;
        this.stateConstructor = stateConstructor;
        this.bindingState = stateConstructor.apply(this);
        this.isBindable = isBindable;
    }

    public T state(){
        return bindingState;
    }

    public ControllerBinding(String key, Function<ControllerBinding<T>,T> stateConstructor){
        this(key, stateConstructor, true);
    }

    public String getKey(){
        return key;
    }

    public ControllerBinding<T> getMapped(){
        return this;
    }

    public ControlTooltip.ComponentIcon getIcon(){
        return ControlType.getActiveControllerType().getIcons().get(getMapped().getKey());
    }

    public boolean isSpecial(){
        return false;
    }

    public static ControllerBinding<BindingState.Button> createButton(String key, ArbitrarySupplier<Button> button){
        return new ControllerBinding<>(key, c-> new BindingState.Button(c, button)){
            @Override
            public ControllerBinding<BindingState.Button> getMapped() {
                return button.get().binding;
            }
        };
    }

    public static ControllerBinding<BindingState.Button> createButton(String key, Button button, boolean isBindable){
        return new ControllerBinding<>(key, c-> new BindingState.Button(c, ()->button), isBindable);
    }

    public static ControllerBinding<BindingState.Button> createButton(String key, Button button){
        return createButton(key, button, true);
    }

    public static <B extends ControllerBinding<?>> B register(B binding){
        map.put(binding.getKey(), binding);
        return binding;
    }

    private static <B extends ControllerBinding<?>> B registerWithDefaults(B binding, Function<Options, List<KeyMapping>> defaultKeyMappings){
        defaultKeyMappingByBinding.put(binding, defaultKeyMappings);
        return register(binding);
    }

    public static ControllerBinding<?> getOrCreate(String key){
        String[] keys = key.split(",");
        return keys.length == 1 ? map.get(key) : register(CompoundControllerBinding.getOrCreate(Arrays.stream(keys).map(map::get).toArray(ControllerBinding[]::new)));
    }

    public static final ControllerBinding<BindingState.Button> DOWN_BUTTON = registerWithDefaults(createButton("down_button", () -> LegacyOptions.invertControllerButtons.get() ? Button.RIGHT : Button.DOWN), o -> List.of(o.keyJump));
    public static final ControllerBinding<BindingState.Button> RIGHT_BUTTON = registerWithDefaults(createButton("right_button",  () -> LegacyOptions.invertControllerButtons.get() ? Button.DOWN : Button.RIGHT), o -> List.of(o.keyDrop));
    public static final ControllerBinding<BindingState.Button> LEFT_BUTTON = registerWithDefaults(createButton("left_button", Button.LEFT), o -> List.of(Legacy4JClient.keyCrafting));
    public static final ControllerBinding<BindingState.Button> UP_BUTTON = registerWithDefaults(createButton("up_button", Button.UP), o -> List.of(o.keyInventory));
    public static final ControllerBinding<BindingState.Button> BACK = registerWithDefaults(createButton("back", Button.BACK), o -> List.of(Legacy4JClient.keyHostOptions));
    public static final ControllerBinding<BindingState.Button> GUIDE = register(createButton("guide", Button.GUIDE));
    public static final ControllerBinding<BindingState.Button> START = register(createButton("start", Button.START));
    public static final ControllerBinding<BindingState.Axis> LEFT_STICK = register(new ControllerBinding<>("left_stick", c-> BindingState.Axis.createStick(c, LegacyOptions::getLeftStickDeadZone, (a, s)->{}, true), false));
    public static final ControllerBinding<BindingState.Axis> RIGHT_STICK = register(new ControllerBinding<>("right_stick", c-> BindingState.Axis.createStick(c, ()->LegacyOptions.rightStickDeadZone.get().floatValue(), ControllerManager::updatePlayerCamera, false), false));
    public static final ControllerBinding<BindingState.Button> LEFT_STICK_BUTTON = registerWithDefaults(createButton("left_stick_button", Button.LEFT_STICK), o -> List.of(o.keyTogglePerspective));
    public static final ControllerBinding<BindingState.Button> RIGHT_STICK_BUTTON = registerWithDefaults(createButton("right_stick_button", Button.RIGHT_STICK), o -> List.of(o.keyShift));
    public static final ControllerBinding<BindingState.Button> LEFT_BUMPER = registerWithDefaults(createButton("left_bumper", Button.LEFT_BUMPER), o -> List.of(Legacy4JClient.keyCycleHeldLeft));
    public static final ControllerBinding<BindingState.Button> RIGHT_BUMPER = registerWithDefaults(createButton("right_bumper", Button.RIGHT_BUMPER), o -> List.of(Legacy4JClient.keyCycleHeldRight));
    public static final ControllerBinding<BindingState.Axis> LEFT_TRIGGER = registerWithDefaults(new ControllerBinding<>("left_trigger", c-> BindingState.Axis.createTrigger(c, ()->LegacyOptions.leftTriggerDeadZone.get().floatValue(), true)), o-> List.of(o.keyUse));
    public static final ControllerBinding<BindingState.Axis> RIGHT_TRIGGER = registerWithDefaults(new ControllerBinding<>("right_trigger", c-> BindingState.Axis.createTrigger(c, ()->LegacyOptions.rightTriggerDeadZone.get().floatValue(), false)), o-> List.of(o.keyAttack));
    public static final ControllerBinding<BindingState.Button> DPAD_UP = registerWithDefaults(createButton("dpad_up", Button.DPAD_UP), o -> List.of(Legacy4JClient.keyFlyUp));
    public static final ControllerBinding<BindingState.Button> DPAD_DOWN = registerWithDefaults(createButton("dpad_down", Button.DPAD_DOWN), o -> List.of(Legacy4JClient.keyFlyDown));
    public static final ControllerBinding<BindingState.Button> DPAD_LEFT = registerWithDefaults(createButton("dpad_left", Button.DPAD_LEFT), o -> List.of(Legacy4JClient.keyFlyLeft));
    public static final ControllerBinding<BindingState.Button> DPAD_RIGHT = registerWithDefaults(createButton("dpad_right", Button.DPAD_RIGHT), o -> List.of(Legacy4JClient.keyFlyRight));
    public static final ControllerBinding<BindingState.Button> TOUCHPAD_BUTTON = registerWithDefaults(createButton("touchpad_button", Button.TOUCHPAD), o -> List.of(o.keyChat));
    public static final ControllerBinding<BindingState.Button> CAPTURE = registerWithDefaults(createButton("capture", Button.CAPTURE), o -> List.of(o.keyScreenshot));
    public static final ControllerBinding<BindingState.Button> LSL_BUTTON = register(createButton("lsl_button", Button.LSL));
    public static final ControllerBinding<BindingState.Button> LSR_BUTTON = register(createButton("lsr_button", Button.LSR));
    public static final ControllerBinding<BindingState.Button> RSL_BUTTON = register(createButton("rsl_button", Button.RSL));
    public static final ControllerBinding<BindingState.Button> RSR_BUTTON = register(createButton("rsr_button", Button.RSR));
    public static final ControllerBinding<BindingState> LEFT_STICK_UP = registerWithDefaults(new ControllerBinding<>("left_stick_up", c-> BindingState.create(c, h-> LEFT_STICK.state().y <= -LEFT_STICK.state().getDeadZone())), o-> List.of(o.keyUp));
    public static final ControllerBinding<BindingState> LEFT_STICK_DOWN = registerWithDefaults(new ControllerBinding<>("left_stick_down", c-> BindingState.create(c, h-> LEFT_STICK.state().y >= LEFT_STICK.state().getDeadZone())), o-> List.of(o.keyDown));
    public static final ControllerBinding<BindingState> LEFT_STICK_RIGHT = registerWithDefaults(new ControllerBinding<>("left_stick_right",c-> BindingState.create(c, h-> LEFT_STICK.state().x >= LEFT_STICK.state().getDeadZone())), o-> List.of(o.keyRight));
    public static final ControllerBinding<BindingState> LEFT_STICK_LEFT = registerWithDefaults(new ControllerBinding<>("left_stick_left",c-> BindingState.create(c, h->  LEFT_STICK.state().x <= -LEFT_STICK.state().getDeadZone())), o-> List.of(o.keyLeft));
    public static final ControllerBinding<BindingState> RIGHT_STICK_UP = register(new ControllerBinding<>("right_stick_up", c-> BindingState.create(c, h-> RIGHT_STICK.state().y <= -RIGHT_STICK.state().getDeadZone())));
    public static final ControllerBinding<BindingState> RIGHT_STICK_DOWN = register(new ControllerBinding<>("right_stick_down", c-> BindingState.create(c, h-> RIGHT_STICK.state().y >= RIGHT_STICK.state().getDeadZone())));
    public static final ControllerBinding<BindingState> RIGHT_STICK_RIGHT = register(new ControllerBinding<>("right_stick_right",c-> BindingState.create(c, h-> RIGHT_STICK.state().x >= RIGHT_STICK.state().getDeadZone())));
    public static final ControllerBinding<BindingState> RIGHT_STICK_LEFT = register(new ControllerBinding<>("right_stick_left",c-> BindingState.create(c, h->  RIGHT_STICK.state().x <= -RIGHT_STICK.state().getDeadZone())));


    public static void setupDefaultBindings(Minecraft minecraft){
        defaultKeyMappingByBinding.forEach(((binding, optionsListFunction) -> {
            for (KeyMapping keyMapping : optionsListFunction.apply(minecraft.options)) {
                LegacyKeyMapping.of(keyMapping).setDefaultBinding(binding);
                LegacyKeyMapping.of(keyMapping).setBinding(binding);
            }
        }));
    }

    public enum Button {
        DOWN(DOWN_BUTTON),RIGHT(RIGHT_BUTTON),LEFT(LEFT_BUTTON),UP(UP_BUTTON),BACK(ControllerBinding.BACK),GUIDE(ControllerBinding.GUIDE),START(ControllerBinding.START),LEFT_STICK(LEFT_STICK_BUTTON),RIGHT_STICK(RIGHT_STICK_BUTTON),LEFT_BUMPER(ControllerBinding.LEFT_BUMPER),RIGHT_BUMPER(ControllerBinding.RIGHT_BUMPER),DPAD_UP(ControllerBinding.DPAD_UP),DPAD_DOWN(ControllerBinding.DPAD_DOWN),DPAD_LEFT(ControllerBinding.DPAD_LEFT),DPAD_RIGHT(ControllerBinding.DPAD_RIGHT),TOUCHPAD(TOUCHPAD_BUTTON),CAPTURE(ControllerBinding.CAPTURE),LSL(LSL_BUTTON),LSR(LSR_BUTTON),RSL(RSL_BUTTON),RSR(RSR_BUTTON);
        public final ControllerBinding<BindingState.Button> binding;

        Button(ControllerBinding<BindingState.Button> binding){
            this.binding = binding;
        }
    }

    public enum Axis {
        LEFT_STICK_X,LEFT_STICK_Y,RIGHT_STICK_X,RIGHT_STICK_Y,LEFT_TRIGGER,RIGHT_TRIGGER
    }

}