package wily.legacy.client.controller;

import com.mojang.blaze3d.platform.InputConstants;
import com.studiohartman.jamepad.ControllerState;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import wily.legacy.LegacyMinecraft;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static wily.legacy.client.controller.ControllerHandler.DEFAULT_CONTROLLER_BUTTONS_BY_KEY;

public enum ControllerComponent {
    DOWN_BUTTON((comp,state) -> comp.update(state.a,state.aJustPressed), InputConstants.KEY_SPACE),
    RIGHT_BUTTON((comp,state) -> comp.update(state.b,state.bJustPressed), InputConstants.KEY_Q),
    LEFT_BUTTON((comp,state) -> comp.update(state.x,state.xJustPressed), InputConstants.KEY_E),
    UP_BUTTON((comp,state) -> comp.update(state.y,state.yJustPressed), InputConstants.KEY_I),
    BACK((comp,state) -> comp.update(state.back,state.backJustPressed), InputConstants.KEY_H),
    GUIDE((comp,state) -> comp.update(state.guide,state.guideJustPressed)),
    START((comp,state) -> comp.update(state.start,state.startJustPressed), InputConstants.KEY_ESCAPE),
    LEFT_STICK((comp,state) -> comp.update(state.leftStickMagnitude > 0.1,true), (k, c)-> matchesLeftStick(Minecraft.getInstance(),k,c), ComponentState.Stick::new,InputConstants.KEY_W, InputConstants.KEY_A, InputConstants.KEY_S, InputConstants.KEY_D),
    RIGHT_STICK(ControllerComponent::updatePlayerCamera,(k, c)->true, ComponentState.Stick::new),
    LEFT_STICK_BUTTON((comp,state) -> comp.update(state.leftStickClick,state.leftStickJustClicked), InputConstants.KEY_F5),
    RIGHT_STICK_BUTTON((comp,state) -> comp.update(state.rightStickClick,state.rightStickJustClicked), InputConstants.KEY_LSHIFT),
    LEFT_BUMPER((comp,state) -> comp.update(state.lb,state.lbJustPressed)),
    RIGHT_BUMPER((comp,state) -> comp.update(state.rb,state.rbJustPressed)),
    LEFT_TRIGGER((comp,state) -> comp.update(state.leftTrigger > 0,true), InputConstants.MOUSE_BUTTON_RIGHT),
    RIGHT_TRIGGER((comp,state) -> comp.update(state.rightTrigger > 0,true), InputConstants.MOUSE_BUTTON_LEFT),
    DPAD_UP((comp,state) -> comp.update(state.dpadUp,state.dpadUpJustPressed)),
    DPAD_DOWN((comp,state) -> comp.update(state.dpadDown,state.dpadDownJustPressed)),
    DPAD_LEFT((comp,state) -> comp.update(state.dpadLeft,state.dpadLeftJustPressed), InputConstants.MOUSE_BUTTON_MIDDLE),
    DPAD_RIGHT((comp,state) -> comp.update(state.dpadRight,state.dpadRightJustPressed));


    public final Component displayName = Component.translatable(LegacyMinecraft.MOD_ID + ".controller_component." + name().toLowerCase(Locale.ENGLISH));
    public final BiConsumer<ComponentState, ControllerState> updateState;
    public final BiPredicate<KeyMapping,ControllerState> validKey;
    public final ComponentState componentState;

    <T extends ComponentState> ControllerComponent(BiConsumer<T,ControllerState> updateState, BiPredicate<KeyMapping,ControllerState> validKey,Function<ControllerComponent,T> stateConstructor,  int... defaultCorrespondentKeys){
        this.updateState = (c,s)-> updateState.accept((T) c,s);
        this.validKey = validKey;
        this.componentState = stateConstructor.apply(this);
        for (int k : defaultCorrespondentKeys) DEFAULT_CONTROLLER_BUTTONS_BY_KEY.put(k,this);
    }
    ControllerComponent(BiConsumer<ComponentState,ControllerState> updateState, int... defaultCorrespondentKeys){
        this.updateState = updateState;
        validKey = (k, c)-> true;
        this.componentState = new ComponentState(this);
        for (int k : defaultCorrespondentKeys) DEFAULT_CONTROLLER_BUTTONS_BY_KEY.put(k,this);
    }


    public boolean matches(KeyMapping mapping){
        return ((LegacyKeyMapping)mapping).getComponent() == this;
    }

    public static boolean matchesLeftStick(Minecraft minecraft, KeyMapping mapping, ControllerState state){
        return (minecraft.options.keyUp == mapping && state.leftStickY > 0) || (minecraft.options.keyDown == mapping && state.leftStickY < 0) || (minecraft.options.keyRight == mapping && state.leftStickX > 0) || (minecraft.options.keyLeft == mapping && state.leftStickX < 0);
    }
    public static void updatePlayerCamera(ComponentState.Stick stick, ControllerState state){
        Minecraft minecraft = Minecraft.getInstance();
        stick.update(state.rightStickMagnitude > 0.15,true);
        if (!minecraft.mouseHandler.isMouseGrabbed() || !minecraft.isWindowActive() || !stick.pressed || minecraft.player == null) return;
        double f = Math.pow(minecraft.options.sensitivity().get() * (double)0.6f + (double)0.2f,3) * minecraft.getDeltaFrameTime() * 800;
        minecraft.player.turn(state.rightStickX * f,state.rightStickY * f * (minecraft.options.invertYMouse().get() ? 1 : -1));
    }
    public static void init() {
    }
}
