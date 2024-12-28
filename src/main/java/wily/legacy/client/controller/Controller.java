package wily.legacy.client.controller;

import net.minecraft.client.resources.language.I18n;
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
        manager.isControllerTheLastInput = true;
        if (!manager.isCursorDisabled && manager.minecraft.screen != null) manager.minecraft.execute(()-> manager.minecraft.screen.repositionElements());
        FactoryAPIClient.getToasts().addToast(new LegacyTip(CONTROLLER_DETECTED, Component.literal(getName())).disappearTime(4500));
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

    default void disconnect(ControllerManager manager){
        manager.isControllerTheLastInput = false;
        if (manager.isCursorDisabled && manager.getCursorMode() != 2) manager.enableCursor();
        manager.updateBindings(Controller.EMPTY);
        manager.connectedController = null;
        FactoryAPIClient.getToasts().addToast(new LegacyTip(CONTROLLER_DISCONNECTED, Component.literal(getName())).disappearTime(4500));
    }

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
        int getBindingIndex(ControllerBinding component);
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
            public int getBindingIndex(ControllerBinding component) {
                return 0;
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

        default boolean onceClickBindings(){
            return true;
        }

        default boolean disableCursorOnInit(){
            return !(this instanceof LegacyMenuAccess<?>);
        }
    }
}
