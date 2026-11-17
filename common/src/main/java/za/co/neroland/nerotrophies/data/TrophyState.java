package za.co.neroland.nerotrophies.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import za.co.neroland.nerolandcore.data.SavedDataCompat;
import za.co.neroland.nerolandcore.data.SavedDataRecovery;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;
import za.co.neroland.nerotrophies.component.TrophyStamp;

/**
 * NeroTrophies' one store: player UUID to {@link TrophyRecord}, plus the retired-token set (no UUIDs).
 * Schema {@value #SCHEMA}; unknown keys are ignored on load so later schemas stay additive.
 *
 * <p>The token index (token to owner and trophy) is <b>derived</b>: rebuilt on load and kept in step by every mutator
 * here, never persisted, so it cannot outlive an erasure. Read through {@link SavedDataRecovery} on the overworld.
 * Server thread only.
 */
public final class TrophyState extends SavedData {

    public static final int SCHEMA = 1;
    public static final Identifier ID = Identifier.fromNamespaceAndPath(NeroTrophiesCommon.MOD_ID, "trophy_state");

    /** Earned entries per record: every possible definition plus every trim. */
    public static final int MAX_EARNED = 4096 + 512;
    public static final int MAX_COLLECTIONS = 512;
    public static final int MAX_RETIRED = 65_536;

    /** Who owns a live token, and for which trophy. */
    public record TokenOwner(UUID player, Identifier trophy) {
    }

    /** Result of {@link #award}: whether it was new, and the token minted for it. */
    public record AwardResult(boolean added, long token) {
        static final AwardResult NOT_ADDED = new AwardResult(false, 0L);
    }

    private final Map<UUID, TrophyRecord> records = new LinkedHashMap<>();
    private final Long2ObjectLinkedOpenHashMap<Identifier> retired = new Long2ObjectLinkedOpenHashMap<>();
    private final Long2ObjectOpenHashMap<TokenOwner> tokenIndex = new Long2ObjectOpenHashMap<>();
    private int duplicatesRetiredOnLoad;

    public TrophyState() {
    }

    /** The store, created lazily on the overworld and read through Core's recovery guard. */
    public static TrophyState get(MinecraftServer server) {
        return SavedDataRecovery.get(server.overworld(), type(), TrophyState::new, ID.toString());
    }

    /** The saved-data type (held in a nested class so plain-JVM tests never build it). */
    public static SavedDataType<TrophyState> type() {
        return Types.TYPE;
    }

    private static final class Types {
        static final SavedDataType<TrophyState> TYPE = SavedDataCompat.type(ID, TrophyState::new, CODEC);
    }

    // ---- records ----------------------------------------------------------------------------------------------

    public Optional<TrophyRecord> record(UUID player) {
        return Optional.ofNullable(records.get(player));
    }

    public boolean hasRecord(UUID player) {
        return records.containsKey(player);
    }

    /** The player's record, created (default toggles, no name) if absent. */
    public TrophyRecord recordOrCreate(UUID player) {
        return records.computeIfAbsent(player, id -> {
            setDirty();
            return new TrophyRecord(id);
        });
    }

    /** Every record (read-only); for erasure checks and retention only, never for display to another player. */
    public Collection<TrophyRecord> records() {
        return Collections.unmodifiableCollection(records.values());
    }

    public int recordCount() {
        return records.size();
    }

    /**
     * Refreshes the stored gamertag of an <em>existing</em> record that holds at least one earned entry. Returns true
     * if it changed.
     */
    public boolean refreshName(UUID player, String rawName, long now) {
        TrophyRecord r = records.get(player);
        if (r == null || r.earned.isEmpty()) {
            return false; // a name is kept only for a player who has trophies to show it on (minimisation)
        }
        String clean = TrophyRecord.sanitiseName(rawName);
        if (clean == null || clean.equals(r.name().orElse(null))) {
            return false;
        }
        r.setName(clean, now);
        setDirty();
        return true;
    }

    public void setAnnounce(UUID player, boolean value) {
        recordOrCreate(player).setAnnounce(value);
        setDirty();
    }

    public void setShowName(UUID player, boolean value) {
        recordOrCreate(player).setShowName(value);
        setDirty();
    }

    // ---- awards -----------------------------------------------------------------------------------------------

