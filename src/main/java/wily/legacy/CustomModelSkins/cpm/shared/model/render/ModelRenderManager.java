package wily.legacy.CustomModelSkins.cpm.shared.model.render;

import wily.legacy.CustomModelSkins.cpl.function.ToFloatFunction;
import wily.legacy.CustomModelSkins.cpl.math.*;
import wily.legacy.CustomModelSkins.cpl.render.RenderTypes;
import wily.legacy.CustomModelSkins.cpl.render.VBuffers;
import wily.legacy.CustomModelSkins.cpl.render.VertexBuffer;
import wily.legacy.CustomModelSkins.cpm.shared.IPlayerRenderManager;
import wily.legacy.CustomModelSkins.cpm.shared.animation.AnimationEngine;
import wily.legacy.CustomModelSkins.cpm.shared.animation.AnimationEngine.AnimationMode;
import wily.legacy.CustomModelSkins.cpm.shared.config.Player;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition;
import wily.legacy.CustomModelSkins.cpm.shared.definition.ModelDefinition.ModelLoadingState;
import wily.legacy.CustomModelSkins.cpm.shared.model.*;
import wily.legacy.CustomModelSkins.cpm.shared.model.render.BatchedBuffers.BufferOutput;
import wily.legacy.CustomModelSkins.cpm.shared.skin.TextureProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public abstract class ModelRenderManager<D, S, P, MB> implements IPlayerRenderManager {
    public static final Predicate<Player<?>> ALWAYS = p -> true;
    protected Map<MB, RedirectHolder<MB, D, S, P>> holders = new HashMap<>();
    private RedirectHolderFactory<D, S, P> factory;
    private RedirectRendererFactory<MB, S, P> redirectFactory;
    private final AnimationEngine animEngine = new AnimationEngine();
    private ModelPartVec3fSetter<P> posSet, rotSet;
    private ToFloatFunction<P> px, py, pz, rx, ry, rz;
    private Predicate<P> getVis;
    private BoolSetter<P> setVis;
    private P renderPart;

    public void setFactory(RedirectHolderFactory<D, S, P> factory) {
        this.factory = factory;
    }

    public void setRedirectFactory(RedirectRendererFactory<MB, S, P> redirectFactory) {
        this.redirectFactory = redirectFactory;
    }

    public void setModelSetters(ModelPartVec3fSetter<P> posSet, ModelPartVec3fSetter<P> rotSet) {
        this.posSet = posSet;
        this.rotSet = rotSet;
    }

    public void setModelPosGetters(ToFloatFunction<P> x, ToFloatFunction<P> y, ToFloatFunction<P> z) {
        this.px = x;
        this.py = y;
        this.pz = z;
    }

    public void setModelRotGetters(ToFloatFunction<P> x, ToFloatFunction<P> y, ToFloatFunction<P> z) {
        this.rx = x;
        this.ry = y;
        this.rz = z;
    }

    public void setRenderPart(P renderPart) {
        this.renderPart = renderPart;
    }

    public void setVis(Predicate<P> getVis, BoolSetter<P> setVis) {
        this.getVis = getVis;
        this.setVis = setVis;
    }

    public void bindModel(MB model, String arg, D addDt, ModelDefinition def, Player<?> player, AnimationMode mode) {
        getHolderSafe(model, arg, h -> h.swapIn(def, addDt, player, mode), true);
    }

    public void bindModel(MB model, D addDt, ModelDefinition def, Player<?> player, AnimationMode mode) {
        bindModel(model, null, addDt, def, player, mode);
    }

    public void bindSubModel(MB mainModel, MB subModel, String arg) {
        RedirectHolder<MB, D, S, P> h = holders.get(mainModel);
        if (h != null && h.swappedIn) getHolderSafe(subModel, arg, s -> s.swapIn(h), true);
    }

    public void unbindModel(MB model) {
        RedirectHolder<MB, D, S, P> h = holders.get(model);
        if (h != null) h.swapOut();
    }

    public void bindSkin(MB model, String arg, S cbi, TextureSheetType tex) {
        getHolderSafe(model, arg, h -> h.bindTexture0(cbi, tex), false);
    }

    public void bindSkin(MB model, S cbi, TextureSheetType tex) {
        bindSkin(model, null, cbi, tex);
    }

    public void setModelPose(MB model) {
        getHolderSafe(model, null, RedirectHolder::poseSetup, false);
    }

    public boolean isBound(MB model, String arg) {
        return getHolderSafe(model, arg, h -> h.swappedIn, false, false);
    }

    public boolean isBound(MB model) {
        return isBound(model, null);
    }

    public <R> R getHolderSafe(MB model, String arg, Function<RedirectHolder<MB, D, S, P>, R> func, R def, boolean make) {
        RedirectHolder<MB, D, S, P> h = holders.get(model);
        if (h == null && make) {
            h = (RedirectHolder<MB, D, S, P>) factory.create(model, arg);
            if (h != null) holders.put(model, h);
        }
        if (h == null) return def;
        else return func.apply(h);
    }

    public void getHolderSafe(MB model, String arg, Consumer<RedirectHolder<MB, D, S, P>> func, boolean make) {
        RedirectHolder<MB, D, S, P> h = holders.get(model);
        if (h == null && make) {
            h = (RedirectHolder<MB, D, S, P>) factory.create(model, arg);
            if (h != null) holders.put(model, h);
        }
        if (h != null) func.accept(h);
    }

    public void flushBatch(MB model, String arg) {
        getHolderSafe(model, arg, RedirectHolder::flushBatch, false);
    }

    public static interface RedirectHolderFactory<D, S, P> {
        <M> RedirectHolder<?, D, S, P> create(M model, String arg);
    }

    public static abstract class RedirectHolder<M, D, S, P> {
        protected final ModelRenderManager<D, S, P, M> mngr;
        public final M model;
        public ModelDefinition def;
        public boolean swappedIn;
        public int sheetX, sheetY;
        public D addDt;
        public List<Field<P>> modelFields;
        public List<RedirectRenderer<P>> redirectRenderers;
        public boolean preRenderSetup, skinBound;
        public Map<RedirectRenderer<P>, RedirectDataHolder<P>> partData;
        public Player<?> playerObj;
        public AnimationMode mode;
        public RenderTypes<RenderMode> renderTypes;
        private boolean loggedWarning;
        private RedirectHolder<M, D, S, P> parent;
        public BatchedBuffers<D, ?, ?> batch;

        public RedirectHolder(ModelRenderManager<D, S, P, M> mngr, M model) {
            this.model = model;
            this.mngr = mngr;
            modelFields = new ArrayList<>();
            redirectRenderers = new ArrayList<>();
            partData = new HashMap<>();
            renderTypes = new RenderTypes<>(RenderMode.class);
        }

        public void swapIn(RedirectHolder<M, D, S, P> h) {
            swapIn(h.def, h.addDt, h.playerObj, h.mode);
            parent = h;
        }

        public final void swapIn(ModelDefinition def, D addDt, Player<?> playerObj, AnimationMode mode) {
            this.def = def;
            this.addDt = addDt;
            this.mode = mode;
            if (swappedIn) return;
            swapIn0();
            for (int i = 0; i < modelFields.size(); i++) {
                Field<P> field = modelFields.get(i);
                RedirectRenderer<P> rd = redirectRenderers.get(i);
                field.set.accept(rd.swapIn());
            }
            partData.values().forEach(RedirectDataHolder::reset);
            this.playerObj = playerObj;
            swappedIn = true;
        }

        public final void swapOut() {
            this.def = null;
            this.addDt = null;
            skinBound = false;
            preRenderSetup = false;
            this.renderTypes.clear();
            this.playerObj = null;
            parent = null;
            if (!swappedIn) return;
            swapOut0();
            for (int i = 0; i < modelFields.size(); i++) {
                Field<P> field = modelFields.get(i);
                field.set.accept(redirectRenderers.get(i).swapOut());
            }
            swappedIn = false;
        }

        private void rebind() {
            if (!swappedIn) return;
            ModelDefinition def = this.def;
            D addDt = this.addDt;
            Player<?> playerObj = this.playerObj;
            AnimationMode mode = this.mode;
            swapOut();
            swapIn(def, addDt, playerObj, mode);
        }

        private void poseSetup() {
            if (def == null) return;
            if (!preRenderSetup) {
                preRenderSetup = true;
                bindFirstSetup(true);
            }
        }

        private void bindTexture0(S cbi, TextureSheetType tex) {
            if (def == null) return;
            TextureProvider skin = def.getTexture(tex, isDirectMode());
            if (skin != null && skin.texture != null) {
                bindTexture(cbi, skin, tex);
                sheetX = skin.getSize().x;
                sheetY = skin.getSize().y;
            } else {
                Vec2i s = tex.getDefSize();
                sheetX = s.x;
                sheetY = s.y;
                bindDefaultTexture(cbi, tex);
            }
            setupRenderSystem(cbi, tex);
        }

        protected void bindTexture(S cbi, TextureProvider skin, TextureSheetType tex) {
            bindTexture(cbi, skin);
        }

        protected void setupRenderSystem(S cbi, TextureSheetType tex) {
        }

        protected void bindTexture(S cbi, TextureProvider skin) {
        }

        protected abstract void swapIn0();

        protected abstract void swapOut0();

        protected void bindSkin() {
        }

        protected void bindDefaultTexture(S cbi, TextureSheetType tex) {
        }

        protected void bindFirstSetup(boolean isPoseSetup) {
            if (playerObj != null) {
                playerObj.updateFromModel(model);
                mngr.animEngine.handleAnimation(playerObj, mode, def);
            }
            if (def.getResolveState() != ModelLoadingState.LOADED) return;
            for (int i = 0; i < redirectRenderers.size(); i++) {
                RedirectRenderer<P> re = redirectRenderers.get(i);
                VanillaModelPart part = re.getPart();
                if (part == null) continue;
                if (part.needsPoseSetup() && !isDirectMode() && !isPoseSetup) continue;
                PartRoot elems = def.getModelElementFor(part);
                if (elems == null) continue;
                P tp = re.getThisPart();
                float px = mngr.px.apply(tp);
                float py = mngr.py.apply(tp);
                float pz = mngr.pz.apply(tp);
                float rx = mngr.rx.apply(tp);
                float ry = mngr.ry.apply(tp);
                float rz = mngr.rz.apply(tp);
                elems.setRootPosAndRot(px, py, pz, rx, ry, rz);
            }
            def.getAnimations().applyCopy();
        }

        protected RedirectRenderer<P> register(Field<P> f) {
            RedirectRenderer<P> rd = mngr.redirectFactory.create(model, this, f.get, f.part);
            modelFields.add(f);
            redirectRenderers.add(rd);
            partData.put(rd, new RedirectDataHolder<>());
            return rd;
        }

        protected RedirectRenderer<P> register(Field<P> f, Predicate<Player<?>> doRender) {
            return register(f).setRenderPredicate(doRender);
        }

        protected RedirectRenderer<P> registerHead(Field<P> f) {
            return register(f).setRenderPredicate(p -> !p.animState.hasSkullOnHead || !def.isHideHeadIfSkull());
        }

        public void copyModel(P from, P to) {
            mngr.posSet.set(to, mngr.px.apply(from), mngr.py.apply(from), mngr.pz.apply(from));
            mngr.rotSet.set(to, mngr.rx.apply(from), mngr.ry.apply(from), mngr.rz.apply(from));
            mngr.setVis.set(to, mngr.getVis.test(from));
        }

        protected boolean skipTransform(RedirectRenderer<P> part) {
            return false;
        }

        protected boolean isDirectMode() {
            return false;
        }

        protected void setupTransform(MatrixStack stack, RedirectRenderer<P> forPart, boolean pre) {
            if (pre && mode == AnimationMode.PLAYER && playerObj != null && wily.legacy.CustomModelSkins.cpm.shared.util.UpsideDownModelFix.isFlipped(playerObj.getUUID())) {
                stack.rotate(new Rotation(0, 0, 180, true).asQ());
                stack.translate(0, -1.0f, 0);
            }
            if (pre && def != null && def.getScale() != null && mode == AnimationMode.PLAYER) {
                transform(stack, def.getScale());
            }
            if (pre && def != null && def.removeBedOffset && mode == AnimationMode.PLAYER && playerObj != null && !playerObj.isClientPlayer() && playerObj.animState.sleeping) {
                stack.translate(0, 0, 0.125f);
            }
            if (pre && def != null && mode == AnimationMode.HAND) {
                if (forPart.getPart() == PlayerModelParts.LEFT_ARM && def.fpLeftHand != null)
                    transform(stack, def.fpLeftHand);
                else if (forPart.getPart() == PlayerModelParts.RIGHT_ARM && def.fpRightHand != null)
                    transform(stack, def.fpRightHand);
            }
        }

        public void transform(MatrixStack stack, PartPosition pos) {
            if (pos != null) {
                Vec3f p = pos.getRPos();
                Rotation r = pos.getRRotation();
                Vec3f s = pos.getRScale();
                RedirectRenderer.translateRotate(p.x, p.y, p.z, r, stack);
                float sx = s.x;
                float sy = s.y;
                float sz = s.z;
                if (sx < 0.1F || sx > 10) sx = 1;
                if (sy < 0.1F || sy > 10) sy = 1;
                if (sz < 0.1F || sz > 10) sz = 1;
                stack.scale(sx, sy, sz);
            }
        }

        public <V> VBuffers nextBatch(Supplier<BufferOutput<V>> prepBuffer, V def) {
            RedirectHolder<M, D, S, P> h = parent != null ? parent : this;
            return ((BatchedBuffers<D, ?, V>) h.batch).nextBatch(prepBuffer, def);
        }

        public void flushBatch() {
            if (batch != null) batch.flush();
        }

        protected boolean enableParentRendering(VanillaModelPart part) {
            return true;
        }
    }

    private static class RedirectDataHolder<P> {
        private RedirectRenderer<P> copyFrom;
        private Predicate<Player<?>> renderPredicate = ALWAYS;
        private boolean wasCalled;

        public void reset() {
            wasCalled = false;
        }
    }

    public static interface RedirectRenderer<P> {
        default P getThisPart() {
            return (P) this;
        }

        P swapIn();

        P swapOut();

        RedirectHolder<?, ?, ?, P> getHolder();

        P getParent();

        VanillaModelPart getPart();

        void renderParent();

        VBuffers getVBuffers();

        Vec4f getColor();

        default RedirectRenderer<P> setCopyFrom(RedirectRenderer<P> from) {
            getHolder().partData.get(this).copyFrom = from;
            return this;
        }

        default RedirectRenderer<P> setRenderPredicate(Predicate<Player<?>> renderPredicate) {
            getHolder().partData.get(this).renderPredicate = renderPredicate;
            return this;
        }

        default void render() {
            RedirectHolder<?, ?, ?, P> holder = getHolder();
            ModelRenderManager<?, ?, P, ?> mngr = holder.mngr;
            P tp = getThisPart();
            P parent = getParent();
            if (holder.mngr.getVis.test(tp)) {
                if (holder.def != null) {
                    RedirectDataHolder<P> dh = holder.partData.get(this);
                    boolean noReset = dh.wasCalled;
                    dh.wasCalled = true;
                    VanillaModelPart part = getPart();
                    if (!holder.preRenderSetup) {
                        holder.preRenderSetup = true;
                        holder.bindFirstSetup(false);
                    }
                    if (holder.def.getResolveState() != ModelLoadingState.LOADED) return;
                    if (!holder.skinBound) {
                        holder.skinBound = true;
                        holder.bindSkin();
                    }
                    if (part == null) {
                        if (dh.copyFrom != null) {
                            holder.copyModel(dh.copyFrom.getThisPart(), tp);
                        }
                        return;
                    }
                    PartRoot elems = holder.def.getModelElementFor(part);
                    if (elems != null) {
                        if (elems.isEmpty()) return;
                        boolean skipTransform = holder.mode == AnimationMode.SKULL || holder.mode == AnimationMode.HAND || holder.skipTransform(this);
                        float px = mngr.px.apply(tp);
                        float py = mngr.py.apply(tp);
                        float pz = mngr.pz.apply(tp);
                        float rx = mngr.rx.apply(tp);
                        float ry = mngr.ry.apply(tp);
                        float rz = mngr.rz.apply(tp);
                        boolean doRender = holder.playerObj == null || dh.renderPredicate.test(holder.playerObj);
                        elems.forEach(elem -> {
                            if (!elem.renderPart()) return;
                            if (holder.def.isRemoveArmorOffset() && elems.getMainRoot() != elem && part.getCopyFrom() != null) {
                                elem.setPosAndRot(holder.def.getModelElementFor(part.getCopyFrom()));
                            }
                            if (!skipTransform) {
                                mngr.posSet.set(tp, elem.getPos());
                                mngr.rotSet.set(tp, elem.getRot().asVec3f(false));
                            }
                            if (elem.doDisplay() && doRender && holder.enableParentRendering(part)) {
                                if (noReset && !skipTransform) {
                                    float _px = mngr.px.apply(parent);
                                    float _py = mngr.py.apply(parent);
                                    float _pz = mngr.pz.apply(parent);
                                    float _rx = mngr.rx.apply(parent);
                                    float _ry = mngr.ry.apply(parent);
                                    float _rz = mngr.rz.apply(parent);
                                    holder.copyModel(tp, parent);
                                    renderParent();
                                    mngr.posSet.set(parent, _px, _py, _pz);
                                    mngr.rotSet.set(parent, _rx, _ry, _rz);
                                } else {
                                    holder.copyModel(tp, parent);
                                    renderParent();
                                }
                            }
                            doRender0(elem, doRender);
                        });
                        if (!noReset) {
                            mngr.posSet.set(parent, px, py, pz);
                            mngr.rotSet.set(parent, rx, ry, rz);
                        }
                        if (!skipTransform) {
                            RootModelElement elem = elems.getMainRoot();
                            mngr.posSet.set(tp, elem.getPos());
                            mngr.rotSet.set(tp, elem.getRot().asVec3f(false));
                        }
                    } else {
                        holder.copyModel(tp, parent);
                        renderParent();
                    }
                } else {
                    holder.copyModel(tp, parent);
                    renderParent();
                }
            }
        }

        public default void translateRotate(RenderedCube rc, MatrixStack matrixStackIn) {
            translateRotate(rc.pos.x, rc.pos.y, rc.pos.z, rc.rotation, matrixStackIn);
            if (rc.renderScale.x != 1 || rc.renderScale.y != 1 || rc.renderScale.z != 1 || rc.renderScale.x > 0 || rc.renderScale.y > 0 || rc.renderScale.z > 0) {
                matrixStackIn.scale(Math.max(rc.renderScale.x, 0.01f), Math.max(rc.renderScale.y, 0.01f), Math.max(rc.renderScale.z, 0.01f));
            }
        }

        public static void translateRotate(float px, float py, float pz, Rotation rot, MatrixStack matrixStackIn) {
            matrixStackIn.translate(px / 16.0F, py / 16.0F, pz / 16.0F);
            matrixStackIn.rotate(rot.asQ());
        }

        public default void translateRotate(MatrixStack matrixStackIn) {
            RedirectHolder<?, ?, ?, P> holder = getHolder();
            ModelRenderManager<?, ?, P, ?> m = holder.mngr;
            P tp = getThisPart();
            translateRotate(m.px.apply(tp), m.py.apply(tp), m.pz.apply(tp), new Rotation(m.rx.apply(tp), m.ry.apply(tp), m.rz.apply(tp), false), matrixStackIn);
        }

        public default void render(RenderedCube elem, MatrixStack matrixStackIn, VBuffers buf, float red, float green, float blue, float alpha, boolean doRenderRoot, boolean doRenderElems) {
            RedirectHolder<?, ?, ?, P> holder = getHolder();
            if (holder.def instanceof IExtraRenderDefinition && buf != null) {
                ((IExtraRenderDefinition) holder.def).render(this, matrixStackIn, buf, holder.renderTypes, elem, doRenderElems);
            }
            if (elem.children == null) return;
            for (RenderedCube cube : elem.children) {
                matrixStackIn.push();
                translateRotate(cube, matrixStackIn);
                if (!cube.display) {
                    render(cube, matrixStackIn, buf, red, green, blue, alpha, doRenderRoot, false);
                    matrixStackIn.pop();
                    continue;
                }
                if (buf != null && doRenderElems) {
                    float r = red;
                    float g = green;
                    float b = blue;
                    if (cube.color != 0xffffff) {
                        r *= ((cube.color & 0xff0000) >> 16) / 255f;
                        g *= ((cube.color & 0x00ff00) >> 8) / 255f;
                        b *= (cube.color & 0x0000ff) / 255f;
                    }
                    if (cube.updateObject || cube.renderObject == null) {
                        if (cube.renderObject != null) cube.renderObject.free();
                        cube.renderObject = createBox(cube, holder);
                        cube.updateObject = false;
                    }
                    Mesh mesh = cube.renderObject;
                    VertexBuffer buffer = buf.getBuffer(holder.renderTypes, mesh.getLayer());
                    mesh.draw(matrixStackIn, buffer, r, g, b, alpha);
                }
                render(cube, matrixStackIn, buf, red, green, blue, alpha, doRenderRoot, doRenderElems);
                matrixStackIn.pop();
            }
        }

        public default void doRender0(RootModelElement elem, boolean doRender) {
            RedirectHolder<?, ?, ?, P> holder = getHolder();
            MatrixStack stack = new MatrixStack();
            holder.setupTransform(stack, this, true);
            translateRotate(stack);
            holder.setupTransform(stack, this, false);
            if (doRender) {
                Vec4f color = getColor();
                VBuffers bufIn = getVBuffers();
                VBuffers buf;
                if (!bufIn.isBatched()) buf = bufIn.replay();
                else buf = bufIn;
                render(elem, stack, buf, color.x, color.y, color.z, color.w, doRender, true);
                if (!bufIn.isBatched()) buf.finishAll();
            } else {
                render(elem, stack, null, 1, 1, 1, 1, doRender, false);
            }
        }

        public default MatrixStack.Entry getPartTransform() {
            return null;
        }
    }

    private static Mesh createBox(RenderedCube elem, RedirectHolder<?, ?, ?, ?> holder) {
        Cube c = elem.getCube();
        if (c.size.x == 0 && c.size.y == 0 && c.size.z == 0 && c.mcScale == 0) {
            return Mesh.EMPTY;
        }
        if (c.texSize == 0) {
            return BoxRender.createColored(c.offset.x, c.offset.y, c.offset.z, c.size.x * c.meshScale.x, c.size.y * c.meshScale.y, c.size.z * c.meshScale.z, c.mcScale, holder.sheetX, holder.sheetY);
        } else {
            if (elem.singleTex)
                return BoxRender.createTexturedSingle(c.offset, c.size, c.meshScale, c.mcScale, c.u, c.v, c.texSize, holder.sheetX, holder.sheetY);
            else if (elem.extrude)
                return BoxRender.createTexturedExtruded(c.offset, c.size, c.meshScale, c.mcScale, c.u, c.v, c.texSize, holder.sheetX, holder.sheetY);
            else if (elem.faceUVs != null)
                return BoxRender.createTextured(c.offset, c.size, c.meshScale, c.mcScale, elem.faceUVs, c.texSize, holder.sheetX, holder.sheetY);
            else
                return BoxRender.createTextured(c.offset, c.size, c.meshScale, c.mcScale, c.u, c.v, c.texSize, holder.sheetX, holder.sheetY);
        }
    }

    public static class Field<V> {
        private Supplier<V> get;
        private Consumer<V> set;
        private VanillaModelPart part;

        public Field(Supplier<V> get, Consumer<V> set, VanillaModelPart part) {
            this.part = part;
            this.get = get;
            this.set = set;
        }
    }

    @FunctionalInterface
    public static interface RedirectRendererFactory<M, S, P> {
        RedirectRenderer<P> create(M model, RedirectHolder<M, ?, S, P> access, Supplier<P> modelPart, VanillaModelPart part);
    }

    @FunctionalInterface
    public static interface ModelPartVec3fSetter<P> {
        void set(P model, float x, float y, float z);

        default void set(P p, Vec3f v) {
            set(p, v.x, v.y, v.z);
        }
    }

    @FunctionalInterface
    public static interface BoolSetter<P> {
        void set(P p, boolean v);
    }

    @Override
    public AnimationEngine getAnimationEngine() {
        return animEngine;
    }
}
