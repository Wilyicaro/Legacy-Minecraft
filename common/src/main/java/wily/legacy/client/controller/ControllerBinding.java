package wily.legacy.client.controller;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import wily.legacy.Legacy4J;
import wily.legacy.util.ScreenUtil;

import java.util.Locale;
import java.util.function.Function;

import static wily.legacy.client.controller.ControllerManager.DEFAULT_CONTROLLER_BUTTONS_BY_KEY;


public enum ControllerBinding {
    DOWN_BUTTON(icon('\uE735','\uE001'), InputConstants.KEY_SPACE),
    RIGHT_BUTTON(icon('\uE736','\uE002'), InputConstants.KEY_Q),
    LEFT_BUTTON(icon('\uE737','\uE003'), InputConstants.KEY_E),
    UP_BUTTON(icon('\uE738','\uE004'), InputConstants.KEY_I),
    BACK(icon('\uE73E','\uE009'), InputConstants.KEY_H),
    GUIDE(icon('\uE745')),
    START(icon('\uE73D','\uE008'), InputConstants.KEY_ESCAPE),
    LEFT_STICK(c-> BindingState.Axis.createStick(c,()->ScreenUtil.getLegacyOptions().leftStickDeadZone().get().floatValue(),(a, s)->{}),icon('\uE746','\uE748','\uE747'),false),
    RIGHT_STICK(c-> BindingState.Axis.createStick(c,()->ScreenUtil.getLegacyOptions().rightStickDeadZone().get().floatValue(),ControllerBinding::updatePlayerCamera),icon('\uE749','\uE74E','\uE74F'),false),
    LEFT_STICK_BUTTON(icon('\uE743','\u000F'), InputConstants.KEY_F5),
    RIGHT_STICK_BUTTON(icon('\uE744','\u0010'), InputConstants.KEY_LSHIFT),
    LEFT_BUMPER(icon('\uE739','\uE74A'), InputConstants.KEY_PAGEDOWN),
    RIGHT_BUMPER(icon('\uE73A','\uE74B'), InputConstants.KEY_PAGEUP),
    LEFT_TRIGGER(c-> BindingState.Axis.createTrigger(c,()->ScreenUtil.getLegacyOptions().leftTriggerDeadZone().get().floatValue()),icon('\uE73B','\uE74C'), InputConstants.MOUSE_BUTTON_RIGHT),
    RIGHT_TRIGGER(c-> BindingState.Axis.createTrigger(c,()->ScreenUtil.getLegacyOptions().rightTriggerDeadZone().get().floatValue()),icon('\uE73C','\uE74D'), InputConstants.MOUSE_BUTTON_LEFT),
    DPAD_UP(icon('\uE742','\uE00F'), InputConstants.KEY_UP),
    DPAD_DOWN(icon('\uE73F','\uE00C'), InputConstants.KEY_DOWN),
    DPAD_LEFT(icon('\uE741','\uE00E'), InputConstants.KEY_LEFT),
    DPAD_RIGHT(icon('\uE740','\uE00D'), InputConstants.KEY_RIGHT),
    LEFT_STICK_UP(c-> BindingState.create(c, h-> LEFT_STICK.bindingState instanceof BindingState.Axis a && a.y <= -a.getDeadZone()),icon('\uE746','\uE748','\uE747'),InputConstants.KEY_W),
    LEFT_STICK_DOWN(c-> BindingState.create(c, h-> LEFT_STICK.bindingState instanceof BindingState.Axis a && a.y >= a.getDeadZone()),icon('\uE746','\uE748','\uE747'),InputConstants.KEY_S),
    LEFT_STICK_RIGHT(c-> BindingState.create(c, h-> LEFT_STICK.bindingState instanceof BindingState.Axis a && a.x >= a.getDeadZone()),icon('\uE746','\uE748','\uE747'),InputConstants.KEY_D),
    LEFT_STICK_LEFT(c-> BindingState.create(c, h-> LEFT_STICK.bindingState instanceof BindingState.Axis a && a.x <= -a.getDeadZone()),icon('\uE746','\uE748','\uE747'),InputConstants.KEY_A),
    RIGHT_STICK_UP(c-> BindingState.create(c, h-> RIGHT_STICK.bindingState instanceof BindingState.Axis a && a.y <= -a.getDeadZone()),icon('\uE749','\uE74E','\uE74F')),
    RIGHT_STICK_DOWN(c-> BindingState.create(c, h-> RIGHT_STICK.bindingState instanceof BindingState.Axis a && a.y >= a.getDeadZone()),icon('\uE749','\uE74E','\uE74F')),
    RIGHT_STICK_RIGHT(c-> BindingState.create(c, h-> RIGHT_STICK.bindingState instanceof BindingState.Axis a && a.x >= a.getDeadZone()),icon('\uE749','\uE74E','\uE74F')),
    RIGHT_STICK_LEFT(c-> BindingState.create(c, h-> RIGHT_STICK.bindingState instanceof BindingState.Axis a && a.x <= -a.getDeadZone()),icon('\uE749','\uE74E','\uE74F'));


