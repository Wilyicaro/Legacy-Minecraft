package wily.legacy.client.controller;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFWGamepadState;
import wily.legacy.client.screen.ControlTooltip;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class ComponentState {

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
    public abstract void update(GLFWGamepadState state);
    public boolean is(ControllerComponent b){
        return component == b;
    }
    public void block(){
        timePressed = Integer.MIN_VALUE;
    }

    public boolean isBlocked() {
        return timePressed == Integer.MIN_VALUE;
    }
    public boolean canToggleKeyMapping(KeyMapping mapping){
        return timePressed == 0;
    }

    public static abstract class Axis extends ComponentState{
        public float y;
        public float x;
        public static Axis createStick(ControllerComponent component, Supplier<Float> deadZoneGetter, Supplier<Integer> xConstantGetter, Supplier<Integer> yConstantGetter, BiPredicate<KeyMapping, Axis> canToggleKeyMapping, BiConsumer<Axis, GLFWGamepadState> update){
            return new Axis(component) {
                @Override
                public boolean canToggleKeyMapping(KeyMapping mapping) {
                    return canToggleKeyMapping.test(mapping,this);
                }
                @Override
                public float getDeadZone() {
                    return deadZoneGetter.get();
                }
                @Override
                public void update(GLFWGamepadState state) {
                    x = state.axes(xConstantGetter.get());
                    y = state.axes(yConstantGetter.get());
                    update(getMagnitude() > getDeadZone());
                    update.accept(this,state);
                }
            };
        }
        public static Axis createTrigger(ControllerComponent component, Supplier<Float> deadZoneGetter, Supplier<Integer> yConstantGetter){
            return new Axis(component) {
                @Override
                public float getDeadZone() {
                    return deadZoneGetter.get();
                }
                @Override
                public void update(GLFWGamepadState state) {;
                    update( (y = state.axes(yConstantGetter.get())) > getDeadZone());
                }
            };
        }
        public abstract float getDeadZone();
        public float getMagnitude(){
            return Math.max(Math.abs(y),Math.abs(x));
        }
        public float getSmoothX(){
            return   (x > getDeadZone() ? x - getDeadZone() : x < -getDeadZone() ? x + getDeadZone() : 0)  / (1 - getDeadZone());
        }
        public float getSmoothY(){
            return  (y > getDeadZone() ? y - getDeadZone() : y < -getDeadZone() ? y + getDeadZone() : 0)  / (1 - getDeadZone());
        }
        protected Axis(ControllerComponent component) {
            super(component);
        }
    }
}
