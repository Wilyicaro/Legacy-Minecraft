package wily.legacy.CustomModelSkins.cpm.shared.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PartRoot {
    private List<RootModelElement> elements;
    private RootModelElement mainRoot;

    public PartRoot(RootModelElement element) {
        elements = new ArrayList<>(1);
        mainRoot = element;
        elements.add(element);
    }

    public void forEach(Consumer<? super RootModelElement> action) {
        elements.forEach(action);
    }

    public boolean add(RootModelElement e) {
        if (elements.contains(e)) return true;
        return elements.add(e);
    }

    public RootModelElement get() {
        return elements.get(0);
    }

    public RootModelElement getMainRoot() {
        return mainRoot;
    }

    public void setRootPosAndRot(float px, float py, float pz, float rx, float ry, float rz) {
        forEach(e -> e.setPosAndRot(px, py, pz, rx, ry, rz));
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }
}
