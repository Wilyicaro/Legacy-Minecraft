package wily.legacy.forge;

import io.netty.buffer.Unpooled;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.crafting.PartialNBTIngredient;
import net.minecraftforge.common.crafting.StrictNBTIngredient;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import wily.legacy.network.CommonNetwork;
import wily.legacy.util.ModInfo;
import wily.legacy.util.RegisterListing;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Legacy4JPlatformImpl {
    public static final Map<String,ModInfo> MOD_INFOS =  new HashMap<>();
    public static Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static TagKey<Item> getCommonItemTag(String commonTag) {
        return ItemTags.create(new ResourceLocation("forge", commonTag));
    }

    public static boolean isLoadingMod(String modId) {
        return LoadingModList.get().getModFileById(modId) != null;
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static Collection<ModInfo> getMods() {
        LoadingModList.get().getMods().forEach(m-> getModInfo(m.getModId()));
        return MOD_INFOS.values();
    }
    public static ModInfo getModInfo(String modId) {
        return ModList.get().isLoaded(modId) ? MOD_INFOS.computeIfAbsent(modId, s-> new ModInfo() {
            IModFileInfo info = ModList.get().getModFileById(modId);
            Optional<? extends ModContainer> opt = ModList.get().getModContainerById(modId);
            @Override
            public Collection<String> getAuthors() {
                return opt.flatMap(c->c.getModInfo().getConfig().getConfigElement("authors").map(s-> Collections.singleton(String.valueOf(s)))).orElse(Collections.emptySet());
            }

            @Override
            public Optional<String> getHomepage() {
                return opt.flatMap(c->c.getModInfo().getConfig().getConfigElement("displayURL").map(String::valueOf));
            }

            @Override
            public Optional<String> getIssues() {
                return Optional.ofNullable(info instanceof ModFileInfo i ? i.getIssueURL() : null).map(URL::toString);
            }

            @Override
            public Optional<String> getSources() {
                return Optional.empty();
            }

            @Override
            public Collection<String> getCredits() {
                return opt.flatMap(c->c.getModInfo().getConfig().getConfigElement("credits").map(o-> Set.of(String.valueOf(o)))).orElse(Collections.emptySet());
            }

            @Override
            public Collection<String> getLicense() {
                return Collections.singleton(info.getLicense());
            }

            @Override
            public String getDescription() {
                return opt.map(c->c.getModInfo().getDescription()).orElse("");
            }

            @Override
            public Optional<String> getLogoFile(int i) {
                return this.info.getMods().stream().filter(m->m.getModId().equals(modId)).findFirst().flatMap(IModInfo::getLogoFile);
            }
            @Override
            public Optional<Path> findResource(String s) {
                return Optional.of(this.info.getFile().findResource(s)).filter(Files::exists);
            }

            @Override
            public String getId() {
                return modId;
            }

            @Override
            public String getVersion() {
                return opt.map(c->c.getModInfo().getVersion().toString()).orElse("");
            }

            @Override
            public String getName() {
                return opt.map(c->c.getModInfo().getDisplayName()).orElse("");
            }

        }) : null;
    }

    public static Ingredient getNBTIngredient(ItemStack... stacks) {
        return stacks[0].getTag() == null ? Ingredient.of(stacks) : PartialNBTIngredient.of(stacks[0].getTag(), Arrays.stream(stacks).map(ItemStack::getItem).toArray(ItemLike[]::new));
    }
    public static Ingredient getStrictNBTIngredient(ItemStack stack) {
        return StrictNBTIngredient.of(stack);
    }
    public static <T> RegisterListing.Holder<T> deferredToRegisterHolder(RegistryObject<T> holder){
        return new RegisterListing.Holder<>() {
            @Override
            public ResourceLocation getId() {
                return holder.getId();
            }
            @Override
            public T get() {
                return holder.get();
            }
        };
    }
    public static <T> RegisterListing<T> createLegacyRegister(String namespace, Registry<T> registry) {
        return new RegisterListing<>() {
            private final DeferredRegister<T> REGISTER = DeferredRegister.create(registry.key(),namespace);

            @Override
            public Collection<Holder<T>> getEntries() {
                return stream().collect(Collectors.toSet());
            }
            @Override
            public Registry<T> getRegistry() {
                return registry;
            }
            @Override
            public String getNamespace() {
                return namespace;
            }
            @Override
            public void register() {
                REGISTER.register(Legacy4JForge.MOD_EVENT_BUS);
            }
            @Override
            public <V extends T> Holder<V> add(String id, Supplier<V> supplier) {
                return deferredToRegisterHolder(REGISTER.register(id,supplier));
            }
            @NotNull
            @Override
            public Iterator<Holder<T>> iterator() {
                return stream().iterator();
            }

            @Override
            public Stream<Holder<T>> stream() {
                return REGISTER.getEntries().stream().map(Legacy4JPlatformImpl::deferredToRegisterHolder);
            }
        };
    }

    public static boolean isForgeLike() {
        return true;
    }
    public static boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }


    public static void sendToPlayer(ServerPlayer serverPlayer, CommonNetwork.PacketHandler packetHandler) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packetHandler.encode(buf);
        PacketDistributor.PLAYER.with(()->serverPlayer).send(NetworkDirection.PLAY_TO_CLIENT.buildPacket(Pair.of(buf,0), packetHandler.id()).getThis());
    }
    public static void sendToServer(CommonNetwork.PacketHandler packetHandler) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packetHandler.encode(buf);
        PacketDistributor.SERVER.noArg().send(NetworkDirection.PLAY_TO_SERVER.buildPacket(Pair.of(buf,0), packetHandler.id()).getThis());
    }

}
