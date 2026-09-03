package wily.legacy.client.control.tooltip;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.entity.vehicle.boat.ChestBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import net.minecraft.world.entity.vehicle.minecart.MinecartFurnace;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import wily.factoryapi.util.FactoryItemUtil;
import wily.factoryapi.util.ListMap;
import wily.legacy.block.entity.WaterCauldronBlockEntity;
import wily.legacy.init.LegacyGameRules;
import wily.legacy.mixin.base.*;
import wily.legacy.util.LegacyComponents;
import wily.legacy.util.LegacyItemUtil;

import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static net.minecraft.world.level.block.JukeboxBlock.HAS_RECORD;

/**
 * Class made to contain useful methods and records to predict and simulate how the vanilla use interaction works and what it does, as this isn't possible without doing the action itself.
 */
public class UsePrediction {
    public static final ListMap<String, ControlTooltip.ActionHolder> GENERIC_USES = new ListMap<>();
    public static final ListMap<String, ControlTooltip.ActionHolder> ENTITY_INTERACT_ACTIONS = new ListMap<>();
    public static final ListMap<String, ControlTooltip.ActionHolder> BLOCK_USE_ACTIONS = new ListMap<>();
    public static final ListMap<String, ControlTooltip.ActionHolder> BLOCK_USE_ITEM_ON_ACTIONS = new ListMap<>();
    public static final ListMap<String, ControlTooltip.ActionHolder> ITEM_USE_ON_ACTIONS = new ListMap<>();
    public static final ListMap<String, ControlTooltip.ActionHolder> ITEM_USE_ACTIONS = new ListMap<>();

