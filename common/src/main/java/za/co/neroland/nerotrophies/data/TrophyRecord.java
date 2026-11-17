package za.co.neroland.nerotrophies.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

/**
 * One player's trophy record: the only player data NeroTrophies keeps. Lives inside
 * {@link TrophyState}; mutate it only through the state so the token index stays right.
 *
 * <p>Holds the earned trophies (and earned trims, {@code nerotrophies:trim/<trim>}, token 0), their award times and
 * claim tokens, the completed collections, the ids whose coin reward was paid, the last-known gamertag and
 * the two toggles. Nothing else: no positions, no progress, no other player.
 */
public final class TrophyRecord {

    /** Longest stored gamertag (Java names are at most 16; the slack covers proxy prefixes). */
    public static final int NAME_MAX = 32;

    /** One earned trophy or trim. */
    public static final class Earned {
        private final Identifier trophy;
        private final long at;
        private long token;
        private boolean claimed;
        private long lastClaim;

        Earned(Identifier trophy, long at, long token, boolean claimed, long lastClaim) {
            this.trophy = trophy;
            this.at = at;
            this.token = token;
            this.claimed = claimed;
            this.lastClaim = lastClaim;
        }

        public Identifier trophy() {
            return trophy;
        }

        /** Epoch millis of the award. */
        public long at() {
            return at;
        }

        public long token() {
            return token;
        }

        public boolean claimed() {
            return claimed;
        }

        /** Epoch millis of the last claim or re-claim (0 never). */
        public long lastClaim() {
            return lastClaim;
        }

        void setToken(long token) {
            this.token = token;
        }

        void markClaimed(long when) {
            this.claimed = true;
            this.lastClaim = when;
        }

        private record Dto(Identifier trophy, long at, long token, boolean claimed, long lastClaim) {
        }

        static final Codec<Earned> CODEC = RecordCodecBuilder.<Dto>create(i -> i.group(
                Identifier.CODEC.fieldOf("trophy").forGetter(Dto::trophy),
                Codec.LONG.fieldOf("at").forGetter(Dto::at),
                Codec.LONG.optionalFieldOf("token", 0L).forGetter(Dto::token),
                Codec.BOOL.optionalFieldOf("claimed", false).forGetter(Dto::claimed),
                Codec.LONG.optionalFieldOf("last_claim", 0L).forGetter(Dto::lastClaim)).apply(i, Dto::new))
                .xmap(d -> new Earned(d.trophy(), d.at(), d.token(), d.claimed(), d.lastClaim()),
                        e -> new Dto(e.trophy, e.at, e.token, e.claimed, e.lastClaim));
    }

    private final UUID id;
    private String name;
    private long nameAt;
    private boolean announce = true;
    private boolean showName = true;
    final Map<Identifier, Earned> earned = new LinkedHashMap<>();
    final Set<Identifier> collections = new LinkedHashSet<>();
    final Set<Identifier> paid = new LinkedHashSet<>();

    TrophyRecord(UUID id) {
        this.id = id;
    }

    public UUID id() {
        return id;
    }

    /** The last-known gamertag, if one has been seen since the record was created. */
    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    public long nameAt() {
        return nameAt;
    }

    public boolean announce() {
        return announce;
    }

    public boolean showName() {
        return showName;
    }

    public Optional<Earned> earned(Identifier trophy) {
        return Optional.ofNullable(earned.get(trophy));
    }

    public boolean has(Identifier trophy) {
        return earned.containsKey(trophy);
    }

    /** Earned entries in award order (read-only view). */
    public Collection<Earned> earnedEntries() {
        return Collections.unmodifiableCollection(earned.values());
    }

    public Set<Identifier> completedCollections() {
        return Collections.unmodifiableSet(collections);
    }

    public boolean paid(Identifier trophy) {
        return paid.contains(trophy);
    }

    /** Whether the record holds anything beyond default toggles (an empty record can be dropped). */
    boolean isEmpty() {
        return earned.isEmpty() && collections.isEmpty() && paid.isEmpty() && announce && showName;
    }

    void setName(String sanitised, long when) {
        this.name = sanitised;
        this.nameAt = when;
    }

    void setAnnounce(boolean value) {
        this.announce = value;
    }

    void setShowName(boolean value) {
        this.showName = value;
    }

    /**
     * Strips control and formatting characters and clamps to {@link #NAME_MAX}. Returns null for a name that is
     * empty afterwards.
     */
    public static String sanitiseName(String raw) {
        if (raw == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(Math.min(raw.length(), NAME_MAX));
        raw.codePoints().forEach(cp -> {
            if (out.length() >= NAME_MAX || Character.isISOControl(cp) || cp == 0x00A7
                    || Character.getType(cp) == Character.FORMAT) {
                return;
            }
            if (Character.charCount(cp) + out.length() <= NAME_MAX) {
                out.appendCodePoint(cp);
            }
        });
        String s = out.toString().strip();
        return s.isEmpty() ? null : s;
    }

    private record Dto(UUID id, Optional<String> name, long nameAt, boolean announce, boolean showName,
            List<Earned> earned, List<Identifier> collections, List<Identifier> paid) {
    }

    static final Codec<TrophyRecord> CODEC = RecordCodecBuilder.<Dto>create(i -> i.group(
            UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(Dto::id),
            Codec.STRING.optionalFieldOf("name").forGetter(Dto::name),
            Codec.LONG.optionalFieldOf("name_at", 0L).forGetter(Dto::nameAt),
            Codec.BOOL.optionalFieldOf("announce", true).forGetter(Dto::announce),
            Codec.BOOL.optionalFieldOf("show_name", true).forGetter(Dto::showName),
            Earned.CODEC.listOf().optionalFieldOf("earned", List.of()).forGetter(Dto::earned),
            Identifier.CODEC.listOf().optionalFieldOf("collections", List.of()).forGetter(Dto::collections),
            Identifier.CODEC.listOf().optionalFieldOf("paid", List.of()).forGetter(Dto::paid)).apply(i, Dto::new))
            .xmap(TrophyRecord::fromDto, TrophyRecord::toDto);

    private static TrophyRecord fromDto(Dto d) {
        TrophyRecord r = new TrophyRecord(d.id());
        d.name().map(TrophyRecord::sanitiseName).ifPresent(n -> r.setName(n, d.nameAt()));
        r.announce = d.announce();
        r.showName = d.showName();
        for (Earned e : d.earned()) {
            if (r.earned.size() >= TrophyState.MAX_EARNED) {
                break;
            }
            r.earned.putIfAbsent(e.trophy(), e);
        }
        d.collections().stream().limit(TrophyState.MAX_COLLECTIONS).forEach(r.collections::add);
        d.paid().stream().limit(TrophyState.MAX_EARNED).forEach(r.paid::add);
        return r;
    }

    private Dto toDto() {
        return new Dto(id, Optional.ofNullable(name), nameAt, announce, showName, new ArrayList<>(earned.values()),
                new ArrayList<>(collections), new ArrayList<>(paid));
    }
}
