package wily.legacy.client.controller;

import wily.legacy.client.screen.ControlTooltip;

import java.io.BufferedReader;
import java.io.IOException;

public interface Controller {

    String getName();
    ControlTooltip.Type getType();
    boolean buttonPressed(int i);
    float axisValue(int i);
    boolean hasLED();
    void setLED(byte r, byte g, byte b);
    void close();
    interface Handler {
        void init();
        void update();
        void setup(ControllerManager manager);
        Controller getController(int jid);
        boolean isValidController(int jid);
        int getBindingIndex(ControllerBinding component);
        void applyGamePadMappingsFromBuffer(BufferedReader reader) throws IOException;
    }
    interface Event {
        void componentTick(BindingState state);
    }
}
