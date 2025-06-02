package wily.legacy.client.controller;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.InputType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.Slot;
import org.joml.Vector2d;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import wily.factoryapi.FactoryEvent;
import wily.factoryapi.base.Stocker;
import wily.factoryapi.util.ListMap;
import wily.legacy.Legacy4J;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.LegacyTipManager;
import wily.legacy.client.screen.CreativeModeScreen;
import wily.legacy.client.screen.LegacyCraftingScreen;
import wily.legacy.client.screen.LegacyMenuAccess;
import wily.legacy.entity.LegacyPlayerInfo;
import wily.legacy.mixin.base.MouseHandlerAccessor;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;


public class ControllerManager {
    private static final float FAST_MOVE_SPEED = 0.18F;
    private static final float VERTICAL_SPEED = 0.8F;
    private static final float DIAGONAL_SPEED = 0.6F;
    private static final float ANGLE_SLOW = 45F * Mth.DEG_TO_RAD;
    private static final float ANGLE_FAST = 30F * Mth.DEG_TO_RAD;

    public Controller connectedController = null;
    public boolean isCursorDisabled = false;
    public boolean resetCursor = false;
    public boolean canChangeSlidersValue = true;
    protected Minecraft minecraft;
    public static final ListMap<String,Controller.Handler> handlers = ListMap.<String,Controller.Handler>builder().put("none",Controller.Handler.EMPTY).put("glfw", GLFWControllerHandler.getInstance()).put("sdl3", SDLControllerHandler.getInstance()).build();

    protected boolean isControllerTheLastInput = false;
    public boolean isControllerSimulatingInput = false;

    public static final Component CONTROLLER_DETECTED = Component.translatable("legacy.controller.detected");
    public static final Component CONTROLLER_DISCONNECTED = Component.translatable("legacy.controller.disconnected");

    private KeyMapping[] orderedKeyMappings;

    public static Controller.Handler getHandler() {
        return LegacyOptions.selectedControllerHandler.get();
    }

