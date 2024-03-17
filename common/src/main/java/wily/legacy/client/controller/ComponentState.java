package wily.legacy.client.controller;

import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;
import wily.legacy.client.screen.ControlTooltip;

public class ComponentState {

    public final ControllerComponent component;
    public boolean justPressed = false;
    public int timePressed = -1;
    public boolean pressed;
    public boolean released;

    protected ComponentState(ControllerComponent component){
        this.component = component;
    }
    public void update(boolean pressed){
        if (this.released = (!pressed && this.pressed)) timePressed = -1;
        if (pressed && timePressed > Integer.MIN_VALUE) timePressed++;
        this.justPressed = pressed && !this.pressed;
        this.pressed = pressed;
    }
    public char getChar(){
        return component.icon.getChars()[0];
    }
    public char getPressedChar(){
        return component.icon.getChars().length > 1 ? component.icon.getChars().length == 2 ? component.icon.getChars()[1] : component.icon.getChars()[1 + (Math.min(4, timePressed / (getDefaultDelay() * 2)) % component.icon.getChars().length - 1)] : getChar();
    }
    public Component getIcon(boolean allowPressed){
        return ControlTooltip.getControlIcon(String.valueOf(allowPressed && component.componentState.pressed ? getPressedChar() : getChar()),ControlTooltip.getActiveControllerType());
    }
    public boolean canClick(){
        return canClick(getDefaultDelay());
    }
    public boolean canClick(int delay){
        return (timePressed == 0 || timePressed >= 5 * delay) && timePressed % delay == 0;
    }
    public boolean onceClick(int timeDelay){
        int lastTimePressed = timePressed;
        if (timePressed == 0) timePressed = timeDelay;
        return lastTimePressed == 0;
    }
    public int getDefaultDelay(){
        return 100;
    }
    public boolean onceClick(boolean block){
        return onceClick(block ? Integer.MIN_VALUE : -(justPressed ? 5 : 1) * getDefaultDelay());
    }
    public void update(GLFWGamepadState state){
        component.updateState.accept(this,state);
    }
    public boolean is(ControllerComponent b){
        return component == b;
    }
    public void block(){
        timePressed = Integer.MIN_VALUE;
    }

    public boolean isBlocked() {
        return timePressed == Integer.MIN_VALUE;
    }

    public static class Stick extends ComponentState{
        public float y;
        public float x;
        public void update(GLFWGamepadState state){
            y = state.axes(component == ControllerComponent.LEFT_STICK ? GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y :  GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y);
            x = state.axes(component == ControllerComponent.LEFT_STICK ? GLFW.GLFW_GAMEPAD_AXIS_LEFT_X :  GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X);
            super.update(state);
        }

        public float getMagnitude(){
            return Math.max(Math.abs(y),Math.abs(x));
        }
        protected Stick(ControllerComponent component) {
            super(component);
        }
    }
}