    /**
     * Records {@code trophy} as earned with {@code token} (0 for a trim). Idempotent: an id already held returns
     * {@link AwardResult#added()} false and changes nothing. Refused once the record is at {@link #MAX_EARNED}.
     */
    public AwardResult award(UUID player, Identifier trophy, long now, long token) {
        TrophyRecord r = recordOrCreate(player);
        if (r.has(trophy) || r.earned.size() >= MAX_EARNED) {
            return AwardResult.NOT_ADDED;
        }
        r.earned.put(trophy, new TrophyRecord.Earned(trophy, now, token, false, 0L));
        if (!TrophyStamp.isReserved(token)) {
            tokenIndex.put(token, new TokenOwner(player, trophy));
        }
        setDirty();
        return new AwardResult(true, token);
    }

    /** Removes an earned entry; its token is retired so every copy becomes a replica. */
    public boolean revoke(UUID player, Identifier trophy) {
        TrophyRecord r = records.get(player);
        if (r == null) {
            return false;
        }
        TrophyRecord.Earned e = r.earned.remove(trophy);
        if (e == null) {
            return false;
        }
        retire(e.token(), trophy);
        setDirty();
        return true;
    }

    /** Marks a collection completed. Returns true only the first time. */
    public boolean completeCollection(UUID player, Identifier collection) {
        TrophyRecord r = recordOrCreate(player);
        if (r.collections.contains(collection) || r.collections.size() >= MAX_COLLECTIONS) {
            return false;
        }
        r.collections.add(collection);
        setDirty();
        return true;
    }

    /** Records that {@code trophy}'s coin reward was paid. Returns true only the first time. */
    public boolean markPaid(UUID player, Identifier trophy) {
        TrophyRecord r = recordOrCreate(player);
        if (r.paid.contains(trophy) || r.paid.size() >= MAX_EARNED) {
            return false;
        }
        r.paid.add(trophy);
        setDirty();
        return true;
    }

    // ---- claims and tokens --------------------------------------------------------------------------------------

    /** First claim: the current token is handed out unchanged. */
    public void markClaimed(UUID player, Identifier trophy, long now) {
        TrophyRecord r = records.get(player);
        if (r != null && r.earned.get(trophy) != null) {
            r.earned.get(trophy).markClaimed(now);
            setDirty();
        }
    }

    /**
     * Re-claim: retires the current token and installs {@code newToken}. Returns the old token, or 0 if the entry is
     * missing.
     */
    public long replaceToken(UUID player, Identifier trophy, long newToken, long now) {
        TrophyRecord r = records.get(player);
        TrophyRecord.Earned e = r == null ? null : r.earned.get(trophy);
        if (e == null) {
            return 0L;
        }
        long old = e.token();
        retire(old, trophy);
        e.setToken(newToken);
        e.markClaimed(now);
        tokenIndex.put(newToken, new TokenOwner(player, trophy));
        setDirty();
        return old;
    }

    public Optional<TokenOwner> owner(long token) {
        return Optional.ofNullable(tokenIndex.get(token));
    }

    /** The trophy a retired token belonged to, if it is still in the (capped) retired set. */
    public Optional<Identifier> retiredTrophy(long token) {
        return Optional.ofNullable(retired.get(token));
    }

    /** Whether {@code token} is live or retired (a freshly minted token must be neither). */
    public boolean tokenKnown(long token) {
        return tokenIndex.containsKey(token) || retired.containsKey(token);
    }

    /** Whether any live token still points at {@code player} (erasure probe). */
    public boolean anyTokenOwnedBy(UUID player) {
        for (TokenOwner o : tokenIndex.values()) {
            if (o.player().equals(player)) {
                return true;
            }
        }
        return false;
    }

    /** The player's live tokens (claimable trophies only). */
    public long[] tokensOf(UUID player) {
        TrophyRecord r = records.get(player);
        if (r == null) {
            return new long[0];
        }
        LongArrayList out = new LongArrayList();
        for (TrophyRecord.Earned e : r.earned.values()) {
            if (!TrophyStamp.isReserved(e.token())) {
                out.add(e.token());
            }
        }
        return out.toLongArray();
    }

    public int retiredCount() {
        return retired.size();
    }

    public int indexedTokenCount() {
        return tokenIndex.size();
    }

    /** How many duplicate tokens the last load found and retired (a count only, never who). */
    public int duplicatesRetiredOnLoad() {
        return duplicatesRetiredOnLoad;
    }

    private void retire(long token, Identifier trophy) {
        if (TrophyStamp.isReserved(token)) {
            return;
        }
        tokenIndex.remove(token);
        retired.putAndMoveToLast(token, trophy);
        while (retired.size() > MAX_RETIRED) {
            retired.removeFirst();
        }
    }

