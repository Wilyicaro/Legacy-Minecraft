package wily.legacy.client.control.tooltip;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;
import wily.legacy.Legacy4JClient;
import wily.legacy.client.control.ControlType;
import wily.legacy.mixin.base.client.KeyboardHandlerAccessor;
import wily.legacy.util.IOUtil;

import java.util.List;
import java.util.Optional;

public class KeyIcon extends CharsIcon {
    public static final Codec<KeyIcon> CODEC = RecordCodecBuilder.create(i -> i.group(Codec.STRING.xmap(InputConstants::getKey, InputConstants.Key::getName).fieldOf("key").forGetter(KeyIcon::key), CharsIcon.CHARS_CODEC.optionalFieldOf("icon").forGetter(KeyIcon::iconChars), CharsIcon.CHARS_CODEC.optionalFieldOf("iconOverlay").forGetter(KeyIcon::iconOverlayChars), Codec.STRING.optionalFieldOf("tipIcon").forGetter(KeyIcon::tipIcon)).apply(i, KeyIcon::new));
    public static final Codec<List<KeyIcon>> LIST_CODEC = IOUtil.createListIdMapCodec(CODEC, "key");

    private final InputConstants.Key key;

    public KeyIcon(InputConstants.Key key, Optional<char[]> iconChars, Optional<char[]> iconOverlayChars, Optional<String> tipIcon) {
        super(iconChars, iconOverlayChars, tipIcon);
        this.key = key;
    }

    public KeyIcon(InputConstants.Key key) {
        this(key, Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Override
    public ControlType getControlType() {
        return ControlType.getKbmActiveType();
    }

    @Override
    public String name() {
        return key.getName();
    }

    @Override
    public void click(MouseButtonEvent event) {
        super.click(event);

        if (key.getValue() == InputConstants.KEY_LSHIFT || key.getValue() == InputConstants.KEY_RSHIFT) {
            Legacy4JClient.controllerManager.simulateShift = true;
        }

        if (key.getType() == InputConstants.Type.KEYSYM)
            ((KeyboardHandlerAccessor) Minecraft.getInstance().keyboardHandler).invokeKeyPress(Minecraft.getInstance().getWindow().handle(), 1, new KeyEvent(key.getValue(), 0, 0));
    }

    @Override
    public void release(MouseButtonEvent event) {
        if (key.getType() == InputConstants.Type.KEYSYM)
            ((KeyboardHandlerAccessor) Minecraft.getInstance().keyboardHandler).invokeKeyPress(Minecraft.getInstance().getWindow().handle(), 0, new KeyEvent(key.getValue(), 0, 0));
    }

    @Override
    public boolean pressed() {
        Window window = Minecraft.getInstance().getWindow();
        return (key.getType() == InputConstants.Type.KEYSYM ? InputConstants.isKeyDown(window, key.getValue()) : GLFW.glfwGetMouseButton(window.handle(), key.getValue()) == 1);
    }

    @Override
    public boolean canLoop() {
        return key.getType() != InputConstants.Type.MOUSE;
    }

    public InputConstants.Key key() {
        return key;
    }
}
