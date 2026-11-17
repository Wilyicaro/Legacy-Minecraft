package wily.legacy.client.control;

import com.mojang.blaze3d.platform.InputConstants;
import wily.legacy.client.control.navigation.LegacyMenuAccess;

public interface ControllerListener {
    ControllerListener EMPTY = new ControllerListener() {
    };

    static ControllerListener of(Object o) {
        return o instanceof ControllerListener e ? e : EMPTY;
    }

    default void controllerTick(Controller controller) {

    }

    default void bindingStateTick(BindingState state) {

    }

    default int getBindingMouseClick(BindingState state) {
        return state.is(ControllerBinding.DOWN_BUTTON) || state.is(ControllerBinding.UP_BUTTON) ? 0 : state.is(ControllerBinding.LEFT_BUTTON) ? 1 : -1;
    }

    default void simulateKeyAction(ControllerManager manager, BindingState state) {
        if (manager.isCursorDisabled)
            manager.simulateKeyAction(s -> s.is(ControllerBinding.DOWN_BUTTON), InputConstants.KEY_RETURN, state);
        manager.simulateKeyAction(s -> s.is(ControllerBinding.RIGHT_BUTTON), InputConstants.KEY_ESCAPE, state, true);
        manager.simulateKeyAction(s -> s.is(ControllerBinding.LEFT_BUTTON), InputConstants.KEY_X, state);
        manager.simulateKeyAction(s -> s.is(ControllerBinding.UP_BUTTON), InputConstants.KEY_O, state);
        if (manager.isCursorDisabled) {
            manager.simulateKeyAction(s -> s.is(ControllerBinding.LEFT_TRIGGER), InputConstants.KEY_PAGEUP, state);
            manager.simulateKeyAction(s -> s.is(ControllerBinding.RIGHT_TRIGGER), InputConstants.KEY_PAGEDOWN, state);
        } else manager.simulateKeyAction(s -> s.is(ControllerBinding.RIGHT_TRIGGER), InputConstants.KEY_W, state);
        manager.simulateKeyAction(s -> s.is(ControllerBinding.RIGHT_BUMPER), InputConstants.KEY_RBRACKET, state);
        manager.simulateKeyAction(s -> s.is(ControllerBinding.LEFT_BUMPER), InputConstants.KEY_LBRACKET, state);
        manager.simulateKeyAction(s -> s.is(ControllerBinding.TOUCHPAD_BUTTON), InputConstants.KEY_T, state);
        manager.simulateKeyAction(s -> s.is(ControllerBinding.CAPTURE), InputConstants.KEY_F2, state);
    }

    default boolean onceClickBindings(BindingState state) {
        return !(state.is(ControllerBinding.RIGHT_BUMPER) || state.is(ControllerBinding.LEFT_BUMPER) || state.is(ControllerBinding.LEFT_TRIGGER) || state.is(ControllerBinding.RIGHT_TRIGGER));
    }

    default boolean disableCursorOnInit() {
        return !(this instanceof LegacyMenuAccess<?>);
    }

    default boolean disableCursorOnWidgets() {
        return disableCursorOnInit();
    }
}
