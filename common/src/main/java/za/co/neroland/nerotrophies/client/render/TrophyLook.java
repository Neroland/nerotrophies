package za.co.neroland.nerotrophies.client.render;

import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.block.TrophyItemHooks;
import za.co.neroland.nerotrophies.blockentity.PlateState;
import za.co.neroland.nerotrophies.blockentity.SlotView;
import za.co.neroland.nerotrophies.client.name.ClientTrophyNames;
import za.co.neroland.nerotrophies.client.name.TrophyView;
import za.co.neroland.nerotrophies.client.net.ClientTrophyRecord;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.network.RecordPayload;

/**
 * The one adapter between the blocks / screens and the name-sync client API: everything a renderer or tooltip may
 * know about a stamped trophy, from (in order) name sync for this token, the display block's own view, and the
 * player's own record. A name is returned only for a token the server resolved as named, never for a replica.
 * Memory only, client only.
 */
public final class TrophyLook {

    /** What to draw for one stamped trophy. {@code name} is non-null only for {@link PlateState#NAMED} / GALLERY. */
    public record Look(PlateState plate, @Nullable String name, @Nullable TrophyTier tier, @Nullable Identifier icon) {

        public boolean replica() {
            return this.plate == PlateState.REPLICA;
        }

        /** Tier for model choice; an unknown definition draws as bronze (a generic base). */
        public TrophyTier tierOrBronze() {
            return this.tier != null ? this.tier : TrophyTier.BRONZE;
        }
    }

    private TrophyLook() {
    }

    /** The look of {@code stamp}; {@code hint} is the display block's own view of the slot, if any. */
    public static Look of(TrophyStamp stamp, Optional<SlotView> hint) {
        long token = stamp.token();
        boolean reserved = TrophyStamp.isReserved(token);
        TrophyView view = reserved ? ClientTrophyNames.viewForTrophy(stamp.trophy()) : ClientTrophyNames.viewFor(token);
        boolean replica;
        if (token == TrophyStamp.UNSTAMPED) {
            replica = true;
        } else if (!reserved && view != null) {
            replica = view.replica();
        } else {
            replica = !reserved && hint.map(SlotView::replica).orElse(false);
        }
        TrophyTier tier = view != null && view.tier() != null ? view.tier() : hint.flatMap(SlotView::tier).orElse(null);
        Identifier icon = view != null && view.icon() != null ? view.icon() : hint.flatMap(SlotView::icon).orElse(null);
        if (tier == null || icon == null) {
            RecordPayload.Entry entry = ownEntry(stamp.trophy());
            if (entry != null && !entry.masked()) {
                tier = tier != null ? tier : entry.tier();
                icon = icon != null ? icon : entry.icon();
            }
        }
        String name = reserved ? null : ClientTrophyNames.nameFor(token);
        PlateState plate = PlateState.of(token, replica, name);
        String shown = switch (plate) {
            case NAMED -> name;
            case GALLERY -> Component.translatable("nerotrophies.gallery.placeholder_name").getString();
            default -> null;
        };
        return new Look(plate, shown, tier, icon);
    }

    /** The viewer's own record entry for {@code trophy}, if the Book's record has arrived. */
    @Nullable
    public static RecordPayload.Entry ownEntry(Identifier trophy) {
        RecordPayload record = ClientTrophyRecord.current();
        return record == null ? null : record.entry(trophy);
    }

    /** A trophy's title lang key: the viewer's record if it has one, else Core's default key. */
    public static String titleKey(Identifier trophy) {
        RecordPayload.Entry entry = ownEntry(trophy);
        if (entry != null && entry.title() != null) {
            return entry.title();
        }
        return TrophyItemHooks.defaultKey("trophy", trophy, "title");
    }

    /** A collection's title lang key from the viewer's record, else Core's default key. */
    public static String collectionTitleKey(Identifier collection) {
        RecordPayload record = ClientTrophyRecord.current();
        if (record != null) {
            for (RecordPayload.CollectionView view : record.collections()) {
                if (view.id().equals(collection)) {
                    return view.title();
                }
            }
        }
        return TrophyItemHooks.defaultKey("collection", collection, "title");
    }
}
