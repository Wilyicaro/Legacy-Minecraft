package wily.legacy.client.controller;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import wily.factoryapi.FactoryAPIClient;
import wily.legacy.Legacy4J;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacyTip;
import wily.legacy.client.screen.LegacyMenuAccess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

import static wily.legacy.client.controller.ControllerManager.CONTROLLER_DETECTED;
import static wily.legacy.client.controller.ControllerManager.CONTROLLER_DISCONNECTED;

public interface Controller {

    /**
     * Empty controller, used when disconnecting or while the window isn't focused
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
        public Handler getHandler() {
            return Handler.EMPTY;
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
            manager.minecraft.execute(() -> manager.minecraft.screen.repositionElements());
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
        if (!LegacyOptions.controllerToasts.get()) return;
        LegacyTip oldToast = FactoryAPIClient.getToasts().getToast(LegacyTip.class, Toast.NO_TOKEN);
        Component tip = Component.literal(getName());
        if (oldToast == null || (oldToast.title != CONTROLLER_DETECTED && oldToast.title != CONTROLLER_DISCONNECTED) || oldToast.visibility == Toast.Visibility.HIDE) {
            Minecraft.getInstance().execute(() -> FactoryAPIClient.getToasts().addToast(new LegacyTip(component, tip).centered().disappearTime(4500)));
        } else {
            oldToast.tip(tip).title(component).disappearTime(4500);
        }
    }

    /**
     * @return {@link Controller.Handler} used by this controller
     */
    Handler getHandler();

    default void manageBindings(Runnable run) {
        run.run();
    }

    interface Handler {
        Component DOWNLOAD_MESSAGE = Component.translatable("legacy.menu.download_natives_message");
        Component DOWNLOADING_NATIVES = Component.translatable("legacy.menu.downloading_natives");
        Component LOADING_NATIVES = Component.translatable("legacy.menu.loading_natives");
        Handler EMPTY = new Handler() {
            @Override
            public Component getName() {
                return CommonComponents.OPTION_OFF;
            }

            @Override
            public void init() {
            }

            @Override
            public boolean update() {
                return false;
            }

            @Override
            public void setup(ControllerManager manager) {
            }

            @Override
            public Controller getController(int jid) {
                return null;
            }

            @Override
            public boolean isValidController(int jid) {
                return false;
            }

            @Override
            public int getButtonIndex(ControllerBinding.Button button) {
                return -1;
            }

            @Override
            public int getAxisIndex(ControllerBinding.Axis axis) {
                return -1;
            }

            @Override
            public void applyGamePadMappingsFromBuffer(BufferedReader reader) {
            }
        };

        /**
         * @return Controller Handler display name
         */
        Component getName();

        /**
         * Starts the downloading and loading of the game pad mappings and the library if needed
         */
        void init();

        boolean update();

        /**
         * Manages the connected controller bindings
         *
         * @param manager Controller Manager instance
         */
        default void setup(ControllerManager manager) {
            manager.connectedController.manageBindings(manager::updateBindings);
        }

        /**
         * @param jid Controller ID, generally based on the connection order
         * @return The controller corresponding to this ID, or null if it's invalid
         */
        Controller getController(int jid);

        /**
         * @param jid Controller ID, generally based on the connection order
         * @return If this ID corresponds to a valid controller
         */
        boolean isValidController(int jid);

        /**
         * @param button {@link ControllerBinding.Button} to convert to a button index used by this Controller Handler
         * @return Button index used by this Controller Handler
         */
        int getButtonIndex(ControllerBinding.Button button);

        /**
         * @param axis {@link ControllerBinding.Axis} to convert to an axis index used by this Controller Handler
         * @return Axis index used by this Controller Handler
         */
        int getAxisIndex(ControllerBinding.Axis axis);

        void applyGamePadMappingsFromBuffer(BufferedReader reader) throws IOException;

        default void tryDownloadAndApplyNewMappings() {
            try {
                applyGamePadMappingsFromBuffer(new BufferedReader(new InputStreamReader(URI.create("https://raw.githubusercontent.com/mdqinc/SDL_GameControllerDB/master/gamecontrollerdb.txt").toURL().openStream())));
            } catch (IOException e) {
                Legacy4J.LOGGER.warn(e.getMessage());
            }
        }
    }

    interface Event {
        Event EMPTY = new Event() {
        };

        static Event of(Object o) {
            return o instanceof Event e ? e : EMPTY;
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
            }
            else manager.simulateKeyAction(s -> s.is(ControllerBinding.RIGHT_TRIGGER), InputConstants.KEY_W, state);
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
}
