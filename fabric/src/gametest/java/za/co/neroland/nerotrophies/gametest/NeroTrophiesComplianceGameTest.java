package za.co.neroland.nerotrophies.gametest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import net.fabricmc.fabric.api.gametest.v1.GameTest;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import za.co.neroland.nerolandcore.config.ConfigValue;
import za.co.neroland.nerolandcore.config.CoreConfig;
import za.co.neroland.nerolandcore.data.ErasureConformance;
import za.co.neroland.nerolandcore.data.PlayerActivity;
import za.co.neroland.nerolandcore.data.PlayerDataErasure;
import za.co.neroland.nerolandcore.data.SavedDataRecovery;
import za.co.neroland.nerolandcore.trophy.AwardSource;
import za.co.neroland.nerolandcore.trophy.TrophyApi;
import za.co.neroland.nerolandcore.trophy.TrophyEvents;

import za.co.neroland.nerotrophies.blockentity.DisplayBlockEntity;
import za.co.neroland.nerotrophies.blockentity.DisplaySave;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.data.TrophyExport;
import za.co.neroland.nerotrophies.data.TrophyState;
import za.co.neroland.nerotrophies.engine.ClaimService;
import za.co.neroland.nerotrophies.engine.EngineHooks;
import za.co.neroland.nerotrophies.engine.EngineServer;
import za.co.neroland.nerotrophies.engine.NameSync;
import za.co.neroland.nerotrophies.engine.TokenResolver;
import za.co.neroland.nerotrophies.engine.TrophyEngine;
import za.co.neroland.nerotrophies.engine.TrophyStatus;
import za.co.neroland.nerotrophies.engine.TrophyTokens;
import za.co.neroland.nerotrophies.registry.ModBlocks;

/**
 * Privacy compliance on a real game-test server (Fabric 26.3 only, never shipped). Every check runs against
 * real serialised world data: block entities, item stacks, a player's save (inventory and ender chest), a chunk as it
 * is written to the region file, and the NeroTrophies store files on disk (primary, Core's recovery backup and any
 * quarantined or temporary copy). Earners are fixed test identities that never join, so any hit is a real leak.
 */
public class NeroTrophiesComplianceGameTest {

    private static final Identifier T1 = Identifier.fromNamespaceAndPath("nerotrophies", "minecraft/dragonslayer");
    private static final Identifier T2 = Identifier.fromNamespaceAndPath("nerotrophies", "minecraft/beaconator");

    private static final Set<Long> INVALIDATED = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> ERASED = ConcurrentHashMap.newKeySet();
    private static final List<String> LOG = new CopyOnWriteArrayList<>();
    private static final Map<UUID, List<AwardSource>> AWARDS = new ConcurrentHashMap<>();
    private static boolean hooked;

    // ---- harness -------------------------------------------------------------------------------------------------

