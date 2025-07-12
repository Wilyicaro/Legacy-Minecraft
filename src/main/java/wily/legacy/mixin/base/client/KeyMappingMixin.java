package wily.legacy.mixin.base.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.client.controller.LegacyKeyMapping;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin implements LegacyKeyMapping {

    @Shadow private InputConstants.Key key;
    @Unique
    private ControllerBinding<?> defaultBinding;
    @Unique
    private ControllerBinding<?> binding;

    @Override
    public ControllerBinding<?> getDefaultBinding() {
        return defaultBinding;
    }

    @Override
    public ControllerBinding<?> getBinding() {
        return binding;
    }

    @Override
    public <T extends BindingState> void setBinding(ControllerBinding<T> binding) {
        this.binding = binding;
    }

    @Override
    public <T extends BindingState> void setDefaultBinding(ControllerBinding<T> binding) {
        this.defaultBinding = binding;
    }

    @Override
    public InputConstants.Key getKey() {
        return key;
    }



    public Component getDisplayName() {
        return LegacyKeyMapping.getDefaultDisplayName(self());
    }
}
