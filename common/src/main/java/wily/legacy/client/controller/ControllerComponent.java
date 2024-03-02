package wily.legacy.client.controller;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;
import wily.legacy.LegacyMinecraft;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static wily.legacy.client.controller.ControllerHandler.DEFAULT_CONTROLLER_BUTTONS_BY_KEY;


public enum ControllerComponent {
    DOWN_BUTTON(GLFW.GLFW_GAMEPAD_BUTTON_A, InputConstants.KEY_SPACE),
    RIGHT_BUTTON(GLFW.GLFW_GAMEPAD_BUTTON_B, InputConstants.KEY_Q),
    LEFT_BUTTON(GLFW.GLFW_GAMEPAD_BUTTON_X, InputConstants.KEY_E),
    UP_BUTTON(GLFW.GLFW_GAMEPAD_BUTTON_Y, InputConstants.KEY_I),
    BACK(GLFW.GLFW_GAMEPAD_BUTTON_BACK, InputConstants.KEY_H),
    GUIDE(GLFW.GLFW_GAMEPAD_BUTTON_GUIDE),
    START(GLFW.GLFW_GAMEPAD_BUTTON_START, InputConstants.KEY_ESCAPE),
    LEFT_STICK((comp,state) -> comp.update(comp.getMagnitude() > 0.25), (k, c)-> matchesLeftStick(Minecraft.getInstance(),k,c), ComponentState.Stick::new,InputConstants.KEY_W, InputConstants.KEY_A, InputConstants.KEY_S, InputConstants.KEY_D),
    RIGHT_STICK(ControllerComponent::updatePlayerCamera,(k, c)->true, ComponentState.Stick::new),
    LEFT_STICK_BUTTON(GLFW.GLFW_GAMEPAD_BUTTON_LEFT_THUMB, InputConstants.KEY_F5),
    RIGHT_STICK_BUTTON(GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_THUMB, InputConstants.KEY_LSHIFT),
    LEFT_BUMPER(GLFW.GLFW_GAMEPAD_BUTTON_LEFT_BUMPER),
    RIGHT_BUMPER(GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER),
    LEFT_TRIGGER((comp,state) -> comp.update(state.axes(GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER) > 0.2), InputConstants.MOUSE_BUTTON_RIGHT),
    RIGHT_TRIGGER((comp,state) -> comp.update(state.axes(GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER) > 0.2), InputConstants.MOUSE_BUTTON_LEFT),
    DPAD_UP(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_UP),
    DPAD_DOWN(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_DOWN),
    DPAD_LEFT(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_LEFT, InputConstants.MOUSE_BUTTON_MIDDLE),
    DPAD_RIGHT(GLFW.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT);


    public final Component displayName = Component.translatable(LegacyMinecraft.MOD_ID + ".controller_component." + name().toLowerCase(Locale.ENGLISH));
    public final BiConsumer<ComponentState, GLFWGamepadState> updateState;
    public final BiPredicate<KeyMapping,ComponentState> validKey;
    public final ComponentState componentState;

    <T extends ComponentState> ControllerComponent(BiConsumer<T, GLFWGamepadState> updateState, BiPredicate<KeyMapping,T> validKey, Function<ControllerComponent,T> stateConstructor, int... defaultCorrespondentKeys){
        this.updateState = (c,s)-> updateState.accept((T) c,s);
        this.validKey = (c,s)->validKey.test(c, (T) s);
        this.componentState = stateConstructor.apply(this);
        for (int k : defaultCorrespondentKeys) DEFAULT_CONTROLLER_BUTTONS_BY_KEY.put(k,this);
    }
    ControllerComponent(BiConsumer<ComponentState, GLFWGamepadState> updateState, int... defaultCorrespondentKeys){
        this.updateState = updateState;
        validKey = (k, c)-> true;
        this.componentState = new ComponentState(this);
        for (int k : defaultCorrespondentKeys) DEFAULT_CONTROLLER_BUTTONS_BY_KEY.put(k,this);
    }
    ControllerComponent(int button, int... defaultCorrespondentKeys){
        this((c,s)-> c.update(s.buttons(button) == GLFW.GLFW_PRESS),defaultCorrespondentKeys);
    }


    public boolean matches(KeyMapping mapping){
        return ((LegacyKeyMapping)mapping).getComponent() == this;
    }

    public static boolean matchesLeftStick(Minecraft minecraft, KeyMapping mapping, ComponentState.Stick stick){
        return (minecraft.options.keyUp == mapping && stick.y < 0) || (minecraft.options.keyDown == mapping && stick.y > 0) || (minecraft.options.keyRight == mapping && stick.x > 0) || (minecraft.options.keyLeft == mapping && stick.x < 0);
    }
    public static void updatePlayerCamera(ComponentState.Stick stick, GLFWGamepadState state){
        Minecraft minecraft = Minecraft.getInstance();
        stick.update(stick.getMagnitude() > 0.2);
        if (!minecraft.mouseHandler.isMouseGrabbed() || !minecraft.isWindowActive() || !stick.pressed || minecraft.player == null) return;
        double f = Math.pow(minecraft.options.sensitivity().get() * (double)0.6f + (double)0.2f,3) * 14;
        minecraft.player.turn( stick.x * f,stick.y * f * (minecraft.options.invertYMouse().get() ? -1 : 1));
    }
    public static void init() {
    }
}
