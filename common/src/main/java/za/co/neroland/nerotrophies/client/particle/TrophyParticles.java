package za.co.neroland.nerotrophies.client.particle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.trophy.TrophyTier;

import za.co.neroland.nerotrophies.blockentity.CabinetBlockEntity;
import za.co.neroland.nerotrophies.blockentity.DisplayBlockEntity;
import za.co.neroland.nerotrophies.blockentity.PedestalBlockEntity;
import za.co.neroland.nerotrophies.blockentity.TrophyBlockEntity;
import za.co.neroland.nerotrophies.client.render.TrophyColours;
import za.co.neroland.nerotrophies.client.render.TrophyLook;
import za.co.neroland.nerotrophies.component.TrophyStamp;
import za.co.neroland.nerotrophies.registry.ModParticles;

/**
 * Award feedback particles (ART-PLAN section 5, constants section 10): provider wiring for every loader, the award
 * burst at the earner's chest (spawned on the earner's own client only), the placement burst when a genuine trophy
 * appears in a loaded chunk, the completion burst when a bound cabinet pool fills, and the idle gold glint / nero mote
 * on a placed genuine trophy within 8 blocks. "Decreased" halves every count (rounded up), "Minimal" draws nothing.
 * Replicas, cabinet slots and ghosts never emit. Nothing here reads or keeps a name or a token. Client only.
 */
public final class TrophyParticles {

    /** A loader's sprite-set provider registration. */
    public interface Sink {
        void register(SimpleParticleType type, Function<SpriteSet, ParticleProvider<SimpleParticleType>> factory);
    }

    private static final double IDLE_RANGE_SQ = 8.0D * 8.0D;
    private static final int SCAN_CHUNKS = 1;
    private static final int COLLECTION = -2;

    /** Delayed spawns (nero's second ring, 4 ticks after the first). */
    private static final List<Pending> PENDING = new ArrayList<>();
    /** Trophy positions seen in chunks scanned last pass: packed pos -> tier ordinal, -1 for a replica / unknown. */
    private static final Map<Long, Integer> TROPHIES = new HashMap<>();
    /** Bound cabinets seen last pass: packed pos -> pool complete. */
    private static final Map<Long, Boolean> CABINETS = new HashMap<>();
    /** Idle countdowns per placed genuine trophy. */
    private static final Map<Long, Integer> IDLE = new HashMap<>();
    private static final Set<Long> SCANNED = new HashSet<>();
    @Nullable
    private static ClientLevel lastLevel;

    private record Pending(Runnable action, int[] ticks) {
    }

    private TrophyParticles() {
    }

    public static void registerAll(Sink sink) {
        sink.register(ModParticles.TROPHY_SPARK.get(), s -> new TrophyParticle.Provider(s, TrophyParticle.Kind.SPARK));
        sink.register(ModParticles.TROPHY_GLINT.get(), s -> new TrophyParticle.Provider(s, TrophyParticle.Kind.GLINT));
        sink.register(ModParticles.TROPHY_SHARD.get(), s -> new TrophyParticle.Provider(s, TrophyParticle.Kind.SHARD));
        sink.register(ModParticles.TROPHY_RING.get(), s -> new TrophyParticle.Provider(s, TrophyParticle.Kind.RING));
        sink.register(ModParticles.TROPHY_MOTE.get(), s -> new TrophyParticle.Provider(s, TrophyParticle.Kind.MOTE));
    }

    // ---- bursts ---------------------------------------------------------------------------------------------------

    /** The award burst for {@code tier}, at the local player's chest height just in front of them. */
    public static void awardBurst(TrophyTier tier) {
        Vec3 at = chest();
        if (at != null) {
            burst(at, tier.ordinal(), 1.0F);
        }
    }

    /** The collection-complete burst at the local player (accent colour: a ring, glints and a few sparks). */
    public static void collectionBurst() {
        Vec3 at = chest();
        if (at != null) {
            burst(at, COLLECTION, 1.0F);
        }
    }

