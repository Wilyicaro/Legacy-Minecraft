package wily.legacy.CustomModelSkins.cpm.shared.definition;

import com.google.common.util.concurrent.UncheckedExecutionException;
import wily.legacy.CustomModelSkins.cpl.math.Vec2i;
import wily.legacy.CustomModelSkins.cpl.math.Vec3f;
import wily.legacy.CustomModelSkins.cpl.util.Image;
import wily.legacy.CustomModelSkins.cpl.util.LocalizedException;
import wily.legacy.CustomModelSkins.cpl.util.StringBuilderStream;
import wily.legacy.CustomModelSkins.cpm.shared.animation.AnimationRegistry;
import wily.legacy.CustomModelSkins.cpm.shared.animation.IModelComponent;
import wily.legacy.CustomModelSkins.cpm.shared.config.Player;
import wily.legacy.CustomModelSkins.cpm.shared.definition.SafetyException.BlockReason;
import wily.legacy.CustomModelSkins.cpm.shared.effects.RenderEffects;
import wily.legacy.CustomModelSkins.cpm.shared.model.*;
import wily.legacy.CustomModelSkins.cpm.shared.model.render.BoxRender;
import wily.legacy.CustomModelSkins.cpm.shared.model.render.VanillaModelPart;
import wily.legacy.CustomModelSkins.cpm.shared.parts.IModelPart;
import wily.legacy.CustomModelSkins.cpm.shared.parts.IResolvedModelPart;
import wily.legacy.CustomModelSkins.cpm.shared.skin.TextureProvider;
import wily.legacy.CustomModelSkins.cpm.shared.skin.TextureType;
import wily.legacy.CustomModelSkins.cpm.shared.util.Log;
import wily.legacy.CustomModelSkins.cpm.shared.util.Msg;
import wily.legacy.CustomModelSkins.cpm.shared.util.TextureStitcher;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ModelDefinition {
    protected Map<VanillaModelPart, PartRoot> rootRenderingCubes;
    protected ModelLoadingState resolveState = ModelLoadingState.NEW;
    private AnimationRegistry animations = new AnimationRegistry();
    private ScaleData scale;
    public PartPosition fpLeftHand, fpRightHand;
    private boolean stitchedTexture;
    public boolean hideHeadIfSkull = true, removeArmorOffset, removeBedOffset;
    private final EnumSet<RenderEffects> usedEffects = EnumSet.noneOf(RenderEffects.class);
    private Throwable error;
    private ModelDefinitionLoader<?> loader;
    private Player<?> playerObj;
    protected List<IModelPart> parts = Collections.emptyList();
    protected List<IResolvedModelPart> resolved;
    protected Map<TextureSheetType, TextureProvider> textures;
    protected List<RenderedCube> cubes;
    protected Map<Integer, RenderedCube> cubeMap;
    protected TextureProvider skinTexture;
    protected wily.legacy.CustomModelSkins.cpm.shared.parts.ModelPartCloneable cloneable;

    public ModelDefinition(ModelDefinitionLoader<?> loader, Player<?> player) {
        this.loader = loader;
        this.playerObj = player;
    }

    public ModelDefinition(Throwable e, Player<?> player) {
        this.loader = null;
        this.playerObj = player;
        setError(e);
    }

    public ModelDefinition setParts(List<IModelPart> parts) {
        this.parts = parts;
        return this;
    }

    protected ModelDefinition() {
        this.loader = null;
        resolveState = ModelLoadingState.LOADED;
        this.playerObj = null;
    }

    public void startResolve() {
        resolveState = ModelLoadingState.RESOLVING;
        ModelDefinitionLoader.THREAD_POOL.execute(() -> {
            try {
                resolveAll();
            } catch (Throwable e) {
                setError(e);
            }
        });
    }

    public void validate() {
    }

    public void markEffectUsed(RenderEffects effect) {
        if (effect != null) usedEffects.add(effect);
    }

    public void resolveAll() throws IOException {
        if (loader == null) return;
        resolved = new ArrayList<>();
        for (IModelPart part : parts) resolved.add(part.resolve());
        textures = new HashMap<>();
        cubes = new ArrayList<>();
        rootRenderingCubes = new HashMap<>();
        Map<Integer, RootModelElement> playerModelParts = new HashMap<>();
        for (int i = 0; i < PlayerModelParts.VALUES.length; i++) {
            RootModelElement elem = new RootModelElement(PlayerModelParts.VALUES[i], this);
            rootRenderingCubes.put(PlayerModelParts.VALUES[i], new PartRoot(elem));
            playerModelParts.put(i, elem);
        }
        resolved.forEach(r -> r.preApply(this));
        for (RenderedCube rc : cubes) {
            int id = rc.getCube().parentId;
            RenderedCube p = playerModelParts.get(id);
            if (p != null) {
                p.addChild(rc);
                rc.setParent(p);
            }
            if (rc.getParent() == null) throw new IOException("Cube without parent");
        }
        int cc = cubes.size();
        for (RenderedCube rc : cubes)
            if (rc.extrude) cc += BoxRender.getExtrudeSize(rc.getCube().size, rc.getCube().texSize);
        TextureStitcher stitcher = new TextureStitcher(8192);
        if (textures.containsKey(TextureSheetType.SKIN)) {
            stitcher.setBase(textures.get(TextureSheetType.SKIN));
            skinTexture = textures.get(TextureSheetType.SKIN);
        } else {
            Image skin;
            try {
                skin = playerObj.getTextures().getTexture(TextureType.SKIN).get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e1) {
                throw new IOException(e1);
            }
            if (skin == null) skin = playerObj.getSkinType().getSkinTexture();
            stitcher.setBase(skin);
            skinTexture = new TextureProvider(skin, new Vec2i(64, 64));
        }
        resolved.forEach(r -> r.stitch(stitcher));
        TextureProvider tx = stitcher.finish();
        if (tx != null) {
            textures.put(TextureSheetType.SKIN, tx);
            skinTexture = tx;
        }
        stitchedTexture = stitcher.hasStitches();
        if (stitchedTexture) for (PlayerModelParts part : PlayerModelParts.VALUES)
            if (part != PlayerModelParts.CUSTOM_PART) convertPart(playerModelParts.get(part.ordinal()));
        cubeMap = new HashMap<>();
        cubes.addAll(playerModelParts.values());
        cubes.forEach(c -> cubeMap.put(c.getId(), c));
        resolved.forEach(r -> r.apply(this));
        animations.finishLoading();
        resetAnimationPos();
        resolveState = ModelLoadingState.LOADED;
        if (!usedEffects.isEmpty()) Log.info("CPM effects used: " + usedEffects);
        Log.debug(this);
    }

    protected void convertPart(RootModelElement p) {
        PlayerModelParts part = (PlayerModelParts) p.getPart();
        if (!p.isHidden()) {
            p.setHidden(true);
            Cube cube = new Cube();
            PlayerPartValues val = PlayerPartValues.getFor(part, playerObj.getSkinType());
            cube.offset = val.getOffset();
            cube.rotation = new Vec3f(0, 0, 0);
            cube.pos = new Vec3f(0, 0, 0);
            cube.size = val.getSize();
            cube.scale = new Vec3f(1, 1, 1);
            cube.meshScale = new Vec3f(1, 1, 1);
            cube.u = val.u;
            cube.v = val.v;
            cube.texSize = 1;
            cube.id = 0xfff0 + part.ordinal();
            RenderedCube rc = new RenderedCube(cube);
            rc.setParent(p);
            p.addChild(rc);
        }
    }

    public void cleanup() {
        resolveState = ModelLoadingState.CLEANED_UP;
        if (loader == null) return;
        if (cubes != null) cubes.forEach(c -> {
            if (c.renderObject != null) c.renderObject.free();
            c.renderObject = null;
        });
        if (textures != null) textures.values().forEach(TextureProvider::free);
        cubes = null;
        textures = null;
    }

    public boolean doRender() {
        return loader != null && resolveState == ModelLoadingState.LOADED;
    }

    public ModelLoadingState getResolveState() {
        return resolveState;
    }

    public PartRoot getModelElementFor(VanillaModelPart part) {
        return rootRenderingCubes.get(part);
    }

    public AnimationRegistry getAnimations() {
        return animations;
    }

    public Player<?> getPlayerObj() {
        return playerObj;
    }

    public RenderedCube getElementById(int id) {
        return cubeMap.get(id);
    }

    public void resetAnimationPos() {
        cubes.forEach(IModelComponent::reset);
    }

    @Override
    public String toString() {
        StringBuilder bb = new StringBuilder("ModelDefinition\n\tResolved: ").append(resolveState);
        switch (resolveState) {
            case NEW:
            case RESOLVING:
                bb.append("\n\tParts:");
                for (IModelPart p : parts) bb.append("\n\t\t").append(p.toString().replace("\n", "\n\t\t"));
                break;
            case LOADED:
                bb.append("\n\tCubes: ").append(cubes.size()).append("\n\tOther:");
                for (IResolvedModelPart p : resolved) bb.append("\n\t\t").append(p.toString().replace("\n", "\n\t\t"));
                break;
            case ERRORRED:
            case SAFETY_BLOCKED:
                bb.append("\n\t\t");
                StringBuilderStream.stacktraceToString(error, bb, "\n\t\t");
                if (error instanceof SafetyException) bb.append(((SafetyException) error).getBlockReason());
                else bb.append("Unexpected error");
                break;
            default:
                break;
        }
        return bb.toString();
    }

    public RootModelElement addRoot(int baseID, VanillaModelPart type) {
        RootModelElement elem = new RootModelElement(type, this);
        elem.children = new ArrayList<>();
        rootRenderingCubes.computeIfAbsent(type, k -> new PartRoot(elem)).add(elem);
        cubes.add(elem);
        cubeMap.put(baseID, elem);
        if (type instanceof PlayerModelParts && stitchedTexture) convertPart(elem);
        return elem;
    }

    public TextureProvider getTexture(TextureSheetType key, boolean inGui) {
        if (key == TextureSheetType.SKIN && inGui) return skinTexture;
        return key.editable ? (textures == null ? null : textures.get(key)) : null;
    }

    public void setTexture(TextureSheetType key, TextureProvider value) {
        textures.put(key, value);
    }

    public SkinType getSkinType() {
        return playerObj.getSkinType();
    }

    public void setScale(ScaleData scale) {
        this.scale = scale;
    }

    public ScaleData getScale() {
        return scale;
    }

    private void clear() {
        parts = Collections.emptyList();
        resolved = null;
        cubes = null;
        cubeMap = null;
        textures = null;
        rootRenderingCubes = null;
        animations = null;
        scale = null;
    }

    public static enum ModelLoadingState {NEW, RESOLVING, LOADED, SAFETY_BLOCKED, ERRORRED, CLEANED_UP}

    public BlockReason getBlockReason() {
        return error instanceof SafetyException ? ((SafetyException) error).getBlockReason() : null;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable ex) {
        cleanup();
        if (ex instanceof ExecutionException || ex instanceof UncheckedExecutionException) ex = ex.getCause();
        resolveState = ex instanceof SafetyException ? ModelLoadingState.SAFETY_BLOCKED : ModelLoadingState.ERRORRED;
        error = ex;
        if (!(ex instanceof IOException || ex instanceof SafetyException)) Log.error("Failed to load model", ex);
        clear();
    }

    public boolean isHideHeadIfSkull() {
        return hideHeadIfSkull;
    }

    public boolean isRemoveArmorOffset() {
        return removeArmorOffset;
    }

    public Msg getStatus() {
        switch (getResolveState()) {
            case ERRORRED:
                if (getError() instanceof LocalizedException)
                    return new Msg("label.cpm.errorLoadingModel", ((LocalizedException) getError()).getLocalizedText());
                return new Msg("label.cpm.errorLoadingModel", getError().toString());
            case NEW:
            case RESOLVING:
                return new Msg("label.cpm.loading");
            case SAFETY_BLOCKED:
                if (getBlockReason() == BlockReason.BLOCK_LIST) return null;
                return new Msg("label.cpm.safetyBlocked");
            case LOADED:
            case CLEANED_UP:
            default:
                return null;
        }
    }

    public void addCubes(Collection<RenderedCube> cubes) {
        this.cubes.addAll(cubes);
    }

    public void setCloneable(wily.legacy.CustomModelSkins.cpm.shared.parts.ModelPartCloneable c) {
        this.cloneable = c;
    }
}
