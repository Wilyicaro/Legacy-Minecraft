package wily.legacy.client.controller;

import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.FloatByReference;
import dev.isxander.sdl3java.api.SdlInit;
import dev.isxander.sdl3java.api.SdlSubSystemConst;
import dev.isxander.sdl3java.api.gamepad.*;
import dev.isxander.sdl3java.api.joystick.SDL_JoystickID;
import dev.isxander.sdl3java.api.joystick.SdlJoystick;
import dev.isxander.sdl3java.api.version.SdlVersionConst;
import dev.isxander.sdl3java.jna.SdlNativeLibraryLoader;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.apache.commons.io.FileUtils;
import wily.factoryapi.FactoryAPIClient;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.base.client.MinecraftAccessor;
import wily.legacy.Legacy4J;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.screen.ConfirmationScreen;
import wily.legacy.client.screen.LegacyLoadingScreen;
import wily.legacy.client.screen.OverlayPanelScreen;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


public class SDLControllerHandler implements Controller.Handler {
    private SDL_JoystickID[] actualIds = new SDL_JoystickID[0];
    private boolean init = false;

    private static final SDLControllerHandler INSTANCE = new SDLControllerHandler();
    public static final String SDL_VERSION = SdlVersionConst.SDL_MAJOR_VERSION + "." + SdlVersionConst.SDL_MINOR_VERSION+ "." + SdlVersionConst.SDL_MICRO_VERSION + "." + SdlVersionConst.SDL_COMMIT;

    public static final String nativesMainURLFormat = "https://maven.isxander.dev/releases/dev/isxander/libsdl4j-natives/%s/%s";
    public NativesStatus natives;

    public static final Component TITLE = Component.literal("SDL3 (isXander's libsdl4j)");
    @Override
    public Component getName() {
        return TITLE;
    }

    public static SDLControllerHandler getInstance() {
        return INSTANCE;
    }

    public void fallback() {
        Legacy4J.LOGGER.warn("{} isn't supported in this system. GLFW will be used instead.", getName());
        LegacyOptions.selectedControllerHandler.set(GLFWControllerHandler.getInstance());
        LegacyOptions.selectedControllerHandler.save();
        init = true;
    }

    public void init() {
        if (!init) {
            Minecraft minecraft = Minecraft.getInstance();
            if (natives == null) {
                natives = getNativesStatus(minecraft);
                if (natives.file() == null) {
                    fallback();
                }
            }

            if (!natives.isPojav()) {
                if (!natives.file().exists()) {
                    LegacyOptions.selectedControllerHandler.set(GLFWControllerHandler.getInstance());
                    LegacyOptions.selectedControllerHandler.save();
                    FactoryAPIClient.SECURE_EXECUTOR.executeNowIfPossible(() -> openNativesScreen(minecraft), () -> !(minecraft.screen instanceof OverlayPanelScreen) && MinecraftAccessor.getInstance().hasGameLoaded());
                    init = true;
                    return;
                } else try {
                    SdlNativeLibraryLoader.loadLibSDL3FromFilePathNow(natives.file().getPath());
                } catch (Exception | UnsatisfiedLinkError e) {
                    Legacy4J.LOGGER.warn("Failed to load {} natives: {}", getName(), e.getMessage());
                    init = true;
                    return;
                }
            }

            if (!SdlInit.SDL_Init(SdlSubSystemConst.SDL_INIT_JOYSTICK | SdlSubSystemConst.SDL_INIT_GAMEPAD)) {
                Legacy4J.LOGGER.warn("SDL Game Controller failed to start!");
                fallback();
                return;
            }
            tryDownloadAndApplyNewMappings();
            init = true;
        }
    }

