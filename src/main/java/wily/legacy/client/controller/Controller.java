package wily.legacy.client.controller;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import wily.factoryapi.FactoryAPIClient;
import wily.legacy.Legacy4J;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyTip;
import wily.legacy.client.screen.LegacyMenuAccess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

import static wily.legacy.client.controller.ControllerManager.CONTROLLER_DETECTED;
import static wily.legacy.client.controller.ControllerManager.CONTROLLER_DISCONNECTED;

public interface Controller {

    String getName();

    ControlType getType();

    boolean buttonPressed(int i);

    float axisValue(int i);

    default boolean hasLED(){
        return false;
    }

    default void setLED(byte r, byte g, byte b){}

    default void connect(ControllerManager manager){
        manager.setControllerTheLastInput(true);
        if (!manager.isCursorDisabled && manager.minecraft.screen != null) manager.minecraft.execute(()-> manager.minecraft.screen.repositionElements());
        addOrSetControllerToast(CONTROLLER_DETECTED);
    }

    default void rumble(char low_frequency_rumble, char high_frequency_rumble, int duration_ms){}

    default void rumbleTriggers(char left_rumble, char right_rumble, int duration_ms){}

    default int getTouchpadsCount(){
        return 0;
    }

    default int getTouchpadFingersCount(int touchpad){
        return 0;
    }

    default boolean hasFingerInTouchpad(int touchpad, int finger, Byte state, Float x, Float y, Float pressure){
        return false;
    }

    boolean hasButton(ControllerBinding.Button button);

    boolean hasAxis(ControllerBinding.Axis axis);

    default void disconnect(ControllerManager manager){
        manager.setControllerTheLastInput(false);
        if (manager.isCursorDisabled && !manager.getCursorMode().isNever()) manager.enableCursor();
        manager.updateBindings(Controller.EMPTY);
        manager.connectedController = null;
        addOrSetControllerToast(CONTROLLER_DISCONNECTED);
    }

    default void addOrSetControllerToast(Component component){
        LegacyTip oldToast = FactoryAPIClient.getToasts().getToast(LegacyTip.class, Toast.NO_TOKEN);
        Component tip = Component.literal(getName());
        if (oldToast == null || (oldToast.title != CONTROLLER_DETECTED && oldToast.title != CONTROLLER_DISCONNECTED) || oldToast.visibility == Toast.Visibility.HIDE) {
            FactoryAPIClient.getToasts().addToast(new LegacyTip(component, tip).centered().disappearTime(4500));
        } else {
            oldToast.tip(tip).title(component).disappearTime(4500);
        }
    }

    Handler getHandler();

    Controller EMPTY = new Controller() {
        public String getName() {
            return "Empty";
        }
        @Override
        public ControlType getType() {
            return ControlType.x360;
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

    interface Handler {
        Component DOWNLOAD_MESSAGE = Component.translatable("legacy.menu.download_natives_message");
        Component DOWNLOADING_NATIVES = Component.translatable("legacy.menu.downloading_natives");
        Component LOADING_NATIVES = Component.translatable("legacy.menu.loading_natives");

        Component getName();

        void init();

        boolean update();

        default void setup(ControllerManager manager) {
            manager.connectedController.manageBindings(manager::updateBindings);
        }

        Controller getController(int jid);

        boolean isValidController(int jid);

        int getButtonIndex(ControllerBinding.Button button);

        int getAxisIndex(ControllerBinding.Axis axis);

        void applyGamePadMappingsFromBuffer(BufferedReader reader) throws IOException;

        default void tryDownloadAndApplyNewMappings(){
            try {
                applyGamePadMappingsFromBuffer(new BufferedReader(new InputStreamReader(URI.create("https://raw.githubusercontent.com/mdqinc/SDL_GameControllerDB/master/gamecontrollerdb.txt").toURL().openStream())));
            } catch (IOException e) {
                Legacy4J.LOGGER.warn(e.getMessage());
            }
        }

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
    }

    default void manageBindings(Runnable run){
        run.run();
    }

    interface Event {
        Event EMPTY = new Event() {};

        static Event of(Object o){
            return o instanceof Event e ? e : EMPTY;
        }

        default void controllerTick(Controller controller){

        }

        default void bindingStateTick(BindingState state){

        }

        default int getBindingMouseClick(BindingState state){
            return state.is(ControllerBinding.DOWN_BUTTON) || state.is(ControllerBinding.UP_BUTTON) ? 0 : state.is(ControllerBinding.LEFT_BUTTON) ? 1 : -1;
        }

        default void simulateKeyAction(ControllerManager manager, BindingState state){
            if (manager.isCursorDisabled) manager.simulateKeyAction(s-> s.is(ControllerBinding.DOWN_BUTTON), InputConstants.KEY_RETURN, state);
            manager.simulateKeyAction(s-> s.is(ControllerBinding.RIGHT_BUTTON),InputConstants.KEY_ESCAPE, state, true);
            manager.simulateKeyAction(s-> s.is(ControllerBinding.LEFT_BUTTON),InputConstants.KEY_X, state);
            manager.simulateKeyAction(s-> s.is(ControllerBinding.UP_BUTTON),InputConstants.KEY_O, state);
            if (manager.isCursorDisabled) manager.simulateKeyAction(s-> s.is(ControllerBinding.LEFT_TRIGGER),InputConstants.KEY_PAGEUP, state);
            else manager.simulateKeyAction(s-> s.is(ControllerBinding.RIGHT_TRIGGER),InputConstants.KEY_W, state);
            manager.simulateKeyAction(s-> s.is(ControllerBinding.RIGHT_TRIGGER),InputConstants.KEY_PAGEDOWN, state);
            manager.simulateKeyAction(s-> s.is(ControllerBinding.RIGHT_BUMPER),InputConstants.KEY_RBRACKET, state);
            manager.simulateKeyAction(s-> s.is(ControllerBinding.LEFT_BUMPER),InputConstants.KEY_LBRACKET, state);
            manager.simulateKeyAction(s-> s.is(ControllerBinding.TOUCHPAD_BUTTON),InputConstants.KEY_T, state);
            manager.simulateKeyAction(s-> s.is(ControllerBinding.CAPTURE),InputConstants.KEY_F2, state);
        }

        default boolean onceClickBindings(BindingState state){
            return !(state.is(ControllerBinding.RIGHT_BUMPER) || state.is(ControllerBinding.LEFT_BUMPER) || state.is(ControllerBinding.LEFT_TRIGGER) || state.is(ControllerBinding.RIGHT_TRIGGER));
        }

        default boolean disableCursorOnInit(){
            return !(this instanceof LegacyMenuAccess<?>);
        }
    }
}
