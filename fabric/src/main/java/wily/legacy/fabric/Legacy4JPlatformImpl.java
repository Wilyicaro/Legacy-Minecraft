package wily.legacy.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.impl.recipe.ingredient.builtin.NbtIngredient;
import net.fabricmc.fabric.impl.tag.convention.TagRegistration;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;
import wily.legacy.util.RegisterListing;
import wily.legacy.util.ModInfo;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Legacy4JPlatformImpl {
    public static final Map<String,ModInfo> MOD_INFOS =  new HashMap<>();

    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static TagKey<Item> getCommonItemTag(String tag) {
        return TagRegistration.ITEM_TAG_REGISTRATION.registerCommon(tag);
    }

    public static boolean isLoadingMod(String modId) {
        return isModLoaded(modId);
    }
    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static Collection<ModInfo> getMods() {
        FabricLoader.getInstance().getAllMods().forEach(m-> getModInfo(m.getMetadata().getId()));
        return MOD_INFOS.values();
    }
    public static ModInfo getModInfo(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId) ? MOD_INFOS.computeIfAbsent(modId,s-> new ModInfo() {
            Optional<ModContainer> opt = FabricLoader.getInstance().getModContainer(modId);
            @Override
            public Collection<String> getAuthors() {
                return opt.map(c-> c.getMetadata().getAuthors().stream().map(Person::getName).toList()).orElse(Collections.emptyList());
            }

            @Override
            public Optional<String> getHomepage() {
                return opt.flatMap(c-> c.getMetadata().getContact().get("homepage"));
            }

            @Override
            public Optional<String> getIssues() {
                return opt.flatMap(c-> c.getMetadata().getContact().get("issues"));
            }

            @Override
            public Optional<String> getSources() {
                return opt.flatMap(c-> c.getMetadata().getContact().get("sources"));
            }

            @Override
            public Collection<String> getCredits() {
                return opt.map(c-> c.getMetadata().getContributors().stream().map(Person::getName).toList()).orElse(Collections.emptyList());
            }

            @Override
            public Collection<String> getLicense() {
                return opt.map(c-> c.getMetadata().getLicense()).orElse(Collections.emptyList());
            }

            @Override
            public String getDescription() {
                return opt.map(c->c.getMetadata().getDescription()).orElse("");
            }

            @Override
            public Optional<String> getLogoFile(int i) {
                return opt.flatMap(c->c.getMetadata().getIconPath(i));
            }

            @Override
            public Optional<Path> findResource(String s) {
                return opt.flatMap(c->c.findPath(s));
            }

            @Override
            public String getId() {
                return modId;
            }

            @Override
            public String getVersion() {
                return opt.map(c->c.getMetadata().getVersion().getFriendlyString()).orElse("");
            }

            @Override
            public String getName() {
                return opt.map(c->c.getMetadata().getName()).orElse("");
            }

            @Override
            public boolean isHidden() {
                return opt.isPresent() && opt.get().getMetadata().containsCustomValue("fabric-api:module-lifecycle");
            }
        }) : null;
    }

    public static Ingredient getNBTIngredient(ItemStack... stacks) {
        return stacks[0].getTag() == null ? Ingredient.of(stacks) : new NbtIngredient(Ingredient.of(stacks),stacks[0].getTag(),false).toVanilla();
    }
    public static Ingredient getStrictNBTIngredient(ItemStack stack) {
        return new NbtIngredient(Ingredient.of(stack),stack.getTag(),true).toVanilla();
    }

    public static <T> RegisterListing<T> createLegacyRegister(String namespace, Registry<T> registry) {
        return new RegisterListing<>() {
            private final List<Holder<T>> REGISTER_LIST = new ArrayList<>();

            @Override
            public Collection<Holder<T>> getEntries() {
                return REGISTER_LIST;
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
                forEach(o-> Registry.register(registry,o.getId(),o.get()));
            }
            @Override
            public <V extends T> Holder<V> add(String id, Supplier<V> supplier) {
                ResourceLocation location = new ResourceLocation(getNamespace(),id);
                Holder<V> h = new Holder<>() {
                    V obj;
                    @Override
                    public ResourceLocation getId() {
                        return location;
                    }

                    @Override
                    public V get() {
                        return obj == null ? (obj = supplier.get()) : obj ;
                    }
                };
                REGISTER_LIST.add((Holder<T>) h);
                return h;
            }
            @NotNull
            @Override
            public Iterator<Holder<T>> iterator() {
                return REGISTER_LIST.iterator();
            }

            @Override
            public Stream<Holder<T>> stream() {
                return REGISTER_LIST.stream();
            }
        };
    }

    public static boolean isForgeLike() {
        return false;
    }
    public static boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    public static<T extends CustomPacketPayload> void sendToPlayer(ServerPlayer serverPlayer, T packetHandler) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        packetHandler.write(buf);
        ServerPlayNetworking.send(serverPlayer,packetHandler.id(),buf);
    }
    public static<T extends CustomPacketPayload> void sendToServer(T packetHandler) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        packetHandler.write(buf);
        ClientPlayNetworking.send(packetHandler.id(),buf);
    }


}