    // ---- erasure ------------------------------------------------------------------------------------------------

    /**
     * Removes the player's whole record and drops its tokens from the index. The tokens are <b>not</b> retired: an
     * erased player's trophies show blank, not replica. Returns the dropped tokens (for same-tick name drops).
     */
    public long[] erase(UUID player) {
        long[] tokens = tokensOf(player);
        TrophyRecord removed = records.remove(player);
        if (removed == null) {
            return tokens;
        }
        for (long t : tokens) {
            tokenIndex.remove(t);
        }
        // Belt and braces: drop any index entry still naming the player (should be none).
        tokenIndex.values().removeIf(o -> o.player().equals(player));
        setDirty();
        return tokens;
    }

    // ---- persistence ----------------------------------------------------------------------------------------------

    private record RetiredDto(long token, Identifier trophy) {
        static final Codec<RetiredDto> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("token").forGetter(RetiredDto::token),
                Identifier.CODEC.fieldOf("trophy").forGetter(RetiredDto::trophy)).apply(i, RetiredDto::new));
    }

    private record Dto(int schema, List<TrophyRecord> players, List<RetiredDto> retired) {
    }

    public static final Codec<TrophyState> CODEC = RecordCodecBuilder.<Dto>create(i -> i.group(
            // Always written (an optional field with a default would be omitted when it equals the default).
            Codec.INT.optionalFieldOf("schema").forGetter(d -> Optional.of(d.schema())),
            TrophyRecord.CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(Dto::players),
            RetiredDto.CODEC.listOf().optionalFieldOf("retired", List.of()).forGetter(Dto::retired))
            .apply(i, (schema, players, retired) -> new Dto(schema.orElse(SCHEMA), players, retired)))
            .xmap(TrophyState::fromDto, TrophyState::toDto);

    /** The schema version a decoded store was written with (for tests and migrations). */
    private int loadedSchema = SCHEMA;

    public int loadedSchema() {
        return loadedSchema;
    }

    private static TrophyState fromDto(Dto d) {
        TrophyState s = new TrophyState();
        s.loadedSchema = d.schema();
        for (TrophyRecord r : d.players()) {
            s.records.putIfAbsent(r.id(), r);
        }
        for (RetiredDto rt : d.retired()) {
            if (!TrophyStamp.isReserved(rt.token())) {
                s.retired.putAndMoveToLast(rt.token(), rt.trophy());
            }
        }
        while (s.retired.size() > MAX_RETIRED) {
            s.retired.removeFirst();
        }
        s.rebuildIndex();
        return s;
    }

    /** Rebuilds the token index; duplicates keep the newest award and the rest are retired. */
    private void rebuildIndex() {
        tokenIndex.clear();
        Map<Long, TrophyRecord.Earned> newest = new LinkedHashMap<>();
        Map<Long, UUID> newestOwner = new LinkedHashMap<>();
        List<TrophyRecord.Earned> losers = new ArrayList<>();
        for (TrophyRecord r : records.values()) {
            for (TrophyRecord.Earned e : r.earned.values()) {
                long t = e.token();
                if (TrophyStamp.isReserved(t)) {
                    continue;
                }
                TrophyRecord.Earned seen = newest.get(t);
                if (seen == null || e.at() > seen.at()) {
                    if (seen != null) {
                        losers.add(seen);
                    }
                    newest.put(t, e);
                    newestOwner.put(t, r.id());
                } else {
                    losers.add(e);
                }
            }
        }
        newest.forEach((t, e) -> tokenIndex.put(t.longValue(), new TokenOwner(newestOwner.get(t), e.trophy())));
        for (TrophyRecord.Earned loser : losers) {
            // The loser's copies become replicas; it keeps the trophy but must re-claim for a genuine copy.
            retired.putAndMoveToLast(loser.token(), loser.trophy());
            loser.setToken(TrophyStamp.UNSTAMPED);
        }
        duplicatesRetiredOnLoad = losers.size();
        // A token that is live must not also sit in the retired set.
        for (LongIterator it = tokenIndex.keySet().iterator(); it.hasNext();) {
            retired.remove(it.nextLong());
        }
    }

    private Dto toDto() {
        List<RetiredDto> out = new ArrayList<>(retired.size());
        retired.long2ObjectEntrySet().forEach(e -> out.add(new RetiredDto(e.getLongKey(), e.getValue())));
        return new Dto(SCHEMA, new ArrayList<>(records.values()), out);
    }
}
