package wily.legacy.client.controller;

import io.github.libsdl4j.api.Sdl;
import io.github.libsdl4j.api.SdlSubSystemConst;
import io.github.libsdl4j.api.gamecontroller.*;
import wily.legacy.Legacy4J;
import wily.legacy.client.screen.ControlTooltip;

import java.io.BufferedReader;
import java.util.stream.Collectors;

import static io.github.libsdl4j.api.gamecontroller.SdlGamecontroller.SDL_GameControllerUpdate;

public class SDLControllerHandler implements Controller.Handler{
    private boolean init = false;

    private static final SDLControllerHandler INSTANCE = new SDLControllerHandler();

    @Override
    public String getName() {
        return "SDL2 (libsdl4j)";
    }
    public static SDLControllerHandler getInstance(){
        return INSTANCE;
    }

    public boolean init() {
        if (!init) {
            if (Sdl.SDL_Init(SdlSubSystemConst.SDL_INIT_JOYSTICK | SdlSubSystemConst.SDL_INIT_GAMECONTROLLER) < 0) {
                Legacy4J.LOGGER.warn("SDL Game Controller failed to start!");
                return false;
            }
            tryDownloadAndApplyNewMappings();
            init = true;
        }
        return true;
    }

    @Override
    public void update() {
        SDL_GameControllerUpdate();
    }

    @Override
    public void setup(ControllerManager manager) {
        manager.updateBindings();
    }

    @Override
    public Controller getController(int jid) {
        SDL_GameController controller = SdlGamecontroller.SDL_GameControllerOpen(jid);
        return new Controller() {
            String name;
            @Override
            public String getName() {
                if (name == null) name = SdlGamecontroller.SDL_GameControllerName(controller);
                return name == null ? "Unknown" : name;
            }

            @Override
            public ControlTooltip.Type getType() {
                int type = SdlGamecontroller.SDL_GameControllerGetType(controller);
                return switch (type){
                    case SDL_GameControllerType.SDL_CONTROLLER_TYPE_PS3 -> ControlTooltip.Type.PS3;
                    case SDL_GameControllerType.SDL_CONTROLLER_TYPE_PS4,SDL_GameControllerType.SDL_CONTROLLER_TYPE_PS5 -> ControlTooltip.Type.PS4;
                    case SDL_GameControllerType.SDL_CONTROLLER_TYPE_XBOX360 -> ControlTooltip.Type.x360;
                    case SDL_GameControllerType.SDL_CONTROLLER_TYPE_XBOXONE -> ControlTooltip.Type.xONE;
                    case SDL_GameControllerType.SDL_CONTROLLER_TYPE_NINTENDO_SWITCH_PRO,SDL_GameControllerType.SDL_CONTROLLER_TYPE_NINTENDO_SWITCH_JOYCON_PAIR -> ControlTooltip.Type.SWITCH;
                    default -> ControlTooltip.Type.STEAM;
                };
            }

            @Override
            public boolean buttonPressed(int i) {
                return SdlGamecontroller.SDL_GameControllerGetButton(controller,i) == 1;
            }

            @Override
            public float axisValue(int i) {
                return SdlGamecontroller.SDL_GameControllerGetAxis(controller,i) / (float) Short.MAX_VALUE;
            }

            @Override
            public boolean hasLED() {
                return SdlGamecontroller.SDL_GameControllerHasLED(controller);
            }

            @Override
            public void setLED(byte r, byte g, byte b) {
                SdlGamecontroller.SDL_GameControllerSetLED(controller,r,g,b);
            }

            @Override
            public void close() {
                SdlGamecontroller.SDL_GameControllerClose(controller);
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
            case DOWN_BUTTON -> SDL_GameControllerButton.SDL_CONTROLLER_BUTTON_A;
            case RIGHT_BUTTON -> SDL_GameControllerButton.SDL_CONTROLLER_BUTTON_B;
            case LEFT_BUTTON -> SDL_GameControllerButton.SDL_CONTROLLER_BUTTON_X;
            case UP_BUTTON -> SDL_GameControllerButton.SDL_CONTROLLER_BUTTON_Y;
            case BACK -> SDL_GameControllerButton.SDL_CONTROLLER_BUTTON_BACK;
            case GUIDE -> SDL_GameControllerButton.SDL_CONTROLLER_BUTTON_GUIDE;
            case START -> SDL_GameControllerButton.SDL_CONTROLLER_BUTTON_START;
            case LEFT_STICK_BUTTON -> SDL_GameControllerButton.SDL_CONTROLLER_BUTTON_LEFTSTICK;
            case RIGHT_STICK_BUTTON -> SDL_GameControllerButton.SDL_CONTROLLER_BUTTON_RIGHTSTICK;
            case LEFT_STICK_RIGHT,LEFT_STICK_LEFT -> SDL_GameControllerAxis.SDL_CONTROLLER_AXIS_LEFTX;
            case RIGHT_STICK_RIGHT,RIGHT_STICK_LEFT -> SDL_GameControllerAxis.SDL_CONTROLLER_AXIS_RIGHTX;
            case LEFT_STICK_UP,LEFT_STICK_DOWN -> SDL_GameControllerAxis.SDL_CONTROLLER_AXIS_LEFTY;
            case RIGHT_STICK_UP,RIGHT_STICK_DOWN -> SDL_GameControllerAxis.SDL_CONTROLLER_AXIS_RIGHTY;
            case LEFT_TRIGGER -> SDL_GameControllerAxis.SDL_CONTROLLER_AXIS_TRIGGERLEFT;
            case RIGHT_TRIGGER -> SDL_GameControllerAxis.SDL_CONTROLLER_AXIS_TRIGGERRIGHT;
            case LEFT_BUMPER -> SDL_GameControllerButton.SDL_CONTROLLER_BUTTON_LEFTSHOULDER;
            case RIGHT_BUMPER -> SDL_GameControllerButton.SDL_CONTROLLER_BUTTON_RIGHTSHOULDER;
            case DPAD_UP -> SDL_GameControllerButton.SDL_CONTROLLER_BUTTON_DPAD_UP;
            case DPAD_DOWN -> SDL_GameControllerButton.SDL_CONTROLLER_BUTTON_DPAD_DOWN;
            case DPAD_LEFT -> SDL_GameControllerButton.SDL_CONTROLLER_BUTTON_DPAD_LEFT;
            case DPAD_RIGHT -> SDL_GameControllerButton.SDL_CONTROLLER_BUTTON_DPAD_RIGHT;
            default -> -1;
        };
    }

    @Override
    public void applyGamePadMappingsFromBuffer(BufferedReader reader) {
        String s = reader.lines().collect(Collectors.joining());
        int i = SdlGamecontroller.SDL_GameControllerAddMapping(s);
        Legacy4J.LOGGER.warn("Added SDL Controller Mappings: " + i + " Code");
        //      Legacy4J.LOGGER.warn("Mapping: " + s);
    }

}
