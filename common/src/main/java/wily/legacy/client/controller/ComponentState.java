package wily.legacy.client.controller;

import com.studiohartman.jamepad.ControllerState;
import net.minecraft.client.Minecraft;

public class ComponentState {

    public final ControllerComponent component;
    public boolean justPressed = false;
    public int timePressed = -1;
    public boolean pressed;
    public boolean released;

    protected ComponentState(ControllerComponent component){
        this.component = component;
    }
    public void update(boolean pressed, boolean justPressed){
        if (this.released = (!pressed && this.pressed)) timePressed = -1;
        if (pressed) timePressed++;
        this.pressed = pressed;
        this.justPressed = justPressed;
    }
    public boolean canPress(){
        return timePressed % Math.max(1,Minecraft.getInstance().getFps() / 5) == 0;
    }
    public void update(ControllerState state){
        component.updateState.accept(this,state);
    }
    public boolean is(ControllerComponent b){
        return component == b;
    }
    public static class Stick extends ComponentState{
        public float y;
        public float x;
        public void update(ControllerState state){
            super.update(state);
            y = component == ControllerComponent.LEFT_STICK ? state.leftStickY :  state.rightStickY;
            x = component == ControllerComponent.LEFT_STICK ? state.leftStickX :  state.rightStickX;
        }

        protected Stick(ControllerComponent component) {
            super(component);
        }
    }
}
