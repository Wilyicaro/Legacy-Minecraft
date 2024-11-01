package wily.legacy.client.controller;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.util.StringRepresentable;
import wily.legacy.client.LegacyOption;
import wily.legacy.util.ScreenUtil;

import java.util.Locale;
import java.util.function.Function;

import static wily.legacy.client.controller.ControllerManager.DEFAULT_CONTROLLER_BUTTONS_BY_KEY;


public enum ControllerBinding implements StringRepresentable {
    DOWN_BUTTON(InputConstants.KEY_SPACE),
    RIGHT_BUTTON(InputConstants.KEY_Q),
    LEFT_BUTTON(InputConstants.KEY_E),
    UP_BUTTON(InputConstants.KEY_I),
    BACK(InputConstants.KEY_H),
    GUIDE,
    START(InputConstants.KEY_ESCAPE),
    LEFT_STICK(c-> BindingState.Axis.createStick(c,()->LegacyOption.leftStickDeadZone.get().floatValue(),(a, s)->{}),false),
    RIGHT_STICK(c-> BindingState.Axis.createStick(c,()->LegacyOption.rightStickDeadZone.get().floatValue(), ControllerManager::updatePlayerCamera),false),
    LEFT_STICK_BUTTON(InputConstants.KEY_F5),
    RIGHT_STICK_BUTTON(InputConstants.KEY_LSHIFT),
    LEFT_BUMPER(InputConstants.KEY_PAGEDOWN),
    RIGHT_BUMPER(InputConstants.KEY_PAGEUP),
    LEFT_TRIGGER(c-> BindingState.Axis.createTrigger(c,()-> LegacyOption.leftTriggerDeadZone.get().floatValue()), InputConstants.MOUSE_BUTTON_RIGHT),
    RIGHT_TRIGGER(c-> BindingState.Axis.createTrigger(c,()->LegacyOption.rightTriggerDeadZone.get().floatValue()), InputConstants.MOUSE_BUTTON_LEFT),
    DPAD_UP(InputConstants.KEY_UP),
    DPAD_DOWN(InputConstants.KEY_DOWN),
    DPAD_LEFT(InputConstants.KEY_LEFT),
    DPAD_RIGHT(InputConstants.KEY_RIGHT),
    LEFT_STICK_UP(c-> BindingState.create(c, h-> LEFT_STICK.bindingState instanceof BindingState.Axis a && a.y <= -a.getDeadZone()),InputConstants.KEY_W),
    LEFT_STICK_DOWN(c-> BindingState.create(c, h-> LEFT_STICK.bindingState instanceof BindingState.Axis a && a.y >= a.getDeadZone()),InputConstants.KEY_S),
    LEFT_STICK_RIGHT(c-> BindingState.create(c, h-> LEFT_STICK.bindingState instanceof BindingState.Axis a && a.x >= a.getDeadZone()),InputConstants.KEY_D),
    LEFT_STICK_LEFT(c-> BindingState.create(c, h-> LEFT_STICK.bindingState instanceof BindingState.Axis a && a.x <= -a.getDeadZone()),InputConstants.KEY_A),
    RIGHT_STICK_UP(c-> BindingState.create(c, h-> RIGHT_STICK.bindingState instanceof BindingState.Axis a && a.y <= -a.getDeadZone())),
    RIGHT_STICK_DOWN(c-> BindingState.create(c, h-> RIGHT_STICK.bindingState instanceof BindingState.Axis a && a.y >= a.getDeadZone())),
    RIGHT_STICK_RIGHT(c-> BindingState.create(c, h-> RIGHT_STICK.bindingState instanceof BindingState.Axis a && a.x >= a.getDeadZone())),
    RIGHT_STICK_LEFT(c-> BindingState.create(c, h-> RIGHT_STICK.bindingState instanceof BindingState.Axis a && a.x <= -a.getDeadZone()));
    public static final EnumCodec<ControllerBinding> CODEC = StringRepresentable.fromEnum(ControllerBinding::values);

    public final BindingState bindingState;
    public final boolean isBindable;

    <T extends BindingState> ControllerBinding(Function<ControllerBinding,T> stateConstructor, boolean isBindable, int... defaultCorrespondentKeys){
        this.bindingState = stateConstructor.apply(this);
        this.isBindable = isBindable;
        for (int k : defaultCorrespondentKeys) DEFAULT_CONTROLLER_BUTTONS_BY_KEY.put(k,this);
    }
    <T extends BindingState> ControllerBinding(Function<ControllerBinding,T> stateConstructor, int... defaultCorrespondentKeys){
        this(stateConstructor,true,defaultCorrespondentKeys);
    }
    ControllerBinding(int... defaultCorrespondentKeys){
        this(c-> BindingState.create(c, h-> h.buttonPressed(ControllerManager.getHandler().getBindingIndex(c.getMapped()))),defaultCorrespondentKeys);
    }

    public static ControllerBinding getDefaultKeyMappingBinding(int i){
        return DEFAULT_CONTROLLER_BUTTONS_BY_KEY.get(i);
    }

    public ControllerBinding getMapped(){
        return getMappedBinding(this);
    }
    public static ControllerBinding getMappedBinding(ControllerBinding component){
        boolean invert = LegacyOption.invertControllerButtons.get();
        return switch (component){
            case DOWN_BUTTON -> invert ? RIGHT_BUTTON : component;
            case RIGHT_BUTTON -> invert ? DOWN_BUTTON : component;
            default -> component;
        };
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ENGLISH);
    }
}
