package wily.legacy.client.controller;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
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
    boolean hasLED();
    void setLED(byte r, byte g, byte b);
    default void connect(ControllerManager manager){
        if (!manager.isCursorDisabled && manager.minecraft.screen != null) manager.minecraft.execute(()-> manager.minecraft.screen.repositionElements());
        manager.minecraft.getToasts().addToast(new LegacyTip(CONTROLLER_DETECTED, Component.literal(getName())).disappearTime(4500));
    }
    default void disconnect(ControllerManager manager){
        if (manager.isCursorDisabled && manager.getCursorMode() != 2) manager.enableCursor();
        manager.updateBindings(Controller.EMPTY);
        manager.connectedController = null;
        manager.minecraft.getToasts().addToast(new LegacyTip(CONTROLLER_DISCONNECTED, Component.literal(getName())).disappearTime(4500));
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
        public boolean hasLED() {
            return false;
        }
        @Override
        public void setLED(byte r, byte g, byte b) {

        }
    };
    interface Handler {
        Component DOWNLOAD_MESSAGE = Component.translatable("legacy.menu.download_natives_message");
        Component DOWNLOADING_NATIVES = Component.translatable("legacy.menu.downloading_natives");
        Component LOADING_NATIVES = Component.translatable("legacy.menu.loading_natives");

        String getName();

        String getId();

        void init();
        boolean update();
        void setup(ControllerManager manager);
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
            public String getName() {
                return I18n.get("options.off");
            }
            @Override
            public String getId() {
                return "none";
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
