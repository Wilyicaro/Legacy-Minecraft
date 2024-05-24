package wily.legacy.client.controller;

import io.github.libsdl4j.api.gamecontroller.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;
import wily.legacy.client.screen.ControlTooltip;
import wily.legacy.util.ScreenUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class GLFWControllerHandler implements Controller.Handler{
    private boolean init = false;
    GLFWGamepadState gamepadState;

    @Override
    public String getName() {
        return "GLFW";
    }

    private static final GLFWControllerHandler INSTANCE = new GLFWControllerHandler();


    public static GLFWControllerHandler getInstance(){
        return INSTANCE;
    }

    @Override
    public boolean init() {
        if (!init){
            tryDownloadAndApplyNewMappings();
            init = true;
        }
        return true;
    }

    @Override
    public void update() {
    }

    @Override
    public void setup(ControllerManager manager) {
        gamepadState = GLFWGamepadState.calloc();
        if (GLFW.glfwGetGamepadState(ScreenUtil.getLegacyOptions().selectedController().get(), gamepadState))
            manager.updateBindings();
        gamepadState.free();
    }

    @Override
    public Controller getController(int jid) {

        return new Controller() {
            String name;
            @Override
            public String getName() {
                if (name == null) name = GLFW.glfwGetGamepadName(jid);
                return name == null ? "Unknown" : name;
            }

            @Override
            public ControlTooltip.Type getType() {
                if (getName() != null) {
                    if (name.contains("PS3")) return ControlTooltip.Type.PS3;
                    else if (name.contains("PS4") || name.contains("PS5")) return ControlTooltip.Type.PS4;
                    else if (name.contains("Xbox 360")) return ControlTooltip.Type.x360;
                    else if (name.contains("Xbox One")) return ControlTooltip.Type.xONE;
                    else if (name.contains("Nintendo Switch")) return ControlTooltip.Type.SWITCH;
                    else if (name.contains("Wii U")) return ControlTooltip.Type.WII_U;
                }
                return ControlTooltip.Type.x360;
            }

            @Override
            public boolean buttonPressed(int i) {
                return gamepadState != null && gamepadState.buttons(i) == GLFW.GLFW_PRESS;
            }

            @Override
            public float axisValue(int i) {
                return gamepadState == null ? 0 : gamepadState.axes(i);
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
    }

    @Override
    public boolean isValidController(int jid) {
        return SdlGamecontroller.SDL_IsGameController(jid);
    }

    @Override
    public int getBindingIndex(ControllerBinding component) {
        return switch (component){
            case DOWN_BUTTON -> GLFW.GLFW_GAMEPAD_BUTTON_A;
            case RIGHT_BUTTON -> GLFW.GLFW_GAMEPAD_BUTTON_B;
            case LEFT_BUTTON -> GLFW.GLFW_GAMEPAD_BUTTON_X;
            case UP_BUTTON -> GLFW.GLFW_GAMEPAD_BUTTON_Y;
            case BACK -> GLFW.GLFW_GAMEPAD_BUTTON_BACK;
            case GUIDE -> GLFW.GLFW_GAMEPAD_BUTTON_GUIDE;
            case START -> GLFW.GLFW_GAMEPAD_BUTTON_START;
            case LEFT_STICK_BUTTON -> GLFW.GLFW_GAMEPAD_BUTTON_LEFT_THUMB;
            case RIGHT_STICK_BUTTON -> GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_THUMB;
            case LEFT_STICK_RIGHT,LEFT_STICK_LEFT -> GLFW.GLFW_GAMEPAD_AXIS_LEFT_X;
            case RIGHT_STICK_RIGHT,RIGHT_STICK_LEFT -> GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X;
            case LEFT_STICK_UP,LEFT_STICK_DOWN -> GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y;
            case RIGHT_STICK_UP,RIGHT_STICK_DOWN -> GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y;
            case LEFT_TRIGGER -> GLFW.GLFW_GAMEPAD_AXIS_LEFT_TRIGGER;
            case RIGHT_TRIGGER -> GLFW.GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER;
            case LEFT_BUMPER ->  GLFW.GLFW_GAMEPAD_BUTTON_LEFT_BUMPER;
            case RIGHT_BUMPER -> GLFW.GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER;
            case DPAD_UP -> GLFW.GLFW_GAMEPAD_BUTTON_DPAD_UP;
            case DPAD_DOWN -> GLFW.GLFW_GAMEPAD_BUTTON_DPAD_DOWN;
            case DPAD_LEFT -> GLFW.GLFW_GAMEPAD_BUTTON_DPAD_LEFT;
            case DPAD_RIGHT -> GLFW.GLFW_GAMEPAD_BUTTON_DPAD_RIGHT;
            default -> -1;
        };
    }

    @Override
    public void applyGamePadMappingsFromBuffer(BufferedReader reader) throws IOException {
        String s = reader.lines().collect(Collectors.joining());
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        ByteBuffer mappingsBuffer = ByteBuffer.allocateDirect(bytes.length + 1);
        mappingsBuffer.put(bytes);
        mappingsBuffer.rewind();
        GLFW.glfwUpdateGamepadMappings(mappingsBuffer);
        mappingsBuffer.clear();
        reader.close();
    }

}