    @Nullable
    private static Vec3 chest() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return null;
        }
        Vec3 look = mc.player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        flat = flat.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : flat.normalize();
        return mc.player.getEyePosition().add(flat.scale(0.9D)).add(0.0D, -0.45D, 0.0D);
    }

    /** {@code kind}: a tier ordinal, or {@link #COLLECTION}. {@code factor} scales the counts (placement: 0.5). */
    private static void burst(Vec3 at, int kind, float factor) {
        RandomSource r = RandomSource.create();
        switch (kind) {
            case 0 -> { // bronze
                int glow = TrophyColours.glow(TrophyTier.BRONZE);
                sparks(at, count(8, factor), glow, 12, 18, r);
            }
            case 1 -> { // silver
                int glow = TrophyColours.glow(TrophyTier.SILVER);
                sparks(at, count(12, factor), glow, 14, 20, r);
                glints(at, count(2, factor), glow, false, 14, 20, r);
            }
            case 2 -> { // gold
                int glow = TrophyColours.glow(TrophyTier.GOLD);
                sparks(at, count(18, factor), glow, 16, 26, r);
                glints(at, count(6, factor), glow, false, 16, 26, r);
                rings(at, count(1, factor), glow, false);
            }
            case 3 -> { // nero
                shards(at, count(16, factor), 20, 34, r);
                glints(at, count(8, factor), 0, true, 20, 34, r);
                int rings = count(2, factor);
                rings(at, Math.min(1, rings), 0, true);
                if (rings > 1) {
                    later(4, () -> rings(at, 1, 0, true));
                }
            }
            default -> { // collection complete
                int accent = TrophyColours.ACCENT_BRIGHT;
                sparks(at, count(10, factor), accent, 16, 26, r);
                glints(at, count(8, factor), accent, false, 16, 26, r);
                rings(at, count(1, factor), accent, false);
            }
        }
    }

    private static void sparks(Vec3 at, int n, int tint, int minLife, int maxLife, RandomSource r) {
        for (int i = 0; i < n; i++) {
            Vec3 dir = randomDir(r, false);
            double speed = 0.08D + r.nextDouble() * 0.08D;
            TrophyParticle p = spawn(ModParticles.TROPHY_SPARK.get(), at, dir.scale(speed));
            if (p != null) {
                p.life(minLife + r.nextInt(maxLife - minLife + 1)).tint(tint);
            }
        }
    }

    private static void shards(Vec3 at, int n, int minLife, int maxLife, RandomSource r) {
        for (int i = 0; i < n; i++) {
            Vec3 dir = randomDir(r, true);
            double speed = 0.05D + r.nextDouble() * 0.05D;
            TrophyParticle p = spawn(ModParticles.TROPHY_SHARD.get(), at, dir.scale(speed));
            if (p != null) {
                p.life(minLife + r.nextInt(maxLife - minLife + 1)).prism(TrophyColours.PRISM_A, TrophyColours.PRISM_B);
            }
        }
    }

    private static void glints(Vec3 at, int n, int tint, boolean prism, int minLife, int maxLife, RandomSource r) {
        for (int i = 0; i < n; i++) {
            Vec3 pos = at.add((r.nextDouble() - 0.5D) * 0.7D, (r.nextDouble() - 0.5D) * 0.6D,
                    (r.nextDouble() - 0.5D) * 0.7D);
            TrophyParticle p = spawn(ModParticles.TROPHY_GLINT.get(), pos, Vec3.ZERO);
            if (p != null) {
                p.life(minLife + r.nextInt(maxLife - minLife + 1));
                if (prism) {
                    p.prism(TrophyColours.PRISM_A, TrophyColours.PRISM_B);
                } else {
                    p.tint(tint);
                }
            }
        }
    }

    private static void rings(Vec3 at, int n, int tint, boolean prism) {
        for (int i = 0; i < n; i++) {
            TrophyParticle p = spawn(ModParticles.TROPHY_RING.get(), at, Vec3.ZERO);
            if (p != null) {
                if (prism) {
                    p.prism(TrophyColours.PRISM_A, TrophyColours.PRISM_B);
                } else {
                    p.tint(tint);
                }
            }
        }
    }

    private static Vec3 randomDir(RandomSource r, boolean upward) {
        for (int i = 0; i < 8; i++) {
            double x = r.nextDouble() * 2.0D - 1.0D;
            double y = upward ? r.nextDouble() * 0.8D + 0.2D : r.nextDouble() * 2.0D - 1.0D;
            double z = r.nextDouble() * 2.0D - 1.0D;
            double len = Math.sqrt(x * x + y * y + z * z);
            if (len > 1.0E-3D && len <= 1.0D) {
                return new Vec3(x / len, y / len, z / len);
            }
        }
        return new Vec3(0.0D, 1.0D, 0.0D);
    }

    @Nullable
    private static TrophyParticle spawn(SimpleParticleType type, Vec3 at, Vec3 velocity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        Particle p = mc.particleEngine.createParticle(type, at.x, at.y, at.z, velocity.x, velocity.y, velocity.z);
        return p instanceof TrophyParticle t ? t : null;
    }

    /** {@code n} scaled by the particle setting and {@code factor}: All as is, Decreased halved (up), Minimal 0. */
    static int count(int n, float factor) {
        int status = status();
        if (status >= 2 || n <= 0) {
            return 0;
        }
        int scaled = (int) Math.ceil(n * factor);
        return status == 1 ? (scaled + 1) / 2 : scaled;
    }

    /** 0 All, 1 Decreased, 2 Minimal. */
    private static int status() {
        return Minecraft.getInstance().options.particles().get().ordinal();
    }

    private static void later(int ticks, Runnable action) {
        PENDING.add(new Pending(action, new int[] {ticks}));
    }

    // ---- the client tick: delayed spawns, placement / completion bursts, idle emitters ----------------------------

    /** End of every client tick (each loader's feedback glue). */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level != lastLevel) {
            clear();
            lastLevel = level;
        }
        if (level == null || mc.player == null || mc.isPaused()) {
            return;
        }
        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext();) {
            Pending p = it.next();
            if (--p.ticks()[0] <= 0) {
                it.remove();
                p.action().run();
            }
        }
        scan(level, mc.player.getEyePosition());
    }

    private static void scan(ClientLevel level, Vec3 eye) {
        int pcx = BlockPos.containing(eye).getX() >> 4;
        int pcz = BlockPos.containing(eye).getZ() >> 4;
        Set<Long> scanned = new HashSet<>();
        Map<Long, Integer> trophies = new HashMap<>();
        Map<Long, Boolean> cabinets = new HashMap<>();
        RandomSource r = level.getRandom();
        for (int cx = pcx - SCAN_CHUNKS; cx <= pcx + SCAN_CHUNKS; cx++) {
            for (int cz = pcz - SCAN_CHUNKS; cz <= pcz + SCAN_CHUNKS; cz++) {
                if (!level.getChunkSource().hasChunk(cx, cz)) {
                    continue;
                }
                long chunkKey = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
                scanned.add(chunkKey);
                boolean known = SCANNED.contains(chunkKey);
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    long key = be.getBlockPos().asLong();
                    if (be instanceof CabinetBlockEntity cabinet) {
                        if (cabinet.collection().isPresent() && cabinet.poolIndex() == 0) {
                            boolean complete = complete(cabinet);
                            cabinets.put(key, complete);
                            Boolean before = CABINETS.get(key);
                            if (known && complete && before != null && !before) {
                                burst(Vec3.atCenterOf(be.getBlockPos()).add(0.0D, 0.6D, 0.0D), COLLECTION, 0.5F);
                            }
                        }
                        continue;
                    }
                    double top = topOf(be);
                    if (Double.isNaN(top)) {
                        continue;
                    }
                    int tier = genuineTier((DisplayBlockEntity) be);
                    if (tier == Integer.MIN_VALUE) {
                        continue;
                    }
                    trophies.put(key, tier);
                    Vec3 at = new Vec3(be.getBlockPos().getX() + 0.5D, be.getBlockPos().getY() + top,
                            be.getBlockPos().getZ() + 0.5D);
                    if (known && tier >= 0 && !TROPHIES.containsKey(key)) {
                        burst(at, tier, 0.5F);
                    }
                    if (tier >= TrophyTier.GOLD.ordinal() && at.distanceToSqr(eye) <= IDLE_RANGE_SQ) {
                        idle(key, tier, at, r);
                    }
                }
            }
        }
        IDLE.keySet().retainAll(trophies.keySet());
        TROPHIES.clear();
        TROPHIES.putAll(trophies);
        CABINETS.clear();
        CABINETS.putAll(cabinets);
        SCANNED.clear();
        SCANNED.addAll(scanned);
    }

    /** The height (blocks above the block's origin) of a placed trophy's lens, NaN for anything else. */
    private static double topOf(BlockEntity be) {
        if (be instanceof TrophyBlockEntity) {
            return 0.8D;
        }
        if (be instanceof PedestalBlockEntity) {
            return 15.0D / 16.0D + 0.75D * 0.8D;
        }
        return Double.NaN;
    }

    /** The tier ordinal of a genuine trophy held in slot 0; -1 for a replica or unknown tier; MIN_VALUE for none. */
    private static int genuineTier(DisplayBlockEntity be) {
        TrophyStamp stamp = DisplayBlockEntity.stampOf(be.getItem(0));
        if (stamp == null) {
            return be.getItem(0).isEmpty() ? Integer.MIN_VALUE : -1;
        }
        if (stamp.token() == TrophyStamp.UNSTAMPED) {
            return -1;
        }
        TrophyLook.Look look = TrophyLook.of(stamp, be.view(0));
        return look.replica() || look.tier() == null ? -1 : look.tier().ordinal();
    }

    private static boolean complete(CabinetBlockEntity cabinet) {
        int members = cabinet.memberCount();
        if (members <= 0 || members > 63) {
            return false;
        }
        long all = (1L << members) - 1L;
        return (cabinet.mask() & all) == all;
    }

    /** Gold: one glint every 60-100 ticks near an arm tip; nero: one rising mote every 20 ticks from the lens. */
    private static void idle(long key, int tier, Vec3 at, RandomSource r) {
        if (status() >= 2) {
            return;
        }
        boolean nero = tier == TrophyTier.NERO.ordinal();
        int left = IDLE.getOrDefault(key, nero ? 20 : 60 + r.nextInt(41)) - 1;
        if (left > 0) {
            IDLE.put(key, left);
            return;
        }
        if (nero) {
            IDLE.put(key, 20);
            TrophyParticle p = spawn(ModParticles.TROPHY_MOTE.get(), at, new Vec3(0.0D, 0.02D, 0.0D));
            if (p != null) {
                p.prism(TrophyColours.PRISM_A, TrophyColours.PRISM_B);
            }
        } else {
            IDLE.put(key, 60 + r.nextInt(41));
            double side = r.nextBoolean() ? 0.22D : -0.22D;
            Vec3 tip = at.add(side, -0.12D, 0.0D);
            TrophyParticle p = spawn(ModParticles.TROPHY_GLINT.get(), tip, Vec3.ZERO);
            if (p != null) {
                p.life(16).tint(TrophyColours.glow(TrophyTier.GOLD));
            }
        }
    }

    /** Forget everything (level change, disconnect). */
    public static void clear() {
        PENDING.clear();
        TROPHIES.clear();
        CABINETS.clear();
        IDLE.clear();
        SCANNED.clear();
        lastLevel = null;
    }
}
