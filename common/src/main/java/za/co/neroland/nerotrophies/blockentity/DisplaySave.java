package za.co.neroland.nerotrophies.blockentity;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerotrophies.component.TrophyStamp;

/**
 * Everything a display block entity writes to the world, as one codec:
 * the stamps it holds (trophy id + token, per slot), its palette finish, and for a cabinet the bound collection, the
 * trim and the gallery-only {@code display_only} flag. No name, no UUID, no award time can be expressed here; an
 * anvil name or any other component on a trophy stack is deliberately not kept (a free-text name could name a
 * person). A pedestal's non-trophy item is the one ordinary {@code ItemStack} a display stores, beside this record.
 */
public record DisplaySave(List<SlotStamp> stamps, Optional<Identifier> finish, Optional<Identifier> collection,
        String trim, boolean displayOnly) {

    /** One occupied trophy slot. */
    public record SlotStamp(int slot, TrophyStamp stamp) {
        public static final Codec<SlotStamp> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 63).fieldOf("slot").forGetter(SlotStamp::slot),
                TrophyStamp.CODEC.fieldOf("stamp").forGetter(SlotStamp::stamp)).apply(i, SlotStamp::new));
    }

    public static final String DEFAULT_TRIM = "standard";

    public static final Codec<DisplaySave> CODEC = RecordCodecBuilder.create(i -> i.group(
            SlotStamp.CODEC.listOf().optionalFieldOf("stamps", List.of()).forGetter(DisplaySave::stamps),
            Identifier.CODEC.optionalFieldOf("finish").forGetter(DisplaySave::finish),
            Identifier.CODEC.optionalFieldOf("collection").forGetter(DisplaySave::collection),
            Codec.STRING.optionalFieldOf("trim", DEFAULT_TRIM).forGetter(DisplaySave::trim),
            Codec.BOOL.optionalFieldOf("display_only", false).forGetter(DisplaySave::displayOnly))
            .apply(i, DisplaySave::new));

    public DisplaySave {
        stamps = List.copyOf(stamps);
    }

    public static DisplaySave empty() {
        return new DisplaySave(List.of(), Optional.empty(), Optional.empty(), DEFAULT_TRIM, false);
    }
}
