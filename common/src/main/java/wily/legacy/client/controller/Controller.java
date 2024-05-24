package wily.legacy.client.controller;

import net.minecraft.client.resources.language.I18n;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.ControlTooltip;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public interface Controller {

    String getName();
    ControlTooltip.Type getType();
    boolean buttonPressed(int i);
    float axisValue(int i);
    boolean hasLED();
    void setLED(byte r, byte g, byte b);
    void close();

    Controller EMPTY = new Controller() {
        public String getName() {
            return "Empty";
        }
        @Override
        public ControlTooltip.Type getType() {
            return ControlTooltip.Type.x360;
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
        @Override
        public void close() {

        }
    };
    interface Handler {
        String getName();
        boolean init();
        void update();
        void setup(ControllerManager manager);
        Controller getController(int jid);
        boolean isValidController(int jid);
        int getBindingIndex(ControllerBinding component);
        void applyGamePadMappingsFromBuffer(BufferedReader reader) throws IOException;
        default void tryDownloadAndApplyNewMappings(){
            try {
                applyGamePadMappingsFromBuffer(new BufferedReader(new InputStreamReader(new URL("https://raw.githubusercontent.com/mdqinc/SDL_GameControllerDB/master/gamecontrollerdb.txt").openStream())));
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
            public boolean init() {
                return false;
            }
            @Override
            public void update() {
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
        default void controllerTick(Controller controller){

        }
        default void bindingStateTick(BindingState state){

        }
    }
}