    public static void updatePlayerCamera(BindingState.Axis stick, Controller controller){
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.mouseHandler.isMouseGrabbed() || !minecraft.isWindowActive() || !stick.pressed || minecraft.player == null) return;
        double f = Math.pow(LegacyOptions.controllerSensitivity.get() * (double)0.6f + (double)0.2f,3) * 7.5f * (minecraft.player.isScoping() ? 0.125: 1.0);
        minecraft.player.turn(getCameraCurve(stick.getSmoothX()) * f, getCameraCurve(stick.getSmoothY()) * f * (LegacyOptions.invertYController.get() ? -1 : 1));
    }

    public static float getCameraCurve(float f){
        if (LegacyOptions.linearCameraMovement.get()) return f;
        return f * f * Math.signum(f);
    }

    public void setup(Minecraft minecraft){
        this.minecraft = minecraft;
        this.orderedKeyMappings = minecraft.options.keyMappings.clone();
        updateCursorInputMode();

        CompletableFuture.runAsync(()-> new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                minecraft.execute(getHandler()::init);
                if (minecraft.isRunning() && getHandler().update()) {
                    Setup.EVENT.invoker.accept(ControllerManager.this);
                    if (!getHandler().isValidController(LegacyOptions.selectedController.get())) {
                        if (connectedController != null) connectedController.disconnect(ControllerManager.this);
                        return;
                    }
                    if (connectedController == null && (connectedController = getHandler().getController(LegacyOptions.selectedController.get())) != null) connectedController.connect(ControllerManager.this);
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
        setPointerPos(x, y, isControllerTheLastInput() && LegacyOptions.controllerVirtualCursor.get());
    }

    public void setPointerPos(double x, double y, boolean onlyVirtual){
        Window window = minecraft.getWindow();
        if (minecraft.screen instanceof LegacyMenuAccess<?> && LegacyOptions.limitCursor.get()) {
            ScreenRectangle rect = ((LegacyMenuAccess<?>) minecraft.screen).getMenuRectangle();
            double scale = window.getGuiScale();
            int left = rect.left() - (minecraft.screen instanceof LegacyCraftingScreen ? 35 : 0);
            int top = rect.top() - (minecraft.screen instanceof CreativeModeScreen || minecraft.screen instanceof LegacyCraftingScreen ? 35 : 0);
            int paddingH = 20;
            int paddingV = 10;
            minecraft.mouseHandler.xpos = Math.max((left - paddingH) * scale, Math.min(x, (rect.right() + paddingH) * scale));
            minecraft.mouseHandler.ypos = Math.max((top - paddingV) * scale, Math.min(y, (rect.bottom() + paddingV) * scale));
        } else {
            minecraft.mouseHandler.xpos = Math.max(0, Math.min(x, window.getScreenWidth()));
            minecraft.mouseHandler.ypos = Math.max(0, Math.min(y, window.getScreenHeight()));
        }
        if (!onlyVirtual) GLFW.glfwSetCursorPos(Minecraft.getInstance().getWindow().getWindow(), minecraft.mouseHandler.xpos,minecraft.mouseHandler.ypos);
    }

    public synchronized void updateBindings() {
        updateBindings(minecraft.isWindowActive() ? connectedController : Controller.EMPTY);
    }

    public synchronized void updateBindings(Controller controller) {
        Arrays.sort(orderedKeyMappings, Comparator.comparingInt(mapping -> LegacyKeyMapping.of(mapping).getBinding() == null ? 2 : LegacyKeyMapping.of(mapping).getBinding().isSpecial() ? 0 : 1));
        for (ControllerBinding<?> binding : ControllerBinding.map.values()) {
            BindingState state = binding.state();
            state.update(controller);
            if (LegacyTipManager.getActualTip() != null) LegacyTipManager.getActualTip().bindingStateTick(state);

            if (state.pressed) {
                setControllerTheLastInput(true);
                //? if >=1.21.2
                /*minecraft.getFramerateLimitTracker().onInputReceived();*/
            }

            if (getCursorMode().isAuto() && state.pressed && !isCursorDisabled) disableCursor();

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

                if (!isCursorDisabled) {
                    if (state.is(ControllerBinding.LEFT_STICK) && state instanceof BindingState.Axis stick && state.pressed) {
                        double moveX;
                        double moveY;
                        if (LegacyOptions.legacyCursor.get()) {
                            double deltaX = stick.x * LegacyOptions.interfaceSensitivity.get() / 2;
                            double deltaY = stick.y * LegacyOptions.interfaceSensitivity.get() / 2;
                            double deltaLength = Vector2d.length(deltaX, deltaY);
                            double angle = Math.atan2(stick.y, stick.x);
                            float snappedSlow = Math.round(angle / ANGLE_SLOW) * ANGLE_SLOW;
                            float snappedFast = Math.round(angle / ANGLE_FAST) * ANGLE_FAST;
                            float finalAngle = deltaLength > FAST_MOVE_SPEED ? snappedFast : snappedSlow;
                            float speed = Math.round(snappedFast * Mth.RAD_TO_DEG) % 90 == 0 ? 1 : DIAGONAL_SPEED;
                            float length = Vector2f.length(stick.x, stick.y);
                            moveX = Math.cos(finalAngle) * speed * length;
                            moveY = Math.sin(finalAngle) * speed * length * VERTICAL_SPEED;
                        } else {
                            moveX = stick.x;
                            moveY = stick.y;
                        }
                        double scale = minecraft.getWindow().getGuiScale();
                        setPointerPos(minecraft.mouseHandler.xpos() + moveX * scale * LegacyOptions.interfaceSensitivity.get() / 2,
                                minecraft.mouseHandler.ypos() + moveY * scale * LegacyOptions.interfaceSensitivity.get() / 2);
                    }
                    if (state.is(ControllerBinding.LEFT_TRIGGER) && minecraft.screen instanceof LegacyMenuAccess<?> m && m.getMenu().getCarried().getCount() > 1){
                        if (state.justPressed) minecraft.screen.mouseClicked(getPointerX(), getPointerY(), 0);
                        else if (state.released) minecraft.screen.mouseReleased(getPointerX(), getPointerY(),0);
                        if (state.pressed) minecraft.screen.mouseDragged(getPointerX(), getPointerY(), 0,0,0);
                    }
                    int mouseClick = Controller.Event.of(minecraft.screen).getBindingMouseClick(state);
                    if (mouseClick != -1) {
                        if (minecraft.screen instanceof LegacyMenuAccess<?> || state.is(ControllerBinding.DOWN_BUTTON)) {
                            isControllerSimulatingInput = true;
                            if (state.pressed && state.onceClick(true))
                                ((MouseHandlerAccessor) minecraft.mouseHandler).pressMouse(minecraft.getWindow().getWindow(), mouseClick, 1, 0);
                            else if (state.released)
                                ((MouseHandlerAccessor) minecraft.mouseHandler).pressMouse(minecraft.getWindow().getWindow(), mouseClick, 0, 0);
                            isControllerSimulatingInput = false;
                        }
                    }
                }

                ControllerBinding<?> cursorBinding = LegacyKeyMapping.of(Legacy4JClient.keyToggleCursor).getBinding();
                if (cursorBinding != null && state.is(cursorBinding) && state.canClick()) toggleCursor();
                Controller.Event.of(minecraft.screen).simulateKeyAction(this, state);
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
        }

        for (KeyMapping keyMapping : orderedKeyMappings) {
            if (LegacyKeyMapping.of(keyMapping).getBinding() == null) break;
            BindingState state = LegacyKeyMapping.of(keyMapping).getBinding().state();
            Screen screen;
            if (this.minecraft.screen == null || (screen = this.minecraft.screen) instanceof PauseScreen/*? if >1.20.1 {*/ && !((PauseScreen) screen).showsPauseMenu()/*?}*/) {
                if (state.is(ControllerBinding.START) && state.pressed) {
                    keyMapping.setDown(false);
                } else {
                    if (state.canClick()) keyMapping.clickCount++;
                    if (state.pressed && state.canDownKeyMapping(keyMapping)) keyMapping.setDown(true);
                    else if (state.canReleaseKeyMapping(keyMapping)) keyMapping.setDown(false);
                    if (state.pressed) {
                        if (state.canBlock(keyMapping)) state.block();
                        else if (keyMapping == minecraft.options.keyAttack) state.onceClick(-state.getDefaultDelay() * (minecraft.player.getAbilities().invulnerable ? 3 : 5));
                    }
                }
            }
        }

        ControllerBinding<?> binding = LegacyKeyMapping.of(minecraft.options.keyScreenshot).getBinding();
        if (binding != null && binding.state().justPressed){
            Screenshot.grab(this.minecraft.gameDirectory, this.minecraft.getMainRenderTarget(), component -> this.minecraft.execute(() -> this.minecraft.gui.getChat().addMessage(component)));
        }

        if (minecraft.screen != null) Controller.Event.of(minecraft.screen).controllerTick(controller);
        if (LegacyTipManager.getActualTip() != null) LegacyTipManager.getActualTip().controllerTick(controller);
    }

    public void simulateKeyAction(Predicate<BindingState> canSimulate, int key, BindingState state){
        simulateKeyAction(canSimulate, key, state, false);
    }

    public void simulateKeyAction(Predicate<BindingState> canSimulate, int key, BindingState state, boolean onlyScreen){
        boolean clicked = state.pressed && state.canClick();
        if (canSimulate.test(state) && (!Controller.Event.of(minecraft.screen).onceClickBindings(state) || state.onceClick(true))){
            simulateKeyAction(key, state, clicked, onlyScreen);
        }
    }

    public void simulateKeyAction(int key, BindingState state){
        simulateKeyAction(key, state, false);
    }

    public void simulateKeyAction(int key, BindingState state, boolean onlyScreen){
        simulateKeyAction(key, state, state.pressed && state.canClick(), onlyScreen);
    }

    public void simulateKeyAction(int key, BindingState state, boolean canPress, boolean onlyScreen){
        if (canPress) simulateKeyAction(key,true, onlyScreen);
        else if (state.released) simulateKeyAction(key,false, onlyScreen);
    }

    public void simulateKeyAction(int key, boolean press, boolean onlyScreen){
        isControllerSimulatingInput = true;
        if (onlyScreen) simulateScreenKeyAction(key, press);
        else minecraft.keyboardHandler.keyPress(minecraft.getWindow().getWindow(), key, 0, press ? 1 : 0, 0);
        isControllerSimulatingInput = false;
    }

    public void simulateScreenKeyAction(int key, boolean press){
        if (press) minecraft.screen.keyPressed(key, 0, 0);
        else minecraft.screen.keyReleased(key, 0, 0);
    }

    public double getPointerX(){
        return minecraft.mouseHandler.xpos() * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth();
    }

    public double getPointerY(){
        return minecraft.mouseHandler.ypos() * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight();
    }

    public <T extends BindingState> T getButtonState(ControllerBinding<T> button){
        return button.state();
    }

    public boolean allowCursorAtFirstInventorySlot(){
        return (isControllerTheLastInput() && LegacyOptions.controllerCursorAtFirstInventorySlot.get()) || (!isControllerTheLastInput() && LegacyOptions.cursorAtFirstInventorySlot.get());
    }

    public void disableCursor(){
        if (getCursorMode().isAlways() || minecraft.screen == null || minecraft.screen instanceof Controller.Event e && !e.disableCursorOnInit()) return;
        setCursorInputMode(true);
        isCursorDisabled = true;
    }

    public void resetCursor(){
        if (!resetCursor || isCursorDisabled) return;
        if (allowCursorAtFirstInventorySlot() && minecraft.screen instanceof LegacyMenuAccess<?> a) {
            for (Slot slot : a.getMenu().slots) {
                if (slot.getContainerSlot() == 0 && (minecraft.player == null || slot.container == minecraft.player.getInventory())){
                    a.movePointerToSlot(slot);
                    break;
                }
            }
        }
        resetCursor = false;
    }

    public void enableCursorAndScheduleReset(){
        enableCursor();
        resetCursor = true;
    }

    public void enableCursor(){
        isCursorDisabled = false;
        updateCursorInputMode();
    }

    public void toggleCursor(){
        setCursorMode(LegacyOptions.CursorMode.values()[Stocker.cyclic(0, getCursorMode().ordinal() + 1, LegacyOptions.CursorMode.values().length)]);
        updateCursorMode();
    }

    public void updateCursorMode(){
        switch (getCursorMode()){
            case ALWAYS -> enableCursor();
            case NEVER -> {
                disableCursor();
                if (minecraft.screen != null) minecraft.screen.repositionElements();
            }
        }
    }

    public LegacyOptions.CursorMode getCursorMode() {
        return LegacyOptions.cursorMode.get();
    }

    public void setCursorMode(LegacyOptions.CursorMode cursorMode) {
        LegacyOptions.cursorMode.set(cursorMode);
        LegacyOptions.cursorMode.save();
    }

    public boolean isControllerTheLastInput() {
        return isControllerTheLastInput;
    }

    public void setCursorInputMode(boolean hidden){
        GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(),GLFW.GLFW_CURSOR, hidden ? GLFW.GLFW_CURSOR_HIDDEN : GLFW.GLFW_CURSOR_NORMAL);
    }

    public void updateCursorInputMode(){
        if (!minecraft.mouseHandler.isMouseGrabbed()) setCursorInputMode(!LegacyOptions.hasSystemCursor());
    }

    public void setControllerTheLastInput(boolean controllerTheLastInput) {
        if (isControllerTheLastInput != controllerTheLastInput){
            isControllerTheLastInput = controllerTheLastInput;
            updateCursorInputMode();
        }
    }

    interface Setup extends Consumer<ControllerManager> {
        FactoryEvent<Setup> EVENT = new FactoryEvent<>(e-> m-> e.invokeAll(l->l.accept(m)));
    }
}
