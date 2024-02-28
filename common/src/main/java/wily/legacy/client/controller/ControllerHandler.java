package wily.legacy.client.controller;

import com.mojang.blaze3d.platform.InputConstants;
import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;
import wily.legacy.LegacyMinecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;


public class ControllerHandler {
    public static final Map<Integer, ControllerComponent> DEFAULT_CONTROLLER_BUTTONS_BY_KEY = new HashMap<>();
    public String connectedController = "";
    public boolean isCursorDisabled = false;
    private final Minecraft minecraft;
    public final ControllerManager manager = new ControllerManager();

    public ControllerHandler(Minecraft minecraft){
        this.minecraft = minecraft;
        ControllerComponent.init();
        manager.initSDLGamepad();
    }
    public void setup(){
        GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(),GLFW.GLFW_CURSOR,GLFW.GLFW_CURSOR_HIDDEN);
        CompletableFuture.runAsync(()->{
            while (minecraft.isRunning()){
                ControllerState currState = manager.getState(0);

                if (!currState.isConnected){
                    connectedController = "";
                    continue;
                }
                connectedController = currState.controllerType;
                minecraft.executeBlocking(()-> componentsPressed(currState));
            }
        }).exceptionally(t-> {
            LegacyMinecraft.LOGGER.warn(t.getMessage());
            return null;
        });
    }


    public synchronized void componentsPressed(ControllerState controllerState) {
        if (controllerState.startJustPressed) {
            minecraft.pauseGame(false);
        }
        for (ControllerComponent component : ControllerComponent.values()) {
            ComponentState state = component.componentState;
            state.update(controllerState);
            boolean wasScreen = false;
            if (state.pressed && state.canPress()) {
                if ((state.is(ControllerComponent.RIGHT_BUMPER) || state.is(ControllerComponent.LEFT_BUMPER)) && minecraft.player != null && minecraft.screen == null)
                    minecraft.player.getInventory().swapPaint(state.is(ControllerComponent.RIGHT_BUMPER) ? -1 : 1);
                if (minecraft.screen != null) {
                    wasScreen = true;
                    if (state.is(ControllerComponent.DOWN_BUTTON))
                        minecraft.screen.keyPressed(InputConstants.KEY_RETURN, 0, 0);
                    else if (state.is(ControllerComponent.RIGHT_BUTTON))
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
                    Predicate<Predicate<ComponentState.Stick>> isStickAnd = s -> state.is(ControllerComponent.LEFT_STICK) && state instanceof ComponentState.Stick stick && s.test(stick);
                    if (isStickAnd.test(s -> s.y > 0 && s.y > Math.abs(s.x)) || state.is(ControllerComponent.DPAD_UP))
                        minecraft.screen.keyPressed(InputConstants.KEY_UP, 0, 0);
                    if (isStickAnd.test(s -> s.y < 0 && Math.abs(s.y) > Math.abs(s.x)) || state.is(ControllerComponent.DPAD_DOWN))
                        minecraft.screen.keyPressed(InputConstants.KEY_DOWN, 0, 0);
                    if (isStickAnd.test(s -> s.x > 0 && s.x > Math.abs(s.y)) || state.is(ControllerComponent.DPAD_RIGHT))
                        minecraft.screen.keyPressed(InputConstants.KEY_RIGHT, 0, 0);
                    if (isStickAnd.test(s -> s.x < 0 && Math.abs(s.x) > Math.abs(s.y)) || state.is(ControllerComponent.DPAD_LEFT))
                        minecraft.screen.keyPressed(InputConstants.KEY_LEFT, 0, 0);
                }
            }
            if (minecraft.screen instanceof AbstractContainerScreen<?> && !minecraft.mouseHandler.isMouseGrabbed()) {
                if (state.is(ControllerComponent.LEFT_STICK) && state instanceof ComponentState.Stick stick && state.pressed) {
                    GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(), minecraft.mouseHandler.xpos = (minecraft.mouseHandler.xpos() + stick.x * ((double)minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth()) * 5), minecraft.mouseHandler.ypos = (minecraft.mouseHandler.ypos() - stick.y * ((double)minecraft.getWindow().getScreenHeight() / minecraft.getWindow().getGuiScaledHeight()) * 5));
                }
                if (state.is(ControllerComponent.DOWN_BUTTON) || state.is(ControllerComponent.UP_BUTTON) || state.is(ControllerComponent.LEFT_BUTTON)) {
                    if (state.pressed && state.canPress())
                        minecraft.screen.mouseClicked(getPointerX(), getPointerY(), state.is(ControllerComponent.LEFT_BUTTON) ? 1 : 0);
                    else if (state.released) minecraft.screen.mouseReleased(getPointerX(), getPointerY(), state.is(ControllerComponent.LEFT_BUTTON) ? 1 : 0);
                }
            }
            if (!wasScreen)
                KeyMapping.ALL.entrySet().stream().filter(k -> state.component.matches(k.getValue())).forEach(e -> {
                    Screen screen;
                    if (this.minecraft.screen == null || (screen = this.minecraft.screen) instanceof PauseScreen && !((PauseScreen) screen).showsPauseMenu()) {
                        if (controllerState.start) {
                            e.getValue().setDown(false);
                        } else {
                            if (state.pressed) {
                                boolean valid = state.component.validKey.test(e.getValue(), controllerState);
                                if (state.canPress()) e.getValue().setDown(valid);
                                if (state.canPress() && valid) KeyMapping.click(InputConstants.getKey(e.getValue().saveString()));
                            } else if (state.released) e.getValue().setDown(false);
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
        minecraft.mouseHandler.mouseGrabbed = false;
        isCursorDisabled = true;
    }
}
