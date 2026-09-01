package wily.legacy.client.control.tooltip;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.input.MouseButtonEvent;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.control.BindingState;
import wily.legacy.client.control.ControlType;
import wily.legacy.client.control.ControllerBinding;
import wily.legacy.util.IOUtil;

import java.util.List;
import java.util.Optional;

public class ControllerIcon extends CharsIcon {
    public static final Codec<ControllerIcon> CODEC = RecordCodecBuilder.create(i -> i.group(ControllerBinding.CODEC.fieldOf("binding").forGetter(ControllerIcon::binding), CharsIcon.CHARS_CODEC.optionalFieldOf("icon").forGetter(ControllerIcon::iconChars), CharsIcon.CHARS_CODEC.optionalFieldOf("iconOverlay").forGetter(ControllerIcon::iconOverlayChars), Codec.STRING.optionalFieldOf("tipIcon").forGetter(ControllerIcon::tipIcon)).apply(i, ControllerIcon::new));
    public static final Codec<List<ControllerIcon>> LIST_CODEC = IOUtil.createListIdMapCodec(CODEC, "binding");

    private final ControllerBinding<?> binding;

    public ControllerIcon(ControllerBinding<?> binding, Optional<char[]> iconChars, Optional<char[]> iconOverlayChars, Optional<String> tipIcon) {
        super(iconChars, iconOverlayChars, tipIcon);
        this.binding = binding;
    }

    @Override
    public boolean pressed() {
        return state().pressed;
    }

    @Override
    public boolean canLoop() {
        return !state().isBlocked();
    }

    @Override
    public void click(MouseButtonEvent event) {
        if (Legacy4JClient.controllerManager.connectedController != null) {
            super.click(event);
            state().nextUpdatePress();
        }
    }

    @Override
    public ControlType getControlType() {
        return ControlType.getActiveControllerType();
    }

    public ControllerBinding<?> binding() {
        return binding;
    }

    public BindingState state() {
        return binding().getMapped().state();
    }

    @Override
    public String name() {
        return binding().getKey();
    }
}
