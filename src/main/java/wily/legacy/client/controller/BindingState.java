package wily.legacy.client.controller;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.ToggleKeyMapping;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.legacy.Legacy4J;
import wily.legacy.client.ControlType;
import wily.legacy.client.screen.ControlTooltip;

import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class BindingState {
    public final ControllerBinding<?> binding;
    public boolean justPressed = false;
    public int timePressed = -1;
    public int blockAmount = 0;
    public boolean pressed;
    public boolean released;

    public static BindingState create(ControllerBinding<?> component, Predicate<Controller> update){
        return new BindingState(component) {
            @Override
            public void update(Controller controller) {
                update(update.test(controller));
            }
        };
    }

    protected BindingState(ControllerBinding<?> binding){
        this.binding = binding;
    }

    public void update(boolean pressed){
        if (this.released = (!pressed && this.pressed)) timePressed = -1;
        if (pressed) timePressed++;
        this.justPressed = pressed && !this.pressed;
        this.pressed = pressed;
        if (justPressed) blockAmount--;
    }

    public ControlTooltip.LegacyIcon getIcon(){
        return binding.getIcon();
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

    public abstract void update(Controller controller);

    public boolean is(ControllerBinding<?> b){
        return binding == b;
    }

    public <T extends BindingState> boolean isAnd(ControllerBinding<T> binding, Predicate<T> predicate){
        return is(binding) && predicate.test((T) this);
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

    public static class Button extends BindingState {

        public final ArbitrarySupplier<ControllerBinding.Button> button;

        protected Button(ControllerBinding<?> binding, ArbitrarySupplier<ControllerBinding.Button> button) {
            super(binding);
            this.button = button;
        }

        @Override
        public void update(Controller controller) {
            update(controller.hasButton(button.get()) && controller.buttonPressed(controller.getHandler().getButtonIndex(button.get())));
        }
    }

    public static abstract class Axis extends BindingState {
        public final ArbitrarySupplier<ControllerBinding.Axis> xAxis;
        public final ArbitrarySupplier<ControllerBinding.Axis> yAxis;
        public float x;
        public float y;

        protected Axis(ControllerBinding<?> component, ArbitrarySupplier<ControllerBinding.Axis> xAxis, ArbitrarySupplier<ControllerBinding.Axis> yAxis) {
            super(component);
            this.xAxis = xAxis;
            this.yAxis = yAxis;
        }

        public static Axis createStick(ControllerBinding<?> component, Supplier<Float> deadZoneGetter, BiConsumer<Axis, Controller> update, boolean left){
            return new Axis(component, ()-> left ? ControllerBinding.Axis.LEFT_STICK_X : ControllerBinding.Axis.RIGHT_STICK_X, ()-> left ? ControllerBinding.Axis.LEFT_STICK_Y : ControllerBinding.Axis.RIGHT_STICK_Y) {

                @Override
                public float getDeadZone() {
                    return deadZoneGetter.get();
                }
                @Override
                public void update(Controller controller) {
                    super.update(controller);
                    update.accept(this, controller);
                }
            };
        }

        public static Axis createTrigger(ControllerBinding<?> component, Supplier<Float> deadZoneGetter, boolean left){
            return new Axis(component, ArbitrarySupplier.empty(), ()-> left ? ControllerBinding.Axis.LEFT_TRIGGER : ControllerBinding.Axis.RIGHT_TRIGGER) {
                @Override
                public float getDeadZone() {
                    return deadZoneGetter.get();
                }
            };
        }

        @Override
        public void update(Controller controller) {
            xAxis.ifPresent(axis-> x = controller.hasAxis(axis) ? controller.axisValue(controller.getHandler().getAxisIndex(axis)) : 0);
            yAxis.ifPresent(axis-> y = controller.hasAxis(axis) ? controller.axisValue(controller.getHandler().getAxisIndex(axis)) : 0);
            update(getMagnitude() >= getDeadZone());
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

    }
}
