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
import wily.factoryapi.FactoryEvent;
import wily.factoryapi.util.ListMap;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOption;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.entity.LegacyPlayerInfo;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;


public  class ControllerManager {
    public static final Map<Integer, ControllerBinding> DEFAULT_CONTROLLER_BUTTONS_BY_KEY = new HashMap<>();
    public Controller connectedController = null;
    public boolean isCursorDisabled = false;
    public boolean resetCursor = false;
    public boolean canChangeSlidersValue = true;
    final Minecraft minecraft;
    public static final ListMap<String,Controller.Handler> handlers = ListMap.<String,Controller.Handler>builder().put("none",Controller.Handler.EMPTY).put("glfw", GLFWControllerHandler.getInstance()).put("sdl3", SDLControllerHandler.getInstance()).build();

    public boolean isControllerTheLastInput = false;

    public static final Component CONTROLLER_DETECTED = Component.translatable("legacy.controller.detected");
    public static final Component CONTROLLER_DISCONNECTED = Component.translatable("legacy.controller.disconnected");

    public ControllerManager(Minecraft minecraft){
        this.minecraft = minecraft;
    }

    public static Controller.Handler getHandler() {
        return LegacyOption.selectedControllerHandler.get();
    }

    public static void updatePlayerCamera(BindingState.Axis stick, Controller handler){
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.mouseHandler.isMouseGrabbed() || !minecraft.isWindowActive() || !stick.pressed || minecraft.player == null) return;
        double f = Math.pow(LegacyOption.controllerSensitivity.get() * (double)0.6f + (double)0.2f,3) * 7.5f * (minecraft.player.isScoping() ? 0.125: 1.0);
        minecraft.player.turn(getCameraCurve(stick.getSmoothX()) * f, getCameraCurve(stick.getSmoothY()) * f * (LegacyOption.invertYController.get() ? -1 : 1));
    }

    public static float getCameraCurve(float f){
        if (LegacyOption.linearCameraMovement.get()) return f;
        return f * f * Math.signum(f);
    }

    public void setup(){
        GLFW.glfwSetInputMode(minecraft.getWindow().getWindow(),GLFW.GLFW_CURSOR,GLFW.GLFW_CURSOR_HIDDEN);

        CompletableFuture.runAsync(()-> new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                minecraft.execute(getHandler()::init);
                if (minecraft.isRunning() && getHandler().update()) {
                    Setup.EVENT.invoker.accept(ControllerManager.this);
                    if (!getHandler().isValidController(LegacyOption.selectedController.get())) {
                        if (connectedController != null) connectedController.disconnect(ControllerManager.this);
                        return;
                    }
                    if (connectedController == null && (connectedController = getHandler().getController(LegacyOption.selectedController.get())) != null) connectedController.connect(ControllerManager.this);
                    minecraft.execute(() -> {
                        if (connectedController != null) getHandler().setup(ControllerManager.this);
                    });
                }
            }
        },0,1)).exceptionally(t-> {
            Legacy4J.LOGGER.warn(t.getMessage());
            return null;
        });
    }
    public void setPointerPos(double x, double y){
        setPointerPos(x,y,isControllerTheLastInput);
    }
    public void setPointerPos(double x, double y, boolean onlyVirtual){
        Window window = minecraft.getWindow();
        minecraft.mouseHandler.xpos = Math.max(0,Math.min(x,window.getScreenWidth()));
        minecraft.mouseHandler.ypos = Math.max(0,Math.min(y,window.getScreenHeight()));
        if (!onlyVirtual) GLFW.glfwSetCursorPos(Minecraft.getInstance().getWindow().getWindow(), minecraft.mouseHandler.xpos,minecraft.mouseHandler.ypos);
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

            if (state.pressed) {
                isControllerTheLastInput = true;
                //? if >=1.21.2
                minecraft.getFramerateLimitTracker().onInputReceived();
            }

            if (getCursorMode() == 0 && state.pressed && !isCursorDisabled) disableCursor();

            if (minecraft.player != null && minecraft.getConnection() != null && controller.hasLED() && minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID()) instanceof LegacyPlayerInfo i){
                float[] color = Legacy4JClient.getVisualPlayerColor(i);
                controller.setLED((byte) (color[0] * 255),(byte) (color[1] * 255),(byte) (color[2] * 255));
            }

            if (state.is(ControllerBinding.START) && state.justPressed)
                if (minecraft.screen == null) minecraft.pauseGame(false);
                else if (minecraft.screen instanceof AbstractContainerScreen<?> || minecraft.screen instanceof PauseScreen) minecraft.screen.onClose();

            s : if (minecraft.screen != null) {
                if (state.pressed && state.canClick()) {
                    minecraft.setLastInputType(InputType.KEYBOARD_ARROW);
                    minecraft.screen.afterKeyboardAction();
                }
                Controller.Event.of(minecraft.screen).bindingStateTick(state);
                if (minecraft.screen == null) break s;
                ControllerBinding cursorBinding = ((LegacyKeyMapping) Legacy4JClient.keyToggleCursor).getBinding();
                if (cursorBinding != null && state.is(cursorBinding) && state.canClick()) toggleCursor();
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
                    minecraft.screen.mouseScrolled(getPointerX(), getPointerY()/*? if >1.20.1 {*/, 0/*?}*/, Math.signum(-stick.y));
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
                    setPointerPos(minecraft.mouseHandler.xpos() + stick.x * ((double) minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth())  * LegacyOption.interfaceSensitivity.get() / 2,minecraft.mouseHandler.ypos() + stick.y * ((double) minecraft.getWindow().getScreenHeight() / minecraft.getWindow().getGuiScaledHeight()) * LegacyOption.interfaceSensitivity.get() / 2);

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
                if (this.minecraft.screen == null || (screen = this.minecraft.screen) instanceof PauseScreen/*? if >1.20.1 {*/ && !((PauseScreen) screen).showsPauseMenu()/*?}*/) {
                    if (state.is(ControllerBinding.START) && state.pressed) {
                        value.setDown(false);
                    } else {
                        if (state.canClick()) value.clickCount++;
                        if (state.pressed && state.canDownKeyMapping(value)) value.setDown(true);
                        else if (state.canReleaseKeyMapping(value)) value.setDown(false);
                        if (state.pressed) {
                            if (value == minecraft.options.keyTogglePerspective || value == minecraft.options.keyUse) state.block();
                            else if (value == minecraft.options.keyAttack) state.onceClick(-state.getDefaultDelay() * (minecraft.player.getAbilities().invulnerable ? 3 : 5));
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
        return LegacyOption.cursorMode.get();
    }

    public void setCursorMode(int cursorMode) {
        LegacyOption.cursorMode.set(cursorMode);
        minecraft.options.save();
    }
    interface Setup extends Consumer<ControllerManager> {
        FactoryEvent<Setup> EVENT = new FactoryEvent<>(e-> m-> e.invokeAll(l->l.accept(m)));
    }
}
