package za.co.neroland.nerotrophies.blockentity;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

/**
 * What a client may know about one occupied display slot (a physical trophy reveals its tier and icon
 * to whoever can see it): tier, icon item, and whether the server resolved it as a replica. Sent in the block
 * entity's update tag only, never saved, never trusted by the server; no token, no name.
 */
public record SlotView(int slot, Optional<TrophyTier> tier, Optional<Identifier> icon, boolean replica) {

    public static final Codec<SlotView> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, 63).fieldOf("slot").forGetter(SlotView::slot),
            TrophyTier.CODEC.optionalFieldOf("tier").forGetter(SlotView::tier),
            Identifier.CODEC.optionalFieldOf("icon").forGetter(SlotView::icon),
            Codec.BOOL.optionalFieldOf("replica", false).forGetter(SlotView::replica)).apply(i, SlotView::new));
}
