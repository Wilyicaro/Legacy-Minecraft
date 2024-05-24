package wily.legacy.client.controller;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import net.minecraft.network.chat.Component;
import wily.legacy.client.screen.ControlTooltip;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class BindingState {

    public final ControllerBinding component;
    public boolean justPressed = false;
    public int timePressed = -1;
    public boolean pressed;
    public boolean released;
    public static BindingState create(ControllerBinding component, Predicate<Controller> update){
        return new BindingState(component) {
            @Override
            public void update(Controller handler) {
                update(update.test(handler));
            }
        };
    }

    protected BindingState(ControllerBinding component){
        this.component = component;
    }
    public void update(boolean pressed){
        if (this.released = (!pressed && this.pressed)) timePressed = -1;
        if (pressed && timePressed > Integer.MIN_VALUE) timePressed++;
        this.justPressed = pressed && !this.pressed;
        this.pressed = pressed;
    }
    public char getChar(){
        return component.getIcon().getChars()[0];
    }
    public char getPressedChar(){
        return component.getIcon().getChars().length > 1 ? component.getIcon().getChars().length == 2 ? component.getIcon().getChars()[1] : component.getIcon().getChars()[1 + (Math.min(4, timePressed / (getDefaultDelay() * 2)) % component.getIcon().getChars().length - 1)] : getChar();
    }
    public Component getIcon(boolean allowPressed){
        return ControlTooltip.getControlIcon(String.valueOf(allowPressed && component.bindingState.pressed ? getPressedChar() : getChar()),ControlTooltip.getActiveControllerType());
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
    public abstract void update(Controller state);
    public boolean is(ControllerBinding b){
        return component == b;
    }
    public void block(){
        timePressed = Integer.MIN_VALUE;
    }

    public boolean isBlocked() {
        return timePressed == Integer.MIN_VALUE;
    }
    public boolean canDownKeyMapping(KeyMapping mapping){
        return( !(mapping instanceof ToggleKeyMapping) || timePressed == 0) && canClick();
    }
    public boolean canReleaseKeyMapping(KeyMapping mapping){
        return released;
    }
    

    public static abstract class Axis extends BindingState {
        public float y;
        public float x;
        public static Axis createStick(ControllerBinding component, Supplier<Float> deadZoneGetter, BiConsumer<Axis, Controller> update){
            return new Axis(component) {

                @Override
                public float getDeadZone() {
                    return deadZoneGetter.get();
                }
                @Override
                public void update(Controller controller) {
                    x = controller.axisValue(ControllerManager.getHandler().getBindingIndex(component.getMapped() == ControllerBinding.LEFT_STICK ? ControllerBinding.LEFT_STICK_RIGHT : ControllerBinding.RIGHT_STICK_RIGHT));
                    y = controller.axisValue(ControllerManager.getHandler().getBindingIndex(component.getMapped() == ControllerBinding.LEFT_STICK ? ControllerBinding.LEFT_STICK_UP : ControllerBinding.RIGHT_STICK_UP));
                    update( getMagnitude() >= getDeadZone());
                    update.accept(this,controller);
                }
            };
        }
        public static Axis createTrigger(ControllerBinding component, Supplier<Float> deadZoneGetter){
            return new Axis(component) {
                @Override
                public float getDeadZone() {
                    return deadZoneGetter.get();
                }
                @Override
                public void update(Controller controller) {;
                    update( (y = controller.axisValue(ControllerManager.getHandler().getBindingIndex(component.getMapped()))) >= getDeadZone());
                }
            };
        }
        public abstract float getDeadZone();
        public float getMagnitude(){
            return Math.max(Math.abs(y),Math.abs(x));
        }
        public float getSmoothX(){
            return (x > getDeadZone() ? x - getDeadZone() : x < -getDeadZone() ? x + getDeadZone() : 0)  / (1 - getDeadZone());
        }
        public float getSmoothY(){
            return (y > getDeadZone() ? y - getDeadZone() : y < -getDeadZone() ? y + getDeadZone() : 0)  / (1 - getDeadZone());
        }
        protected Axis(ControllerBinding component) {
            super(component);
        }
    }
}
