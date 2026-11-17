package za.co.neroland.nerotrophies.gallery;

import java.util.Optional;

import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import za.co.neroland.nerolandcore.data.SavedDataCompat;
import za.co.neroland.nerolandcore.data.SavedDataRecovery;

import za.co.neroland.nerotrophies.NeroTrophiesCommon;

/**
 * The one gallery a server can have at a time: where it stands and the snapshot of what it replaced, so
 * {@code gallery clear} works after a restart. World data only; it is not keyed by, and never holds, a player. Written
 * once the build's capture completes (before any block changes) and emptied when a clear finishes. Read through Core's
 * recovery guard on the overworld.
 */
public final class GalleryStore extends SavedData {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(NeroTrophiesCommon.MOD_ID, "gallery");

    public static final Codec<GalleryStore> CODEC = GallerySnapshot.CODEC.optionalFieldOf("gallery").codec()
            .xmap(GalleryStore::new, GalleryStore::snapshot);

    private Optional<GallerySnapshot> snapshot;

    public GalleryStore() {
        this(Optional.empty());
    }

    private GalleryStore(Optional<GallerySnapshot> snapshot) {
        this.snapshot = snapshot;
    }

    public static GalleryStore get(MinecraftServer server) {
        return SavedDataRecovery.get(server.overworld(), type(), GalleryStore::new, ID.toString());
    }

    public static SavedDataType<GalleryStore> type() {
        return Types.TYPE;
    }

    private static final class Types {
        static final SavedDataType<GalleryStore> TYPE = SavedDataCompat.type(ID, GalleryStore::new, CODEC);
    }

    public Optional<GallerySnapshot> snapshot() {
        return this.snapshot;
    }

    public void set(GallerySnapshot snapshot) {
        this.snapshot = Optional.of(snapshot);
        this.setDirty();
    }

    public void clear() {
        this.snapshot = Optional.empty();
        this.setDirty();
    }
}
