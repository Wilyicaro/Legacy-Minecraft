package wily.legacy.client.controller;

import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;
import wily.legacy.client.ControlType;
import wily.legacy.util.ScreenUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GLFWControllerHandler implements Controller.Handler{
    private boolean init = false;
    public static final Component TITLE = Component.literal("GLFW");

    @Override
    public Component getName() {
        return TITLE;
    }

    private static final GLFWControllerHandler INSTANCE = new GLFWControllerHandler();

    public static GLFWControllerHandler getInstance(){
        return INSTANCE;
    }

    @Override
    public void init() {
        if (!init){
            tryDownloadAndApplyNewMappings();
            init = true;
        }
    }

    @Override
    public boolean update() {
        return true;
    }

    @Override
    public Controller getController(int jid) {
        return new Controller() {
            GLFWGamepadState gamepadState;
            String name;
            @Override
            public String getName() {
                if (name == null) name = GLFW.glfwGetGamepadName(jid);
                return name == null ? "Unknown" : name;
            }

            @Override
            public ControlType getType() {
                String name = getName();
                if (name != null) {
                    if (name.contains("PS3")) return ControlType.PS3;
                    else if (name.contains("PS4")) return ControlType.PS4;
                    else if (name.contains("PS5")) return ControlType.PS5;
                    else if (name.contains("Xbox 360")) return ControlType.x360;
                    else if (name.contains("Xbox One")) return ControlType.xONE;
                    else if (name.contains("Nintendo Switch")) return ControlType.SWITCH;
                    else if (name.contains("Wii U")) return ControlType.WII_U;
                }
                return ControlType.x360;
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
            public void manageBindings(Runnable run) {
                gamepadState = GLFWGamepadState.calloc();
                if (GLFW.glfwGetGamepadState(jid, gamepadState))
                    Controller.super.manageBindings(run);
                gamepadState.free();
            }
        };
    }

    @Override
    public boolean isValidController(int jid) {
        return GLFW.glfwJoystickIsGamepad(jid);
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
