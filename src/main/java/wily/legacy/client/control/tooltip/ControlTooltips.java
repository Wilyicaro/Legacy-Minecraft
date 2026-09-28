package wily.legacy.client.control.tooltip;

import com.google.common.collect.Iterators;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;

public class ControlTooltips implements Iterable<ControlTooltipList> {
    private final ControlTooltipList[] lists = new ControlTooltipList[Corner.values().length];

    public static ControlTooltips of(Object o) {
        return o instanceof ControlTooltip.Listener e ? e.getControlTooltips() : ControlTooltipRenderer.getInstance().tooltips();
    }

    public ControlTooltips() {
        for (int i = 0; i < lists.length; i++) {
            lists[i] = new ControlTooltipList();
        }
    }

    public ControlTooltipList list() {
        return bottomLeft();
    }

    public ControlTooltipList list(Corner corner) {
        return lists[corner.ordinal()];
    }

    public ControlTooltipList bottomLeft() {
        return list(Corner.BOTTOM_LEFT);
    }

    public ControlTooltipList bottomRight() {
        return list(Corner.BOTTOM_RIGHT);
    }

    public ControlTooltipList topLeft() {
        return list(Corner.TOP_LEFT);
    }

    public ControlTooltipList topRight() {
        return list(Corner.TOP_RIGHT);
    }

    @Override
    public @NonNull Iterator<ControlTooltipList> iterator() {
        return Iterators.forArray(lists);
    }

    public enum Corner {
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        TOP_LEFT,
        TOP_RIGHT
    }
}
