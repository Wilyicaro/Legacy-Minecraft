package wily.legacy.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarrationThunk;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import wily.legacy.util.ScreenUtil;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


public class MultilineTooltip extends Tooltip {
    protected final List<FormattedCharSequence> lines;
    protected List<Component> narrationLines;
    public MultilineTooltip(List<FormattedCharSequence> message, Component narration){
        super(Component.empty(), narration);
        lines = message;
    }
    public MultilineTooltip(List<FormattedCharSequence> message){
        this(message, Component.empty());
    }
    public MultilineTooltip(Component message, int width){
        this(split(message, width),message);
    }
    public MultilineTooltip(List<Component> content, int width){
        this(content.stream().map(c-> split(c,width)).flatMap(List::stream).toList());
    }
    public MultilineTooltip(int width, Component... message){
        this(Arrays.stream(message).toList(), width);
    }
    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
        if (narrationLines != null) narrationElementOutput.add(NarratedElementType.HINT, NarrationThunk.from(narrationLines));
        else super.updateNarration(narrationElementOutput);
    }

    @Override
    public List<FormattedCharSequence> toCharSequence(Minecraft minecraft) {
        return lines;
    }

    private static List<FormattedCharSequence> split(Component message, int width) {
        AtomicReference<List<FormattedCharSequence>> lines = new AtomicReference<>(List.of());
        ScreenUtil.applySDFont(ignored -> lines.set(Minecraft.getInstance().font.split(message, width)));
        return lines.get();
    }
}