    public void openNativesScreen(Minecraft minecraft) {
        Screen s = minecraft.screen;
        minecraft.setScreen(new ConfirmationScreen(s, Component.translatable("legacy.menu.download_natives",getName()), Controller.Handler.DOWNLOAD_MESSAGE, b -> {
            Stocker<Long> fileSize = new Stocker<>(1L);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            LegacyLoadingScreen screen = new LegacyLoadingScreen(Controller.Handler.DOWNLOADING_NATIVES, CommonComponents.EMPTY) {
                @Override
                public void tick() {
                    if (getProgress() == 100) {
                        LegacyOptions.selectedControllerHandler.set(getInstance());
                        LegacyOptions.CLIENT_STORAGE.save();
                        onClose();
                        return;
                    }
                    setProgress(natives.file().exists() ? Math.round(Math.min(1,FileUtils.sizeOf(natives.file()) / (float) fileSize.get()) * 80) : 0);
                    super.tick();
                }

                @Override
                public void onClose() {
                    minecraft.setScreen(s);
                    LegacyLoadingScreen.closeExecutor(executor);
                }

                @Override
                public boolean shouldCloseOnEsc() {
                    return true;
                }
            };
            minecraft.setScreen(screen);
            CompletableFuture.runAsync(()->{
                try {
                    fileSize.set(getNativesURI().toURL().openConnection().getContentLengthLong());
                    FileUtils.copyURLToFile(getNativesURI().toURL(), natives.file());
                    screen.setLoadingHeader(Controller.Handler.LOADING_NATIVES);
                    screen.setProgress(100);
                    init = false;
                } catch (IOException | URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }, executor);
        }) {
            @Override
            public void onClose() {
                super.onClose();
                init = false;
            }
        });
    }

    public static URI getNativesURI() throws URISyntaxException {
        return new URI(nativesMainURLFormat.formatted(SDL_VERSION, getNativesFileName()));
    }

    public static NativesStatus getNativesStatus(Minecraft minecraft) {
        String fileName = getNativesFileName();
        boolean pojav = false;

        if (System.getenv("POJAV_NATIVEDIR") != null){
            Legacy4J.LOGGER.warn("Pojav-based Launcher Detected.");
            pojav = true;
        }

        return new NativesStatus(fileName == null ? null : new File(minecraft.gameDirectory, "natives/" + fileName), pojav);
    }

    public static String getNativesFileName(){
        try {
            Class.forName("com.sun.jna.Native");
        } catch (ClassNotFoundException e) {
            Legacy4J.LOGGER.warn("JNA wasn't found.");
            return null;
        }
        String arch = System.getProperty("os.arch");
        String base = switch (Util.getPlatform()){
            case WINDOWS -> arch.contains("64") ? "libsdl4j-natives-%s-windows-x86_64.dll" : "libsdl4j-natives-%s-windows-x86.dll";
            case OSX -> "libsdl4j-natives-%s-macos-universal.dylib";
            case LINUX -> arch.contains("aarch") || arch.contains("arm") ? "libsdl4j-natives-%s-linux-aarch64.so" : "libsdl4j-natives-%s-linux-x86_64.so";
            default -> null;
        };
        return base != null ? base.formatted(SDL_VERSION) : null;
    }

    @Override
    public boolean update() {
        if (!init) return false;
        SdlGamepad.SDL_UpdateGamepads();
        SdlJoystick.SDL_UpdateJoysticks();
        actualIds = SdlGamepad.SDL_GetGamepads();
        return true;
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
                return switch (type) {
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
                return SdlGamepad.SDL_GetGamepadButton(controller,i);
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
            public void rumble(char low_frequency_rumble, char high_frequency_rumble, int duration_ms) {
                SdlGamepad.SDL_RumbleGamepad(controller,low_frequency_rumble,high_frequency_rumble,duration_ms);
            }
            @Override
            public void rumbleTriggers(char left_rumble, char right_rumble, int duration_ms) {
                SdlGamepad.SDL_RumbleGamepadTriggers(controller,left_rumble,right_rumble,duration_ms);
            }

            @Override
            public int getTouchpadsCount() {
                return SdlGamepad.SDL_GetNumGamepadTouchpads(controller);
            }

            @Override
            public int getTouchpadFingersCount(int touchpad) {
                return SdlGamepad.SDL_GetNumGamepadTouchpadFingers(controller,touchpad);
            }

            @Override
            public boolean hasFingerInTouchpad(int touchpad, int finger, Byte state, Float x, Float y, Float pressure) {
                return SdlGamepad.SDL_GetGamepadTouchpadFinger(controller,touchpad,finger,state == null ? null : new ByteByReference(state),x == null ? null : new FloatByReference(x),y == null ? null : new FloatByReference(y),pressure == null ? null : new FloatByReference(pressure));
            }

            @Override
            public boolean hasButton(ControllerBinding.Button button) {
                int index = getButtonIndex(button);
                return index != -1 && SdlGamepad.SDL_GamepadHasButton(controller, index);
            }

            @Override
            public boolean hasAxis(ControllerBinding.Axis axis) {
                int index = getAxisIndex(axis);
                return index != -1 && SdlGamepad.SDL_GamepadHasAxis(controller, index);
            }

            @Override
            public void disconnect(ControllerManager manager) {
                Controller.super.disconnect(manager);
                SdlGamepad.SDL_CloseGamepad(controller);
            }

            @Override
            public Handler getHandler() {
                return SDLControllerHandler.this;
            }
        };
    }

    @Override
    public boolean isValidController(int jid) {
        if (actualIds.length <= jid) return false;
        return SdlGamepad.SDL_IsGamepad(actualIds[jid]);
    }

    @Override
    public int getButtonIndex(ControllerBinding.Button button) {
        return switch (button) {
            case DOWN-> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_SOUTH;
            case RIGHT -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_EAST;
            case LEFT-> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_WEST;
            case UP -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_NORTH;
            case BACK -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_BACK;
            case GUIDE -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_GUIDE;
            case START -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_START;
            case LEFT_STICK-> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_LEFT_STICK;
            case RIGHT_STICK -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_RIGHT_STICK;
            case LEFT_BUMPER -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER;
            case RIGHT_BUMPER -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER;
            case DPAD_UP -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_DPAD_UP;
            case DPAD_DOWN -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_DPAD_DOWN;
            case DPAD_LEFT -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_DPAD_LEFT;
            case DPAD_RIGHT -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_DPAD_RIGHT;
            case TOUCHPAD -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_TOUCHPAD;
            case CAPTURE -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_MISC1;
            case LSL -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_LEFT_PADDLE1;
            case LSR -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_LEFT_PADDLE2;
            case RSL -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_RIGHT_PADDLE2;
            case RSR -> SDL_GamepadButton.SDL_GAMEPAD_BUTTON_RIGHT_PADDLE1;
        };
    }

    @Override
    public int getAxisIndex(ControllerBinding.Axis axis) {
        return switch (axis) {
            case LEFT_STICK_X -> SDL_GamepadAxis.SDL_GAMEPAD_AXIS_LEFTX;
            case LEFT_STICK_Y -> SDL_GamepadAxis.SDL_GAMEPAD_AXIS_LEFTY;
            case RIGHT_STICK_X -> SDL_GamepadAxis.SDL_GAMEPAD_AXIS_RIGHTX;
            case RIGHT_STICK_Y -> SDL_GamepadAxis.SDL_GAMEPAD_AXIS_RIGHTY;
            case LEFT_TRIGGER -> SDL_GamepadAxis.SDL_GAMEPAD_AXIS_LEFT_TRIGGER;
            case RIGHT_TRIGGER -> SDL_GamepadAxis.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER;
        };
    }

    @Override
    public void applyGamePadMappingsFromBuffer(BufferedReader reader) {
        String s = reader.lines().collect(Collectors.joining());
        int i = SdlGamepad.SDL_AddGamepadMapping(s);
        Legacy4J.LOGGER.warn("Added SDL Controller Mappings: {} Code", i);
    }


    public record NativesStatus(File file, boolean isPojav) {
    }

}
