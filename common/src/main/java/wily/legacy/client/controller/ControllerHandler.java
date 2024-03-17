package wily.legacy.client.controller;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.InputType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;
import wily.legacy.LegacyMinecraft;
import wily.legacy.client.LegacyTip;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.util.ScreenUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public class ControllerHandler {
    protected static final Map<Integer, ControllerComponent> DEFAULT_CONTROLLER_BUTTONS_BY_KEY = new HashMap<>();
    public String connectedController = null;
    public boolean isCursorDisabled = false;
    public boolean canChangeSlidersValue = true;
    private final Minecraft minecraft;

    public ControllerHandler(Minecraft minecraft){
        this.minecraft = minecraft;
    }
    public static ControllerComponent getDefaultKeyMappingComponent(int i){
        ControllerComponent.init();
        return DEFAULT_CONTROLLER_BUTTONS_BY_KEY.get(i);
    }
    public static void tryDownloadAndApplyNewMappings(){
        try {
            applyGamePadMappingsFromBuffer(new BufferedReader(new InputStreamReader(new URL("https://raw.githubusercontent.com/mdqinc/SDL_GameControllerDB/master/gamecontrollerdb.txt").openStream())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void applyGamePadMappingsFromBuffer(BufferedReader reader) throws IOException {
        String s = reader.lines().collect(Collectors.joining());
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        ByteBuffer mappingsBuffer = ByteBuffer.allocateDirect(bytes.length + 1);
        mappingsBuffer.put(bytes);
        mappingsBuffer.rewind();
        GLFW.glfwUpdateGamepadMappings(mappingsBuffer);
        mappingsBuffer.clear();
        reader.close();
    }

    public void setup(){
        GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(),GLFW.GLFW_CURSOR,GLFW.GLFW_CURSOR_HIDDEN);
        tryDownloadAndApplyNewMappings();
        CompletableFuture.runAsync(()->{
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (minecraft.isRunning()) {
                        if (!GLFW.glfwJoystickIsGamepad(GLFW.GLFW_JOYSTICK_1)) {
                            if (connectedController != null) {
                                if (isCursorDisabled) enableCursor();
                                minecraft.getToasts().addToast(new LegacyTip(Component.translatable("legacy.controller.disconnected"), Component.literal(connectedController)).disappearTime(4500));
                                connectedController = null;
                            }
                            return;
                        }
                        if (connectedController == null)
                            minecraft.getToasts().addToast(new LegacyTip(Component.translatable("legacy.controller.detected"), Component.literal(connectedController = GLFW.glfwGetGamepadName(GLFW.GLFW_JOYSTICK_1))).disappearTime(4500));
                        minecraft.execute(() -> {
                            GLFWGamepadState gamepadState = GLFWGamepadState.calloc();
                            if (GLFW.glfwGetGamepadState(GLFW.GLFW_JOYSTICK_1, gamepadState))
                                componentsPressed(gamepadState);
                            gamepadState.free();
                        });
                    }
                }
            },0,1);
        }).exceptionally(t-> {
            LegacyMinecraft.LOGGER.warn(t.getMessage());
            return null;
        });
    }
    public void setPointerPos(double x, double y){
        GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(),minecraft.mouseHandler.xpos = x,minecraft.mouseHandler.ypos = y);
    }

    public synchronized void componentsPressed(GLFWGamepadState gamepadState) {

        for (ControllerComponent component : ControllerComponent.values()) {
            ComponentState state = component.componentState;
            state.update(gamepadState);
            if (minecraft.screen != null && !isCursorDisabled) {
                if (state.is(ControllerComponent.LEFT_STICK) && state instanceof ComponentState.Stick stick && state.pressed)
                    setPointerPos(minecraft.mouseHandler.xpos = (minecraft.mouseHandler.xpos() + stick.x * ((double) minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth()) * Math.pow(ScreenUtil.getLegacyOptions().interfaceSensitivity().get() * 0.9 + 0.3, 4.5)), minecraft.mouseHandler.ypos = (minecraft.mouseHandler.ypos() + stick.y * ((double) minecraft.getWindow().getScreenHeight() / minecraft.getWindow().getGuiScaledHeight()) * Math.pow(minecraft.options.sensitivity().get() * 0.9 + 0.3, 4.5)));

                if (state.is(ControllerComponent.DOWN_BUTTON) || state.is(ControllerComponent.UP_BUTTON) || state.is(ControllerComponent.LEFT_BUTTON)) {
                    if (state.pressed && state.canClick())
                        minecraft.screen.mouseClicked(getPointerX(), getPointerY(), state.is(ControllerComponent.LEFT_BUTTON) ? 1 : 0);
                    else if (state.released)
                        minecraft.screen.mouseReleased(getPointerX(), getPointerY(), state.is(ControllerComponent.LEFT_BUTTON) ? 1 : 0);
                }
            }

            if (state.is(ControllerComponent.START) && state.justPressed && minecraft.screen == null)
                minecraft.pauseGame(false);

            if (minecraft.screen != null) {
                if (minecraft.screen instanceof ControllerEvent e) e.componentTick(state);
                if (state.pressed && state.canClick()) {
                    this.minecraft.setLastInputType(InputType.KEYBOARD_ARROW);
                    minecraft.screen.afterKeyboardAction();
                }
                if (isCursorDisabled) simulateKeyAction(s-> state.is(ControllerComponent.DOWN_BUTTON) && state.onceClick(false),InputConstants.KEY_RETURN, state);
                simulateKeyAction(s-> s.is(ControllerComponent.RIGHT_BUTTON) && state.onceClick(true),InputConstants.KEY_ESCAPE, state);
                simulateKeyAction(s-> s.is(ControllerComponent.LEFT_BUTTON),InputConstants.KEY_X, state);
                simulateKeyAction(s->s.is(ControllerComponent.UP_BUTTON),InputConstants.KEY_O, state);
                simulateKeyAction(s->s.is(ControllerComponent.RIGHT_TRIGGER),InputConstants.KEY_W, state);
                simulateKeyAction(s->s.is(ControllerComponent.RIGHT_BUMPER),InputConstants.KEY_RBRACKET, state);
                simulateKeyAction(s->state.is(ControllerComponent.LEFT_BUMPER),InputConstants.KEY_LBRACKET, state);
                if (state.is(ControllerComponent.RIGHT_STICK) && state instanceof ComponentState.Stick stick && Math.abs(stick.y) > Math.abs(stick.x) && state.pressed && state.canClick())
                    minecraft.screen.mouseScrolled(getPointerX(), getPointerY(), 0, Math.signum(-stick.y));
                Predicate<Predicate<ComponentState.Stick>> isStickAnd = s -> state.is(ControllerComponent.LEFT_STICK) && state instanceof ComponentState.Stick stick && s.test(stick);
                if (isCursorDisabled) {
                    if (isStickAnd.test(s -> s.y < 0 && -s.y > Math.abs(s.x)) || state.is(ControllerComponent.DPAD_UP))
                        simulateKeyAction(InputConstants.KEY_UP, state);
                    if (isStickAnd.test(s -> s.y > 0 && s.y > Math.abs(s.x)) || state.is(ControllerComponent.DPAD_DOWN))
                        simulateKeyAction(InputConstants.KEY_DOWN, state);
                    if (isStickAnd.test(s -> s.x > 0 && s.x > Math.abs(s.y)) || state.is(ControllerComponent.DPAD_RIGHT))
                        simulateKeyAction(InputConstants.KEY_RIGHT, state);
                    if (isStickAnd.test(s -> s.x < 0 && -s.x > Math.abs(s.y)) || state.is(ControllerComponent.DPAD_LEFT))
                        simulateKeyAction(InputConstants.KEY_LEFT, state);
                }
            }


            if (minecraft.screen instanceof LegacyMenuAccess<?> a && !isCursorDisabled) {

                Predicate<Predicate<ComponentState.Stick>> isStickAnd = s -> state.is(ControllerComponent.LEFT_STICK) && state instanceof ComponentState.Stick stick && s.test(stick) && stick.getMagnitude() <= 0.65 && state.onceClick(true);
                if (state.pressed) {
                    if (isStickAnd.test(s -> s.y < 0 && -s.y > Math.abs(s.x)) || state.is(ControllerComponent.DPAD_UP) && state.canClick())
                        a.movePointerToSlotIn(ScreenDirection.UP);
                    if (isStickAnd.test(s -> s.y > 0 && s.y > Math.abs(s.x)) || state.is(ControllerComponent.DPAD_DOWN) && state.canClick())
                        a.movePointerToSlotIn(ScreenDirection.DOWN);
                    if (isStickAnd.test(s -> s.x > 0 && s.x > Math.abs(s.y)) || state.is(ControllerComponent.DPAD_RIGHT) && state.canClick())
                        a.movePointerToSlotIn(ScreenDirection.RIGHT);
                    if (isStickAnd.test(s -> s.x < 0 && -s.x > Math.abs(s.y)) || state.is(ControllerComponent.DPAD_LEFT) && state.canClick())
                        a.movePointerToSlotIn(ScreenDirection.LEFT);
                }else if (state.is(ControllerComponent.LEFT_STICK) && state.released) a.movePointerToSlot(a.getHoveredSlot());
            }

            KeyMapping.ALL.entrySet().stream().filter(k -> state.component.matches(k.getValue())).forEach(e -> {
                Screen screen;

                if (this.minecraft.screen == null || (screen = this.minecraft.screen) instanceof PauseScreen && !((PauseScreen) screen).showsPauseMenu()) {
                    if (gamepadState.buttons(GLFW.GLFW_GAMEPAD_BUTTON_START) == GLFW.GLFW_PRESS) {
                        e.getValue().setDown(false);
                    } else {
                        if (state.pressed) {
                            boolean valid = state.component.validKey.test(e.getValue(), state);
                            if (valid) e.getValue().setDown(true);
                            if (state.canClick()) KeyMapping.click(((LegacyKeyMapping)e.getValue()).getKey());
                        } else if (state.released) e.getValue().setDown(false);
                        if (e.getValue() == minecraft.options.keyTogglePerspective) state.onceClick(true);
                    }
                }
            });
        }
    }
    public void simulateKeyAction(int key, ComponentState state){
        if (state.pressed && state.canClick()) minecraft.screen.keyPressed(key, 0, 0);
        else if (state.released) minecraft.screen.keyReleased(key, 0, 0);
    }
    public void simulateKeyAction(Predicate<ComponentState> canSimulate, int key, ComponentState state){
        boolean clicked = state.pressed && state.canClick();
        boolean simulate = canSimulate.test(state);
        if (simulate){
            if (clicked && simulate) minecraft.screen.keyPressed(key, 0, 0);
            else if (state.released) minecraft.screen.keyReleased(key, 0, 0);
        }
    }
    public double getPointerX(){
        return minecraft.mouseHandler.xpos() * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth();
    }
    public double getPointerY(){
        return minecraft.mouseHandler.ypos() * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight();
    }

    public ComponentState getButtonState(ControllerComponent button){
        return button.componentState;
    }
    public void disableCursor(){
        GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(),GLFW.GLFW_CURSOR,GLFW.GLFW_CURSOR_HIDDEN);
        minecraft.mouseHandler.xpos = -1;
        minecraft.mouseHandler.ypos = -1;
        //minecraft.mouseHandler.mouseGrabbed = !minecraft.mouseHandler.mouseGrabbed;
        isCursorDisabled = true;
    }
    public void enableCursor(){
        minecraft.mouseHandler.xpos = minecraft.getWindow().getScreenWidth() / 2d;
        minecraft.mouseHandler.ypos = minecraft.getWindow().getScreenHeight() / 2d;
        isCursorDisabled = false;
    }
}
