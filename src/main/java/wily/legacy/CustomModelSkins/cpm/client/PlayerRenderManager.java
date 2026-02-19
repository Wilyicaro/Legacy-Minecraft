package wily.legacy.CustomModelSkins.cpm.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import wily.legacy.CustomModelSkins.cpl.math.Vec4f;
import wily.legacy.CustomModelSkins.cpl.render.VBuffers;
import wily.legacy.CustomModelSkins.cpm.client.MinecraftObject.DynTexture;
import wily.legacy.mixin.base.cpm.access.HumanoidModelAccessor;
import wily.legacy.mixin.base.cpm.access.ModelAccessor;
import wily.legacy.mixin.base.cpm.access.PlayerModelAccessor;
import wily.legacy.CustomModelSkins.cpm.shared.model.PlayerModelParts;
import wily.legacy.CustomModelSkins.cpm.shared.model.RootModelType;
import wily.legacy.CustomModelSkins.cpm.shared.model.TextureSheetType;
import wily.legacy.CustomModelSkins.cpm.shared.model.render.ModelRenderManager;
import wily.legacy.CustomModelSkins.cpm.shared.model.render.VanillaModelPart;
import wily.legacy.CustomModelSkins.cpm.shared.skin.TextureProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PlayerRenderManager extends ModelRenderManager<Void, ModelTexture, ModelPart, Model> {
    public static final Function<ResourceLocation, RenderType> entity = RenderType::entityTranslucent;
    public static final Function<ResourceLocation, RenderType> armor = RenderType::armorCutoutNoCull;

    public PlayerRenderManager() {
        setFactory(new RedirectHolderFactory<Void, ModelTexture, ModelPart>() {
            @SuppressWarnings("unchecked")
            @Override
            public <M> RedirectHolder<?, Void, ModelTexture, ModelPart> create(M model, String arg) {
                if ("api".equals(arg) && model instanceof HumanoidModel) {
                    return new RedirectHolderApi(PlayerRenderManager.this, (HumanoidModel<HumanoidRenderState>) model);
                } else if (model instanceof PlayerCapeModel) {
                    return new RedirectHolderCape(PlayerRenderManager.this, (PlayerCapeModel) model);
                } else if (model instanceof HumanoidModel && "armor1".equals(arg)) {
                    return new RedirectHolderArmorHelm(PlayerRenderManager.this, (HumanoidModel) model);
                } else if (model instanceof HumanoidModel && "armor2".equals(arg)) {
                    return new RedirectHolderArmorLegs(PlayerRenderManager.this, (HumanoidModel) model);
                } else if (model instanceof HumanoidModel && "armor3".equals(arg)) {
                    return new RedirectHolderArmorChest(PlayerRenderManager.this, (HumanoidModel) model);
                } else if (model instanceof HumanoidModel && "armor4".equals(arg)) {
                    return new RedirectHolderArmorFeet(PlayerRenderManager.this, (HumanoidModel) model);
                } else if (model instanceof PlayerModel) {
                    return new RedirectHolderPlayer(PlayerRenderManager.this, (PlayerModel) model);
                } else if (model instanceof SkullModel) {
                    return new RedirectHolderSkull(PlayerRenderManager.this, (SkullModel) model);
                } else if (model instanceof ElytraModel) {
                    return new RedirectHolderElytra(PlayerRenderManager.this, (ElytraModel) model);
                }
                return null;
            }
        });
        setRedirectFactory(new RedirectRendererFactory<Model, ModelTexture, ModelPart>() {
            @Override
            public RedirectRenderer<ModelPart> create(Model model, RedirectHolder<Model, ?, ModelTexture, ModelPart> access, Supplier<ModelPart> modelPart, VanillaModelPart part) {
                return new RedirectModelRenderer((RDH<?>) access, modelPart, part);
            }
        });
        setVis(m -> m.visible, (m, v) -> m.visible = v);
        setModelPosGetters(m -> m.x, m -> m.y, m -> m.z);
        setModelRotGetters(m -> m.xRot, m -> m.yRot, m -> m.zRot);
        setModelSetters((m, x, y, z) -> {
            m.x = x;
            m.y = y;
            m.z = z;
        }, (m, x, y, z) -> {
            m.xRot = x;
            m.yRot = y;
            m.zRot = z;
        });
        setRenderPart(new ModelPart(Collections.emptyList(), Collections.emptyMap()));
    }

    public static abstract class RDH<M extends Model> extends ModelRenderManager.RedirectHolder<Model, Void, ModelTexture, ModelPart> {
        public List<Supplier<ModelPart>> renderedParts = new ArrayList<>();
        private ModelPart rootPart;
        private ModelPart rootRenderer;

        public RDH(ModelRenderManager<Void, ModelTexture, ModelPart, Model> mngr, M model) {
            this(mngr, model, false);
        }

        public RDH(ModelRenderManager<Void, ModelTexture, ModelPart, Model> mngr, M model, boolean storeStateOnSubmit) {
            super(mngr, model);
            rootPart = getRoot();
            rootRenderer = new ModelPart(Collections.emptyList(), Collections.emptyMap());
            ModelPartHooks.registerSelfRenderer(rootRenderer, new RootModelPart(this, storeStateOnSubmit));
        }

        @Override
        public void setupRenderSystem(ModelTexture cbi, TextureSheetType tex) {
            CustomPlayerModelsClient.mc.renderBuilder.build(renderTypes, cbi);
        }

        @Override
        protected void bindTexture(ModelTexture cbi, TextureProvider skin) {
            skin.bind();
            cbi.setTexture(DynTexture.getBoundLoc());
        }

        @Override
        public void swapOut0() {
            setRoot(rootPart);
        }

        @Override
        public void swapIn0() {
            setRoot(rootRenderer);
        }

        protected Field<ModelPart> createRendered(Supplier<ModelPart> get, Consumer<ModelPart> set, VanillaModelPart part) {
            renderedParts.add(get);
            return new Field<>(get, set, part);
        }

        protected Field<ModelPart> create2ndLayer(Supplier<ModelPart> get, Consumer<ModelPart> set, RedirectRenderer<ModelPart> layer2) {
            if (layer2 instanceof RedirectModelRenderer r) {
                r.layer2 = get.get();
            }
            return new Field<>(get, set, null);
        }

        protected ModelPart getRoot() {
            return ((ModelAccessor) (Object) model()).cpm$getRoot();
        }

        protected void setRoot(ModelPart part) {
            ((ModelAccessor) (Object) model()).cpm$setRoot(part);
        }

        @SuppressWarnings("unchecked")
        protected M model() {
            return (M) model;
        }
    }

    private static class RedirectHolderPlayer extends RDH<PlayerModel> {
        private RedirectRenderer<ModelPart> head;
        private RedirectRenderer<ModelPart> body;
        private RedirectRenderer<ModelPart> leftArm;
        private RedirectRenderer<ModelPart> rightArm;
        private RedirectRenderer<ModelPart> leftLeg;
        private RedirectRenderer<ModelPart> rightLeg;

        public RedirectHolderPlayer(PlayerRenderManager mngr, PlayerModel model) {
            super(mngr, model, true);
            head = registerHead(createRendered(() -> ((HumanoidModelAccessor) (Object) model).cpm$getHead(), v -> ((HumanoidModelAccessor) (Object) model).cpm$setHead(v), PlayerModelParts.HEAD));
            body = register(createRendered(() -> model.body, v -> ((HumanoidModelAccessor) (Object) model).cpm$setBody(v), PlayerModelParts.BODY));
            rightArm = register(createRendered(() -> model.rightArm, v -> ((HumanoidModelAccessor) (Object) model).cpm$setRightArm(v), PlayerModelParts.RIGHT_ARM));
            leftArm = register(createRendered(() -> model.leftArm, v -> ((HumanoidModelAccessor) (Object) model).cpm$setLeftArm(v), PlayerModelParts.LEFT_ARM));
            rightLeg = register(createRendered(() -> model.rightLeg, v -> ((HumanoidModelAccessor) (Object) model).cpm$setRightLeg(v), PlayerModelParts.RIGHT_LEG));
            leftLeg = register(createRendered(() -> model.leftLeg, v -> ((HumanoidModelAccessor) (Object) model).cpm$setLeftLeg(v), PlayerModelParts.LEFT_LEG));
            register(create2ndLayer(() -> model.hat, v -> ((HumanoidModelAccessor) (Object) model).cpm$setHat(v), head));
            register(create2ndLayer(() -> model.leftSleeve, v -> ((PlayerModelAccessor) (Object) model).cpm$setLeftSleeve(v), leftArm));
            register(create2ndLayer(() -> model.rightSleeve, v -> ((PlayerModelAccessor) (Object) model).cpm$setRightSleeve(v), rightArm));
            register(create2ndLayer(() -> model.leftPants, v -> ((PlayerModelAccessor) (Object) model).cpm$setLeftPants(v), leftLeg));
            register(create2ndLayer(() -> model.rightPants, v -> ((PlayerModelAccessor) (Object) model).cpm$setRightPants(v), rightLeg));
            register(create2ndLayer(() -> model.jacket, v -> ((PlayerModelAccessor) (Object) model).cpm$setJacket(v), body));
        }
    }

    private static class RedirectHolderApi extends RDH<HumanoidModel<? extends HumanoidRenderState>> {
        private RedirectRenderer<ModelPart> head;

        public RedirectHolderApi(PlayerRenderManager mngr, HumanoidModel<? extends HumanoidRenderState> model) {
            super(mngr, model);
            head = registerHead(createRendered(() -> ((HumanoidModelAccessor) (Object) model).cpm$getHead(), v -> ((HumanoidModelAccessor) (Object) model).cpm$setHead(v), PlayerModelParts.HEAD));
            register(createRendered(() -> model.body, v -> ((HumanoidModelAccessor) (Object) model).cpm$setBody(v), PlayerModelParts.BODY));
            register(createRendered(() -> model.rightArm, v -> ((HumanoidModelAccessor) (Object) model).cpm$setRightArm(v), PlayerModelParts.RIGHT_ARM));
            register(createRendered(() -> model.leftArm, v -> ((HumanoidModelAccessor) (Object) model).cpm$setLeftArm(v), PlayerModelParts.LEFT_ARM));
            register(createRendered(() -> model.rightLeg, v -> ((HumanoidModelAccessor) (Object) model).cpm$setRightLeg(v), PlayerModelParts.RIGHT_LEG));
            register(createRendered(() -> model.leftLeg, v -> ((HumanoidModelAccessor) (Object) model).cpm$setLeftLeg(v), PlayerModelParts.LEFT_LEG));
            register(new Field<>(() -> model.hat, v -> ((HumanoidModelAccessor) (Object) model).cpm$setHat(v), null)).setCopyFrom(head);
            if (model instanceof PlayerModel) {
                PlayerModel mp = (PlayerModel) model;
                register(new Field<>(() -> mp.leftSleeve, v -> ((PlayerModelAccessor) (Object) mp).cpm$setLeftSleeve(v), null));
                register(new Field<>(() -> mp.rightSleeve, v -> ((PlayerModelAccessor) (Object) mp).cpm$setRightSleeve(v), null));
                register(new Field<>(() -> mp.leftPants, v -> ((PlayerModelAccessor) (Object) mp).cpm$setLeftPants(v), null));
                register(new Field<>(() -> mp.rightPants, v -> ((PlayerModelAccessor) (Object) mp).cpm$setRightPants(v), null));
                register(new Field<>(() -> mp.jacket, v -> ((PlayerModelAccessor) (Object) mp).cpm$setJacket(v), null));
            }
        }
    }

    private static class RedirectHolderSkull extends RDH<SkullModel> {
        public RedirectHolderSkull(PlayerRenderManager mngr, SkullModel model) {
            super(mngr, model);
            register(createRendered(() -> ((ModelAccessor) (Object) model).cpm$getRoot(), v -> ((ModelAccessor) (Object) model).cpm$setRoot(v), PlayerModelParts.HEAD));
        }
    }

    private static class RedirectHolderElytra extends RDH<ElytraModel> {
        public RedirectHolderElytra(PlayerRenderManager mngr, ElytraModel model) {
            super(mngr, model);
        }
    }

    private static class RedirectHolderArmorHelm extends RDH<HumanoidModel<? extends HumanoidRenderState>> {
        public RedirectHolderArmorHelm(PlayerRenderManager mngr, HumanoidModel<? extends HumanoidRenderState> model) {
            super(mngr, model);
            var h = register(createRendered(() -> ((HumanoidModelAccessor) (Object) model).cpm$getHead(), v -> ((HumanoidModelAccessor) (Object) model).cpm$setHead(v), RootModelType.ARMOR_HELMET));
            register(createRendered(() -> model.hat, v -> ((HumanoidModelAccessor) (Object) model).cpm$setHat(v), null)).setCopyFrom(h);
        }
    }

    private static class RedirectHolderArmorChest extends RDH<HumanoidModel<? extends HumanoidRenderState>> {
        public RedirectHolderArmorChest(PlayerRenderManager mngr, HumanoidModel<? extends HumanoidRenderState> model) {
            super(mngr, model);
            register(createRendered(() -> model.body, v -> ((HumanoidModelAccessor) (Object) model).cpm$setBody(v), RootModelType.ARMOR_BODY));
            register(createRendered(() -> model.rightArm, v -> ((HumanoidModelAccessor) (Object) model).cpm$setRightArm(v), RootModelType.ARMOR_RIGHT_ARM));
            register(createRendered(() -> model.leftArm, v -> ((HumanoidModelAccessor) (Object) model).cpm$setLeftArm(v), RootModelType.ARMOR_LEFT_ARM));
        }
    }

    private static class RedirectHolderArmorLegs extends RDH<HumanoidModel<? extends HumanoidRenderState>> {
        public RedirectHolderArmorLegs(PlayerRenderManager mngr, HumanoidModel<? extends HumanoidRenderState> model) {
            super(mngr, model);
            register(createRendered(() -> model.body, v -> ((HumanoidModelAccessor) (Object) model).cpm$setBody(v), RootModelType.ARMOR_LEGGINGS_BODY));
            register(createRendered(() -> model.rightLeg, v -> ((HumanoidModelAccessor) (Object) model).cpm$setRightLeg(v), RootModelType.ARMOR_RIGHT_LEG));
            register(createRendered(() -> model.leftLeg, v -> ((HumanoidModelAccessor) (Object) model).cpm$setLeftLeg(v), RootModelType.ARMOR_LEFT_LEG));
        }
    }

    private static class RedirectHolderArmorFeet extends RDH<HumanoidModel<? extends HumanoidRenderState>> {
        public RedirectHolderArmorFeet(PlayerRenderManager mngr, HumanoidModel<? extends HumanoidRenderState> model) {
            super(mngr, model);
            register(createRendered(() -> model.rightLeg, v -> ((HumanoidModelAccessor) (Object) model).cpm$setRightLeg(v), RootModelType.ARMOR_RIGHT_FOOT));
            register(createRendered(() -> model.leftLeg, v -> ((HumanoidModelAccessor) (Object) model).cpm$setLeftLeg(v), RootModelType.ARMOR_LEFT_FOOT));
        }
    }

    private static class RedirectHolderCape extends RDH<PlayerCapeModel> {
        public RedirectHolderCape(PlayerRenderManager mngr, PlayerCapeModel model) {
            super(mngr, model);
            register(createRendered(() -> ((ModelAccessor) (Object) model).cpm$getRoot(), v -> ((ModelAccessor) (Object) model).cpm$setRoot(v), RootModelType.CAPE));
        }
    }

    public static class RootModelPart implements SelfRenderer {
        private final RDH<?> holder;
        private final boolean storeStateOnSubmit;

        public RootModelPart(RDH<?> holder, boolean storeStateOnSubmit) {
            this.holder = holder;
            this.storeStateOnSubmit = storeStateOnSubmit;
        }

        @Override
        public void submitSelf(RenderCollector collector) {
            PoseStack ps = collector.pose();
            boolean flipArmor = false;
            try {
                Object st = collector.state();
                if (st instanceof wily.legacy.Skins.client.render.RenderStateSkinIdAccess a) {
                    java.util.UUID uuid = a.consoleskins$getEntityUuid();
                    flipArmor = wily.legacy.CustomModelSkins.cpm.shared.util.UpsideDownModelFix.isFlipped(uuid);
                }
            } catch (Throwable ignored) {
            }
            boolean isArmorHolder = holder.getClass().getName().contains("RedirectHolderArmor");
            if (flipArmor && isArmorHolder) {
                ps.pushPose();
                ps.mulPose(new org.joml.Quaternionf().rotationZ((float) Math.PI));
                ps.translate(0.0D, -1.0D, 0.0D);
            }
            try {
                for (final Supplier<ModelPart> modelPart : holder.renderedParts) {
                    ModelPart mp = modelPart.get();
                    SelfRenderer sr = ModelPartHooks.getSelfRenderer(mp);
                    if (sr != null) sr.submitSelf(collector);
                }
                if (storeStateOnSubmit && holder.model instanceof PlayerModel pm) {
                    collector.storeState(pm);
                }
            } finally {
                if (flipArmor && isArmorHolder) ps.popPose();
            }
        }
    }

    public static class RedirectModelRenderer implements RedirectRenderer<ModelPart>, SelfRenderer {
        protected final RDH<?> holder;
        protected final VanillaModelPart part;
        protected final Supplier<ModelPart> parentProvider;
        protected ModelPart parent;
        protected VBuffers buffers;
        protected ModelPart layer2;
        private final ModelPart proxy;
        private RenderCollector collector;

        public RedirectModelRenderer(RDH<?> holder, Supplier<ModelPart> parent, VanillaModelPart part) {
            this.part = part;
            this.holder = holder;
            this.parentProvider = parent;
            this.proxy = new ModelPart(Collections.emptyList(), Collections.emptyMap());
            ModelPartHooks.registerSelfRenderer(this.proxy, this);
            ModelPartHooks.registerRedirectRenderer(this.proxy, this);
        }

        @Override
        public ModelPart getThisPart() {
            return proxy;
        }

        @Override
        public VBuffers getVBuffers() {
            return buffers;
        }

        @Override
        public ModelPart swapIn() {
            if (parent != null) return proxy;
            parent = parentProvider.get();
            holder.copyModel(parent, proxy);
            proxy.setInitialPose(parent.getInitialPose());
            proxy.resetPose();
            return proxy;
        }

        @Override
        public ModelPart swapOut() {
            if (parent == null) return parentProvider.get();
            ModelPart p = parent;
            parent = null;
            return p;
        }

        @Override
        public RedirectHolder<?, ?, ?, ModelPart> getHolder() {
            return holder;
        }

        @Override
        public ModelPart getParent() {
            return parent;
        }

        @Override
        public VanillaModelPart getPart() {
            return part;
        }

        protected int color;

        @Override
        public Vec4f getColor() {
            return new Vec4f(ARGB.red(color) / 255f, ARGB.green(color) / 255f, ARGB.blue(color) / 255f, ARGB.alpha(color) / 255f);
        }

        @Override
        public void renderParent() {
            collector.submitVanilla(parent);
        }

        @Override
        public void submitSelf(RenderCollector collector) {
            this.collector = collector;
            this.color = collector.tint();
            this.buffers = VBuffers.record(collector.recordBuffer());
            render();
            this.buffers = null;
            this.collector = null;
        }
    }
}
