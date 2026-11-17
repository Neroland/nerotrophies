package za.co.neroland.nerotrophies.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * S2C {@code nerotrophies:record}: the receiving player's <b>own</b> record for the Trophy Book (decisions 009, 010 section 4).
 * Every loaded trophy appears once; a hidden unearned one is {@link Entry#masked() masked} (id and category only).
 * Every loaded collection appears, sorted by id. Carries no token, no name and no UUID.
 */
public record RecordPayload(List<Entry> trophies, List<CollectionView> collections, List<String> trims,
        boolean announce, boolean showName, long serverNow) implements CustomPacketPayload {

    public static final int MAX_TROPHIES = 4096;
    public static final int MAX_COLLECTIONS = 512;
    public static final int MAX_MEMBERS = 256;
    public static final int MAX_TRIMS = 512;
    static final int MAX_TRIM = 32;

    private static final int EARNED = 1;
    private static final int CLAIMED = 2;
    private static final int HIDDEN = 4;
    private static final int MASKED = 8;

    /**
     * One trophy. When {@code masked}: tier, icon, title and description are null and there is no progress. Times are
     * server epoch ms ({@code canReclaimAt} 0 = no cooldown). {@code target} 0 = no progress bar.
     */
    public record Entry(Identifier id, String category, boolean earned, boolean claimed, boolean hidden,
            boolean masked, TrophyTier tier, Identifier icon, String title, String description, long earnedAt,
            long canReclaimAt, int order, int progress, int target) {

        /** A hidden, unearned trophy: "???" in the Book. */
        public static Entry masked(Identifier id, String category, int order) {
            return new Entry(id, category, false, false, true, true, null, null, null, null, 0L, 0L, order, 0, 0);
        }

        static void write(RegistryFriendlyByteBuf buf, Entry e) {
            PayloadIo.writeId(buf, e.id);
            buf.writeUtf(e.category, PayloadIo.MAX_KEY);
            int flags = (e.earned ? EARNED : 0) | (e.claimed ? CLAIMED : 0) | (e.hidden ? HIDDEN : 0)
                    | (e.masked ? MASKED : 0);
            buf.writeByte(flags);
            buf.writeVarInt(e.order);
            if (e.masked) {
                return;
            }
            buf.writeByte(PayloadIo.tierByte(e.tier));
            PayloadIo.writeOptionalId(buf, e.icon);
            PayloadIo.writeOptionalString(buf, e.title, PayloadIo.MAX_KEY);
            PayloadIo.writeOptionalString(buf, e.description, PayloadIo.MAX_KEY);
            buf.writeLong(e.earnedAt);
            buf.writeLong(e.canReclaimAt);
            buf.writeVarInt(e.progress);
            buf.writeVarInt(e.target);
        }

        static Entry read(RegistryFriendlyByteBuf buf) {
            Identifier id = PayloadIo.readId(buf);
            String category = buf.readUtf(PayloadIo.MAX_KEY);
            int flags = buf.readByte();
            int order = buf.readVarInt();
            if ((flags & MASKED) != 0) {
                return masked(id, category, order);
            }
            return new Entry(id, category, (flags & EARNED) != 0, (flags & CLAIMED) != 0, (flags & HIDDEN) != 0,
                    false, PayloadIo.tierOf(buf.readByte()), PayloadIo.readOptionalId(buf),
                    PayloadIo.readOptionalString(buf, PayloadIo.MAX_KEY),
                    PayloadIo.readOptionalString(buf, PayloadIo.MAX_KEY), buf.readLong(), buf.readLong(), order,
                    buf.readVarInt(), buf.readVarInt());
        }
    }

    /** One loaded collection and whether the player has completed it. Titles are lang keys. */
    public record CollectionView(Identifier id, String title, String description, Identifier icon, String trim,
            List<Identifier> members, boolean completed) {

        public CollectionView {
            members = List.copyOf(members);
        }

        static void write(RegistryFriendlyByteBuf buf, CollectionView c) {
            PayloadIo.writeId(buf, c.id);
            buf.writeUtf(c.title, PayloadIo.MAX_KEY);
            buf.writeUtf(c.description, PayloadIo.MAX_KEY);
            PayloadIo.writeId(buf, c.icon);
            buf.writeUtf(c.trim, MAX_TRIM);
            PayloadIo.writeCount(buf, c.members.size(), MAX_MEMBERS);
            for (Identifier m : c.members) {
                PayloadIo.writeId(buf, m);
            }
            buf.writeBoolean(c.completed);
        }

        static CollectionView read(RegistryFriendlyByteBuf buf) {
            Identifier id = PayloadIo.readId(buf);
            String title = buf.readUtf(PayloadIo.MAX_KEY);
            String desc = buf.readUtf(PayloadIo.MAX_KEY);
            Identifier icon = PayloadIo.readId(buf);
            String trim = buf.readUtf(MAX_TRIM);
            int n = PayloadIo.readCount(buf, MAX_MEMBERS);
            List<Identifier> members = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                members.add(PayloadIo.readId(buf));
            }
            return new CollectionView(id, title, desc, icon, trim, members, buf.readBoolean());
        }
    }

    public static final CustomPacketPayload.Type<RecordPayload> TYPE = TrophyNetwork.type(TrophyIds.PAYLOAD_RECORD);

    public static final StreamCodec<RegistryFriendlyByteBuf, RecordPayload> CODEC =
            StreamCodec.of(RecordPayload::write, RecordPayload::read);

    public RecordPayload {
        trophies = List.copyOf(trophies);
        collections = List.copyOf(collections);
        trims = List.copyOf(trims);
    }

    /** An empty record (no record yet, or just erased) with default toggles. */
    public static RecordPayload empty(List<Entry> trophies, List<CollectionView> collections, long now) {
        return new RecordPayload(trophies, collections, List.of(), true, true, now);
    }

    /** The entry for {@code id}, or null. */
    public Entry entry(Identifier id) {
        for (Entry e : trophies) {
            if (e.id().equals(id)) {
                return e;
            }
        }
        return null;
    }

    /** This record with one entry replaced (or appended), as applied by a {@code record_delta}. */
    public RecordPayload with(Entry changed, long now) {
        List<Entry> out = new ArrayList<>(trophies);
        boolean replaced = false;
        for (int i = 0; i < out.size(); i++) {
            if (out.get(i).id().equals(changed.id())) {
                out.set(i, changed);
                replaced = true;
                break;
            }
        }
        if (!replaced && out.size() < MAX_TROPHIES) {
            out.add(changed);
        }
        return new RecordPayload(out, collections, trims, announce, showName, now);
    }

    private static void write(RegistryFriendlyByteBuf buf, RecordPayload p) {
        PayloadIo.writeCount(buf, p.trophies.size(), MAX_TROPHIES);
        for (Entry e : p.trophies) {
            Entry.write(buf, e);
        }
        PayloadIo.writeCount(buf, p.collections.size(), MAX_COLLECTIONS);
        for (CollectionView c : p.collections) {
            CollectionView.write(buf, c);
        }
        PayloadIo.writeCount(buf, p.trims.size(), MAX_TRIMS);
        for (String t : p.trims) {
            buf.writeUtf(t, MAX_TRIM);
        }
        buf.writeBoolean(p.announce);
        buf.writeBoolean(p.showName);
        buf.writeLong(p.serverNow);
    }

    private static RecordPayload read(RegistryFriendlyByteBuf buf) {
        int n = PayloadIo.readCount(buf, MAX_TROPHIES);
        List<Entry> trophies = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            trophies.add(Entry.read(buf));
        }
        int c = PayloadIo.readCount(buf, MAX_COLLECTIONS);
        List<CollectionView> collections = new ArrayList<>(c);
        for (int i = 0; i < c; i++) {
            collections.add(CollectionView.read(buf));
        }
        int t = PayloadIo.readCount(buf, MAX_TRIMS);
        List<String> trims = new ArrayList<>(t);
        for (int i = 0; i < t; i++) {
            trims.add(buf.readUtf(MAX_TRIM));
        }
        return new RecordPayload(trophies, collections, trims, buf.readBoolean(), buf.readBoolean(), buf.readLong());
    }

    @Override
    public CustomPacketPayload.Type<RecordPayload> type() {
        return TYPE;
    }
}
