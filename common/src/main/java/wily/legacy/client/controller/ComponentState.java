package wily.legacy.client.controller;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;

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
