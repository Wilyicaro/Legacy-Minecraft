package wily.legacy.client.controller;

import dev.isxander.sdl3java.api.SdlInit;
import dev.isxander.sdl3java.api.SdlSubSystemConst;
import dev.isxander.sdl3java.api.gamepad.*;
import dev.isxander.sdl3java.api.joystick.SDL_JoystickID;
import dev.isxander.sdl3java.api.version.SdlVersionConst;
import dev.isxander.sdl3java.jna.SdlNativeLibraryLoader;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.apache.commons.io.FileUtils;
import wily.legacy.Legacy4J;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.ConfirmationScreen;
import wily.legacy.client.screen.LegacyLoadingScreen;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;


public class SDLControllerHandler implements Controller.Handler{
    private SDL_JoystickID[] actualIds = new SDL_JoystickID[]{};
    private boolean init = false;

    private static final SDLControllerHandler INSTANCE = new SDLControllerHandler();
    public static String SDL_VERSION = SdlVersionConst.SDL_MAJOR_VERSION + "." + SdlVersionConst.SDL_COMMIT;

    public static File nativesFile;

    @Override
    public String getName() {
        return "SDL3 (isXander's libsdl4j)";
    }
    public static SDLControllerHandler getInstance(){
        return INSTANCE;
    }

