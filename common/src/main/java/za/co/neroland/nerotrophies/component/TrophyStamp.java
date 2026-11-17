package za.co.neroland.nerotrophies.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/**
 * The {@code nerotrophies:trophy} data component: which trophy a stack or block is, and its claim token. Nothing
 * else ever goes on a trophy: no name, no UUID, no award time. The server turns the token into a
 * status (genuine, blank, replica) from its one store; a client only learns a name through name sync.
 *
 * <p>Reserved tokens ({@link #isReserved}): {@link #UNSTAMPED} (a replica), {@link #GALLERY_TOKEN} (a gallery copy
 * with the placeholder name), {@link #GALLERY_BLANK_TOKEN} (a gallery copy with no name), and the rest of
 * {@code -16..-3} kept for later.
 */
public record TrophyStamp(Identifier trophy, long token) {

    /** An unstamped trophy: always a replica, never indexed. */
    public static final long UNSTAMPED = 0L;
    /** A gallery display copy: the client draws the placeholder name, the server never resolves it. */
    public static final long GALLERY_TOKEN = -1L;
    /** A gallery display copy that shows the blank (nameless) ornament. */
    public static final long GALLERY_BLANK_TOKEN = -2L;
    /** The lowest reserved token; every value in {@code [RESERVED_MIN, 0]} is reserved. */
    public static final long RESERVED_MIN = -16L;

    public static final Codec<TrophyStamp> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("trophy").forGetter(TrophyStamp::trophy),
            Codec.LONG.fieldOf("token").forGetter(TrophyStamp::token)).apply(i, TrophyStamp::new));

    public static final StreamCodec<ByteBuf, TrophyStamp> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, TrophyStamp::trophy,
            ByteBufCodecs.VAR_LONG, TrophyStamp::token,
            TrophyStamp::new);

    /** Whether {@code token} is one of the reserved values that are never minted or indexed. */
    public static boolean isReserved(long token) {
        return token >= RESERVED_MIN && token <= 0L;
    }
}
