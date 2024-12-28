package wily.legacy.mixin.base;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ContainerEventHandler.class)
public interface ContainerEventHandlerMixin extends ContainerEventHandler {
    /**
     * @author Wilyicaro
     * @reason Legacy Edition Accuracy: Cyclical Navigation
     */
    @Overwrite
    default ComponentPath handleArrowNavigation(FocusNavigationEvent.ArrowNavigation arrowNavigation) {
        GuiEventListener guiEventListener = this.getFocused();
        if (guiEventListener == null) {
            ScreenDirection screenDirection = arrowNavigation.direction();
            ScreenRectangle screenRectangle = this.getRectangle().getBorder(screenDirection.getOpposite());
            return ComponentPath.path(this, this.nextFocusPathInDirection(screenRectangle, screenDirection, null, arrowNavigation));
        } else {
            ScreenRectangle oldRec = guiEventListener.getRectangle();
            ComponentPath path =  ComponentPath.path(this, this.nextFocusPathInDirection(oldRec, arrowNavigation.direction(), guiEventListener, arrowNavigation));
            if (path != null) return path;
            ScreenRectangle screenRec = getRectangle();
            ScreenRectangle rec = new ScreenRectangle(arrowNavigation.direction().getAxis() == ScreenAxis.HORIZONTAL ? arrowNavigation.direction().isPositive() ? 0 : screenRec.width() : oldRec.left() + oldRec.width()/2,arrowNavigation.direction().getAxis() == ScreenAxis.VERTICAL ? arrowNavigation.direction().isPositive() ? 0 : screenRec.height() : oldRec.top() + oldRec.height()/2,0,0);
            getFocused().setFocused(false);
            path = this.nextFocusPathInDirection(rec, arrowNavigation.direction(), null, arrowNavigation);
            getFocused().setFocused(true);
            return path == null ? null : path.component() == guiEventListener ? null : ComponentPath.path(this, path);
        }
    }
}
