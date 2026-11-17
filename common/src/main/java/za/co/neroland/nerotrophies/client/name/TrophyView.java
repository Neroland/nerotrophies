package za.co.neroland.nerotrophies.client.name;

import net.minecraft.resources.Identifier;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

/**
 * What a client may draw for a trophy it can see: the name state (and name, only when {@link NameState#NAMED}), plus
 * the trophy's tier and icon item id. {@code tier} and {@code icon} are null only when the server has no loaded definition for the trophy.
 */
public record TrophyView(NameState state, String name, TrophyTier tier, Identifier icon) {

    public boolean replica() {
        return state == NameState.REPLICA;
    }

    public boolean named() {
        return state == NameState.NAMED && name != null;
    }
}
