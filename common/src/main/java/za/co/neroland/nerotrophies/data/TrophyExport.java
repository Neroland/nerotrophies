package za.co.neroland.nerotrophies.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.trophy.TrophyDefinition;
import za.co.neroland.nerolandcore.trophy.TrophyDefinitions;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;
import za.co.neroland.nerotrophies.component.TrophyStamp;

/**
 * {@code /nerotrophies export}: everything NeroTrophies holds about one player and nothing about anyone
 * else, as JSON. The command sends it to the requester's chat as click-to-copy chunks; it is never written to a file
 * or the server log.
 */
public final class TrophyExport {

    /**
     * Largest chunk sent in one chat line, in bytes as the chunk travels. A click event's value is sent as an NBT string
     * (at most 65,535 bytes of modified UTF-8) and may be shown or logged as escaped JSON, so chunks are measured in the
     * worst of those encodings and kept at half the limit.
     */
    public static final int CHUNK_BYTES = 32 * 1024;

    /** The hard limit a chunk must never reach: an NBT string's 65,535 bytes. */
    public static final int STRING_LIMIT_BYTES = 65_535;

    private TrophyExport() {
    }

    /** The player's record as JSON, or a JSON object saying there is none. */
    public static JsonObject toJson(TrophyState state, UUID player, TrophyDefinitions.Snapshot defs) {
        JsonObject out = new JsonObject();
        out.addProperty("mod", NeroTrophiesCommon.MOD_ID);
        out.addProperty("schema", TrophyState.SCHEMA);
        out.addProperty("player", player.toString());
        TrophyRecord record = state.record(player).orElse(null);
        out.addProperty("has_record", record != null);
        if (record == null) {
            return out;
        }
        record.name().ifPresent(n -> out.addProperty("last_known_name", n));
        if (record.nameAt() > 0) {
            out.addProperty("name_refreshed_at", record.nameAt());
        }
        out.addProperty("announce", record.announce());
        out.addProperty("show_name", record.showName());
        JsonArray earned = new JsonArray();
        for (TrophyRecord.Earned e : record.earnedEntries()) {
            JsonObject row = new JsonObject();
            row.addProperty("id", e.trophy().toString());
            TrophyDefinition def = defs.trophies().get(e.trophy());
            if (def != null) {
                row.addProperty("title", def.title());
                row.addProperty("tier", def.tier().getSerializedName());
            }
            row.addProperty("awarded_at", e.at());
            if (!TrophyStamp.isReserved(e.token())) {
                row.addProperty("token", hex(e.token()));
            }
            row.addProperty("claimed", e.claimed());
            if (e.lastClaim() > 0) {
                row.addProperty("last_claim", e.lastClaim());
            }
            earned.add(row);
        }
        out.add("earned", earned);
        out.add("collections", ids(record.completedCollections()));
        // Every paid id, including trophies revoked since (the record keeps them so a reward never pays twice).
        out.add("coins_paid_for", ids(record.paidTrophies()));
        return out;
    }

    /** Pretty JSON text of {@link #toJson}. */
    public static String toText(TrophyState state, UUID player, TrophyDefinitions.Snapshot defs) {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                .toJson(toJson(state, player, defs));
    }

    /**
     * Splits {@code text} into chunks whose encoded size ({@link #encodedBytes}) is at most {@code maxBytes}. A chunk
     * never ends inside a surrogate pair, and the chunks joined are exactly {@code text}.
     */
    public static List<String> chunks(String text, int maxBytes) {
        if (maxBytes < 6) {
            throw new IllegalArgumentException("maxBytes must be at least 6");
        }
        List<String> out = new ArrayList<>();
        int start = 0;
        int size = 0;
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            int n = Character.charCount(cp);
            int cost = cost(cp);
            if (size + cost > maxBytes && i > start) {
                out.add(text.substring(start, i));
                start = i;
                size = 0;
            }
            size += cost;
            i += n;
        }
        if (start < text.length() || out.isEmpty()) {
            out.add(text.substring(start));
        }
        return out;
    }

    /**
     * The worst-case size of {@code text} in bytes: each code point counted as the larger of its JSON-escaped form and
     * its modified UTF-8 form (NBT strings), which is never smaller than its plain UTF-8 form.
     */
    public static int encodedBytes(String text) {
        int total = 0;
        for (int i = 0; i < text.length(); i += Character.charCount(text.codePointAt(i))) {
            total += cost(text.codePointAt(i));
        }
        return total;
    }

    private static int cost(int cp) {
        if (cp == '"' || cp == '\\') {
            return 2;
        }
        if (cp < 0x20 || cp == 0x7f || cp == '<' || cp == '>' || cp == '&' || cp == '=' || cp == '\''
                || cp == 0x2028 || cp == 0x2029) {
            return 6; // a six-byte JSON unicode escape (control characters, and what HTML-safe JSON escapes)
        }
        if (cp < 0x80) {
            return 1;
        }
        if (cp < 0x800) {
            return 2;
        }
        if (cp < 0x10000) {
            return 3;
        }
        return 6; // a surrogate pair, three bytes each in modified UTF-8
    }

    /** A token as 16 lowercase hex digits. */
    public static String hex(long token) {
        return String.format(Locale.ROOT, "%016x", token);
    }

    private static JsonArray ids(Iterable<Identifier> ids) {
        JsonArray a = new JsonArray();
        ids.forEach(id -> a.add(id.toString()));
        return a;
    }
}
