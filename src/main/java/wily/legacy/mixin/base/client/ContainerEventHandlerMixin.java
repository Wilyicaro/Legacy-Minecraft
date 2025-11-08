package wily.legacy.mixin.base.client;

import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import wily.legacy.Legacy4JClient;

import java.util.Optional;

@Mixin(ContainerEventHandler.class)
public interface ContainerEventHandlerMixin extends ContainerEventHandler {
    @Shadow Optional<GuiEventListener> getChildAt(double d, double e);

    /**
     * @author Wilyicaro
     * @reason Legacy Edition Accuracy: Cyclical Navigation
     */
    @Overwrite
    default ComponentPath handleArrowNavigation(FocusNavigationEvent.ArrowNavigation arrowNavigation) {
        GuiEventListener guiEventListener = this.getFocused();
        if (guiEventListener == null && Legacy4JClient.controllerManager.isCursorDisabled) {
            ScreenDirection screenDirection = arrowNavigation.direction();
            ScreenRectangle screenRectangle = this.getRectangle().getBorder(screenDirection.getOpposite());
            return ComponentPath.path(this, this.nextFocusPathInDirection(screenRectangle, screenDirection, null, arrowNavigation));
        } else {
            ScreenRectangle oldRec = guiEventListener == null ? Legacy4JClient.controllerManager.getPointerRectangle() : guiEventListener.getRectangle();
            if (guiEventListener == null)
                guiEventListener = getChildAt(Legacy4JClient.controllerManager.getPointerX(), Legacy4JClient.controllerManager.getPointerY()).orElse(null);
            ComponentPath path = ComponentPath.path(this, this.nextFocusPathInDirection(oldRec, arrowNavigation.direction(), guiEventListener, arrowNavigation));
            if (path != null) return path;
            ScreenRectangle screenRec = getRectangle();
            ScreenRectangle rec = new ScreenRectangle(arrowNavigation.direction().getAxis() == ScreenAxis.HORIZONTAL ? arrowNavigation.direction().isPositive() ? -oldRec.width() : screenRec.width() : oldRec.left(), arrowNavigation.direction().getAxis() == ScreenAxis.VERTICAL ? arrowNavigation.direction().isPositive() ? -oldRec.height() : screenRec.height() : oldRec.top(), oldRec.width(), oldRec.height());
            boolean unfocus = guiEventListener != null && guiEventListener.isFocused();
            if (unfocus)
                guiEventListener.setFocused(false);
            path = this.nextFocusPathInDirection(rec, arrowNavigation.direction(), null, arrowNavigation);
            if (unfocus)
                guiEventListener.setFocused(true);
            return path == null ? null : path.component() == guiEventListener ? null : ComponentPath.path(this, path);
        }
    }
}
