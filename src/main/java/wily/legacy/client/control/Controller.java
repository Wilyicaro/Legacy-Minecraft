package wily.legacy.client.control;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.network.chat.Component;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.client.MinecraftAccessor;
import wily.factoryapi.base.client.UIAccessor;
import wily.legacy.client.LegacyTip;

import static wily.legacy.client.control.ControllerManager.CONTROLLER_DETECTED;
import static wily.legacy.client.control.ControllerManager.CONTROLLER_DISCONNECTED;

public interface Controller {

    /**
     * Empty control, used when disconnecting or while the window isn't focused
     */
    Controller EMPTY = new Controller() {
        public String getName() {
            return "Empty";
        }

        @Override
        public ControlType getType() {
            return ControlType.get(ControlType.x360);
        }

        @Override
        public boolean buttonPressed(int i) {
            return false;
        }

        @Override
        public float axisValue(int i) {
            return 0;
        }

        @Override
        public boolean hasButton(ControllerBinding.Button button) {
            return false;
        }

        @Override
        public boolean hasAxis(ControllerBinding.Axis axis) {
            return false;
        }

        @Override
        public ControllerHandler getHandler() {
            return ControllerHandler.EMPTY;
        }

        @Override
        public void manageBindings(Runnable run) {
        }
    };

    /**
     * @return Controller name given by the Controller Handler implementation
     */
    String getName();

    /**
     * @return ControlType corresponding to the Controller Handler's controller type, this is based on the controller name if the handler does not provide this information, as in GLFW
     */
    ControlType getType();

    /**
     * @param i Controller Handler Button Index, the available buttons are in {@link ControllerBinding.Button}
     * @return If this button is being pressed
     */
    boolean buttonPressed(int i);

    /**
     * @param i Controller Handler Axis Index, the available axes are in {@link ControllerBinding.Axis}
     * @return Axis value in 0,1 range
     */
    float axisValue(int i);

    /**
     * @return If this controller has LED
     * This always returns true on SDL3 and false on GLFW
     */
    default boolean hasLED() {
        return false;
    }

    /**
     * @param r Red RGB value
     * @param g Green RGB value
     * @param b Blue RGB value
     */
    default void setLED(byte r, byte g, byte b) {

    }

    default void connect(ControllerManager manager) {
        manager.setControllerTheLastInput(true);
        if (!manager.isCursorDisabled && manager.minecraft.screen != null)
            manager.minecraft.execute(() -> UIAccessor.of(manager.minecraft.screen).reloadUI());
        addOrSetControllerToast(CONTROLLER_DETECTED);
    }

    default void rumble(char low_frequency_rumble, char high_frequency_rumble, int duration_ms) {

    }

    default void rumbleTriggers(char left_rumble, char right_rumble, int duration_ms) {

    }

    default int getTouchpadsCount() {
        return 0;
    }

    default int getTouchpadFingersCount(int touchpad) {
        return 0;
    }

    default boolean hasFingerInTouchpad(int touchpad, int finger, Byte state, Float x, Float y, Float pressure) {
        return false;
    }

    /**
     * @param button {@link ControllerBinding.Button}
     * @return If this controller contains this button
     */
    boolean hasButton(ControllerBinding.Button button);

    /**
     * @param axis {@link ControllerBinding.Axis}
     * @return If this controller contains this axis
     */
    boolean hasAxis(ControllerBinding.Axis axis);

    default void disconnect(ControllerManager manager) {
        addOrSetControllerToast(CONTROLLER_DISCONNECTED);
    }

    default void addOrSetControllerToast(Component component) {
        if (!LegacyControlsOptions.controllerToasts.get() || !MinecraftAccessor.getInstance().hasGameLoaded()) return;
        LegacyTip oldToast = FactoryAPIClient.getToasts().getToast(LegacyTip.class, Toast.NO_TOKEN);
        Component tip = Component.literal(getName());
        if (oldToast == null || (oldToast.title != CONTROLLER_DETECTED && oldToast.title != CONTROLLER_DISCONNECTED) || oldToast.visibility == Toast.Visibility.HIDE) {
            Minecraft.getInstance().execute(() -> FactoryAPIClient.getToasts().addToast(new LegacyTip(component, tip).centered().disappearTime(4500)));
        } else {
            oldToast.tip(tip).title(component).disappearTime(4500);
        }
    }

    /**
     * @return {@link ControllerHandler} used by this controller
     */
    ControllerHandler getHandler();

    default void manageBindings(Runnable run) {
        run.run();
    }

}
