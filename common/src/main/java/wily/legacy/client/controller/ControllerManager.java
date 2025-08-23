package wily.legacy.client.controller;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.InputType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.player.LegacyPlayerInfo;
import wily.legacy.util.ScreenUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;


public  class ControllerManager {
    public static final Map<Integer, ControllerBinding> DEFAULT_CONTROLLER_BUTTONS_BY_KEY = new HashMap<>();
    public Controller connectedController = null;
    public boolean isCursorDisabled = false;
    public boolean resetCursor = false;
    public boolean canChangeSlidersValue = true;
    final Minecraft minecraft;
    public static final List<Controller.Handler> handlers = List.of(Controller.Handler.EMPTY, GLFWControllerHandler.getInstance(), SDLControllerHandler.getInstance());

    public static Controller.Handler handlerById(String id){
        try {
            int v = Integer.parseInt(id);
            return v == 0 ? GLFWControllerHandler.getInstance() : v == 1 ? SDLControllerHandler.getInstance() : Controller.Handler.EMPTY;
        } catch (NumberFormatException i) {
        }
        for (Controller.Handler handler : handlers) {
            if (handler.getId().equals(id)) return handler;
        }
        return Controller.Handler.EMPTY;
    }

    public static final Component CONTROLLER_DETECTED = Component.translatable("legacy.controller.detected");
    public static final Component CONTROLLER_DISCONNECTED = Component.translatable("legacy.controller.disconnected");

    public ControllerManager(Minecraft minecraft){
        this.minecraft = minecraft;
    }

    public static Controller.Handler getHandler() {
        return handlers.get(ScreenUtil.getLegacyOptions().selectedControllerHandler().get());
    }

