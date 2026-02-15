package wily.legacy.CustomModelSkins.cpm.client;

import net.minecraft.client.model.geom.ModelPart;
import wily.legacy.CustomModelSkins.cpl.render.RecordBuffer;
import wily.legacy.CustomModelSkins.cpm.shared.model.render.ModelRenderManager.RedirectRenderer;

import java.util.Map;
import java.util.WeakHashMap;

public final class ModelPartHooks {
    private ModelPartHooks() {
    }

    private static final Map<ModelPart, SelfRenderer> SELF_RENDERERS = new WeakHashMap<>();
    private static final Map<ModelPart, RedirectRenderer<ModelPart>> REDIRECT_RENDERERS = new WeakHashMap<>();
    private static final Map<ModelPart, RecordBuffer> RECORD_BUFFERS = new WeakHashMap<>();

    public static void registerSelfRenderer(ModelPart part, SelfRenderer renderer) {
        if (part != null && renderer != null) {
            SELF_RENDERERS.put(part, renderer);
        }
    }

    public static SelfRenderer getSelfRenderer(ModelPart part) {
        return part != null ? SELF_RENDERERS.get(part) : null;
    }

    public static void registerRedirectRenderer(ModelPart part, RedirectRenderer<ModelPart> renderer) {
        if (part != null && renderer != null) {
            REDIRECT_RENDERERS.put(part, renderer);
        }
    }

    public static RedirectRenderer<ModelPart> getRedirectRenderer(ModelPart part) {
        return part != null ? REDIRECT_RENDERERS.get(part) : null;
    }

    public static void registerRecordBuffer(ModelPart part, RecordBuffer buffer) {
        if (part != null && buffer != null) {
            RECORD_BUFFERS.put(part, buffer);
        }
    }

    public static RecordBuffer getRecordBuffer(ModelPart part) {
        return part != null ? RECORD_BUFFERS.get(part) : null;
    }
}
