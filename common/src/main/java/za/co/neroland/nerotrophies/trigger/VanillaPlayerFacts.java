package za.co.neroland.nerotrophies.trigger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import za.co.neroland.nerolandcore.economy.CoreCurrencies;
import za.co.neroland.nerolandcore.economy.Currency;
import za.co.neroland.nerolandcore.economy.CurrencyApi;
import za.co.neroland.nerolandcore.progression.Gate;
import za.co.neroland.nerolandcore.progression.GateDefinitions;
import za.co.neroland.nerolandcore.progression.GateScope;
import za.co.neroland.nerolandcore.progression.MaterialMilestone;
import za.co.neroland.nerolandcore.progression.MaterialMilestoneDefinitions;
import za.co.neroland.nerolandcore.progression.MaterialMilestones;
import za.co.neroland.nerolandcore.progression.ProgressionGates;
import za.co.neroland.nerolandcore.reputation.ReputationApi;

/**
 * {@link PlayerFacts} read from the live game: vanilla statistics, the inventory, advancement progress and Core's
 * read-only progression, reputation and currency APIs. Identical on every loader and MC line. One instance serves one
 * player for one tick; the inventory is scanned at most once per instance. Server thread only; never logs.
 */
final class VanillaPlayerFacts implements PlayerFacts {

    private record StatKey(Identifier type, Identifier stat) {
    }

    /** Resolved statistics, cleared whenever the definitions change. */
    private static final Map<StatKey, Optional<Stat<?>>> STATS = new HashMap<>();
    private static final int MAX_CACHED_STATS = 8192;

    private final MinecraftServer server;
    private final ServerPlayer player;
    private Map<Item, Long> inventory;

    VanillaPlayerFacts(MinecraftServer server, ServerPlayer player) {
        this.server = server;
        this.player = player;
    }

    ServerPlayer player() {
        return player;
    }

    static void clearCache() {
        STATS.clear();
    }

    @Override
    public boolean advancementDone(Identifier advancement) {
        AdvancementHolder holder = server.getAdvancements().get(advancement);
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    @Override
    public boolean playerGateOpen(Identifier gate) {
        Gate g = GateDefinitions.forServer(server).get(gate);
        return g != null && g.scope() == GateScope.PLAYER && ProgressionGates.isOpen(server, player.getUUID(), gate);
    }

    @Override
    public boolean playerMilestoneObserved(Identifier milestone, Optional<Identifier> material) {
        MaterialMilestone m = MaterialMilestoneDefinitions.forServer(server).get(milestone);
        if (m == null || m.scope() != GateScope.PLAYER) {
            return false;
        }
        if (material.isPresent()) {
            return MaterialMilestones.isObserved(player, milestone, material.get());
        }
        Set<Identifier> seen = MaterialMilestones.exportPlayer(server, player.getUUID()).get(milestone);
        return seen != null && !seen.isEmpty();
    }

    @Override
    public OptionalLong reputation(Identifier faction) {
        if (!ReputationApi.hasRealProvider()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(ReputationApi.getReputation(player.getUUID(), faction));
    }

    @Override
    public OptionalLong balance(Identifier currency) {
        if (!CurrencyApi.hasRealProvider()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(CurrencyApi.getBalance(player.getUUID(), currencyOf(currency)));
    }

    @Override
    public long stat(Identifier statType, Identifier stat) {
        Optional<Stat<?>> resolved = resolve(statType, stat);
        return resolved.isPresent() ? player.getStats().getValue(resolved.get()) : UNKNOWN;
    }

    @Override
    public long itemsObtained(Identifier item) {
        Item it = item(item);
        if (it == null) {
            return UNKNOWN;
        }
        return (long) player.getStats().getValue(Stats.ITEM_PICKED_UP.get(it))
                + player.getStats().getValue(Stats.ITEM_CRAFTED.get(it));
    }

    @Override
    public long inventoryCount(Identifier item) {
        Item it = item(item);
        if (it == null) {
            return UNKNOWN;
        }
        if (inventory == null) {
            inventory = new HashMap<>();
            Inventory inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty()) {
                    inventory.merge(stack.getItem(), (long) stack.getCount(), Long::sum);
                }
            }
        }
        return inventory.getOrDefault(it, 0L);
    }

    @Override
    public long blocksPlaced(Identifier block) {
        Optional<Item> it = BuiltInRegistries.BLOCK.getOptional(block).map(b -> b.asItem());
        if (it.isEmpty() || it.get() == Items.AIR) {
            return UNKNOWN;
        }
        return player.getStats().getValue(Stats.ITEM_USED.get(it.get()));
    }

    @Override
    public Identifier dimension() {
        return player.level().dimension().identifier();
    }

    private static Item item(Identifier id) {
        Optional<Item> it = BuiltInRegistries.ITEM.getOptional(id);
        return it.isEmpty() || it.get() == Items.AIR ? null : it.get();
    }

    private static Optional<Stat<?>> resolve(Identifier statType, Identifier stat) {
        StatKey key = new StatKey(statType, stat);
        Optional<Stat<?>> cached = STATS.get(key);
        if (cached != null) {
            return cached;
        }
        Optional<Stat<?>> found = BuiltInRegistries.STAT_TYPE.getOptional(statType)
                .flatMap(type -> resolveIn(type, stat));
        if (STATS.size() < MAX_CACHED_STATS) {
            STATS.put(key, found);
        }
        return found;
    }

    private static <T> Optional<Stat<?>> resolveIn(StatType<T> type, Identifier value) {
        return type.getRegistry().getOptional(value).<Stat<?>>map(type::get);
    }

    private static Currency currencyOf(Identifier id) {
        if (CoreCurrencies.CREDITS.id().equals(id)) {
            return CoreCurrencies.CREDITS;
        }
        return Currency.of(id, "currency." + id.getNamespace() + "." + id.getPath().replace('/', '.'));
    }
}