    /** Wraps the live name sync to record same-tick invalidations, listens for RecordErased and captures INFO+ logs. */
    private static synchronized void hook() {
        if (hooked) {
            return;
        }
        hooked = true;
        NameSync live = EngineHooks.names();
        EngineHooks.setNameSync(new NameSync() {
            @Override
            public void invalidate(MinecraftServer server, long[] tokens) {
                Arrays.stream(tokens).forEach(INVALIDATED::add);
                live.invalidate(server, tokens);
            }

            @Override
            public void tokensChanged(MinecraftServer server, long[] tokens) {
                Arrays.stream(tokens).forEach(INVALIDATED::add);
                live.tokensChanged(server, tokens);
            }

            @Override
            public void playerJoined(ServerPlayer player) {
                live.playerJoined(player);
            }

            @Override
            public void playerLeft(MinecraftServer server, UUID player) {
                live.playerLeft(server, player);
            }

            @Override
            public void tick(MinecraftServer server) {
                live.tick(server);
            }
        });
        TrophyEvents.onRecordErased(e -> ERASED.add(e.player()));
        TrophyEvents.onAwarded(e -> AWARDS.computeIfAbsent(e.player(), k -> new CopyOnWriteArrayList<>()).add(e.source()));
        AbstractAppender capture = new AbstractAppender("NeroTrophiesComplianceCapture", null, null, false,
                Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                if (event.getLevel().isMoreSpecificThan(Level.INFO) && event.getMessage() != null) {
                    LOG.add(event.getLoggerName() + " " + event.getMessage().getFormattedMessage());
                }
            }
        };
        capture.start();
        ((org.apache.logging.log4j.core.Logger) LogManager.getRootLogger()).addAppender(capture);
    }

    private static RuntimeException fail(GameTestHelper h, String message) {
        return h.assertionException(Component.literal(message));
    }

    private static void check(GameTestHelper h, boolean ok, String message) {
        if (!ok) {
            throw fail(h, message);
        }
    }

    /** UUID (string, bare hex, NBT int-array text and raw 16 bytes) and gamertag (text and UTF-8 bytes). */
    private static void assertNoIdentity(GameTestHelper h, String where, String text, byte[] bytes, UUID id,
            String name) {
        int[] ints = UUIDUtil.uuidToIntArray(id);
        String intArray = "[I;" + ints[0] + "," + ints[1] + "," + ints[2] + "," + ints[3] + "]";
        check(h, !text.contains(id.toString()), where + " holds the UUID");
        check(h, !text.contains(id.toString().replace("-", "")), where + " holds the UUID hex");
        check(h, !text.contains(intArray), where + " holds the UUID int array");
        check(h, !text.contains(name), where + " holds the gamertag");
        if (bytes != null) {
            byte[] raw = ByteBuffer.allocate(16).putLong(id.getMostSignificantBits())
                    .putLong(id.getLeastSignificantBits()).array();
            check(h, indexOf(bytes, raw) < 0, where + " holds the UUID bytes");
            check(h, indexOf(bytes, name.getBytes(StandardCharsets.UTF_8)) < 0, where + " holds the gamertag bytes");
        }
    }

    private static int indexOf(byte[] hay, byte[] needle) {
        outer:
        for (int i = 0; i <= hay.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (hay[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static byte[] bytes(CompoundTag tag) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            NbtIo.write(tag, new DataOutputStream(out));
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void scan(GameTestHelper h, String where, CompoundTag tag, UUID id, String name) {
        assertNoIdentity(h, where, tag.toString(), bytes(tag), id, name);
    }

    private static CompoundTag itemTag(ServerLevel level, ItemStack stack) {
        return (CompoundTag) ItemStack.CODEC.encodeStart(level.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                stack).getOrThrow();
    }

    private static CompoundTag playerTag(ServerLevel level, ServerPlayer player) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        player.saveWithoutId(out);
        return out.buildResult();
    }

    private static CompoundTag chunkTag(ServerLevel level, BlockPos abs) {
        return SerializableChunkData.copyOf(level, level.getChunk(abs)).write();
    }

    /** Every trophy stamp stored anywhere inside {@code tag} (as a block entity or an item writes it). */
    private static void stampsIn(Tag tag, List<TrophyStamp> out) {
        if (tag instanceof CompoundTag c) {
            if (c.contains("trophy") && c.contains("token")) {
                TrophyStamp.CODEC.parse(NbtOps.INSTANCE, c).result().ifPresent(out::add);
            }
            for (String key : c.keySet()) {
                stampsIn(c.get(key), out);
            }
        } else if (tag instanceof ListTag list) {
            for (Tag t : list) {
                stampsIn(t, out);
            }
        }
    }

    private static DisplayBlockEntity place(GameTestHelper h, BlockPos rel, Block block, ItemStack content) {
        h.setBlock(rel, block);
        DisplayBlockEntity be = h.getBlockEntity(rel, DisplayBlockEntity.class);
        be.setItem(0, content.copy());
        return be;
    }

    private static long award(TrophyState state, UUID player, Identifier trophy, String name) {
        if (state.record(player).map(r -> r.has(trophy)).orElse(false)) {
            state.erase(player); // a left-over record from an earlier run in the same (persisted) test world
        }
        long token = TrophyTokens.mint(state);
        state.award(player, trophy, System.currentTimeMillis(), token);
        state.refreshName(player, name, System.currentTimeMillis());
        return token;
    }

    // ---- 1. real BE, item, player and chunk NBT carry no identity; breaking returns everything ----------------------

    @GameTest
    public void worldDataCarriesNoIdentity(GameTestHelper h) {
        hook();
        UUID earner = UUID.fromString("5eed0000-0000-4000-8000-000000000a01");
        String name = "NbtScanAlice";
        ServerLevel level = h.getLevel();
        TrophyState state = TrophyState.get(level.getServer());
        long token = award(state, earner, T1, name);
        ItemStack stack = ClaimService.stamped(T1, token);

        // Displays take any item: a plain item sits beside the trophy in the cabinet and alone on a plaque. It is the
        // player's own item (stored whole, like a chest's contents); NeroTrophies' own data must still hold no identity.
        ItemStack plain = new ItemStack(Items.DIAMOND_SWORD);
        plain.set(DataComponents.CUSTOM_NAME, Component.literal("Plain Showpiece"));
        List<DisplayBlockEntity> displays = List.of(
                place(h, new BlockPos(0, 1, 1), ModBlocks.TROPHY.get(), stack),
                place(h, new BlockPos(2, 1, 1), ModBlocks.DISPLAY_PEDESTAL.get(), stack),
                place(h, new BlockPos(4, 1, 1), ModBlocks.TROPHY_CABINET.get(), stack),
                place(h, new BlockPos(6, 1, 1), ModBlocks.TROPHY_PLAQUE.get(), plain));
        displays.get(2).setItem(1, plain.copy());
        check(h, displays.get(3).stamps().isEmpty() && displays.get(2).stamps().size() == 1,
                "a plain item was read as a trophy stamp");
        ServerPlayer holder = h.makeMockServerPlayerInLevel();
        holder.getInventory().add(stack.copy());
        holder.getEnderChestInventory().setItem(0, stack.copy());

        check(h, TokenResolver.resolve(state, T1, token).name().filter(name::equals).isPresent(),
                "precondition: the genuine trophy resolves to its earner's name");
        for (DisplayBlockEntity be : displays) {
            scan(h, be.getType() + " saved data", be.saveWithFullMetadata(level.registryAccess()), earner, name);
            scan(h, be.getType() + " update tag", be.getUpdateTag(level.registryAccess()), earner, name);
            CompoundTag own = (CompoundTag) DisplaySave.CODEC.encodeStart(
                    level.registryAccess().createSerializationContext(NbtOps.INSTANCE), be.toSave()).getOrThrow();
            check(h, !own.toString().contains("Plain Showpiece"),
                    be.getType() + ": a plain item leaked into the stamp record");
            scan(h, be.getType() + " stamp record", own, earner, name);
        }
        CompoundTag item = itemTag(level, stack);
        check(h, item.toString().contains(Long.toString(token)), "precondition: the item tag holds the token");
        scan(h, "trophy item", item, earner, name);
        CompoundTag player = playerTag(level, holder);
        check(h, player.toString().contains(Long.toString(token)), "precondition: the player save holds the trophies");
        scan(h, "holder's player data (inventory + ender chest)", player, earner, name);
        CompoundTag chunk = chunkTag(level, h.absolutePos(new BlockPos(0, 1, 1)));
        List<TrophyStamp> inChunk = new ArrayList<>();
        stampsIn(chunk, inChunk);
        check(h, inChunk.stream().anyMatch(s -> s.token() == token), "precondition: the chunk holds the stamps");
        scan(h, "chunk NBT", chunk, earner, name);

        // Breaking any display returns everything inside it (and the trophy block drops itself, stamped).
        StringBuilder lost = new StringBuilder();
        BlockPos plaqueAt = new BlockPos(6, 1, 1);
        BlockPos plaqueAbs = h.absolutePos(plaqueAt);
        level.destroyBlock(plaqueAbs, true);
        List<ItemEntity> plaqueDrops = level.getEntitiesOfClass(ItemEntity.class, new AABB(plaqueAbs).inflate(0.9D));
        check(h, plaqueDrops.stream().anyMatch(e -> ItemStack.matches(e.getItem(), plain)),
                "breaking the plaque did not return its plain item intact");
        plaqueDrops.forEach(ItemEntity::discard);
        for (BlockPos rel : List.of(new BlockPos(0, 1, 1), new BlockPos(2, 1, 1), new BlockPos(4, 1, 1))) {
            BlockPos abs = h.absolutePos(rel);
            String what = level.getBlockState(abs).getBlock().toString();
            level.destroyBlock(abs, true);
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(abs).inflate(3));
            // Count items, not entities: identical stacks dropped side by side merge into one entity.
            long stamped = drops.stream().map(ItemEntity::getItem).filter(i -> {
                TrophyStamp st = DisplayBlockEntity.stampOf(i);
                return st != null && st.token() == token;
            }).mapToLong(ItemStack::getCount).sum();
            List<String> seen = drops.stream().map(e -> e.getItem().toString()).toList();
            boolean plainBack = drops.stream().anyMatch(e -> ItemStack.matches(e.getItem(), plain));
            if (rel.getX() == 4 && !plainBack) {
                lost.append(what).append(" did not return its plain item ").append(seen).append("; ");
            }
            drops.forEach(ItemEntity::discard);
            if (stamped != 1) {
                lost.append(what).append(" dropped ").append(stamped).append(" stamped ").append(seen).append("; ");
            }
        }
        check(h, lost.length() == 0, "breaking displays: " + lost);
        state.erase(earner);
        h.succeed();
    }

    // ---- 2. erasure blanks every copy, everywhere, and no file on disk brings the name back -------------------------

    @GameTest
    public void erasureBlanksEveryCopyAndNoBackupRestoresTheName(GameTestHelper h) {
        hook();
        UUID earner = UUID.fromString("5eed0000-0000-4000-8000-000000000a02");
        String name = "EraseMeAlice";
        ServerLevel level = h.getLevel();
        MinecraftServer server = level.getServer();
        TrophyState state = TrophyState.get(server);
        long t1 = award(state, earner, T1, name);
        long t2 = award(state, earner, T2, name);
        ItemStack s1 = ClaimService.stamped(T1, t1);
        ItemStack s2 = ClaimService.stamped(T2, t2);

        place(h, new BlockPos(0, 1, 1), ModBlocks.TROPHY.get(), s1);
        place(h, new BlockPos(2, 1, 1), ModBlocks.DISPLAY_PEDESTAL.get(), s2);
        DisplayBlockEntity cabinet = place(h, new BlockPos(4, 1, 1), ModBlocks.TROPHY_CABINET.get(), s1);
        cabinet.setItem(1, s2.copy());
        ServerPlayer holder = h.makeMockServerPlayerInLevel();
        holder.getInventory().add(s1.copy());
        ServerPlayer other = h.makeMockServerPlayerInLevel();
        other.getEnderChestInventory().setItem(3, s2.copy());
        // "Unloaded chunk": the chunk exactly as it is written to the region file; its stamps are read back from it.
        CompoundTag savedChunk = chunkTag(level, h.absolutePos(new BlockPos(0, 1, 1)));

        List<TrophyStamp> everywhere = new ArrayList<>();
        for (BlockPos rel : List.of(new BlockPos(0, 1, 1), new BlockPos(2, 1, 1), new BlockPos(4, 1, 1))) {
            everywhere.addAll(h.getBlockEntity(rel, DisplayBlockEntity.class).stamps());
        }
        stampsIn(playerTag(level, holder), everywhere);
        stampsIn(playerTag(level, other), everywhere);
        stampsIn(savedChunk, everywhere);
        check(h, everywhere.size() >= 7, "precondition: found " + everywhere.size() + " stamped copies");
        List<TrophyStamp> chunkStamps = new ArrayList<>();
        stampsIn(savedChunk, chunkStamps);
        check(h, !chunkStamps.isEmpty(), "precondition: the saved chunk holds a stamped trophy");
        for (TrophyStamp s : everywhere) {
            check(h, TokenResolver.resolve(state, s.trophy(), s.token()).name().filter(name::equals).isPresent(),
                    "precondition: every copy shows the earner's name");
        }
        SavedDataRecovery.backupNow(server.overworld(), TrophyState.type(), state, TrophyState.ID.toString());
        server.overworld().getDataStorage().saveAndJoin();
        check(h, storeFiles(server).stream().anyMatch(f -> readText(f).contains(name)),
                "precondition: the store and its backup on disk hold the name");

        check(h, PlayerDataErasure.eraseAndReport(server, earner), "Core reports the erasure complete and durable");

        check(h, !state.hasRecord(earner) && !state.anyTokenOwnedBy(earner), "record and token index removed");
        check(h, ERASED.contains(earner), "TrophyEvents.RecordErased fired");
        check(h, INVALIDATED.contains(t1) && INVALIDATED.contains(t2), "loaded clients told to drop the names");
        for (TrophyStamp s : everywhere) {
            TokenResolver.Resolution r = TokenResolver.resolve(state, s.trophy(), s.token());
            check(h, r.status() == TrophyStatus.BLANK && r.name().isEmpty(),
                    "after erasure a copy resolves " + r.status() + " (want BLANK, no name)");
        }
        // Restart: every NeroTrophies store file (primary, recovery backup, quarantine, temp) is what a restarted
        // server would load. None may hold the earner, and the decoded store must resolve their trophies blank.
        List<Path> files = storeFiles(server);
        check(h, !files.isEmpty(), "store files found on disk");
        for (Path f : files) {
            String text = readText(f);
            assertNoIdentity(h, "store file " + f.getFileName(), text, readBytes(f), earner, name);
            CompoundTag root = readTag(f);
            if (root != null && root.get("data") != null) {
                TrophyState reloaded = TrophyState.CODEC.parse(NbtOps.INSTANCE, root.get("data")).getOrThrow();
                check(h, !reloaded.hasRecord(earner), "a reloaded " + f.getFileName() + " holds the record");
                check(h, TokenResolver.resolve(reloaded, T1, t1).status() == TrophyStatus.BLANK,
                        "a reloaded " + f.getFileName() + " does not resolve blank");
            }
        }
        for (String line : LOG) {
            check(h, !line.contains(name) && !line.contains(earner.toString()), "a log line at INFO+ holds identity");
        }
        h.succeed();
    }

    // ---- 3. showname off blanks without erasing; a rename updates on the next login -------------------------------

    @GameTest
    public void shownameAndRenameOnALivePlayer(GameTestHelper h) {
        hook();
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        TrophyState state = TrophyState.get(server);
        long token = award(state, p.getUUID(), T1, "OldGamertag");

        TrophyEngine.INSTANCE.setShowName(server, p, false);
        check(h, TokenResolver.resolve(state, T1, token).name().isEmpty(), "showname off blanks the name");
        check(h, TokenResolver.resolve(state, T1, token).counts(), "showname off keeps the trophy genuine");
        check(h, state.hasRecord(p.getUUID()), "showname off keeps the record");
        check(h, INVALIDATED.contains(token), "showname off tells holders the same tick");

        state.refreshName(p.getUUID(), "OldGamertag", 1L);
        TrophyEngine.INSTANCE.setShowName(server, p, true);
        state.refreshName(p.getUUID(), "OldGamertag", 2L);
        INVALIDATED.remove(token);
        TrophyEngine.INSTANCE.playerJoined(server, p);
        check(h, TokenResolver.resolve(state, T1, token).name().filter(TrophyEngine.nameOf(p)::equals).isPresent(),
                "the next login shows the current gamertag");
        check(h, INVALIDATED.contains(token), "the rename is pushed to holders");
        state.erase(p.getUUID());
        h.succeed();
    }

    // ---- 4. export: everything about one player, nothing about another --------------------------------------------

    @GameTest
    public void exportIsOnePlayerOnly(GameTestHelper h) {
        hook();
        MinecraftServer server = h.getLevel().getServer();
        UUID a = UUID.fromString("5eed0000-0000-4000-8000-000000000a04");
        UUID b = UUID.fromString("5eed0000-0000-4000-8000-000000000b04");
        TrophyState state = TrophyState.get(server);
        long ta = award(state, a, T1, "ExportLiveA");
        long tb = award(state, b, T2, "ExportLiveB");
        String out = TrophyExport.toText(state, a, TrophyApi.definitions(server));
        check(h, out.contains(a.toString()) && out.contains("ExportLiveA") && out.contains(TrophyExport.hex(ta)),
                "export holds the requester's record");
        assertNoIdentity(h, "export of A", out, null, b, "ExportLiveB");
        check(h, !out.contains(TrophyExport.hex(tb)) && !out.contains(T2.toString()), "export of A holds B's trophy");
        state.erase(a);
        state.erase(b);
        h.succeed();
    }

    // ---- 5. retention honours Core's dataRetentionDays --------------------------------------------------------------

    @GameTest
    public void retentionHonoursDataRetentionDays(GameTestHelper h) throws ReflectiveOperationException {
        hook();
        MinecraftServer server = h.getLevel().getServer();
        UUID stale = UUID.fromString("5eed0000-0000-4000-8000-000000000a05");
        UUID fresh = UUID.fromString("5eed0000-0000-4000-8000-000000000b05");
        TrophyState state = TrophyState.get(server);
        award(state, stale, T1, "RetentionStale");
        award(state, fresh, T1, "RetentionFresh");
        PlayerActivity activity = PlayerActivity.get(server);
        activity.touch(stale);
        activity.touch(fresh);
        Method restore = PlayerActivity.class.getDeclaredMethod("restoreLastSeen", UUID.class, long.class);
        restore.setAccessible(true);
        restore.invoke(activity, stale, System.currentTimeMillis() - 40L * 24 * 60 * 60 * 1000);
        Method load = ConfigValue.class.getDeclaredMethod("loadFrom", String.class);
        load.setAccessible(true);
        Object before = CoreConfig.DATA_RETENTION_DAYS.get();
        try {
            load.invoke(CoreConfig.DATA_RETENTION_DAYS, "0");
            PlayerDataErasure.purgeInactiveReport(server);
            check(h, state.hasRecord(stale), "retention 0 (off) purges nothing");
            load.invoke(CoreConfig.DATA_RETENTION_DAYS, "30");
            PlayerDataErasure.PurgeResult result = PlayerDataErasure.purgeInactiveReport(server);
            check(h, result.purged() >= 1 && result.failed() == 0, "the sweep purged the stale player");
            check(h, !state.hasRecord(stale) && !state.anyTokenOwnedBy(stale), "a stale player's record is erased");
            check(h, state.hasRecord(fresh), "an active player's record is kept");
        } finally {
            load.invoke(CoreConfig.DATA_RETENTION_DAYS, String.valueOf(before));
        }
        state.erase(fresh);
        h.succeed();
    }

    // ---- 6. Core's ErasureConformance on a live server --------------------------------------------------------------

    @GameTest
    public void coreErasureConformancePasses(GameTestHelper h) {
        hook();
        MinecraftServer server = h.getLevel().getServer();
        UUID c = UUID.fromString("5eed0000-0000-4000-8000-000000000a06");
        award(TrophyState.get(server), c, T1, "ConformanceC");
        ErasureConformance.Report report = ErasureConformance.create()
                .probe("nerotrophies", u -> TrophyState.get(server).hasRecord(u))
                .probe("nerotrophies.tokens", u -> TrophyState.get(server).anyTokenOwnedBy(u))
                .run(server, c);
        check(h, report.passed(), "ErasureConformance failed: " + report.failures());
        h.succeed();
    }

    // ---- 7. erasure discards a quarantined store copy even when the live store has no record --------------------

    @GameTest
    public void erasureDiscardsAQuarantinedCopyWithoutALiveRecord(GameTestHelper h) throws IOException {
        hook();
        MinecraftServer server = h.getLevel().getServer();
        UUID quarantined = UUID.fromString("5eed0000-0000-4000-8000-000000000a07");
        String name = "QuarantinedQuinn";
        TrophyState state = TrophyState.get(server);
        state.setDirty();
        server.overworld().getDataStorage().saveAndJoin();
        Path primary = storeFiles(server).stream().filter(f -> f.getFileName().toString().equals("trophy_state.dat"))
                .findFirst().orElseThrow(() -> fail(h, "precondition: the store's primary file exists"));
        // What Core leaves behind after it replaced an unreadable store with a fresh one.
        Path corrupt = primary.resolveSibling(primary.getFileName() + ".corrupt");
        // An unreadable (truncated) file that still holds the player's row in plain bytes.
        Files.write(corrupt, ("\u001f\u008btruncated id=" + quarantined + " name=" + name)
                .getBytes(StandardCharsets.ISO_8859_1));
        check(h, Files.exists(corrupt) && readText(corrupt).contains(name), "precondition: the quarantined copy names them");
        check(h, !state.hasRecord(quarantined), "precondition: the live store has no record for them");

        check(h, PlayerDataErasure.eraseAndReport(server, quarantined), "Core reports the erasure complete");

        check(h, !Files.exists(corrupt), "the quarantined copy holding the player was deleted");
        for (Path f : storeFiles(server)) {
            assertNoIdentity(h, "store file " + f.getFileName(), readText(f), readBytes(f), quarantined, name);
        }
        h.succeed();
    }

    // ---- 8. old progress is retroactive; an erased online player is awarded nothing ---------------------------------

    private static void grant(MinecraftServer server, ServerPlayer player, String advancement, boolean done) {
        AdvancementHolder holder = server.getAdvancements().get(Identifier.withDefaultNamespace(advancement));
        if (holder == null) {
            return;
        }
        for (String criterion : holder.value().criteria().keySet()) {
            if (done) {
                player.getAdvancements().award(holder, criterion);
            } else {
                player.getAdvancements().revoke(holder, criterion);
            }
        }
    }

    @GameTest(maxTicks = 400)
    public void oldProgressIsRetroactiveAndAnErasedPlayerIsParked(GameTestHelper h) {
        hook();
        MinecraftServer server = h.getLevel().getServer();
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        UUID id = p.getUUID();
        check(h, server.getPlayerList().getPlayer(id) == p, "precondition: the mock player is on the player list");
        // Progress made before the engine's login pass: the live advancement trigger fires before the pass runs.
        grant(server, p, "end/kill_dragon", true);
        h.runAfterDelay(EngineServer.LOGIN_DELAY_TICKS + 30, () -> {
            TrophyState state = TrophyState.get(server);
            check(h, state.record(id).map(r -> r.has(T1)).orElse(false), "the old progress was recorded");
            check(h, AWARDS.getOrDefault(id, List.of()).stream().allMatch(s -> s == AwardSource.RETROACTIVE),
                    "old progress awarded as " + AWARDS.get(id) + " (want RETROACTIVE only: no coins, no announcement)");
            check(h, !EngineServer.retro().isQueued(id), "the login pass finished");

            check(h, PlayerDataErasure.eraseAndReport(server, id), "Core reports the erasure complete");
            check(h, EngineServer.retro().isParked(id), "an erased online player is parked");
            int before = AWARDS.getOrDefault(id, List.of()).size();
            // Re-trigger live while parked: nothing may be awarded or recorded.
            grant(server, p, "end/kill_dragon", false);
            grant(server, p, "end/kill_dragon", true);
            h.runAfterDelay(200, () -> {
                check(h, !TrophyState.get(server).hasRecord(id), "nothing recorded again after erasure");
                check(h, AWARDS.getOrDefault(id, List.of()).size() == before, "no award for a parked player");
                h.succeed();
            });
        });
    }

    // ---- files ------------------------------------------------------------------------------------------------------

    private static List<Path> storeFiles(MinecraftServer server) {
        try (Stream<Path> walk = Files.walk(server.getWorldPath(LevelResource.ROOT))) {
            return walk.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().contains("trophy_state")).toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] readBytes(Path f) {
        try {
            byte[] raw = Files.readAllBytes(f);
            CompoundTag tag = readTag(f);
            return tag == null ? raw : bytes(tag);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String readText(Path f) {
        CompoundTag tag = readTag(f);
        if (tag != null) {
            return tag.toString();
        }
        try {
            return new String(Files.readAllBytes(f), StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static CompoundTag readTag(Path f) {
        try {
            byte[] raw = Files.readAllBytes(f);
            if (raw.length > 2 && (raw[0] & 0xff) == 0x1f && (raw[1] & 0xff) == 0x8b) {
                return NbtIo.readCompressed(new ByteArrayInputStream(raw), NbtAccounter.unlimitedHeap());
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }
}
