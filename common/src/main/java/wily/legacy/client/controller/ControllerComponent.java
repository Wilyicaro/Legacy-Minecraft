package wily.legacy.client.controller;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;
import wily.legacy.Legacy4J;
import wily.legacy.util.ScreenUtil;

import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

import static wily.legacy.client.controller.ControllerHandler.DEFAULT_CONTROLLER_BUTTONS_BY_KEY;


public enum ControllerComponent {
    DOWN_BUTTON(getButtonInput(GLFW.GLFW_GAMEPAD_BUTTON_A,GLFW.GLFW_GAMEPAD_BUTTON_B),buttonIcon(new char[]{'\uE736','\uE002'},'\uE735','\uE001'), InputConstants.KEY_SPACE),
    RIGHT_BUTTON(getButtonInput(GLFW.GLFW_GAMEPAD_BUTTON_B,GLFW.GLFW_GAMEPAD_BUTTON_A),buttonIcon(new char[]{'\uE735','\uE001'},'\uE736','\uE002'), InputConstants.KEY_Q),
    LEFT_BUTTON(getButtonInput(GLFW.GLFW_GAMEPAD_BUTTON_X,GLFW.GLFW_GAMEPAD_BUTTON_Y),buttonIcon(new char[]{'\uE738','\uE004'},'\uE737','\uE003'), InputConstants.KEY_E),
    UP_BUTTON(getButtonInput(GLFW.GLFW_GAMEPAD_BUTTON_Y,GLFW.GLFW_GAMEPAD_BUTTON_X),buttonIcon(new char[]{'\uE737','\uE003'},'\uE738','\uE004'), InputConstants.KEY_I),
    BACK(GLFW.GLFW_GAMEPAD_BUTTON_BACK,icon('\uE73E','\uE009'), InputConstants.KEY_H),
    GUIDE(GLFW.GLFW_GAMEPAD_BUTTON_GUIDE,icon('\uE745')),
    START(GLFW.GLFW_GAMEPAD_BUTTON_START,icon('\uE73D','\uE008'), InputConstants.KEY_ESCAPE),
    LEFT_STICK(c-> ComponentState.Axis.createStick(c,()->0.25f,()->GLFW.GLFW_GAMEPAD_AXIS_LEFT_X,()->GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y, ControllerComponent::matchesLeftStick,(a, s)->{}),icon('\uE746','\uE748','\uE747'),InputConstants.KEY_W, InputConstants.KEY_A, InputConstants.KEY_S, InputConstants.KEY_D),
    RIGHT_STICK(c-> ComponentState.Axis.createStick(c,()->0.2f,()->GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X,()->GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y,(k, s)->s.timePressed == 0, ControllerComponent::updatePlayerCamera),icon('\uE749','\uE74E','\uE74F')),
    LEFT_STICK_BUTTON(GLFW.GLFW_GAMEPAD_BUTTON_LEFT_THUMB,icon('\uE743','\u000F'), InputConstants.KEY_F5),
    RIGHT_STICK_BUTTON(GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_THUMB,icon('\uE744','\u0010'), InputConstants.KEY_LSHIFT),
    LEFT_BUMPER(GLFW.GLFW_GAMEPAD_BUTTON_LEFT_BUMPER,icon('\uE739','\uE74A'), InputConstants.KEY_PAGEDOWN),
    RIGHT_BUMPER(GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER,icon('\uE73A','\uE74B'), InputConstants.KEY_PAGEUP),
    LEFT_TRIGGER(c-> ComponentState.Axis.createTrigger(c,()->0.2f,()->GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER),icon('\uE73B','\uE74C'), InputConstants.MOUSE_BUTTON_RIGHT),
    RIGHT_TRIGGER(c-> ComponentState.Axis.createTrigger(c,()->0.2f,()->GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER),icon('\uE73C','\uE74D'), InputConstants.MOUSE_BUTTON_LEFT),
    DPAD_UP(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_UP,icon('\uE742','\uE00F'), InputConstants.KEY_UP),
    DPAD_DOWN(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_DOWN,icon('\uE73F','\uE00C'), InputConstants.KEY_DOWN),
    DPAD_LEFT(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_LEFT,icon('\uE741','\uE00E'), InputConstants.KEY_LEFT),
    DPAD_RIGHT(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT,icon('\uE740','\uE00D'), InputConstants.KEY_RIGHT);


    public final Component displayName = Component.translatable(Legacy4J.MOD_ID + ".controller_component." + name().toLowerCase(Locale.ENGLISH));
    public final ComponentState componentState;
    public final Icon icon;

    <T extends ComponentState> ControllerComponent(Function<ControllerComponent,T> stateConstructor, Icon icon, int... defaultCorrespondentKeys){
        this.componentState = stateConstructor.apply(this);
        this.icon = icon;
        for (int k : defaultCorrespondentKeys) DEFAULT_CONTROLLER_BUTTONS_BY_KEY.put(k,this);
    }
    ControllerComponent(Supplier<Integer> constant, Icon icon, int... defaultCorrespondentKeys){
        this(c->new ComponentState(c){

            @Override
            public void update(GLFWGamepadState state) {
                update(state.buttons(constant.get()) == GLFW.GLFW_PRESS);
            }
        },icon,defaultCorrespondentKeys);
    }
    ControllerComponent(int constant, Icon icon, int... defaultCorrespondentKeys){
        this(()->constant, icon,defaultCorrespondentKeys);
    }

    public boolean matches(KeyMapping mapping){
        return ((LegacyKeyMapping)mapping).getComponent() == this;
    }

    public static boolean matchesLeftStick(KeyMapping mapping, ComponentState.Axis s){
        Minecraft minecraft = Minecraft.getInstance();
        return (minecraft.options.keyUp == mapping && s.y < 0 && -s.y > Math.abs(s.x)) || (minecraft.options.keyDown == mapping && s.y > 0 && s.y > Math.abs(s.x)) || (minecraft.options.keyRight == mapping && s.x > 0 && s.x > Math.abs(s.y)) || (minecraft.options.keyLeft == mapping && s.x < 0 && -s.x > Math.abs(s.y));
    }
    public static void updatePlayerCamera(ComponentState.Axis stick, GLFWGamepadState state){
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.mouseHandler.isMouseGrabbed() || !minecraft.isWindowActive() || !stick.pressed || minecraft.player == null) return;
        double f = Math.pow(minecraft.options.sensitivity().get() * (double)0.6f + (double)0.2f,3) * 14 * (minecraft.player.isScoping() ? 0.125: 1.0);
        minecraft.player.turn(stick.getSmoothX() * f,stick.getSmoothY() * f * (ScreenUtil.getLegacyOptions().invertYController().get() ? -1 : 1));
    }
    public static void init() {;
    }
    public interface Icon{
        char[] getChars();
    }
    public static Icon icon(char... chars){
        return ()-> chars;
    }
    public static Icon icon(Supplier<Boolean> alternative,char[] alternativeChars, char... chars){
        return ()-> alternative.get() ? alternativeChars : chars;
    }
    public static Icon buttonIcon(char[] alternativeChars, char... chars){
        return icon(()->ScreenUtil.getLegacyOptions().invertControllerButtons().get(),alternativeChars,chars);
    }
    public static Supplier<Integer> getButtonInput(int input, int alternativeInput){
        return ()->ScreenUtil.getLegacyOptions().invertControllerButtons().get() ? alternativeInput : input;
    }
}
