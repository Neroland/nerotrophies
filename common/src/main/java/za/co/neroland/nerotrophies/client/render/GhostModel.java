package za.co.neroland.nerotrophies.client.render;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerotrophies.registry.TrophyIds;

/**
 * The cabinet ghost's geometry, read from {@code models/block/trophy_ghost.json} so the model
 * file is the one source of its boxes and UVs: base and stage are drawn face by face with the model's own UVs on the
 * scanline texture, and the {@code marker} element's quad carries the tier pip cell. The model is parsed, not baked:
 * a ghost needs a per-slot tier tint at 35 % alpha, which no MC line's baked-block submission can carry (1.21.1-1.21.4
 * take RGB only, 1.21.5+ / 26.x submit a block state with no colour at all), and a baked standalone model would need
 * three loader-specific registration paths. Re-read when the resource manager changes (a reload); a missing or broken
 * file (or a resource pack override without those elements) falls back to the built-in copy of the same boxes.
 * Client only.
 */
final class GhostModel {

    static final Identifier MODEL = TrophyIds.id("models/block/trophy_ghost.json");

    /** One face of one element: the face, its box (pixels) and its UV rectangle (pixels of the 16x16 texture). */
    record Face(String dir, float x0, float y0, float z0, float x1, float y1, float z1, float u0, float v0, float u1,
            float v1) {
    }

    /** The parsed model: the faces of base and stage, and the marker's plane. */
    record Parts(List<Face> body, float mx0, float my0, float mx1, float my1, float mz) {
    }

    private static final Parts FALLBACK = new Parts(List.of(
            new Face("north", 2, 0, 2, 14, 4, 14, 0, 0, 12, 4), new Face("south", 2, 0, 2, 14, 4, 14, 0, 0, 12, 4),
            new Face("east", 2, 0, 2, 14, 4, 14, 0, 0, 12, 4), new Face("west", 2, 0, 2, 14, 4, 14, 0, 0, 12, 4),
            new Face("up", 2, 0, 2, 14, 4, 14, 0, 0, 12, 12),
            new Face("north", 5, 4, 5, 11, 5, 11, 0, 4, 6, 5), new Face("south", 5, 4, 5, 11, 5, 11, 0, 4, 6, 5),
            new Face("east", 5, 4, 5, 11, 5, 11, 0, 4, 6, 5), new Face("west", 5, 4, 5, 11, 5, 11, 0, 4, 6, 5),
            new Face("up", 5, 4, 5, 11, 5, 11, 0, 0, 6, 6)), 4F, 9F, 12F, 13F, 8F);

    @Nullable
    private static ResourceManager loadedFrom;
    private static Parts parts = FALLBACK;

    private GhostModel() {
    }

    static Parts parts() {
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        if (manager != loadedFrom) {
            loadedFrom = manager;
            parts = load(manager);
        }
        return parts;
    }

    private static Parts load(ResourceManager manager) {
        try {
            Optional<Resource> resource = manager.getResource(MODEL);
            if (resource.isEmpty()) {
                return FALLBACK;
            }
            JsonObject root;
            try (Reader reader = resource.get().openAsReader()) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
            List<Face> body = new ArrayList<>();
            float[] marker = null;
            for (JsonElement e : root.getAsJsonArray("elements")) {
                JsonObject el = e.getAsJsonObject();
                float[] from = vec(el.getAsJsonArray("from"));
                float[] to = vec(el.getAsJsonArray("to"));
                String name = el.has("name") ? el.get("name").getAsString() : "";
                if ("marker".equals(name)) {
                    marker = new float[] {from[0], from[1], to[0], to[1], from[2]};
                    continue;
                }
                JsonObject faces = el.getAsJsonObject("faces");
                for (String dir : faces.keySet()) {
                    JsonObject face = faces.getAsJsonObject(dir);
                    float[] uv = face.has("uv") ? vec(face.getAsJsonArray("uv")) : new float[] {0, 0, 16, 16};
                    body.add(new Face(dir, from[0], from[1], from[2], to[0], to[1], to[2], uv[0], uv[1], uv[2],
                            uv[3]));
                }
            }
            if (body.isEmpty() || marker == null) {
                return FALLBACK;
            }
            return new Parts(List.copyOf(body), marker[0], marker[1], marker[2], marker[3], marker[4]);
        } catch (Exception e) {
            return FALLBACK;
        }
    }

    private static float[] vec(JsonArray array) {
        float[] out = new float[array.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = array.get(i).getAsFloat();
        }
        return out;
    }

    /** Draws the body faces (pixels, the slot's trophy-local unit cube) in {@code argb}. */
    static void body(VertexConsumer c, PoseStack.Pose pose, Parts parts, int argb, int light) {
        for (Face f : parts.body()) {
            face(c, pose, f, argb, light);
        }
    }

    private static void face(VertexConsumer c, PoseStack.Pose pose, Face f, int argb, int light) {
        float x0 = f.x0() / 16F;
        float y0 = f.y0() / 16F;
        float z0 = f.z0() / 16F;
        float x1 = f.x1() / 16F;
        float y1 = f.y1() / 16F;
        float z1 = f.z1() / 16F;
        float u0 = f.u0() / 16F;
        float v0 = f.v0() / 16F;
        float u1 = f.u1() / 16F;
        float v1 = f.v1() / 16F;
        // Corners top-left, bottom-left, bottom-right, top-right as seen from outside (counter-clockwise).
        switch (f.dir()) {
            case "north" -> quad(c, pose, argb, light, 0F, 0F, -1F,
                    x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0, u0, v0, u1, v1);
            case "south" -> quad(c, pose, argb, light, 0F, 0F, 1F,
                    x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1, u0, v0, u1, v1);
            case "west" -> quad(c, pose, argb, light, -1F, 0F, 0F,
                    x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1, u0, v0, u1, v1);
            case "east" -> quad(c, pose, argb, light, 1F, 0F, 0F,
                    x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0, u0, v0, u1, v1);
            case "up" -> quad(c, pose, argb, light, 0F, 1F, 0F,
                    x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, u0, v0, u1, v1);
            case "down" -> quad(c, pose, argb, light, 0F, -1F, 0F,
                    x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, u0, v0, u1, v1);
            default -> {
            }
        }
    }

    private static void quad(VertexConsumer c, PoseStack.Pose pose, int argb, int light, float nx, float ny, float nz,
            float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy,
            float dz, float u0, float v0, float u1, float v1) {
        vertex(c, pose, ax, ay, az, u0, v0, argb, light, nx, ny, nz);
        vertex(c, pose, bx, by, bz, u0, v1, argb, light, nx, ny, nz);
        vertex(c, pose, cx, cy, cz, u1, v1, argb, light, nx, ny, nz);
        vertex(c, pose, dx, dy, dz, u1, v0, argb, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer c, PoseStack.Pose pose, float x, float y, float z, float u, float v,
            int argb, int light, float nx, float ny, float nz) {
        c.addVertex(pose, x, y, z)
                .setColor((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, (argb >>> 24) & 0xFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