    public void init() {
        if (!init) {
            Minecraft minecraft = Minecraft.getInstance();
            if (nativesFile == null) nativesFile = new File(minecraft.gameDirectory,"natives/" + getNativesFile());
            if (!nativesFile.exists()){
                minecraft.executeBlocking(()-> {
                    Screen s = minecraft.screen;
                    minecraft.setScreen(new ConfirmationScreen(s, Component.translatable("legacy.menu.download_natives",getName()), Controller.Handler.DOWNLOAD_MESSAGE, b -> {
                        AtomicLong fileSize = new AtomicLong(1);
                        LegacyLoadingScreen screen = new LegacyLoadingScreen(Controller.Handler.DOWNLOADING_NATIVES, CommonComponents.EMPTY){
                            @Override
                            public void tick() {
                                if (progress == 100) {
                                    minecraft.setScreen(s);
                                    return;
                                }
                                progress = nativesFile.exists() ? Math.round(Math.min(1,FileUtils.sizeOf(nativesFile) / (float) fileSize.get()) * 80) : 0;
                                super.tick();
                            }
                        };
                        minecraft.setScreen(screen);
                        CompletableFuture.runAsync(()->{
                            try {
                                fileSize.set(getNativesURI().toURL().openConnection().getContentLengthLong());
                                FileUtils.copyURLToFile(getNativesURI().toURL(), nativesFile);
                                screen.lastLoadingHeader = Controller.Handler.LOADING_NATIVES;
                                minecraft.execute(()->{
                                    init();
                                    screen.progress = 100;
                                });
                            } catch (IOException | URISyntaxException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }){
                        @Override
                        public void onClose() {
                            ((LegacyOptions)minecraft.options).selectedControllerHandler().set(0);
                            minecraft.options.save();
                            super.onClose();
                        }
                    });
                });
                return;
            }else SdlNativeLibraryLoader.loadLibSDL3FromFilePathNow(nativesFile.getPath());

            if (SdlInit.SDL_Init(SdlSubSystemConst.SDL_INIT_JOYSTICK | SdlSubSystemConst.SDL_INIT_GAMEPAD) < 0) {
                Legacy4J.LOGGER.warn("SDL Game Controller failed to start!");
                return;
            }
            tryDownloadAndApplyNewMappings();
            init = true;
        }
    }
    public static URI getNativesURI() throws URISyntaxException {
        String nativesPath = "https://maven.isxander.dev/releases/dev/isxander/libsdl4j-natives/" + SDL_VERSION + "/";
        return new URI(nativesPath + getNativesFile());
    }
    public static String getNativesFile(){
        String arch = System.getProperty("os.arch");
        return (switch (Util.getPlatform()){
            case WINDOWS -> arch.contains("64") ? "libsdl4j-natives-%s-windows64.dll" : "libsdl4j-natives-%s-windows32.dll";
            case OSX -> arch.contains("arm") || arch.contains("aarch") ? "libsdl4j-natives-%s-macos-aarch64.dylib" : "libsdl4j-natives-%s-macos-x86_64.dylib";
            default -> "libsdl4j-natives-%s-linux64.so";
        }).formatted(SDL_VERSION);
    }

    @Override
    public boolean update() {
        if (!init) return false;
        SdlGamepad.SDL_UpdateGamepads();
        return true;
    }

    @Override
    public void setup(ControllerManager manager) {
        manager.updateBindings();
    }

    @Override
    public Controller getController(int jid) {
        if (actualIds.length <= jid) return Controller.EMPTY;
        SDL_Gamepad controller = SdlGamepad.SDL_OpenGamepad(actualIds[jid]);
        return new Controller() {
            String name;
            @Override
            public String getName() {
                if (name == null) name = SdlGamepad.SDL_GetGamepadName(controller);
                return name == null ? "Unknown" : name;
            }

            @Override
            public ControlType getType() {
                int type = SdlGamepad.SDL_GetGamepadType(controller);
                return switch (type){
                    case SDL_GamepadType.SDL_GAMEPAD_TYPE_PS3 -> ControlType.PS3;
                    case SDL_GamepadType.SDL_GAMEPAD_TYPE_PS4 -> ControlType.PS4;
                    case SDL_GamepadType.SDL_GAMEPAD_TYPE_PS5 -> ControlType.PS5;
                    case SDL_GamepadType.SDL_GAMEPAD_TYPE_XBOX360 -> ControlType.x360;
                    case SDL_GamepadType.SDL_GAMEPAD_TYPE_XBOXONE -> ControlType.xONE;
                    case SDL_GamepadType.SDL_GAMEPAD_TYPE_NINTENDO_SWITCH_PRO,SDL_GamepadType.SDL_GAMEPAD_TYPE_NINTENDO_SWITCH_JOYCON_PAIR -> ControlType.SWITCH;
                    default -> ControlType.STEAM;
                };
            }

            @Override
            public boolean buttonPressed(int i) {
                return SdlGamepad.SDL_GetGamepadButton(controller,i) == 1;
            }

            @Override
            public float axisValue(int i) {
                return SdlGamepad.SDL_GetGamepadAxis(controller,i) / (float) Short.MAX_VALUE;
            }

            @Override
            public boolean hasLED() {
                return true;
            }

            @Override
            public void setLED(byte r, byte g, byte b) {
                SdlGamepad.SDL_SetGamepadLED(controller,r,g,b);
            }

            @Override
            public void disconnect(ControllerManager manager) {
                Controller.super.disconnect(manager);
                SdlGamepad.SDL_CloseGamepad(controller);
            }
        };
    }

    @Override
    public boolean isValidController(int jid) {
        actualIds = SdlGamepad.SDL_GetGamepads();
        if (actualIds.length <= jid) return false;
        return SdlGamepad.SDL_IsGamepad(actualIds[jid]);
    }

    @Override
    public int getBindingIndex(ControllerBinding component) {
        return switch (component){
            case DOWN_BUTTON -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_SOUTH;
            case RIGHT_BUTTON -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_EAST;
            case LEFT_BUTTON -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_WEST;
            case UP_BUTTON -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_NORTH;
            case BACK -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_BACK;
            case GUIDE -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_GUIDE;
            case START -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_START;
            case LEFT_STICK_BUTTON -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_LEFT_STICK;
            case RIGHT_STICK_BUTTON -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_RIGHT_STICK;
            case LEFT_STICK_RIGHT,LEFT_STICK_LEFT -> SDL_GamepadAxis.SDL_GAMEPAD_AXIS_LEFTX;
            case RIGHT_STICK_RIGHT,RIGHT_STICK_LEFT -> SDL_GamepadAxis.SDL_GAMEPAD_AXIS_RIGHTX;
            case LEFT_STICK_UP,LEFT_STICK_DOWN -> SDL_GamepadAxis.SDL_GAMEPAD_AXIS_LEFTY;
            case RIGHT_STICK_UP,RIGHT_STICK_DOWN -> SDL_GamepadAxis.SDL_GAMEPAD_AXIS_RIGHTY;
            case LEFT_TRIGGER -> SDL_GamepadAxis.SDL_GAMEPAD_AXIS_LEFT_TRIGGER;
            case RIGHT_TRIGGER -> SDL_GamepadAxis.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER;
            case LEFT_BUMPER -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER;
            case RIGHT_BUMPER -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER;
            case DPAD_UP -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_DPAD_UP;
            case DPAD_DOWN -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_DPAD_DOWN;
            case DPAD_LEFT -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_DPAD_LEFT;
            case DPAD_RIGHT -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_DPAD_RIGHT;
            default -> -1;
        };
    }

    @Override
    public void applyGamePadMappingsFromBuffer(BufferedReader reader) {
        String s = reader.lines().collect(Collectors.joining());
        int i = SdlGamepad.SDL_AddGamepadMapping(s);
        Legacy4J.LOGGER.warn("Added SDL Controller Mappings: " + i + " Code");
        //      Legacy4J.LOGGER.warn("Mapping: " + s);
    }

}
