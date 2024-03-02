package wily.legacy.client.controller;

import com.mojang.blaze3d.platform.InputConstants;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;


public class ControllerHandler {
    protected static final Map<Integer, ControllerComponent> DEFAULT_CONTROLLER_BUTTONS_BY_KEY = new HashMap<>();
    public String connectedController = "";
    public boolean isCursorDisabled = false;
    private final Minecraft minecraft;

    public ControllerHandler(Minecraft minecraft){
        this.minecraft = minecraft;
    }
    public static ControllerComponent getDefaultKeyMappingComponent(int i){
        ControllerComponent.init();
        return DEFAULT_CONTROLLER_BUTTONS_BY_KEY.get(i);
    }

    public void setup(){
        GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(),GLFW.GLFW_CURSOR,GLFW.GLFW_CURSOR_HIDDEN);

        CompletableFuture.runAsync(()->{
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (minecraft.isRunning()) {
                        if (!GLFW.glfwJoystickIsGamepad(GLFW.GLFW_JOYSTICK_1)) {
                            connectedController = "";
                            return;
                        }
                        if (connectedController.isEmpty())
                            minecraft.getToasts().addToast(new LegacyTip(Component.translatable("legacy.controller.detected"), Component.literal(connectedController = GLFW.glfwGetGamepadName(GLFW.GLFW_JOYSTICK_1))));
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

            if (minecraft.screen instanceof LegacyMenuAccess<?> a && !minecraft.mouseHandler.isMouseGrabbed()) {

                if (state.is(ControllerComponent.LEFT_STICK) && state instanceof ComponentState.Stick stick && state.pressed && !isCursorDisabled)
                    setPointerPos(minecraft.mouseHandler.xpos = (minecraft.mouseHandler.xpos() + stick.x * ((double)minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth()) * Math.pow(minecraft.options.sensitivity().get() * 0.9 + 0.3,4.5)), minecraft.mouseHandler.ypos = (minecraft.mouseHandler.ypos() + stick.y * ((double)minecraft.getWindow().getScreenHeight() / minecraft.getWindow().getGuiScaledHeight()) * Math.pow(minecraft.options.sensitivity().get() * 0.9 + 0.3,4.5)));

                if (state.is(ControllerComponent.DOWN_BUTTON) || state.is(ControllerComponent.UP_BUTTON) || state.is(ControllerComponent.LEFT_BUTTON)) {
                    if (state.pressed && state.canClick())
                        minecraft.screen.mouseClicked(getPointerX(), getPointerY(), state.is(ControllerComponent.LEFT_BUTTON) ? 1 : 0);
                    else if (state.released) minecraft.screen.mouseReleased(getPointerX(), getPointerY(), state.is(ControllerComponent.LEFT_BUTTON) ? 1 : 0);
                }
                Predicate<Predicate<ComponentState.Stick>> isStickAnd = s -> state.is(ControllerComponent.LEFT_STICK) && state instanceof ComponentState.Stick stick && s.test(stick) && state.canClick();
                if (state.pressed) {
                    if (isStickAnd.test(s -> s.y < 0 && -s.y > Math.abs(s.x)) || state.is(ControllerComponent.DPAD_UP) && state.canClick())
                        a.movePointerToSlotIn(ScreenDirection.UP);
                    if (isStickAnd.test(s -> s.y > 0 && s.y > Math.abs(s.x)) || state.is(ControllerComponent.DPAD_DOWN) && state.canClick())
                        a.movePointerToSlotIn(ScreenDirection.DOWN);
                    if (isStickAnd.test(s -> s.x > 0 && s.x > Math.abs(s.y)) || state.is(ControllerComponent.DPAD_RIGHT) && state.canClick())
                        a.movePointerToSlotIn(ScreenDirection.RIGHT);
                    if (isStickAnd.test(s -> s.x < 0 && -s.x > Math.abs(s.y)) || state.is(ControllerComponent.DPAD_LEFT) && state.canClick())
                        a.movePointerToSlotIn(ScreenDirection.LEFT);
                }else if ((state.is(ControllerComponent.LEFT_STICK) || state.component.ordinal() >= 15) && state.released)a.movePointerToSlot(a.getHoveredSlot());
            }
            if (state.pressed && state.canClick()) {
                if (state.is(ControllerComponent.START) && state.justPressed)
                    minecraft.pauseGame(false);
                if ((state.is(ControllerComponent.RIGHT_BUMPER) || state.is(ControllerComponent.LEFT_BUMPER)) && minecraft.player != null && minecraft.screen == null)
                    minecraft.player.getInventory().swapPaint(state.is(ControllerComponent.RIGHT_BUMPER) ? -1 : 1);
                if (minecraft.screen != null) {
                    if (state.is(ControllerComponent.DOWN_BUTTON) && state.onceClick(false) && isCursorDisabled)
                        minecraft.screen.keyPressed(InputConstants.KEY_RETURN, 0, 0);
                    else if (state.is(ControllerComponent.RIGHT_BUTTON) && state.onceClick(true))
                        minecraft.screen.keyPressed(InputConstants.KEY_ESCAPE, 0, 0);
                    else if (state.is(ControllerComponent.LEFT_BUTTON))
                        minecraft.screen.keyPressed(InputConstants.KEY_X, 0, 0);
                    else if (state.is(ControllerComponent.UP_BUTTON))
                        minecraft.screen.keyPressed(InputConstants.KEY_O, 0, 0);
                    else if (state.is(ControllerComponent.RIGHT_TRIGGER))
                        minecraft.screen.keyPressed(InputConstants.KEY_W, 0, 0);
                    else if (state.is(ControllerComponent.RIGHT_BUMPER))
                        minecraft.screen.keyPressed(InputConstants.KEY_RBRACKET, 0, 0);
                    else if (state.is(ControllerComponent.LEFT_BUMPER))
                        minecraft.screen.keyPressed(InputConstants.KEY_LBRACKET, 0, 0);
                    if (state.is(ControllerComponent.RIGHT_STICK) && state instanceof ComponentState.Stick stick && Math.abs(stick.y) > Math.abs(stick.x) && state.pressed && state.canClick())
                        minecraft.screen.mouseScrolled(getPointerX(), getPointerY(), 0, Math.signum(-stick.y));
                    Predicate<Predicate<ComponentState.Stick>> isStickAnd = s -> state.is(ControllerComponent.LEFT_STICK) && state instanceof ComponentState.Stick stick && s.test(stick);
                    if (isCursorDisabled) {
                        if (isStickAnd.test(s -> s.y < 0 && -s.y > Math.abs(s.x)) || state.is(ControllerComponent.DPAD_UP))
                            minecraft.screen.keyPressed(InputConstants.KEY_UP, 0, 0);
                        if (isStickAnd.test(s -> s.y > 0 && s.y > Math.abs(s.x)) || state.is(ControllerComponent.DPAD_DOWN))
                            minecraft.screen.keyPressed(InputConstants.KEY_DOWN, 0, 0);
                        if (isStickAnd.test(s -> s.x > 0 && s.x > Math.abs(s.y)) || state.is(ControllerComponent.DPAD_RIGHT))
                            minecraft.screen.keyPressed(InputConstants.KEY_RIGHT, 0, 0);
                        if (isStickAnd.test(s -> s.x < 0 && -s.x > Math.abs(s.y)) || state.is(ControllerComponent.DPAD_LEFT))
                            minecraft.screen.keyPressed(InputConstants.KEY_LEFT, 0, 0);
                    }
                }
            }

            KeyMapping.ALL.entrySet().stream().filter(k -> state.component.matches(k.getValue())).forEach(e -> {
                Screen screen;

                if (this.minecraft.screen == null || (screen = this.minecraft.screen) instanceof PauseScreen && !((PauseScreen) screen).showsPauseMenu()) {
                    if (gamepadState.buttons(GLFW.GLFW_GAMEPAD_BUTTON_START) == GLFW.GLFW_PRESS) {
                        e.getValue().setDown(false);
                    } else {
                        if (state.pressed) {
                            boolean valid = state.component.validKey.test(e.getValue(), state);
                            if (state.timePressed == 0) e.getValue().setDown(valid);
                            if (state.canClick() && valid) KeyMapping.click(InputConstants.getKey(e.getValue().saveString()));
                        } else if (state.released) e.getValue().setDown(false);
                        if (e.getValue() == minecraft.options.keyTogglePerspective) state.onceClick(true);
                    }
                }
            });
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
