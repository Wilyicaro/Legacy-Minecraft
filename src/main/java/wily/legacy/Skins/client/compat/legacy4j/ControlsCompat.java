package wily.legacy.Skins.client.compat.legacy4j;

import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.ControllerBinding;

public final class ControlsCompat {

    private ControlsCompat() {
    }

    public static boolean squareOnce(BindingState state) {
        return state != null && state.is(ControllerBinding.LEFT_BUTTON) && state.onceClick(true);
    }

    public static boolean triangleOnce(BindingState state) {
        return state != null && state.is(ControllerBinding.UP_BUTTON) && state.onceClick(true);
    }

    public static boolean r3Once(BindingState state) {
        return state != null && state.is(ControllerBinding.RIGHT_STICK_BUTTON) && state.onceClick(true);
    }
}
