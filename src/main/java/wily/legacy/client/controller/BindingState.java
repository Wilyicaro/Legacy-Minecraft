package wily.legacy.client.controller;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import wily.legacy.client.ControlType;
import wily.legacy.client.screen.ControlTooltip;

import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class BindingState {

    public final ControllerBinding binding;
    public boolean justPressed = false;
    public int timePressed = -1;
    public int blockAmount = 0;
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

    protected BindingState(ControllerBinding binding){
        this.binding = binding;
    }
    public void update(boolean pressed){
        if (this.released = (!pressed && this.pressed)) timePressed = -1;
        if (pressed) timePressed++;
        this.justPressed = pressed && !this.pressed;
        this.pressed = pressed;
        if (justPressed) blockAmount--;
    }


    public ControlTooltip.ComponentIcon getIcon(){
        return ControlType.getActiveControllerType().getIcons().get(binding.getMapped().getSerializedName());
    }
    public boolean canClick(){
        return canClick(getDefaultDelay());
    }
    public boolean canClick(int delay){
        return (timePressed == 0 || timePressed >= 3 * delay) && timePressed % delay == 0 && !isBlocked();
    }
    public boolean onceClick(int timeDelay){
        int lastTimePressed = timePressed;
        if (timePressed == 0) timePressed = timeDelay;
        return lastTimePressed == 0 && !isBlocked();
    }
    public int getDefaultDelay(){
        return 100;
    }
    public boolean onceClick(boolean block){
        boolean onceClick = onceClick(-(justPressed ? 3 : 1) * getDefaultDelay());
        if (block) block();
        return onceClick;
    }
    public abstract void update(Controller state);
    public boolean is(ControllerBinding b){
        return binding == b;
    }
    public void block(){
        block(1);
    }
    public void block(int blockAmount){
        this.blockAmount= blockAmount;
    }

    public boolean isBlocked() {
        return blockAmount > 0;
    }
    public boolean canDownKeyMapping(KeyMapping mapping){
        return !(mapping instanceof ToggleKeyMapping) && canClick() || timePressed == 0;
    }
    public boolean canReleaseKeyMapping(KeyMapping mapping){
        return released;
    }

    public boolean matches(KeyMapping mapping){
        return ((LegacyKeyMapping)mapping).getBinding() == binding;
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
                    x = controller.axisValue(ControllerManager.getHandler().getBindingIndex(binding.getMapped() == ControllerBinding.LEFT_STICK ? ControllerBinding.LEFT_STICK_RIGHT : ControllerBinding.RIGHT_STICK_RIGHT));
                    y = controller.axisValue(ControllerManager.getHandler().getBindingIndex(binding.getMapped() == ControllerBinding.LEFT_STICK ? ControllerBinding.LEFT_STICK_UP : ControllerBinding.RIGHT_STICK_UP));
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
                    update( (y = controller.axisValue(ControllerManager.getHandler().getBindingIndex(binding.getMapped()))) >= getDeadZone());
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
