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

    /** Largest chunk sent in one chat line (the export is split above it). */
    public static final int CHUNK_CHARS = 256 * 1024;

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
        JsonArray paid = new JsonArray();
        for (TrophyRecord.Earned e : record.earnedEntries()) {
            if (record.paid(e.trophy())) {
                paid.add(e.trophy().toString());
            }
        }
        out.add("coins_paid_for", paid);
        return out;
    }

    /** Pretty JSON text of {@link #toJson}. */
    public static String toText(TrophyState state, UUID player, TrophyDefinitions.Snapshot defs) {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
                .toJson(toJson(state, player, defs));
    }

    /** Splits {@code text} into chunks of at most {@code max} characters. */
    public static List<String> chunks(String text, int max) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < text.length(); i += max) {
            out.add(text.substring(i, Math.min(text.length(), i + max)));
        }
        if (out.isEmpty()) {
            out.add("");
        }
        return out;
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
