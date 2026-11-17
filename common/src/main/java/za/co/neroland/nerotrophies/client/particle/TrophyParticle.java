package za.co.neroland.nerotrophies.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
//? if >=1.21.9 {
import net.minecraft.client.particle.SingleQuadParticle;
//?} else {
/*import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
*///?}
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * The five award particles, one class tuned per {@link Kind}. Sprites are greyscale; the spawner
 * tints each particle after creation ({@link #tint} for a tier colour, {@link #prism} for nero's age lerp). All are
 * full-bright and translucent. The spawner passes the initial velocity. Client only.
 */
//? if >=1.21.9 {
public class TrophyParticle extends SingleQuadParticle {
//?} else {
/*public class TrophyParticle extends TextureSheetParticle {
*///?}

    public enum Kind {
        /** Bursts outward, gravity 0.03, four frames once, fades over the last third. */
        SPARK(0.09F),
        /** Stationary four-point star: grows and shrinks once, turns 45 degrees over its life. */
        GLINT(0.14F),
        /** Nero hex fragment: drifts outward and up (gravity -0.005) and spins. */
        SHARD(0.10F),
        /** A camera-facing hex ring expanding through its frames in 12 ticks. */
        RING(0.45F),
        /** A single texel rising 0.02 block per tick for 30 ticks. */
        MOTE(0.04F);

        final float size;

        Kind(float size) {
            this.size = size;
        }
    }

    private final SpriteSet sprites;
    private final Kind kind;
    private boolean prism;
    private int prismFrom;
    private int prismTo;

    protected TrophyParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
            SpriteSet sprites, Kind kind) {
        //? if >=1.21.9 {
        super(level, x, y, z, sprites.first());
        //?} else {
        /*super(level, x, y, z);
        *///?}
        this.sprites = sprites;
        this.kind = kind;
        this.hasPhysics = false;
        this.quadSize = kind.size;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.friction = 1.0F;
        this.gravity = 0.0F;
        this.lifetime = 16;
        switch (kind) {
            case SPARK -> {
                this.gravity = 0.03F;
                this.friction = 0.92F;
            }
            case GLINT, RING -> {
                this.xd = 0.0D;
                this.yd = 0.0D;
                this.zd = 0.0D;
                this.lifetime = kind == Kind.RING ? 12 : 16;
            }
            case SHARD -> {
                this.gravity = -0.005F;
                this.friction = 0.95F;
                this.roll = this.random.nextFloat() * (float) Math.PI * 2F;
                this.oRoll = this.roll;
            }
            case MOTE -> {
                this.xd = 0.0D;
                this.yd = 0.02D;
                this.zd = 0.0D;
                this.lifetime = 30;
            }
        }
        if (kind == Kind.SHARD) {
            this.setSprite(sprites.get(this.random));
        } else {
            this.setSpriteFromAge(sprites);
        }
    }

    /** Lifetime in ticks (the spawner draws it from the tier's range, section 10). */
    public TrophyParticle life(int ticks) {
        if (this.kind != Kind.RING && this.kind != Kind.MOTE) {
            this.lifetime = Math.max(1, ticks);
        }
        return this;
    }

    /** A fixed tint, ARGB (alpha ignored). */
    public TrophyParticle tint(int argb) {
        this.prism = false;
        this.setColor(((argb >> 16) & 0xFF) / 255F, ((argb >> 8) & 0xFF) / 255F, (argb & 0xFF) / 255F);
        return this;
    }

    /** A tint lerped from {@code from} to {@code to} by the particle's age (the nero prism). */
    public TrophyParticle prism(int from, int to) {
        this.prism = true;
        this.prismFrom = from;
        this.prismTo = to;
        this.applyPrism(0F);
        return this;
    }

    private void applyPrism(float t) {
        float r = lerp(t, (this.prismFrom >> 16) & 0xFF, (this.prismTo >> 16) & 0xFF);
        float g = lerp(t, (this.prismFrom >> 8) & 0xFF, (this.prismTo >> 8) & 0xFF);
        float b = lerp(t, this.prismFrom & 0xFF, this.prismTo & 0xFF);
        this.setColor(r / 255F, g / 255F, b / 255F);
    }

    private static float lerp(float t, int a, int b) {
        return a + (b - a) * t;
    }

    //? if >=1.21.9 {
    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }
    //?} else {
    /*@Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
    *///?}

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        float t = this.age / (float) Math.max(1, this.lifetime);
        switch (this.kind) {
            case SPARK -> {
                this.setSpriteFromAge(this.sprites);
                this.setAlpha(t < 2F / 3F ? 1F : Math.max(0F, (1F - t) * 3F));
            }
            case GLINT -> {
                this.setSpriteFromAge(this.sprites);
                this.oRoll = this.roll;
                this.roll = t * (float) Math.PI / 4F;
            }
            case SHARD -> {
                this.oRoll = this.roll;
                this.roll += 0.25F;
                this.setAlpha(t < 0.7F ? 1F : Math.max(0F, (1F - t) / 0.3F));
            }
            case RING -> {
                this.setSpriteFromAge(this.sprites);
            }
            case MOTE -> {
                this.yd = 0.02D;
                this.setAlpha(t < 0.7F ? 1F : Math.max(0F, (1F - t) / 0.3F));
            }
        }
        if (this.prism) {
            this.applyPrism(t);
        }
    }

    @Override
    //? if >=26.1 {
    protected int getLightCoords(float partialTick) {
        return 0xF000F0;
    }
    //?} else {
    /*protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }
    *///?}

    /** One provider per type, created by each loader with the type's sprite set. */
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Kind kind;

        public Provider(SpriteSet sprites, Kind kind) {
            this.sprites = sprites;
            this.kind = kind;
        }

        @Override
        @org.jetbrains.annotations.Nullable
        //? if >=1.21.9 {
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z,
                double xd, double yd, double zd, RandomSource random) {
        //?} else {
        /*public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z,
                double xd, double yd, double zd) {
        *///?}
            return new TrophyParticle(level, x, y, z, xd, yd, zd, this.sprites, this.kind);
        }
    }
}
