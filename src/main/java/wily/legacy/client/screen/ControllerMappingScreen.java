package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.ArrayUtils;
import wily.factoryapi.base.ArbitrarySupplier;
import wily.factoryapi.base.client.FactoryConfigWidgets;
import wily.factoryapi.base.client.SimpleLayoutRenderable;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.base.config.FactoryConfigControl;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyOptions;
import wily.legacy.client.controller.*;
import wily.legacy.util.LegacyComponents;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

public class ControllerMappingScreen extends LegacyKeyMappingScreen {
    private Set<ControllerBinding<?>> recordedBindings = new ObjectOpenHashSet<>();

    public ControllerMappingScreen(Screen parent) {
        super(parent, Component.translatable("legacy.options.selectedController"));
    }

    @Override
    public void addButtons() {
        KeyMapping[] keyMappings = ArrayUtils.clone(Minecraft.getInstance().options.keyMappings);
        Arrays.sort(keyMappings);
        String lastCategory = null;
        renderableVList.addRenderable(Button.builder(Component.translatable("legacy.menu.reset_defaults"),button -> minecraft.setScreen(new ConfirmationScreen(this, Component.translatable("legacy.menu.reset_controls"),Component.translatable("legacy.menu.reset_controls_message"), b-> {
            for (KeyMapping keyMapping : keyMappings)
                LegacyKeyMapping.of(keyMapping).setBinding(LegacyKeyMapping.of(keyMapping).getDefaultBinding());
            LegacyOptions.CLIENT_STORAGE.save();
            minecraft.setScreen(this);
        }))).size(240,20).build());
        renderableVList.addOptions(
                LegacyOptions.unbindConflictingButtons,
                LegacyOptions.controllerToasts,
                LegacyOptions.controllerToggleCrouch,
                LegacyOptions.controllerToggleSprint,
                LegacyOptions.invertControllerButtons,
                LegacyOptions.controllerVirtualCursor,
                LegacyOptions.legacyCursor,
                LegacyOptions.limitCursor,
                LegacyOptions.controllerDoubleClick,
                LegacyOptions.controllerCursorAtFirstInventorySlot,
                LegacyOptions.selectedController,
                LegacyOptions.selectedControllerHandler);
        renderableVList.addMultSliderOption(LegacyOptions.controllerSensitivity, 2);
        renderableVList.addOptions(
                LegacyOptions.leftStickDeadZone,
                LegacyOptions.rightStickDeadZone,
                LegacyOptions.leftTriggerDeadZone,
                LegacyOptions.rightTriggerDeadZone);

        for (KeyMapping keyMapping : keyMappings) {
            String category = keyMapping.getCategory();
            if (!Objects.equals(lastCategory, category)) {
                renderableVList.addCategory(Component.translatable(category));
                if (category.equals("key.categories.movement"))
                    renderableVList.addOptions(
                            LegacyOptions.invertYController,
                            LegacyOptions.smoothMovement,
                            LegacyOptions.forceSmoothMovement,
                            LegacyOptions.linearCameraMovement);
            }
            lastCategory = keyMapping.getCategory();
            renderableVList.addRenderable(new MappingButton(0,0,240,20, LegacyKeyMapping.of(keyMapping)) {
                @Override
                public ControlTooltip.ComponentIcon getIcon() {
                    return mapping.getBinding().getIcon();
                }

                @Override
                public boolean isNone() {
                    return mapping.getBinding() == null;
                }

                @Override
                public void onPress() {
                    if (Screen.hasShiftDown() || ControllerBinding.LEFT_STICK_BUTTON.state().pressed){
                        mapping.setBinding(mapping.getDefaultBinding());
                        LegacyOptions.CLIENT_STORAGE.save();
                        setAndUpdateMappingTooltip(ArbitrarySupplier.empty());
                    } else if (!ControlType.getActiveType().isKbm()) {
                        setSelectedMapping(mapping);
                        setAndUpdateMappingTooltip(ControllerMappingScreen.this::getCancelTooltip);
                    }
                }
            });
        }
    }

    @Override
    protected boolean areConflicting(LegacyKeyMapping keyMapping, LegacyKeyMapping comparison){
        return keyMapping.getBinding() == comparison.getBinding();
    }

    protected void setNone(LegacyKeyMapping keyMapping){
        keyMapping.setBinding(null);
        LegacyOptions.CLIENT_STORAGE.save();
    }

    public boolean unbindConflictingBindings(){
        return LegacyOptions.unbindConflictingButtons.get();
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        if (i == InputConstants.KEY_ESCAPE && selectedMapping != null && !Legacy4JClient.controllerManager.isControllerSimulatingInput) {
            setSelectedMapping(null);
            setAndUpdateMappingTooltip(ArbitrarySupplier.empty());
            return true;
        }
        if (selectedMapping != null) return false;
        return super.keyPressed(i, j, k);
    }

    @Override
    public boolean allowsKey() {
        return false;
    }

    @Override
    public Component getCancelTooltip() {
        return Component.translatable("legacy.options.controllerMappingTooltip", ControlTooltip.CANCEL_BINDING.get().getComponent());
    }

    @Override
    public Component getConflictingTooltip(){
        return LegacyComponents.CONFLICTING_BUTTONS;
    }

    public void applyBinding(ControllerBinding<?> binding) {
        selectedMapping.setBinding(binding);
        LegacyOptions.CLIENT_STORAGE.save();
        setAndUpdateMappingTooltip(ArbitrarySupplier.empty());
        resolveConflictingMappings();
        setSelectedMapping(null);
    }

    @Override
    public boolean onceClickBindings(BindingState state) {
        return selectedMapping == null && super.onceClickBindings(state);
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if (selectedMapping != null) {
            if (state.is(ControllerBinding.BACK)) {
                if (state.canClick() && state.timePressed >= state.getDefaultDelay())
                    applyBinding(ControllerBinding.BACK);
                else if (state.released) applyBinding(null);
            }
        }
    }

    @Override
    public void controllerTick(Controller controller) {
        if (selectedMapping != null) {
            for (ControllerBinding<?> binding : ControllerBinding.map.values()) {
                if (binding.state().justPressed && binding.isBindable && !binding.state().isBlocked() && !binding.equals(ControllerBinding.BACK) && !binding.isSpecial()) recordedBindings.add(binding);
            }

            if (!recordedBindings.isEmpty() && recordedBindings.stream().noneMatch(binding-> binding.state().pressed)) {
                BindingState state = CompoundControllerBinding.getOrCreateAndUpdate(controller, recordedBindings.toArray(ControllerBinding[]::new)).state();
                applyBinding(state.binding);
                recordedBindings.clear();
                state.block();
            }
        }
    }
}
