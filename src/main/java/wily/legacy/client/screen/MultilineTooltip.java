package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.Arrays;
import java.util.List;


public class MultilineTooltip {
    protected final List<FormattedCharSequence> lines;
    protected final Component narration;

    public MultilineTooltip(List<FormattedCharSequence> message, Component narration) {
        lines = message;
        this.narration = narration;
    }

    public MultilineTooltip(List<FormattedCharSequence> message) {
        this(message, Component.empty());
    }

    public MultilineTooltip(Component message, int width) {
        this(Minecraft.getInstance().font.split(message, width), message);
    }

    public MultilineTooltip(List<Component> content, int width) {
        this(content.stream().map(c -> Minecraft.getInstance().font.split(c, width)).flatMap(List::stream).toList());
    }

    public MultilineTooltip(int width, Component... message) {
        this(Arrays.stream(message).toList(), width);
    }

    public Tooltip asTooltip() {
        return Tooltip.create(narration);
    }

    public List<FormattedCharSequence> toCharSequence(Minecraft minecraft) {
        return lines;
    }

    public static Tooltip create(List<FormattedCharSequence> message, Component narration) {
        return new MultilineTooltip(message, narration).asTooltip();
    }

    public static Tooltip create(List<Component> content, int width) {
        return new MultilineTooltip(content, width).asTooltip();
    }

    public static Tooltip create(int width, Component... message) {
        return new MultilineTooltip(width, message).asTooltip();
    }
}
