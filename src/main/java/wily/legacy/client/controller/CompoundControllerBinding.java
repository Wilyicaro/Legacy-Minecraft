package wily.legacy.client.controller;

import net.minecraft.Util;
import wily.legacy.client.screen.ControlTooltip;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CompoundControllerBinding extends ControllerBinding<BindingState> {
    private static final Function<ControllerBinding<?>[],CompoundControllerBinding> CACHE = Util.memoize(CompoundControllerBinding::new);

    private final ControllerBinding<?>[] bindings;

    public CompoundControllerBinding(ControllerBinding<?>[] bindings) {
        super(Arrays.stream(bindings).map(ControllerBinding::getKey).sorted().collect(Collectors.joining(",")), binding-> new BindingState(binding) {
            @Override
            public void update(Controller controller) {
                boolean press = true;
                for (ControllerBinding<?> controllerBinding : bindings) {
                    if (!controllerBinding.state().pressed) {
                        press = false;
                        break;
                    }
                }
                update(press);
            }

            @Override
            public void nextUpdatePress() {
                for (ControllerBinding<?> controllerBinding : bindings) {
                    controllerBinding.state().nextUpdatePress();
                }
            }

            @Override
            public void block() {
                super.block();
                for (ControllerBinding<?> controllerBinding : bindings) {
                    controllerBinding.state().block();
                    controllerBinding.state().released = true;
                }
            }
        });
        Arrays.sort(bindings, Comparator.comparing(ControllerBinding::getKey));
        this.bindings = bindings;
    }

    public static CompoundControllerBinding getOrCreate(ControllerBinding<?>... bindings){
        return CACHE.apply(bindings);
    }

    public static ControllerBinding<?> getOrCreateAndUpdate(Controller controller, ControllerBinding<?>... bindings){
        if (bindings.length == 1) return bindings[0];
        CompoundControllerBinding compoundControllerBinding = getOrCreate(bindings);
        if (!ControllerBinding.map.containsKey(compoundControllerBinding.getKey())) {
            ControllerBinding.register(compoundControllerBinding);
            compoundControllerBinding.state().update(controller);
        }
        return compoundControllerBinding;
    }

    public ControllerBinding<?>[] bindings() {
        return bindings;
    }

    @Override
    public ControlTooltip.ComponentIcon getIcon() {
        ControlTooltip.ComponentIcon[] icons = new ControlTooltip.ComponentIcon[bindings.length * 2 - 1];
        for (int i = 0; i < icons.length; i++) {
            boolean isDelimiter = i % 2 != 0 && i < icons.length - 1;
            icons[i] = isDelimiter ? ControlTooltip.PLUS_ICON : bindings[i / 2].getIcon();
        }
        return ControlTooltip.CompoundComponentIcon.of(icons);
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}