    public static final ControlTooltip.ActionHolder WAKE_UP = registerGenericUse("wake_up", Player::isSleeping, LegacyComponents.WAKE_UP);
    public static final ControlTooltip.ActionHolder TRADE = registerEntityInteract("trade", ctx -> ctx.entity instanceof AbstractVillager m && (!(m instanceof Villager v) || /*? if <1.21.5 {*//*v.getVillagerData().getProfession() != VillagerProfession.NONE*//*?} else {*/!v.getVillagerData().profession().is(VillagerProfession.NONE)/*?}*/) && !m.isTrading(), LegacyComponents.TRADE);
    public static final ControlTooltip.ActionHolder ITEM_FRAME = registerEntityInteract("item_frame", ctx -> {
        if (ctx.entity instanceof ItemFrame frame) {
            if (!frame.getItem().isEmpty()) return LegacyComponents.ROTATE;
            else if (!ctx.handItem.isEmpty()) return LegacyComponents.PLACE;
        }
        return null;
    });
    public static final ControlTooltip.ActionHolder LEASH_ENTITY = registerEntityInteract("leash", ctx -> {
        if (ctx.entity instanceof LeashFenceKnotEntity knot) {
            BlockPos fencePos = knot.getPos();
            if (!ctx.entity.level().getEntities((Entity) null, new AABB(fencePos).inflate(8.0D), e -> (e instanceof Mob mob && mob.getLeashHolder() == ctx.player) || e instanceof Boat boat && boat.getLeashHolder() == ctx.player || e instanceof ChestBoat chestBoat && chestBoat.getLeashHolder() == ctx.player).isEmpty())
                return LegacyComponents.ATTACH;
            return LegacyComponents.DETACH;
        }
        boolean isLeashedToPlayer = (ctx.entity instanceof Mob m && m.getLeashHolder() == ctx.player) || ctx.entity instanceof Boat b && b.getLeashHolder() == ctx.player || ctx.entity instanceof ChestBoat cb && cb.getLeashHolder() == ctx.player;
        if (isLeashedToPlayer)
            return LegacyComponents.UNLEASH;

        if (ctx.handItem.getItem() instanceof LeadItem) {
            boolean isLeashableEntity = (ctx.entity instanceof Mob m && m.canBeLeashed(/* ? if <1.20.5 { *//* ctx.player *//* ?} */)) || ctx.entity instanceof AbstractHorse || ctx.entity instanceof Llama || ctx.entity instanceof Parrot || ctx.entity instanceof Boat || ctx.entity instanceof ChestBoat;
            if (isLeashableEntity)
                return LegacyComponents.LEASH;
        }
        return null;
    });
    public static final ControlTooltip.ActionHolder ARMOR_STAND = registerEntityInteract("armor_stand", ctx -> {
        if (ctx.entity instanceof ArmorStand stand) {
            if (ctx.player.isShiftKeyDown())
                return LegacyComponents.CHANGE_POSE;
            return ctx.handItem.isEmpty() ? stand.getItemBySlot(((ArmorStandAccessor) stand).getLocationClickedSlot(ctx.location)).isEmpty() ? null : LegacyComponents.TAKE  : LegacyComponents.EQUIP;
        }
        return null;
    });
    public static final ControlTooltip.ActionHolder CREEPER = registerEntityInteract("creeper", ctx -> ctx.entity instanceof Creeper creeper && !creeper.isIgnited() && ctx.handItem.is(Items.FLINT_AND_STEEL), LegacyComponents.IGNITE);
    //? if >=1.21.11 {
    public static final ControlTooltip.ActionHolder NAUTILUS_INVENTORY = registerEntityInteract("nautilus_inventory", ctx -> ctx.entity instanceof AbstractNautilus nautilus && canOpenNautilusInventory(ctx.player, nautilus), LegacyComponents.IGNITE);
    public static final ControlTooltip.ActionHolder NAUTILUS_SADDLE = registerEntityInteract("nautilus_inventory", ctx -> ctx.entity instanceof AbstractNautilus nautilus && canEquipNautilus(nautilus, ctx.handItem, EquipmentSlot.SADDLE), LegacyComponents.SADDLE);
    public static final ControlTooltip.ActionHolder NAUTILUS_EQUIP = registerEntityInteract("nautilus_equip", ctx -> ctx.entity instanceof AbstractNautilus nautilus && canEquipNautilus(nautilus, ctx.handItem, EquipmentSlot.BODY), LegacyComponents.EQUIP);
    //?}
    public static final ControlTooltip.ActionHolder WOLF_EQUIP = registerEntityInteract("wolf_equip", ctx -> ctx.entity instanceof Wolf wolf && wolf.isTame() && ctx.handItem.has(DataComponents.EQUIPPABLE) && ctx.handItem.get(DataComponents.EQUIPPABLE).slot() == EquipmentSlot.BODY && wolf.getItemBySlot(EquipmentSlot.BODY).isEmpty(), LegacyComponents.EQUIP);
    public static final ControlTooltip.ActionHolder TAMABLE_ANIMAL = registerEntityInteract("tamable_animal", ctx -> {
        if (ctx.entity instanceof TamableAnimal a && a.isTame() && a.isOwnedBy(ctx.player)/*? if >=1.21.11 {*/ && !(a instanceof AbstractNautilus)/*?}*/ && !canDyeEntity(ctx) && (!(a instanceof Parrot p) || (p.onGround() && !ctx.player.isPassenger())))
            return a.isInSittingPose() ? LegacyComponents.FOLLOW_ME : LegacyComponents.SIT;
        return null;
    });
    public static final ControlTooltip.ActionHolder ALLAY = registerEntityInteract("allay", ctx -> {
        if (ctx.entity instanceof Allay allay) {
            ItemStack allayItem = allay.getItemInHand(InteractionHand.MAIN_HAND);
            if (ctx.handItem.isEmpty() && !allayItem.isEmpty())
                return LegacyComponents.TAKE;
            if (allayItem.isEmpty() && !ctx.handItem.isEmpty() && !ctx.handItem.is(Items.LEAD))
                return LegacyComponents.GIVE;
        }
        return null;
    });
    public static final ControlTooltip.ActionHolder CHESTED_HORSE = registerEntityInteract("chested_horse", ctx -> ctx.entity instanceof AbstractChestedHorse horse && ctx.handItem.is(Items.CHEST) && horse.isTamed() && !horse.hasChest() && !horse.isVehicle() && !ctx.player.isSecondaryUseActive(), LegacyComponents.ATTACH_CHEST);
    public static final ControlTooltip.ActionHolder COMMAND_BLOCK_MINECART = registerEntityInteract("command_block_minecart", ctx -> ctx.entity != null && ctx.entity.getType() == EntityType.COMMAND_BLOCK_MINECART && ctx.player.canUseGameMasterBlocks(), LegacyComponents.EDIT);
    public static final ControlTooltip.ActionHolder HORSE_INVENTORY = registerEntityInteract("horse_inventory", ctx -> ctx.entity instanceof AbstractHorse h && h.isTamed() && ctx.player.isSecondaryUseActive(), LegacyComponents.OPEN);
    public static final ControlTooltip.ActionHolder MINECART_INVENTORY = registerEntityInteract("minecart_inventory", ctx -> ctx.entity instanceof AbstractMinecartContainer, LegacyComponents.OPEN);
    public static final ControlTooltip.ActionHolder BOAT_INVENTORY = registerEntityInteract("boat_inventory", ctx -> ctx.entity instanceof AbstractChestBoat, LegacyComponents.OPEN);
    public static final ControlTooltip.ActionHolder HAPPY_GHAST_HARNESS = registerEntityInteract("happy_ghast_harness", ctx -> ctx.entity instanceof HappyGhast ghast && ctx.handItem.is(ItemTags.HARNESSES) && ghast.canUseSlot(EquipmentSlot.BODY) && ghast.getItemBySlot(EquipmentSlot.BODY).isEmpty(), LegacyComponents.HARNESS);
    public static final ControlTooltip.ActionHolder SADDLABLE = registerEntityInteract("saddle", ctx -> /*? if <1.21.5 {*//*MainHand.getItem() instanceof SaddleItem && *//*?}*/ctx.entity instanceof /*? if <1.21.5 {*//*Saddleable*//*?} else {*/ Mob/*?}*/ s &&/*? if <1.21.5 {*//*s.isSaddleable()*//*?} else {*/s.isEquippableInSlot(ctx.handItem, EquipmentSlot.SADDLE)/*?}*/ && !s.isSaddled() && (!(s instanceof AbstractHorse h) || h.isTamed()), LegacyComponents.SADDLE);
    public static final ControlTooltip.ActionHolder SHEAR_EQUIPMENT = registerEntityInteract("shear_equipment", ctx -> {
        if (ctx.entity instanceof LivingEntity living && ctx.handItem.getItem() instanceof ShearsItem) {
            if (canShearEquipment(living, EquipmentSlot.BODY) && living.getItemBySlot(EquipmentSlot.BODY).is(ItemTags.HARNESSES))
                return LegacyComponents.REMOVE_HARNESS;
            //? if >=1.21.11 {
            if (living instanceof AbstractNautilus && canShearEquipment(living, EquipmentSlot.BODY))
                return LegacyComponents.REMOVE_ARMOR;
            //?}
            if (canShearEquipment(living, EquipmentSlot.SADDLE))
                return LegacyComponents.REMOVE_SADDLE;
        }
        return null;
    });
    public static final ControlTooltip.ActionHolder HORSE_EQUIP = registerEntityInteract("horse_equip", ctx -> ctx.entity instanceof AbstractHorse h && h.isTamed() && !ctx.handItem.is(Items.SADDLE) && ctx.handItem.has(DataComponents.EQUIPPABLE) && ctx.handItem.get(DataComponents.EQUIPPABLE).slot().equals(EquipmentSlot.BODY) && h.isEquippableInSlot(ctx.handItem, EquipmentSlot.BODY) && h.getItemBySlot(EquipmentSlot.BODY).isEmpty(), LegacyComponents.EQUIP);
    public static final ControlTooltip.ActionHolder LLAMA_EQUIP = registerEntityInteract("llama_equip", ctx -> ctx.entity instanceof Llama llama && llama.isTamed() && ctx.handItem.is(ItemTags.WOOL_CARPETS) && llama.getItemBySlot(EquipmentSlot.BODY).isEmpty(), LegacyComponents.EQUIP);
    public static final ControlTooltip.ActionHolder RIDE = registerEntityInteract("ride", ctx -> {
        if (ctx.entity != null && ((EntityAccessor)ctx.entity).canVehicleAddPassenger(ctx.player) && ((EntityAccessor)ctx.entity).canRideVehicle(ctx.entity)) {
            boolean holdingLead = ctx.player.getMainHandItem().getItem() instanceof LeadItem;
            if (!holdingLead) {
                if (ctx.entity instanceof Boat || ctx.entity instanceof ChestBoat) return LegacyComponents.SAIL;
                else if (ctx.entity instanceof AbstractMinecart m && /*? if <1.21.2 {*//*m.getMinecartType() == AbstractMinecart.Type.RIDEABLE*//*?} else {*/m.isRideable()/*?}*/)
                    return LegacyComponents.RIDE;
                else if ((ctx.entity instanceof HappyGhast ghast && !ctx.entity.isVehicle() && !ghast.getItemBySlot(EquipmentSlot.BODY).isEmpty()) || ctx.entity instanceof /*? if <1.21.5 {*//*Saddleable*//*?} else {*/ Mob/*?}*/ s && !ctx.entity.isVehicle() && ((!(ctx.entity instanceof AbstractHorse) && s.isSaddled()) || ctx.entity instanceof AbstractHorse h && !ctx.player.isSecondaryUseActive() && !h.isBaby() && (h.isTamed() && !h.isFood(ctx.player.getMainHandItem()) || ctx.player.getMainHandItem().isEmpty())))
                    return LegacyComponents.MOUNT;
            }
        }
        return null;
    });
    public static final ControlTooltip.ActionHolder CURE_ZOMBIE_VILLAGER = registerEntityInteract("cure_zombie_villager", ctx -> ctx.entity instanceof ZombieVillager zombieVillager && ctx.handItem.is(Items.GOLDEN_APPLE) && !zombieVillager.isConverting(), LegacyComponents.CURE);
    public static final ControlTooltip.ActionHolder BARTER_PIGLIN = registerEntityInteract("barter_piglin", ctx -> ctx.entity instanceof Piglin piglin && ctx.handItem.is(Items.GOLD_INGOT) && !piglin.isBaby() && piglin.getOffhandItem().isEmpty(), LegacyComponents.BARTER);
    public static final ControlTooltip.ActionHolder COLLECT_BUCKETABLE = registerEntityInteract("collect_bucketable", ctx -> ctx.entity instanceof Bucketable && ctx.handItem.is(Items.WATER_BUCKET), LegacyComponents.COLLECT);
    public static final ControlTooltip.ActionHolder MILK_MUSHROOM = registerEntityInteract("milk_mushroom", ctx -> ctx.entity instanceof MushroomCow mushroomCow && ctx.handItem.is(Items.BOWL) && !mushroomCow.isBaby(), LegacyComponents.MILK);
    public static final ControlTooltip.ActionHolder MILK_COW = registerEntityInteract("milk_cow", ctx -> ctx.entity instanceof AbstractCow cow && ctx.handItem.is(Items.BUCKET) && !cow.isBaby(), LegacyComponents.MILK);
    public static final ControlTooltip.ActionHolder FURNACE_MINECART = registerEntityInteract("furnace_minecart", ctx -> ctx.entity instanceof MinecartFurnace && ctx.handItem.is(ItemTags.COALS), LegacyComponents.FUEL);
    public static final ControlTooltip.ActionHolder TAME_ANIMAL = registerEntityInteract("tame_animal", UsePrediction::canTame, LegacyComponents.TAME);
    public static final ControlTooltip.ActionHolder DYE_COLLAR = registerEntityInteract("dye_collar", UsePrediction::canDyeCollar, LegacyComponents.DYE_COLLAR);
    public static final ControlTooltip.ActionHolder DYE_ENTITY = registerEntityInteract("dye_entity", UsePrediction::canDyeEntity, LegacyComponents.DYE);
    public static final ControlTooltip.ActionHolder FEED_ANIMAL = registerEntityInteract("feed_animal", ctx -> canFeed(ctx) || canFeedWithGoldenDandelion(ctx.entity, ctx.handItem), LegacyComponents.FEED);
    public static final ControlTooltip.ActionHolder LOVE_MODE_ANIMAL = registerEntityInteract("love_mode_animal", UsePrediction::canSetLoveMode, LegacyComponents.LOVE_MODE);
    public static final ControlTooltip.ActionHolder REPAIR_IRON_GOLEM = registerEntityInteract("repair_iron_golem", ctx -> ctx.entity instanceof IronGolem g && ctx.handItem.is(Items.IRON_INGOT) && g.getHealth() < g.getMaxHealth(), LegacyComponents.REPAIR);
    public static final ControlTooltip.ActionHolder SHEAR_ENTITY = registerEntityInteract("shear_entity", ctx -> ctx.entity instanceof Shearable shearable && shearable.readyForShearing() && ctx.handItem.is(Items.SHEARS), LegacyComponents.SHEAR);
    public static final ControlTooltip.ActionHolder BRUSH_ARMADILLO = registerEntityInteract("brush_armadillo", ctx -> ctx.entity instanceof Armadillo armadillo && !armadillo.isBaby() && ctx.handItem.is(Items.BRUSH), LegacyComponents.BRUSH);
    public static final ControlTooltip.ActionHolder NAME_ENTITY = registerEntityInteract("name_entity", ctx -> ctx.entity instanceof LivingEntity e && !(e instanceof Player) && e.isAlive() && ctx.handItem.getItem() instanceof NameTagItem && FactoryItemUtil.hasCustomName(ctx.handItem), LegacyComponents.NAME);
    public static final ControlTooltip.ActionHolder FENCE = registerBlockUse("fence", ctx -> {
        if (ctx.state != null && ctx.state.is(BlockTags.FENCES))
            if (!ctx.level.getEntities((Entity) null, new AABB(ctx.pos).inflate(8.0D), e -> (e instanceof Mob mob && mob.getLeashHolder() == ctx.player) || e instanceof Boat boat && boat.getLeashHolder() == ctx.player || e instanceof ChestBoat chestBoat && chestBoat.getLeashHolder() == ctx.player).isEmpty())
                return LegacyComponents.ATTACH;
        return null;
    });
    public static final ControlTooltip.ActionHolder SLEEP = registerResultBlockUse("sleep", ctx -> ctx.state.getBlock() instanceof BedBlock ? canSleep(ctx) ? ControlTooltip.ResultAction.of(LegacyComponents.SLEEP) : ControlTooltip.ResultAction.cancel() : ControlTooltip.ResultAction.pass());
    public static final ControlTooltip.ActionHolder CHANGE_PITCH = registerBlockUse("change_pitch", ctx -> ctx.state.getBlock() instanceof NoteBlock, LegacyComponents.CHANGE_PITCH);
    public static final ControlTooltip.ActionHolder REDSTONE_ORE_USE = registerBlockUse("redstone_ore_use", ctx -> ctx.state.getBlock() instanceof RedStoneOreBlock, LegacyComponents.USE);
    public static final ControlTooltip.ActionHolder MECHANISMS_USE = registerBlockUse("mechanisms_use", ctx -> ctx.state.getBlock() instanceof RepeaterBlock || ctx.state.getBlock() instanceof ComparatorBlock || ctx.state.getBlock() instanceof RedStoneWireBlock, LegacyComponents.USE);
    public static final ControlTooltip.ActionHolder COLLECT_COMPOSTER = registerBlockUse("collect_composter", ctx -> ctx.state.getBlock() instanceof ComposterBlock && ctx.state.getValue(ComposterBlock.LEVEL) == 8, LegacyComponents.COLLECT);
    public static final ControlTooltip.ActionHolder EJECT_RECORD = registerBlockUse("eject_record", ctx -> ctx.state.getBlock() instanceof JukeboxBlock && ctx.state.getValue(HAS_RECORD), LegacyComponents.EJECT);
    public static final ControlTooltip.ActionHolder INVERT_DETECTOR = registerBlockUse("invert_detector", ctx -> ctx.state.getBlock() instanceof DaylightDetectorBlock, LegacyComponents.INVERT);
    public static final ControlTooltip.ActionHolder RING_BELL = registerBlockUse("ring_bell", ctx -> ctx.state.getBlock() instanceof BellBlock, LegacyComponents.INVERT);
    public static final ControlTooltip.ActionHolder READ_LECTERN = registerBlockUse("read_lectern", ctx -> ctx.state.getBlock() instanceof LecternBlock && ctx.state.getValue(LecternBlock.HAS_BOOK), LegacyComponents.READ);
    public static final ControlTooltip.ActionHolder EDIT_COMMAND_BLOCK = registerBlockUse("edit_command_block", ctx -> ctx.state.getBlock() instanceof CommandBlock && ctx.player.canUseGameMasterBlocks(), LegacyComponents.EDIT);
    public static final ControlTooltip.ActionHolder CONFIGURE_STRUCTURE_BLOCK = registerBlockUse("configure_structure_block", ctx -> ctx.state.getBlock() instanceof StructureBlock && ctx.player.canUseGameMasterBlocks(), LegacyComponents.CONFIGURE);
    public static final ControlTooltip.ActionHolder CONFIGURE_JIGSAW_BLOCK = registerBlockUse("configure_jigsaw_block", ctx -> ctx.state.getBlock() instanceof StructureBlock && ctx.player.canUseGameMasterBlocks(), LegacyComponents.CONFIGURE);
    public static final ControlTooltip.ActionHolder ADJUST_LIGHT = registerBlockUseItemOn("adjust_light", ctx -> ctx.state.getBlock() instanceof LightBlock && ctx.player.canUseGameMasterBlocks() && ctx.itemStack.is(Items.LIGHT), LegacyComponents.ADJUST);
    public static final ControlTooltip.ActionHolder COLLECT_FLOWER_POT = registerBlockUse("collect_flower_pot", ctx -> ctx.state.getBlock() instanceof FlowerPotBlock pot && /*? if <1.20.2 {*//*pot.getContent()*//*?} else {*/pot.getPotted()/*?}*/ != Blocks.AIR, LegacyComponents.COLLECT);
    public static final ControlTooltip.ActionHolder PLACE_BOOK_LECTERN = registerBlockUseItemOn("place_book_lectern", ctx -> ctx.state.getBlock() instanceof LecternBlock && (ctx.itemStack.is(Items.WRITABLE_BOOK) || ctx.itemStack.is(Items.WRITTEN_BOOK)), LegacyComponents.PLACE);
    public static final ControlTooltip.ActionHolder EAT_CAKE = registerBlockUseItemOn("eat_cake", ctx -> (ctx.state.getBlock() instanceof CakeBlock || ctx.state.getBlock() instanceof CandleCakeBlock) && ctx.player.canEat(false), LegacyComponents.EAT);
    public static final ControlTooltip.ActionHolder HARVEST_SWEET_BERRIES = registerBlockUseItemOn("harvest_sweet_berries", ctx -> canHarvestSweetBerries(ctx.state, ctx.player), LegacyComponents.HARVEST);
    public static final ControlTooltip.ActionHolder HARVEST_GLOW_BERRIES = registerBlockUseItemOn("harvest_glow_berries", ctx -> canHarvestGlowBerries(ctx.state), LegacyComponents.HARVEST);
    public static final ControlTooltip.ActionHolder PLACE_DECORATED_POT  = registerBlockUseItemOn("place_decorated_pot", ctx -> {
        if (!(ctx.state.getBlock() instanceof DecoratedPotBlock) || !(ctx.level.getBlockEntity(ctx.hitResult.getBlockPos()) instanceof DecoratedPotBlockEntity pot))
            return null;
        ItemStack stored = pot.getTheItem();
        if (!ctx.itemStack.isEmpty() && (stored.isEmpty() || ItemStack.isSameItemSameComponents(stored, ctx.itemStack) && stored.getCount() < stored.getMaxStackSize()))
            return LegacyComponents.PLACE;
        return null;
    });
    public static final ControlTooltip.ActionHolder OPEN_CLOSE_DOOR = registerBlockUseItemOn("open_close_door", ctx ->  {
        if (DoorBlock.isWoodenDoor(ctx.state) || ctx.state.getBlock() instanceof TrapDoorBlock && ctx.state.getBlock() != Blocks.IRON_TRAPDOOR || ctx.state.getBlock() instanceof FenceGateBlock)
            return ctx.state.getValue(BlockStateProperties.OPEN) ? LegacyComponents.CLOSE : LegacyComponents.OPEN;
        return null;
    });
    public static final ControlTooltip.ActionHolder OPEN_BLOCK = registerBlockUseItemOn("open_block", ctx ->  {
        if ((ctx.state.getBlock() instanceof ButtonBlock || ctx.state.getBlock() instanceof LeverBlock || ctx.state.getBlock() instanceof EnderChestBlock || ctx.state.getMenuProvider(ctx.level, ctx.pos) != null || ctx.level.getBlockEntity(ctx.pos) instanceof MenuProvider))
            return (ctx.state.getBlock() instanceof AbstractChestBlock || ctx.state.getBlock() instanceof ShulkerBoxBlock || ctx.state.getBlock() instanceof BarrelBlock || ctx.state.getBlock() instanceof HopperBlock || ctx.state.getBlock() instanceof DropperBlock) ? LegacyComponents.OPEN : LegacyComponents.USE;
        return null;
    });
    public static final ControlTooltip.ActionHolder INSERT_END_PORTAL_FRAME = registerBlockUseItemOn("insert_end_portal_frame", ctx -> ctx.state.getBlock() instanceof EndPortalFrameBlock && ctx.state.getValue(EndPortalFrameBlock.HAS_EYE) && ctx.itemStack.is(Items.ENDER_EYE), LegacyComponents.INSERT);
    public static final ControlTooltip.ActionHolder CHARGE_RESPAWN_ANCHOR = registerBlockUseItemOn("charge_respawn_anchor", ctx -> ctx.state.getBlock() instanceof RespawnAnchorBlock && ctx.itemStack.is(Items.GLOWSTONE) && ctx.state.getValue(RespawnAnchorBlock.CHARGE) < RespawnAnchorBlock.MAX_CHARGES, LegacyComponents.CHARGE);
    public static final ControlTooltip.ActionHolder UNLOCK_VAULT = registerBlockUseItemOn("unlock_vault", ctx -> canUnlockVault(ctx.state, ctx.itemStack), LegacyComponents.UNLOCK);
    public static final ControlTooltip.ActionHolder PLAY_JUKEBOX = registerBlockUseItemOn("play_jukebox", ctx -> ctx.state.getBlock() instanceof JukeboxBlock && ctx.itemStack.has(DataComponents.JUKEBOX_PLAYABLE), LegacyComponents.PLAY);
    public static final ControlTooltip.ActionHolder COLLECT_BEEHIVE = registerBlockUseItemOn("collect_beehive", ctx -> ctx.state.getBlock() instanceof BeehiveBlock && ctx.itemStack.is(Items.GLASS_BOTTLE) && ctx.state.getValue(BeehiveBlock.HONEY_LEVEL) >= BeehiveBlock.MAX_HONEY_LEVELS, LegacyComponents.COLLECT);
    public static final ControlTooltip.ActionHolder SHEAR_BEEHIVE = registerBlockUseItemOn("shear_beehive", ctx -> ctx.state.getBlock() instanceof BeehiveBlock && ctx.itemStack.is(Items.SHEARS) && ctx.state.getValue(BeehiveBlock.HONEY_LEVEL) >= BeehiveBlock.MAX_HONEY_LEVELS, LegacyComponents.SHEAR);
    public static final ControlTooltip.ActionHolder FILL_COMPOSTER = registerBlockUseItemOn("fill_composter", ctx -> ctx.state.getBlock() instanceof ComposterBlock && ctx.state.getValue(ComposterBlock.LEVEL) < ComposterBlock.MAX_LEVEL && ComposterBlock.COMPOSTABLES.containsKey(ctx.itemStack.getItem()), LegacyComponents.FILL);
    public static final ControlTooltip.ActionHolder COOK_CAMPFIRE = registerBlockUseItemOn("cook_campfire", ctx -> !ctx.itemStack.isEmpty() && ctx.level.getBlockEntity(ctx.pos) instanceof CampfireBlockEntity e && /*? if <1.21.2 {*//*e.getCookableRecipe(actualItem).isPresent()*//*?} else {*/ctx.level.recipeAccess().propertySet(RecipePropertySet.CAMPFIRE_INPUT).test(ctx.itemStack)/*?}*/, LegacyComponents.COOK);
    public static final ControlTooltip.ActionHolder PLACE_LECTERN_BOOK = registerBlockUseItemOn("place_lectern_book", ctx -> ctx.state.getBlock() instanceof LecternBlock && !ctx.state.getValue(LecternBlock.HAS_BOOK) && ctx.itemStack.is(ItemTags.LECTERN_BOOKS), LegacyComponents.PLACE);
    public static final ControlTooltip.ActionHolder REMOVE_CHISELED_BOOK_SHELF = registerBlockUse("remove_chiseled_book_shelf", ctx -> {
        if (ctx.state.getBlock() instanceof ChiseledBookShelfBlock shelf) {
            OptionalInt slot = shelf.getHitSlot(ctx.hitResult, ctx.hitResult.getDirection());
            if (slot.isPresent()) {
                int s = slot.getAsInt();
                if (ctx.state.getValue(ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(s)))
                    return LegacyComponents.REMOVE;
            }
        }
        return null;
    });
    public static final ControlTooltip.ActionHolder PLACE_CHISELED_BOOK_SHELF = registerBlockUseItemOn("place_chiseled_book_shelf", ctx -> {
        if (ctx.state.getBlock() instanceof ChiseledBookShelfBlock shelf && shelf.getHitSlot(ctx.hitResult, ctx.hitResult.getDirection()).isPresent()) {
            if (ctx.itemStack.is(Items.BOOK) || ctx.itemStack.is(Items.WRITABLE_BOOK) || ctx.itemStack.is(Items.WRITTEN_BOOK) || ctx.itemStack.is(Items.ENCHANTED_BOOK))
                return LegacyComponents.PLACE;
        }
        return null;
    });
    public static final ControlTooltip.ActionHolder PLACE_SHELF = registerBlockUseItemOn("place_shelf", ctx -> {
        if (ctx.state.getBlock() instanceof ShelfBlock shelf && ctx.level.getBlockEntity(ctx.pos) instanceof ShelfBlockEntity shelfEntity) {
            OptionalInt slot = shelf.getHitSlot(ctx.hitResult, ctx.state.getValue(ShelfBlock.FACING));
            if (slot.isPresent()) {
                ItemStack item = shelfEntity.getItem(slot.getAsInt());
                if (ctx.itemStack.isEmpty())
                    return item.isEmpty() ? null : LegacyComponents.TAKE;
                return item.isEmpty() ? LegacyComponents.PLACE : LegacyComponents.SWAP;
            }
        }
        return null;
    });
    public static final ControlTooltip.ActionHolder CHANGE_COPPER_GOLEM_POSE = registerBlockUseItemOn("change_copper_golem_pose", ctx -> ctx.state.is(BlockTags.COPPER_GOLEM_STATUES) && !ctx.itemStack.is(ItemTags.AXES), LegacyComponents.CHANGE_POSE);
    public static final ControlTooltip.ActionHolder SIGN_EDIT = registerBlockUse("sign_edit", ctx -> ctx.state.getBlock() instanceof SignBlock, LegacyComponents.EDIT);
    public static final ControlTooltip.ActionHolder SIGN_TEXT_STYLE = registerBlockUseItemOn("sign_text_style", ctx -> {
        if (ctx.level.getBlockEntity(ctx.hitResult.getBlockPos()) instanceof SignBlockEntity sign) {
            if (ctx.itemStack.is(Items.HONEYCOMB) && !sign.isWaxed())
                return LegacyComponents.WAX;

            SignText text = sign.isFacingFrontText(ctx.player) ? sign.getFrontText() : sign.getBackText();

            if (ctx.itemStack.is(Items.GLOW_INK_SAC) && !text.hasGlowingText())
                return LegacyComponents.GLOW;
            if (ctx.itemStack.is(Items.INK_SAC) && text.hasGlowingText())
                return LegacyComponents.REMOVE_GLOW;
            if (LegacyItemUtil.getDyeColorOrNull(ctx.itemStack.getItem()) != null && text.getColor() != LegacyItemUtil.getDyeColor(ctx.itemStack.getItem()))
                return LegacyComponents.DYE;
        }
        return null;
    });
    public static final ControlTooltip.ActionHolder CAULDRON_INTERACTIONS = registerBlockUseItemOn("cauldron_interactions", ctx -> {
        if (ctx.state.getBlock() instanceof AbstractCauldronBlock) {
            Block block = ctx.state.getBlock();
            boolean isEmptyBottle = ctx.itemStack.is(Items.GLASS_BOTTLE);
            boolean isWaterBottle = LegacyItemUtil.isWaterBottle(ctx.itemStack);
            boolean isEmptyBucket = ctx.itemStack.is(Items.BUCKET);
            boolean isWaterBucket = ctx.itemStack.is(Items.WATER_BUCKET);
            boolean isPowderBucket = ctx.itemStack.is(Items.POWDER_SNOW_BUCKET);
            boolean isLavaBucket = ctx.itemStack.is(Items.LAVA_BUCKET);
            boolean isOtherPotion = (ctx.itemStack.is(Items.POTION) || ctx.itemStack.is(Items.SPLASH_POTION) || ctx.itemStack.is(Items.LINGERING_POTION)) && !isWaterBottle;
            boolean isArrow = ctx.itemStack.is(Items.ARROW);
            WaterCauldronBlockEntity be = null;
            if (ctx.level.getBlockEntity(ctx.pos) instanceof WaterCauldronBlockEntity wbe) be = wbe;
            int level = ctx.state.hasProperty(LayeredCauldronBlock.LEVEL) ? ctx.state.getValue(LayeredCauldronBlock.LEVEL) : 0;
            boolean isDyed = be != null && be.waterColor != null;
            if (isDyed && (isWaterBottle || isWaterBucket))
                return LegacyComponents.FLUSH;
            if (isArrow && be != null && !be.hasWater() && level > 0)
                return ctx.itemStack.getCount() > 1 ? LegacyComponents.TIP_ARROWS : LegacyComponents.TIP_ARROW;
            if (isEmptyBottle && block == Blocks.WATER_CAULDRON && !isDyed && level > 0)
                return LegacyComponents.COLLECT;
            if (isEmptyBucket) {
                if (block == Blocks.WATER_CAULDRON && level == 3)
                    return LegacyComponents.COLLECT;
                if (block == Blocks.LAVA_CAULDRON || block == Blocks.POWDER_SNOW_CAULDRON)
                    return LegacyComponents.COLLECT;
            }
            if (isWaterBottle && block == Blocks.CAULDRON)
                return LegacyComponents.FILL;
            if (isWaterBottle && block == Blocks.WATER_CAULDRON && !isDyed && level < 3)
                return LegacyComponents.FILL;
            if (isWaterBucket) {
                if (block == Blocks.CAULDRON || block == Blocks.LAVA_CAULDRON || block == Blocks.POWDER_SNOW_CAULDRON)
                    return LegacyComponents.FILL;
                if (block == Blocks.WATER_CAULDRON && level < 3)
                    return LegacyComponents.FILL;
            }
            if (isPowderBucket && (block == Blocks.CAULDRON || block == Blocks.WATER_CAULDRON || block == Blocks.LAVA_CAULDRON))
                return LegacyComponents.FILL;
            if (isLavaBucket && (block == Blocks.CAULDRON || block == Blocks.WATER_CAULDRON || block == Blocks.POWDER_SNOW_CAULDRON))
                return LegacyComponents.FILL;
            if (isOtherPotion && block == Blocks.CAULDRON)
                return LegacyComponents.FILL;
        }
        return null;
    });
    public static final ControlTooltip.ActionHolder EMPTY_COLLECT_CAULDRON = registerBlockUseItemOn("empty_collect_cauldron", ctx -> {
        BlockHitResult bucketHitResult;
        if (ctx.itemStack.getItem() instanceof BucketItem i  && !(ctx.state != null && ctx.state.getBlock() instanceof AbstractCauldronBlock) && (bucketHitResult = mayInteractItemAt(ctx.level, ctx.player, ctx.itemStack, Item.getPlayerPOVHitResult(ctx.level, ctx.player, LegacyItemUtil.getBucketFluid(i) == Fluids.EMPTY ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE))) != null) {
            BlockState state = ctx.level.getBlockState(bucketHitResult.getBlockPos());
            if (LegacyItemUtil.getBucketFluid(i) != Fluids.EMPTY) {
                BlockState resultState = state.getBlock() instanceof LiquidBlockContainer && LegacyItemUtil.getBucketFluid(i) == Fluids.WATER ? state : ctx.level.getBlockState(bucketHitResult.getBlockPos().relative(bucketHitResult.getDirection()));
                if (resultState.canBeReplaced(LegacyItemUtil.getBucketFluid(i)) || resultState.isAir() || resultState.getBlock() instanceof LiquidBlockContainer container && container.canPlaceLiquid(/*? if >=1.20.2 {*/ctx.player, /*?}*/ctx.level, bucketHitResult.getBlockPos(), resultState, LegacyItemUtil.getBucketFluid(i)))
                    return LegacyComponents.EMPTY;
            } else if (state.getBlock() instanceof BucketPickup && !state.getFluidState().isEmpty()) return LegacyComponents.COLLECT;
        }
        return null;
    });
    public static final ControlTooltip.ActionHolder WATER_CAULDRON_INTERACTIONS = registerBlockUseItemOn("water_cauldron_interactions", ctx -> {
        if (ctx.level.getBlockEntity(ctx.pos) instanceof WaterCauldronBlockEntity be) {
            boolean dyedItem = LegacyItemUtil.isDyedItem(ctx.itemStack);
            boolean isDyeable = LegacyItemUtil.isDyeableItem(ctx.itemStack.typeHolder());
            if (isDyeable) {
                if (be.waterColor == null && dyedItem)
                    return LegacyComponents.CLEAN;
                if (be.waterColor != null)
                    return LegacyComponents.DYE;
            }
            if (LegacyItemUtil.getDyeColorOrNull(ctx.itemStack.getItem()) != null)
                return LegacyComponents.MIX;
        }
        return null;
    });
    public static final ControlTooltip.ActionHolder PLANT_FLOWER_POT = registerBlockUseItemOn("plant_flower_pot", ctx -> ctx.state.getBlock() instanceof FlowerPotBlock pot && /*? if <1.20.2 {*//*pot.getContent()*//*?} else {*/pot.getPotted()/*?}*/ == Blocks.AIR && ctx.itemStack.getItem() instanceof BlockItem b && FlowerPotBlockAccessor.getPottedByContent().containsKey(b.getBlock()), LegacyComponents.PLANT);
    public static final ControlTooltip.ActionHolder CARVE_PUMPKIN = registerBlockUseItemOn("carve_pumpkin", ctx -> ctx.state.getBlock() instanceof PumpkinBlock && ctx.itemStack.is(Items.SHEARS), LegacyComponents.CARVE);
    public static final ControlTooltip.ActionHolder BRUSH_BLOCK = registerBlockUseItemOn("brush_block", ctx -> ctx.state.getBlock() instanceof BrushableBlock && ctx.itemStack.is(Items.BRUSH), LegacyComponents.BRUSH);
    public static final ControlTooltip.ActionHolder PLACE_ITEM = registerUseItemOn("place_item", ctx -> {
        if (canPlace(ctx)) {
            if (ctx.itemStack.getItem() instanceof BlockItem b && b.getBlock() instanceof LanternBlock && isHangingLanternPlacement(ctx))
                return LegacyComponents.HANG;
            return ctx.itemStack.getItem() instanceof BlockItem b && isPlant(b.getBlock()) ? LegacyComponents.PLANT : LegacyComponents.PLACE;
        }
        return null;
    });
    public static final ControlTooltip.ActionHolder HANG_ITEM = registerUseItemOn("hang_item", UsePrediction::canHang, LegacyComponents.HANG);
    public static final ControlTooltip.ActionHolder TILL = registerUseItemOn("till", UsePrediction::canTill, LegacyComponents.TILL);
    public static final ControlTooltip.ActionHolder PEEL_BARK = registerUseItemOn("peel_bark", ctx -> ctx.itemStack.getItem() instanceof AxeItem && AxeItem.STRIPPABLES.get(ctx.state.getBlock()) != null && !(ctx.hand.equals(InteractionHand.MAIN_HAND) && ctx.player.getOffhandItem().is(Items.SHIELD) && !ctx.player.isSecondaryUseActive()), LegacyComponents.PEEL_BARK);
    public static final ControlTooltip.ActionHolder DIG_PATH = registerUseItemOn("dig_path", ctx -> ctx.itemStack.getItem() instanceof ShovelItem && ctx.level.getBlockState(ctx.pos.above()).isAir() && ShovelItem.FLATTENABLES.get(ctx.state.getBlock()) != null, LegacyComponents.DIG_PATH);
    public static final ControlTooltip.ActionHolder DOUSE = registerUseItemOn("douse", ctx -> ctx.itemStack.getItem() instanceof ShovelItem && ctx.state.getBlock() instanceof CampfireBlock && ctx.state.getValue(CampfireBlock.LIT), LegacyComponents.DOUSE);
    public static final ControlTooltip.ActionHolder WAX_BLOCK = registerUseItemOn("wax_block", ctx -> ctx.itemStack.is(Items.HONEYCOMB) && HoneycombItem.WAXABLES.get().containsKey(ctx.state.getBlock()), LegacyComponents.WAX);
    public static final ControlTooltip.ActionHolder SCRAPE_BLOCK = registerUseItemOn("scrape_block", ctx -> ctx.itemStack.getItem() instanceof AxeItem && (HoneycombItem.WAX_OFF_BY_BLOCK.get().containsKey(ctx.state.getBlock()) || WeatheringCopper.getPrevious(ctx.state).isPresent()), LegacyComponents.SCRAPE);
    public static final ControlTooltip.ActionHolder IGNITE = registerUseItemOn("ignite", ctx -> (ctx.itemStack.getItem() instanceof FlintAndSteelItem || ctx.itemStack.getItem() instanceof FireChargeItem) && (BaseFireBlock.canBePlacedAt(ctx.level, ctx.pos.relative(ctx.hitResult.getDirection()), ctx.player.getDirection()) || CampfireBlock.canLight(ctx.state) || CandleBlock.canLight(ctx.state) || CandleCakeBlock.canLight(ctx.state)), LegacyComponents.IGNITE);
    public static final ControlTooltip.ActionHolder IGNITE_CANDLE_CAKE = registerUseItemOn("ignite_candle_cake", ctx -> (ctx.itemStack.getItem() instanceof FlintAndSteelItem || ctx.itemStack.getItem() instanceof FireChargeItem) && ctx.state.getBlock() instanceof CandleCakeBlock && CandleCakeBlock.canLight(ctx.state), LegacyComponents.IGNITE);
    public static final ControlTooltip.ActionHolder COLLECT_GLASS_BOTTLE = registerUseItem("collect_glass_bottle", ctx -> {
        if (ctx.itemStack.is(Items.GLASS_BOTTLE)) {
            BlockHitResult hit = Item.getPlayerPOVHitResult( ctx.level, ctx.player, ClipContext.Fluid.SOURCE_ONLY);
            if (hit.getType() == HitResult.Type.BLOCK && ctx.level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER))
                return LegacyComponents.COLLECT;
        }
        return null;
    });
    public static final ControlTooltip.ActionHolder MOISTEN = registerUseItemOn("moisten", ctx -> LegacyItemUtil.isWaterBottle(ctx.itemStack) && ctx.state.is(BlockTags.CONVERTABLE_TO_MUD), LegacyComponents.MOISTEN);
    public static final ControlTooltip.ActionHolder LODESTONE_COMPASS = registerUseItemOn("lodestone_compass", ctx -> ctx.itemStack.getItem() instanceof CompassItem && ctx.state.is(Blocks.LODESTONE), LegacyComponents.DIRECT);
    public static final ControlTooltip.ActionHolder SHEAR_PLANT = registerUseItemOn("shear_plant", ctx -> ctx.itemStack.getItem() instanceof ShearsItem && canShearPlant(ctx.state), LegacyComponents.HANG);
    public static final ControlTooltip.ActionHolder BONEMEAL_PLANT = registerUseItemOn("bonemeal_plant", ctx -> ctx.itemStack.getItem() instanceof BoneMealItem && ctx.state.getBlock() instanceof BonemealableBlock b && b.isValidBonemealTarget(ctx.level, ctx.pos, ctx.state/*? if <=1.20.2 {*//*,true*//*?}*/), LegacyComponents.GROW);
    public static final ControlTooltip.ActionHolder LAUNCH_FIREWORK = registerUseItemOn("launch_firework", ctx -> ctx.itemStack.getItem() instanceof FireworkRocketItem, LegacyComponents.LAUNCH);
    public static final ControlTooltip.ActionHolder PLACE_BOAT = registerUseItem("place_boat", ctx -> ctx.itemStack.getItem() instanceof BoatItem && canPlaceBoat(ctx), LegacyComponents.PLACE);
    public static final ControlTooltip.ActionHolder PLACE_ON_WATER = registerUseItem("place_on_water", ctx -> (ctx.itemStack.is(Items.LILY_PAD) || ctx.itemStack.is(Items.FROGSPAWN)) && canPlaceOnWater(ctx), LegacyComponents.PLACE);
    public static final ControlTooltip.ActionHolder BLOCK = registerUseItem("block", ctx -> ctx.itemStack.getUseAnimation().equals(/*? if <1.21.2 {*//*UseAnim*//*?} else {*/ItemUseAnimation/*?}*/.BLOCK) && (!(ctx.itemStack.getItem() instanceof ShieldItem) || LegacyGameRules.getSidedBooleanGamerule(ctx.player, LegacyGameRules.LEGACY_SHIELD_CONTROLS.get())), LegacyComponents.BLOCK);
    public static final ControlTooltip.ActionHolder EQUIP_SWAP = registerUseItem("equip_swap", UsePrediction::canEquipSwap, LegacyComponents.EQUIP);
    public static final ControlTooltip.ActionHolder BOOST_VEHICLE = registerUseItem("boost_vehicle", UsePrediction::canBoost, LegacyComponents.BOOST);
    public static final ControlTooltip.ActionHolder THROW_CHARGE_TRIDENT = registerUseItem("throw_charge_trident", ctx -> {
        if (ctx.itemStack.getItem() instanceof TridentItem) {
            float riptide = EnchantmentHelper./*? if <1.20.5 {*//*getRiptide(ctx.itemStack)*//*?} else {*/getTridentSpinAttackStrength(ctx.itemStack, ctx.player)/*?}*/;
            if (ctx.player.getUseItem() == ctx.itemStack) {
                if (riptide > 0.0F)
                    return ctx.player.getTicksUsingItem() >= 10 ? LegacyComponents.DASH : LegacyComponents.CHARGE;
                return LegacyComponents.THROW;
            } else if (riptide <= 0.0F || ctx.player.isInWaterOrRain())
                return LegacyComponents.CHARGE;
        }
        return null;
    });
    public static final ControlTooltip.ActionHolder THROW_PROJECTILE = registerUseItem("throw_projectile", ctx -> ctx.itemStack.getItem() instanceof EggItem || ctx.itemStack.getItem() instanceof SnowballItem || ctx.itemStack.getItem() instanceof EnderpearlItem || ctx.itemStack.getItem() instanceof EnderEyeItem || ctx.itemStack.getItem() instanceof ThrowablePotionItem || ctx.itemStack.getItem() instanceof ExperienceBottleItem || ctx.itemStack.getItem() instanceof WindChargeItem, LegacyComponents.THROW);
    public static final ControlTooltip.ActionHolder BOOST_FIREWORK = registerUseItem("boost_firework", ctx -> ctx.itemStack.getItem() instanceof FireworkRocketItem && ctx.player.isFallFlying(), LegacyComponents.LAUNCH);
    public static final ControlTooltip.ActionHolder DRAW_BOW = registerUseItem("draw_bow", ctx -> (ctx.itemStack.getItem() instanceof BowItem || ctx.itemStack.getItem() instanceof CrossbowItem) && !ctx.player.isUsingItem() && !ctx.player.getProjectile(ctx.itemStack).isEmpty(), LegacyComponents.DRAW);
    public static final ControlTooltip.ActionHolder RELEASE_BOW = registerUseItem("release_bow", ctx -> (ctx.itemStack.getItem() instanceof BowItem && ctx.player.isUsingItem() && ctx.player.getUseItem() == ctx.itemStack) || (ctx.itemStack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(ctx.itemStack)), LegacyComponents.RELEASE);
    public static final ControlTooltip.ActionHolder ZOOM_SPYGLASS = registerUseItem("zoom_spyglass", ctx -> ctx.itemStack.getItem() instanceof SpyglassItem, LegacyComponents.ZOOM);
    public static final ControlTooltip.ActionHolder OPEN_WRITABLE_BOOK = registerUseItem("open_writable_book", ctx -> ctx.itemStack.getItem() instanceof WritableBookItem, LegacyComponents.OPEN);
    public static final ControlTooltip.ActionHolder READ_WRITTEN_BOOK = registerUseItem("open_written_book", ctx -> ctx.itemStack.getItem() instanceof WrittenBookItem, LegacyComponents.READ);
    public static final ControlTooltip.ActionHolder REEL_FISHING_ROD = registerUseItem("reel_fishing_rod", ctx -> ctx.itemStack.getItem() instanceof FishingRodItem && ctx.player.fishing != null, LegacyComponents.REEL);
    public static final ControlTooltip.ActionHolder CAST_FISHING_ROD = registerUseItem("cast_fishing_rod", ctx -> ctx.itemStack.getItem() instanceof FishingRodItem && ctx.player.fishing == null, LegacyComponents.REEL);
    public static final ControlTooltip.ActionHolder BLOW_INSTRUMENT = registerUseItem("blow_instrument", ctx -> ctx.itemStack.getItem() instanceof InstrumentItem && !ctx.player.isUsingItem(), LegacyComponents.BLOW);
    public static final ControlTooltip.ActionHolder RELEASE_BUNDLE = registerUseItem("release_bundle", ctx -> ControlTooltip.isBundle(ctx.itemStack) && BundleItem.getFullnessDisplay(ctx.itemStack) > 0, LegacyComponents.RELEASE);
    public static final ControlTooltip.ActionHolder CONSUME_ITEM = registerUseItem("consume_item", ctx -> isConsumable(ctx.itemStack, ctx.player) ? isDrinkable(ctx.itemStack) ? LegacyComponents.DRINK : LegacyComponents.EAT : null);

    public static ControlTooltip.ActionHolder registerGenericUse(String id, Function<Player, Component> function) {
        ControlTooltip.ActionHolder holder = ctx -> ctx instanceof Player p ? function.apply(p) : null;
        GENERIC_USES.put(id, holder);
        return holder;
    }

    public static ControlTooltip.ActionHolder registerGenericUse(String id, Predicate<Player> predicate, Component action) {
        return registerGenericUse(id, ctx -> predicate.test(ctx) ? action : null);
    }

    public static ControlTooltip.ActionHolder registerEntityInteract(String id, Function<EntityInteract, Component> function) {
        ControlTooltip.ActionHolder holder = ctx -> ctx instanceof EntityInteract entityInteract ? function.apply(entityInteract) : null;
        ENTITY_INTERACT_ACTIONS.put(id, holder);
        return holder;
    }

    public static ControlTooltip.ActionHolder registerEntityInteract(String id, Predicate<EntityInteract> predicate, Component action) {
        return registerEntityInteract(id, ctx -> predicate.test(ctx) ? action : null);
    }

    public static ControlTooltip.ActionHolder registerBlockUse(String id, Function<BlockUse, Component> function) {
        ControlTooltip.ActionHolder holder = ctx -> ctx instanceof BlockUse blockUse ? function.apply(blockUse) : null;
        BLOCK_USE_ACTIONS.put(id, holder);
        return holder;
    }

    public static ControlTooltip.ActionHolder registerResultBlockUse(String id, Function<BlockUse, ControlTooltip.ResultAction> function) {
        ControlTooltip.ResultActionHolder holder = ctx -> ctx instanceof BlockUse blockUse ? function.apply(blockUse) : null;
        BLOCK_USE_ACTIONS.put(id, holder);
        return holder;
    }

    public static ControlTooltip.ActionHolder registerBlockUse(String id, Predicate<BlockUse> predicate, Component action) {
        return registerBlockUse(id, ctx -> predicate.test(ctx) ? action : null);
    }

    public static ControlTooltip.ActionHolder registerBlockUseItemOn(String id, Function<BlockUseItemOn, Component> function) {
        ControlTooltip.ActionHolder holder = ctx -> ctx instanceof BlockUseItemOn blockUseItemOn ? function.apply(blockUseItemOn) : null;
        BLOCK_USE_ITEM_ON_ACTIONS.put(id, holder);
        return holder;
    }

    public static ControlTooltip.ActionHolder registerResultBlockUseItemOn(String id, Function<BlockUseItemOn, ControlTooltip.ResultAction> function) {
        ControlTooltip.ResultActionHolder holder = ctx -> ctx instanceof BlockUseItemOn blockUseItemOn ? function.apply(blockUseItemOn) : null;
        BLOCK_USE_ITEM_ON_ACTIONS.put(id, holder);
        return holder;
    }

    public static ControlTooltip.ActionHolder registerBlockUseItemOn(String id, Predicate<BlockUseItemOn> predicate, Component action) {
        return registerBlockUseItemOn(id, ctx -> predicate.test(ctx) ? action : null);
    }

    public static ControlTooltip.ActionHolder registerUseItemOn(String id, Function<BlockUseItemOn, Component> function) {
        ControlTooltip.ActionHolder holder = ctx -> ctx instanceof BlockUseItemOn useOnContext ? function.apply(useOnContext) : null;
        ITEM_USE_ON_ACTIONS.put(id, holder);
        return holder;
    }

    public static ControlTooltip.ActionHolder registerUseItemOn(String id, Predicate<BlockUseItemOn> predicate, Component action) {
        return registerUseItemOn(id, ctx -> predicate.test(ctx) ? action : null);
    }

    public static ControlTooltip.ActionHolder registerUseItem(String id, Function<UseItem, Component> function) {
        ControlTooltip.ActionHolder holder = ctx -> ctx instanceof UseItem useContext ? function.apply(useContext) : null;
        ITEM_USE_ACTIONS.put(id, holder);
        return holder;
    }

    public static ControlTooltip.ActionHolder registerUseItem(String id, Predicate<UseItem> predicate, Component action) {
        return registerUseItem(id, ctx -> predicate.test(ctx) ? action : null);
    }

    public static Component evaluate(Minecraft minecraft) {
        if (minecraft.player.isHandsBusy() || (minecraft.hitResult != null && minecraft.hitResult.getType() != HitResult.Type.MISS && !minecraft.level.getWorldBorder().isWithinBounds(minecraft.hitResult.getLocation())))
            return null;

        for (ControlTooltip.ActionHolder value : GENERIC_USES.values()) {
            Component c = value.getAction(minecraft.player);
            if (c != null) return c;
        }

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack actualItem = minecraft.player.getItemInHand(hand);

            //Not sure why, but this is done in vanilla instead of continue
            if (!actualItem.isItemEnabled(minecraft.level.enabledFeatures())) return null;

            if (minecraft.hitResult instanceof EntityHitResult entityHitResult && !ENTITY_INTERACT_ACTIONS.isEmpty()) {
                EntityInteract entityInteract = new EntityInteract(entityHitResult.getEntity(), minecraft.player, hand, actualItem, entityHitResult.getLocation());
                for (ControlTooltip.ActionHolder value : ENTITY_INTERACT_ACTIONS.values()) {
                    ControlTooltip.ResultAction action = value.getResultAction(entityInteract);
                    if (action.canReturn()) return action.action();
                }
            }

            if (minecraft.hitResult instanceof BlockHitResult hitResult && hitResult.getType() == HitResult.Type.BLOCK) {

                boolean haveSomethingInOurHands = !minecraft.player.getMainHandItem().isEmpty() || !minecraft.player.getOffhandItem().isEmpty();
                boolean suppressUsingBlock = minecraft.player.isSecondaryUseActive() && haveSomethingInOurHands;

                if (!BLOCK_USE_ACTIONS.isEmpty() && !suppressUsingBlock) {
                    BlockUse blockUse = new BlockUse(minecraft.level.getBlockState(hitResult.getBlockPos()), minecraft.level, hitResult.getBlockPos(), minecraft.player, hitResult);

                    if (blockUse.state.getBlock() instanceof ControlTooltip.ActionHolder value) {
                        ControlTooltip.ResultAction action = value.getResultAction(blockUse);
                        if (action.canReturn()) return action.action();
                    }

                    for (ControlTooltip.ActionHolder value : BLOCK_USE_ACTIONS.values()) {
                        ControlTooltip.ResultAction action = value.getResultAction(blockUse);
                        if (action.canReturn()) return action.action();
                    }
                }

                if (!BLOCK_USE_ITEM_ON_ACTIONS.isEmpty() || !ITEM_USE_ON_ACTIONS.isEmpty()) {
                    BlockUseItemOn blockUse = new BlockUseItemOn(actualItem, minecraft.level.getBlockState(hitResult.getBlockPos()), minecraft.level, hitResult.getBlockPos(), minecraft.player, hand, hitResult);

                    if (!suppressUsingBlock) {
                        if (blockUse.state.getBlock() instanceof ControlTooltip.ActionHolder value) {
                            ControlTooltip.ResultAction action = value.getResultAction(blockUse);
                            if (action.canReturn()) return action.action();
                        }

                        for (ControlTooltip.ActionHolder value : BLOCK_USE_ITEM_ON_ACTIONS.values()) {
                            ControlTooltip.ResultAction action = value.getResultAction(blockUse);
                            if (action.canReturn()) return action.action();
                        }
                    }

                    if (!actualItem.isEmpty() && !minecraft.player.getCooldowns().isOnCooldown(actualItem)) {
                        if (actualItem.getItem() instanceof ControlTooltip.ActionHolder value) {
                            ControlTooltip.ResultAction action = value.getResultAction(blockUse);
                            if (action.canReturn()) return action.action();
                        }

                        for (ControlTooltip.ActionHolder value : ITEM_USE_ON_ACTIONS.values()) {
                            ControlTooltip.ResultAction action = value.getResultAction(blockUse);
                            if (action.canReturn()) return action.action();
                        }
                    }
                }
            }

            if (!ITEM_USE_ACTIONS.isEmpty() && !actualItem.isEmpty() && !minecraft.player.getCooldowns().isOnCooldown(actualItem)) {
                UseItem useItem = new UseItem(actualItem, minecraft.level, minecraft.player, hand);

                if (actualItem.getItem() instanceof ControlTooltip.ActionHolder value) {
                    ControlTooltip.ResultAction action = value.getResultAction(useItem);
                    if (action.canReturn()) return action.action();
                }

                for (ControlTooltip.ActionHolder value : ITEM_USE_ACTIONS.values()) {
                    ControlTooltip.ResultAction action = value.getResultAction(useItem);
                    if (action.canReturn()) return action.action();
                }
            }
        }
       return null;
    }

    public static boolean canSleep(BlockUse ctx) {
        return !ctx.player.isSleeping() && ctx.player.isAlive() && ctx.level.environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, ctx.pos).canSleep(ctx.level);
    }

    public static BlockHitResult mayInteractItemAt(Level level, Player player, ItemStack itemStack, HitResult result) {
        if (result instanceof BlockHitResult r && r.getType() != HitResult.Type.MISS && level.mayInteract(player, r.getBlockPos()) && player.mayUseItemAt(r.getBlockPos().relative(r.getDirection()), r.getDirection(), itemStack)) {
            return r;
        }
        return null;
    }

    public static boolean isHoldingBoneMeal(Player player) {
        return player.getMainHandItem().is(Items.BONE_MEAL) || player.getOffhandItem().is(Items.BONE_MEAL);
    }

    public static boolean canHarvestSweetBerries(BlockState state, Player player) {
        if (!(state.getBlock() instanceof SweetBerryBushBlock)) return false;
        int age = state.getValue(SweetBerryBushBlock.AGE);
        return age > 1 && (age < SweetBerryBushBlock.MAX_AGE || !isHoldingBoneMeal(player));
    }

    public static boolean canHarvestGlowBerries(BlockState state) {
        return CaveVines.hasGlowBerries(state);
    }

    public static boolean canSetLoveMode(EntityInteract ctx) {
        return (ctx.entity instanceof Animal a && !a.isBaby() && a.isFood(ctx.handItem) && a.canFallInLove() && !a.isInLove() && (!(a instanceof AbstractHorse) || isLoveFood(a, ctx.handItem)));
    }

    public static boolean canUnlockVault(BlockState state, ItemStack item) {
        if (!(state != null && state.getBlock() instanceof VaultBlock) || state.getValue(VaultBlock.STATE) != VaultState.ACTIVE)
            return false;
        return state.getValue(VaultBlock.OMINOUS) ? item.is(Items.OMINOUS_TRIAL_KEY) : item.is(Items.TRIAL_KEY);
    }

    public static boolean canFeed(EntityInteract ctx) {
        return (ctx.entity instanceof Animal a && a.isFood(ctx.handItem) && (!(a instanceof AbstractHorse) && a.isBaby() || a instanceof AbstractHorse h && (a instanceof Llama || (a.isBaby() || !ctx.handItem.is(Items.HAY_BLOCK))) && (!h.isTamed() || !isLoveFood(a, ctx.handItem) && a.getHealth() < a.getMaxHealth() && !ctx.player.isSecondaryUseActive()))) || (ctx.entity instanceof Panda panda && ctx.handItem.is(Items.BAMBOO) && panda.isFood(ctx.handItem) && !panda.isEating() && !panda.canFallInLove()) || ctx.entity instanceof Dolphin && ctx.handItem.is(ItemTags.FISHES);
    }

    public static boolean canFeedWithGoldenDandelion(Entity entity, ItemStack usedItem) {
        return entity instanceof AgeableMob mob && AgeableMob.canUseGoldenDandelion(usedItem, mob.isBaby(), 0, mob);
    }

    public static boolean isLoveFood(Animal a, ItemStack stack) {
        return (a instanceof Llama && stack.is(Items.HAY_BLOCK)) || a instanceof Horse && ((stack.is(Items.GOLDEN_CARROT) || stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)));
    }

    public static boolean canShearEquipment(LivingEntity entity, EquipmentSlot slot) {
        ItemStack item = entity.getItemBySlot(slot);
        return item.has(DataComponents.EQUIPPABLE) && item.get(DataComponents.EQUIPPABLE).canBeSheared();
    }

    //? if >=1.21.11 {
    public static boolean canOpenNautilusInventory(Player player, AbstractNautilus nautilus) {
        return !nautilus.isBaby() && nautilus.isTame() && player.isSecondaryUseActive() && (!nautilus.isVehicle() || nautilus.hasPassenger(player));
    }

    public static boolean canEquipNautilus(AbstractNautilus nautilus, ItemStack item, EquipmentSlot slot) {
        return nautilus.canUseSlot(slot) && nautilus.isEquippableInSlot(item, slot) && nautilus.getItemBySlot(slot).isEmpty();
    }
    //?}

    public static boolean canEquipSwap(UseItem ctx) {
        Equippable equippable = ctx.itemStack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.swappable() && ctx.player.canUseSlot(equippable.slot()) && equippable.canBeEquippedBy(ctx.player.typeHolder());
    }

    public static boolean canBoost(UseItem ctx) {
        Entity vehicle = ctx.player.getControlledVehicle();
        return ctx.itemStack.getItem() instanceof FoodOnAStickItem<?> i && vehicle instanceof ItemSteerable && vehicle.is(i.canInteractWith) &&
                (!(vehicle instanceof Pig pig) || !((ItemBasedSteeringAccessor)((PigAccessor)pig).getSteering()).getBoosting());
    }

    public static boolean canPlace(BlockUseItemOn ctx) {
        if (ctx.itemStack.isEmpty())
            return false;
        if (ctx.itemStack.getItem() instanceof SpawnEggItem e)
            return true;
        if (ctx.itemStack.getItem() instanceof BlockItem b) {
            BlockPlaceContext c = new BlockPlaceContext(ctx.player, ctx.hand, ctx.itemStack, ctx.hitResult);
            return c.canPlace() && ((BlockItemAccessor) b).getPlacementBlockState(c) != null;
        }
        return canPlaceArmorStand(ctx) || canPlaceMinecart(ctx) || canPlaceEndCrystal(ctx);
    }

    public static boolean canPlaceArmorStand(BlockUseItemOn ctx) {
        if (!(ctx.itemStack.getItem() instanceof ArmorStandItem) || ctx.hitResult.getDirection() == Direction.DOWN)
            return false;
        BlockPlaceContext context = new BlockPlaceContext(ctx.player, ctx.hand, ctx.itemStack, ctx.hitResult);
        AABB box = EntityType.ARMOR_STAND.getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(context.getClickedPos()));
        return ctx.level.noCollision(null, box) && ctx.level.getEntities(null, box).isEmpty();
    }

    public static boolean canPlaceMinecart(BlockUseItemOn ctx) {
        return ctx.itemStack.getItem() instanceof MinecartItem && ctx.level.getBlockState(ctx.hitResult.getBlockPos()).is(BlockTags.RAILS);
    }

    public static boolean canPlaceEndCrystal(BlockUseItemOn ctx) {
        if (!(ctx.itemStack.getItem() instanceof EndCrystalItem))
            return false;
        BlockPos above = ctx.pos.above();
        return (ctx.state.is(Blocks.OBSIDIAN) || ctx.state.is(Blocks.BEDROCK)) && ctx.level.isEmptyBlock(above) && ctx.level.getEntities(null, new AABB(above)).isEmpty();
    }

    public static boolean canPlaceBoat(UseItem ctx) {
        BlockHitResult hitResult = Item.getPlayerPOVHitResult(ctx.level, ctx.player, ClipContext.Fluid.ANY);
        return hitResult.getType() == HitResult.Type.BLOCK && ctx.level.mayInteract(ctx.player, hitResult.getBlockPos());
    }

    public static boolean canPlaceOnWater(UseItem ctx) {
        BlockHitResult hitResult = mayInteractItemAt(ctx.level, ctx.player, ctx.itemStack, Item.getPlayerPOVHitResult(ctx.level, ctx.player, ClipContext.Fluid.SOURCE_ONLY));
        return hitResult != null && ctx.level.getFluidState(hitResult.getBlockPos()).is(FluidTags.WATER);
    }

    public static boolean isHangingLanternPlacement(BlockUseItemOn ctx) {
        if (!(ctx.itemStack.getItem() instanceof BlockItem blockItem))
            return false;
        BlockPlaceContext context = new BlockPlaceContext(ctx.player, ctx.hand, ctx.itemStack, ctx.hitResult);
        if (!context.canPlace())
            return false;
        BlockState state = ((BlockItemAccessor) blockItem).getPlacementBlockState(context);
        return state != null && state.hasProperty(BlockStateProperties.HANGING) && state.getValue(BlockStateProperties.HANGING);
    }

    public static boolean canHang(BlockUseItemOn ctx) {
        if (!(ctx.itemStack.getItem() instanceof HangingEntityItemAccessor hanging && (Block.canSupportCenter(ctx.level, ctx.hitResult.getBlockPos(), ctx.hitResult.getDirection()) || ctx.state.isSolid() || DiodeBlock.isDiode(ctx.state))))
            return false;

        if (hanging.getType() == EntityType.PAINTING)
            return Direction.Plane.HORIZONTAL.test(ctx.hitResult.getDirection());

        return hanging.getType() == EntityType.ITEM_FRAME || hanging.getType() == EntityType.GLOW_ITEM_FRAME;
    }

    public static boolean canTill(BlockUseItemOn ctx) {
        if (!(ctx.itemStack.getItem() instanceof HoeItem)) return false;
        Pair<Predicate<UseOnContext>, Consumer<UseOnContext>> use = HoeItem.TILLABLES.get(ctx.level.getBlockState(ctx.pos).getBlock());
        return use != null && use.getFirst().test(new UseOnContext(ctx.player, ctx.hand, ctx.hitResult));
    }

    public static boolean canShearPlant(BlockState state) {
        return state != null && state.getBlock() instanceof GrowingPlantHeadBlock plant && !plant.isMaxAge(state);
    }

    public static boolean canTame(EntityInteract ctx) {
        return ((ctx.entity instanceof TamableAnimal t && !t.isTame() && ((t instanceof Wolf && ctx.handItem.is(Items.BONE)) || (!(t instanceof Wolf) && t.isFood(ctx.handItem)))) || (ctx.entity instanceof Parrot && (ctx.handItem.is(Items.WHEAT_SEEDS) || ctx.handItem.is(Items.MELON_SEEDS) || ctx.handItem.is(Items.PUMPKIN_SEEDS) || ctx.handItem.is(Items.BEETROOT_SEEDS))) || (ctx.hand == InteractionHand.MAIN_HAND && ctx.entity instanceof AbstractHorse h && !h.isTamed() && !ctx.player.isSecondaryUseActive() && ctx.handItem.isEmpty()));
    }

    public static boolean isPlant(Block block) {
        return block instanceof BushBlock || block instanceof SugarCaneBlock || block instanceof GrowingPlantBlock || block instanceof BambooStalkBlock || block instanceof CactusBlock || block instanceof SaplingBlock || block instanceof FlowerBlock || block instanceof DoublePlantBlock || block instanceof MushroomBlock || block instanceof CropBlock || block instanceof KelpPlantBlock || block instanceof SeagrassBlock || block instanceof StemBlock || block instanceof CocoaBlock;
    }

    public static boolean isConsumable(ItemStack stack, Player player) {
        return stack.has(DataComponents.CONSUMABLE) && stack.get(DataComponents.CONSUMABLE).canConsume(player, stack);
    }

    public static boolean isDrinkable(ItemStack stack) {
        return stack.getUseAnimation() == /*? if <1.21.2 {*//*UseAnim*//*?} else {*/ItemUseAnimation/*?}*/.DRINK;
    }

    public static boolean canDyeEntity(EntityInteract ctx) {
        DyeColor color = LegacyItemUtil.getDyeColorOrNull(ctx.handItem.getItem());
        if (color == null || ctx.player == null)
            return false;
        return ctx.entity instanceof Sheep sheep && sheep.getColor() != color || ctx.entity instanceof Shulker shulker && shulker.getColor() != color || canDyeCollar(ctx.entity, ctx.player, color);
    }

    public static boolean canDyeCollar(EntityInteract ctx) {
        DyeColor color = LegacyItemUtil.getDyeColorOrNull(ctx.handItem.getItem());
        return color != null && ctx.player != null && canDyeCollar(ctx.entity, ctx.player, color);
    }

    public static boolean canDyeCollar(Entity entity, Player player, DyeColor color) {
        return entity instanceof Wolf w && w.isTame() && w.isOwnedBy(player) && w.getCollarColor() != color || entity instanceof Cat c && c.isTame() && c.isOwnedBy(player) && c.getCollarColor() != color;
    }

    public record EntityInteract(Entity entity, Player player, InteractionHand hand, ItemStack handItem, Vec3 location) {

    }

    public record BlockUse(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {

    }

    public record BlockUseItemOn(ItemStack itemStack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        public BlockUseItemOn(Player player, InteractionHand hand, BlockHitResult hitResult) {
            this(player.getItemInHand(hand), player.level().getBlockState(hitResult.getBlockPos()), player.level(), hitResult.getBlockPos(), player, hand, hitResult);
        }
    }

    public record UseItem(ItemStack itemStack, Level level, Player player, InteractionHand hand) {

    }
}