    public final Component displayName = Component.translatable(Legacy4J.MOD_ID + ".controller_component." + name().toLowerCase(Locale.ENGLISH));
    public final BindingState bindingState;
    public final boolean isBindable;
    private final Icon icon;

    <T extends BindingState> ControllerBinding(Function<ControllerBinding,T> stateConstructor, Icon icon, boolean isBindable, int... defaultCorrespondentKeys){
        this.bindingState = stateConstructor.apply(this);
        this.icon = icon;
        this.isBindable = isBindable;
        for (int k : defaultCorrespondentKeys) DEFAULT_CONTROLLER_BUTTONS_BY_KEY.put(k,this);
    }
    <T extends BindingState> ControllerBinding(Function<ControllerBinding,T> stateConstructor, Icon icon, int... defaultCorrespondentKeys){
        this(stateConstructor,icon,true,defaultCorrespondentKeys);
    }
    ControllerBinding(Icon icon, int... defaultCorrespondentKeys){
        this(c-> BindingState.create(c, h-> h.buttonPressed(ControllerManager.getHandler().getBindingIndex(c.getMapped()))),icon,defaultCorrespondentKeys);
    }

    public boolean matches(KeyMapping mapping){
        return ((LegacyKeyMapping)mapping).getBinding() == this;
    }

    public Icon getIcon(){
        return getMappedBinding(this).icon;
    }

    public static void updatePlayerCamera(BindingState.Axis stick, Controller handler){
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.mouseHandler.isMouseGrabbed() || !minecraft.isWindowActive() || !stick.pressed || minecraft.player == null) return;
        double f = Math.pow(minecraft.options.sensitivity().get() * (double)0.6f + (double)0.2f,3) * 14 * (minecraft.player.isScoping() ? 0.125: 1.0);
        minecraft.player.turn(stick.getSmoothX() * f,stick.getSmoothY() * f * (ScreenUtil.getLegacyOptions().invertYController().get() ? -1 : 1));
    }
    public static void init() {
    }
    public interface Icon{
        char[] getChars();
    }
    public static Icon icon(char... chars){
        return ()-> chars;
    }
    public ControllerBinding getMapped(){
        return getMappedBinding(this);
    }
    public static ControllerBinding getMappedBinding(ControllerBinding component){
        boolean invert = ScreenUtil.getLegacyOptions().invertControllerButtons().get();
        return switch (component){
            case DOWN_BUTTON -> invert ? RIGHT_BUTTON : component;
            case RIGHT_BUTTON -> invert ? DOWN_BUTTON : component;
            case LEFT_BUTTON -> invert ? UP_BUTTON : component;
            case UP_BUTTON -> invert ? LEFT_BUTTON : component;
            default -> component;
        };
    }
}
