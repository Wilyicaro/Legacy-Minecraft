package wily.legacy.CustomModelSkins.cpm.shared.model.render;

import wily.legacy.CustomModelSkins.cpl.math.MatrixStack;
import wily.legacy.CustomModelSkins.cpl.render.RenderTypes;
import wily.legacy.CustomModelSkins.cpl.render.VBuffers;
import wily.legacy.CustomModelSkins.cpm.shared.model.RenderedCube;
import wily.legacy.CustomModelSkins.cpm.shared.model.render.ModelRenderManager.RedirectRenderer;

public interface IExtraRenderDefinition {
    void render(RedirectRenderer<?> renderer, MatrixStack stack, VBuffers buf, RenderTypes<RenderMode> renderTypes, RenderedCube cube, boolean doRenderElems);
}