    public static void updatePlayerCamera(BindingState.Axis stick, Controller handler){
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.mouseHandler.isMouseGrabbed() || !minecraft.isWindowActive() || !stick.pressed || minecraft.player == null) return;
        double f = Math.pow(minecraft.options.sensitivity().get() * (double)0.6f + (double)0.2f,3) * 7.5f * (minecraft.player.isScoping() ? 0.125: 1.0);
        minecraft.player.turn(sqr(stick.getSmoothX()) * f,sqr(stick.getSmoothY()) * f * (ScreenUtil.getLegacyOptions().invertYController().get() ? -1 : 1));
    }

    public static float sqr(float f){
        return f * f * Math.signum(f);
    }

    public void setup(){
        GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(),GLFW.GLFW_CURSOR,GLFW.GLFW_CURSOR_HIDDEN);
        getHandler().init();

        CompletableFuture.runAsync(()-> new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (minecraft.isRunning() && getHandler().update()) {
                    if (!getHandler().isValidController(ScreenUtil.getLegacyOptions().selectedController().get())) {
                        if (connectedController != null) connectedController.disconnect(ControllerManager.this);
                        return;
                    }
                    if (connectedController == null && (connectedController = getHandler().getController(ScreenUtil.getLegacyOptions().selectedController().get())) != null) connectedController.connect(ControllerManager.this);
                    if (connectedController == null) return;
                    minecraft.execute(() -> getHandler().setup(ControllerManager.this));
                }
            }
        },0,1)).exceptionally(t-> {
            Legacy4J.LOGGER.warn(t.getMessage());
            return null;
        });
    }
    public void setPointerPos(double x, double y){
        Window window = minecraft.getWindow();
        GLFW.glfwSetCursorPos(window.getWindow(),minecraft.mouseHandler.xpos = Math.max(0,Math.min(x,window.getScreenWidth())),minecraft.mouseHandler.ypos = Math.max(0,Math.min(y,window.getScreenHeight())));
    }
    public synchronized void updateBindings() {
        updateBindings(minecraft.isWindowActive() ? connectedController : Controller.EMPTY);
    }

    public synchronized void updateBindings(Controller controller) {
        if (minecraft.screen != null) Controller.Event.of(minecraft.screen).controllerTick(controller);
        if (LegacyTipManager.getActualTip() != null) LegacyTipManager.getActualTip().controllerTick(controller);

        for (ControllerBinding binding : ControllerBinding.values()) {
            if (controller == null) break;
            BindingState state = binding.bindingState;
            state.update(controller);
            if (LegacyTipManager.getActualTip() != null) LegacyTipManager.getActualTip().bindingStateTick(state);
            if (getCursorMode() == 0 && state.pressed && !isCursorDisabled) disableCursor();

            if (minecraft.player != null && minecraft.getConnection() != null && controller.hasLED() && minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID()) instanceof LegacyPlayerInfo i){
                float[] colors = Legacy4JClient.getVisualPlayerColor(i);
                controller.setLED((byte) (colors[0] * 255),(byte) (colors[1] * 255),(byte) (colors[2] * 255));
            }

            if (state.is(ControllerBinding.START) && state.justPressed)
                if (minecraft.screen == null) minecraft.pauseGame(false);
                else if (minecraft.screen instanceof AbstractContainerScreen<?> || minecraft.screen instanceof PauseScreen) minecraft.screen.onClose();

            s : if (minecraft.screen != null) {
                if (state.pressed && state.canClick()) {
                    this.minecraft.setLastInputType(InputType.KEYBOARD_ARROW);
                    minecraft.screen.afterKeyboardAction();
                }
                Controller.Event.of(minecraft.screen).bindingStateTick(state);
                if (minecraft.screen == null) break s;
                ControllerBinding cursorComponent = ((LegacyKeyMapping) Legacy4JClient.keyToggleCursor).getBinding();
                if (cursorComponent != null && state.is(cursorComponent) && state.canClick()) toggleCursor();
                if (isCursorDisabled) simulateKeyAction(s-> state.is(ControllerBinding.DOWN_BUTTON) && (minecraft.screen instanceof Controller.Event e && !e.onceClickBindings() || state.onceClick(true)),InputConstants.KEY_RETURN, state);
                simulateKeyAction(s-> s.is(ControllerBinding.RIGHT_BUTTON) && state.onceClick(true),InputConstants.KEY_ESCAPE, state);
                simulateKeyAction(s-> s.is(ControllerBinding.LEFT_BUTTON),InputConstants.KEY_X, state);
                simulateKeyAction(s->s.is(ControllerBinding.UP_BUTTON),InputConstants.KEY_O, state);
                simulateKeyAction(s->s.is(ControllerBinding.RIGHT_TRIGGER),InputConstants.KEY_W, state);
                simulateKeyAction(s->s.is(ControllerBinding.LEFT_TRIGGER),InputConstants.KEY_PAGEUP, state);
                simulateKeyAction(s->s.is(ControllerBinding.RIGHT_TRIGGER),InputConstants.KEY_PAGEDOWN, state);
                simulateKeyAction(s->s.is(ControllerBinding.RIGHT_BUMPER),InputConstants.KEY_RBRACKET, state);
                simulateKeyAction(s->s.is(ControllerBinding.LEFT_BUMPER),InputConstants.KEY_LBRACKET, state);
                if (state.is(ControllerBinding.RIGHT_STICK) && state instanceof BindingState.Axis stick && Math.abs(stick.y) > Math.abs(stick.x) && state.pressed && state.canClick())
                    minecraft.screen.mouseScrolled(getPointerX(), getPointerY(), 0, Math.signum(-stick.y));
                Predicate<Predicate<BindingState.Axis>> isStickAnd = s -> state.is(ControllerBinding.LEFT_STICK) && state instanceof BindingState.Axis stick && s.test(stick);
                if (isCursorDisabled) {
                    if (isStickAnd.test(s -> s.y < 0 && -s.y > Math.abs(s.x)) || state.is(ControllerBinding.DPAD_UP))
                        simulateKeyAction(InputConstants.KEY_UP, state);
                    if (isStickAnd.test(s -> s.y > 0 && s.y > Math.abs(s.x)) || state.is(ControllerBinding.DPAD_DOWN))
                        simulateKeyAction(InputConstants.KEY_DOWN, state);
                    if (isStickAnd.test(s -> s.x > 0 && s.x > Math.abs(s.y)) || state.is(ControllerBinding.DPAD_RIGHT))
                        simulateKeyAction(InputConstants.KEY_RIGHT, state);
                    if (isStickAnd.test(s -> s.x < 0 && -s.x > Math.abs(s.y)) || state.is(ControllerBinding.DPAD_LEFT))
                        simulateKeyAction(InputConstants.KEY_LEFT, state);
                }
            }

            if (minecraft.screen != null && !isCursorDisabled) {
                if (state.is(ControllerBinding.LEFT_STICK) && state instanceof BindingState.Axis stick && state.pressed)
                    setPointerPos(minecraft.mouseHandler.xpos() + stick.x * ((double) minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth())  * ScreenUtil.getLegacyOptions().interfaceSensitivity().get() / 2,minecraft.mouseHandler.ypos() + stick.y * ((double) minecraft.getWindow().getScreenHeight() / minecraft.getWindow().getGuiScaledHeight()) * ScreenUtil.getLegacyOptions().interfaceSensitivity().get() / 2);

                if (state.is(ControllerBinding.LEFT_TRIGGER) && minecraft.screen instanceof LegacyMenuAccess<?> m && m.getMenu().getCarried().getCount() > 1){
                    if (state.justPressed) minecraft.screen.mouseClicked(getPointerX(), getPointerY(), 0);
                    else if (state.released) minecraft.screen.mouseReleased(getPointerX(), getPointerY(),0);
                    if (state.pressed) minecraft.screen.mouseDragged(getPointerX(), getPointerY(), 0,0,0);
                }
                if (state.is(ControllerBinding.DOWN_BUTTON) || state.is(ControllerBinding.UP_BUTTON) || state.is(ControllerBinding.LEFT_BUTTON)) {
                    if (state.pressed && state.onceClick(true))
                        minecraft.screen.mouseClicked(getPointerX(), getPointerY(), state.is(ControllerBinding.LEFT_BUTTON) ? 1 : 0);
                    else if (state.released)
                        minecraft.screen.mouseReleased(getPointerX(), getPointerY(), state.is(ControllerBinding.LEFT_BUTTON) ? 1 : 0);
                }
            }

            if (minecraft.screen instanceof LegacyMenuAccess<?> a && !isCursorDisabled) {
                if (state.pressed && state.canClick()) {
                    if (state.is(ControllerBinding.DPAD_UP))
                        a.movePointerToSlotIn(ScreenDirection.UP);
                    else if (state.is(ControllerBinding.DPAD_DOWN))
                        a.movePointerToSlotIn(ScreenDirection.DOWN);
                    else if (state.is(ControllerBinding.DPAD_RIGHT))
                        a.movePointerToSlotIn(ScreenDirection.RIGHT);
                    else if (state.is(ControllerBinding.DPAD_LEFT))
                        a.movePointerToSlotIn(ScreenDirection.LEFT);
                }else if (state.is(ControllerBinding.LEFT_STICK) && state.released) a.movePointerToSlot(a.findSlotAt(getPointerX(),getPointerY()));
            }

            KeyMapping.ALL.forEach((key, value) -> {
                if (!state.matches(value)) return;
                Screen screen;
                if (this.minecraft.screen == null || (screen = this.minecraft.screen) instanceof PauseScreen && !((PauseScreen) screen).showsPauseMenu()) {
                    if (state.is(ControllerBinding.START) && state.pressed) {
                        value.setDown(false);
                    } else {
                        if (state.canClick()) value.clickCount++;
                        if (state.pressed && state.canDownKeyMapping(value)) value.setDown(true);
                        else if (state.canReleaseKeyMapping(value)) value.setDown(false);
                        if (state.pressed) {
                            if (value == minecraft.options.keyTogglePerspective || value == minecraft.options.keyUse) state.block();
                            else if (value == minecraft.options.keyAttack) state.onceClick(-state.getDefaultDelay() * (minecraft.player.getAbilities().invulnerable ? 2 : 5));
                        }
                    }
                }
            });
        }
    }
    public void simulateKeyAction(int key, BindingState state){
        if (state.pressed && state.canClick()) minecraft.screen.keyPressed(key, 0, 0);
        else if (state.released) minecraft.screen.keyReleased(key, 0, 0);
    }
    public void simulateKeyAction(Predicate<BindingState> canSimulate, int key, BindingState state){
        boolean clicked = state.pressed && state.canClick();
        if (canSimulate.test(state)){
            if (clicked) minecraft.screen.keyPressed(key, 0, 0);
            else if (state.released) minecraft.screen.keyReleased(key, 0, 0);
        }
    }
    public double getPointerX(){
        return minecraft.mouseHandler.xpos() * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth();
    }
    public double getPointerY(){
        return minecraft.mouseHandler.ypos() * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight();
    }

    public BindingState getButtonState(ControllerBinding button){
        return button.bindingState;
    }
    public void disableCursor(){
        if (getCursorMode() == 1 || minecraft.screen == null || minecraft.screen instanceof Controller.Event e && !e.disableCursorOnInit()) return;
        GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(),GLFW.GLFW_CURSOR,GLFW.GLFW_CURSOR_HIDDEN);
        isCursorDisabled = true;
    }
    public void resetCursor(){
        if (!resetCursor || isCursorDisabled) return;
        if (minecraft.screen instanceof LegacyMenuAccess<?> a) {
            for (Slot slot : a.getMenu().slots) {
                if (slot.getContainerSlot() == 0 && (minecraft.player == null || slot.container == minecraft.player.getInventory())){
                    a.movePointerToSlot(slot);
                    break;
                }
            }
        }
        resetCursor = false;
    }
    public void enableAndResetCursor(){
        enableCursor();
        resetCursor = true;
    }
    public void enableCursor(){
        isCursorDisabled = false;
    }
    public void toggleCursor(){
        if (getCursorMode() < 2) setCursorMode(getCursorMode() + 1);
        else setCursorMode(0);
        if (getCursorMode() == 1) {
            enableCursor();
        }else if (getCursorMode() == 2){
            disableCursor();
            if (minecraft.screen != null) minecraft.screen.repositionElements();
        }
    }

    public int getCursorMode() {
        return ((LegacyOptions)minecraft.options).cursorMode().get();
    }

    public void setCursorMode(int cursorMode) {
        ((LegacyOptions)minecraft.options).cursorMode().set(cursorMode);
        minecraft.options.save();
    }
}
