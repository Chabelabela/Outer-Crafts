package org.chabelabela.outer_crafts.client.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.client.timeloop.TimeLoopClientState;
import org.chabelabela.outer_crafts.physics.CelestialBody;
import org.chabelabela.outer_crafts.registry.CelestialBodyRegistry;
import org.chabelabela.outer_crafts.world.dimension.OuterCraftsDimensions;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Set;

/**
 * Draws a parallax sky dome of textured planet billboards.
 * <p>
 * Each body is rendered as a camera-facing emissive quad at a fixed shell radius,
 * scaled by angular size with a per-body minimum so even tiny moons are visible from afar.
 * <p>
 * Registered via {@code RenderLevelStageEvent.AfterTranslucentBlocks} in
 * {@code OuterCrafts.ClientForgeEvents}.
 */
public final class SpaceSkyRenderer {

    private static final float SHELL_RADIUS = 1500f;
    private static final float FADE_START_OFFSET = 250f; // alpha=0 at <= r + this
    private static final float FADE_END_OFFSET = 400f;   // alpha=1 at >= r + this

    /**
     * Minimum angular size in radians, per body. After the dimension rework with
     * planet radii of 55–180 blocks at distances 1100–3500 blocks, the real angular
     * size is usually dominant; these minimums kick in only for very distant viewing.
     */
    private static final Map<String, Float> MIN_ANGULAR = Map.ofEntries(
            Map.entry("sun", 0.18f),
            Map.entry("timber_hearth", 0.030f),
            Map.entry("brittle_hollow", 0.030f),
            Map.entry("giants_deep", 0.038f),
            Map.entry("dark_bramble", 0.026f),
            Map.entry("quantum_moon", 0.018f),
            Map.entry("ash_twin", 0.022f),
            Map.entry("ember_twin", 0.022f),
            Map.entry("hollows_lantern", 0.014f)
    );

    /** Bodies that get an atmospheric halo pass. */
    private static final Set<String> ATMOSPHERE_BODIES = Set.of(
            "timber_hearth", "giants_deep", "brittle_hollow"
    );

    private static final Identifier SUN_GLOW_TEX =
            Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "textures/sky/sun_glow.png");

    private SpaceSkyRenderer() {}

    private static Identifier bodyTex(String id) {
        return Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "textures/sky/" + id + ".png");
    }

    private static Identifier atmosphereTex(String id) {
        return Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "textures/sky/atmosphere_" + id + ".png");
    }

    /**
     * Called from {@code OuterCrafts.ClientForgeEvents.onRenderLevelStage}.
     * Only runs when fired as {@code AfterTranslucentBlocks} in the solar system dimension.
     */
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!mc.level.dimension().equals(OuterCraftsDimensions.SOLAR_SYSTEM_LEVEL)) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        long tick = mc.level.getGameTime();
        Map<String, Vec3> positions = CelestialBodyRegistry.resolveAllPositions(tick);

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Matrix4f pose = poseStack.last().pose();

        // Pass 1: planet bodies (emissive translucent so they read as bright)
        for (Map.Entry<String, Vec3> entry : positions.entrySet()) {
            String id = entry.getKey();
            if ("twin_center".equals(id)) continue; // virtual orbital anchor
            CelestialBody body = CelestialBodyRegistry.get(id);
            if (body == null) continue;

            BillboardPlan plan = computeBillboard(entry.getValue(), camPos, body, id);
            if (plan == null) continue;

            VertexConsumer vc = bufferSource.getBuffer(RenderTypes.entityTranslucentEmissive(bodyTex(id)));
            emitQuad(vc, pose, plan, plan.alpha, 1.0f);
        }

        // Pass 2: sun glow (larger, additive-feeling)
        Vec3 sunPos = positions.get("sun");
        if (sunPos != null) {
            CelestialBody sun = CelestialBodyRegistry.get("sun");
            BillboardPlan sunPlan = computeBillboard(sunPos, camPos, sun, "sun");
            if (sunPlan != null) {
                VertexConsumer glow = bufferSource.getBuffer(RenderTypes.entityTranslucentEmissive(SUN_GLOW_TEX));
                emitQuad(glow, pose, sunPlan, sunPlan.alpha * 0.85f, 3.0f);
            }
        }

        // Pass 3: atmospheric halos for bodies that have them
        for (String id : ATMOSPHERE_BODIES) {
            Vec3 pos = positions.get(id);
            if (pos == null) continue;
            CelestialBody body = CelestialBodyRegistry.get(id);
            BillboardPlan plan = computeBillboard(pos, camPos, body, id);
            if (plan == null) continue;

            VertexConsumer halo = bufferSource.getBuffer(RenderTypes.entityTranslucentEmissive(atmosphereTex(id)));
            emitQuad(halo, pose, plan, plan.alpha * 0.7f, 1.08f);
        }

        bufferSource.endBatch();
    }

    /** Per-body billboard geometry. */
    private record BillboardPlan(float cx, float cy, float cz,
                                  Vector3f right, Vector3f up,
                                  float halfSize, float alpha) {}

    private static BillboardPlan computeBillboard(Vec3 bodyPos, Vec3 camPos,
                                                   CelestialBody body, String id) {
        double dx = bodyPos.x - camPos.x;
        double dy = bodyPos.y - camPos.y;
        double dz = bodyPos.z - camPos.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 1e-3) return null;

        // Near-field alpha fade.
        // For the sun, use the dynamic supernova-driven radius from the time-loop
        // network state so the player visually sees it expand during the warning
        // and supernova phases. Falls back to body.radius() if the live state is
        // smaller than the base (e.g. before any payload arrived).
        float bodyRadius;
        if ("sun".equals(id)) {
            float liveRadius = (float) TimeLoopClientState.getSunRadius();
            float baseRadius = (float) body.radius();
            bodyRadius = Math.max(liveRadius, baseRadius);
        } else {
            bodyRadius = (float) body.radius();
        }
        float fadeStart = bodyRadius + FADE_START_OFFSET;
        float fadeEnd = bodyRadius + FADE_END_OFFSET;
        float alpha;
        if (dist <= fadeStart) return null; // fully faded = skip entirely
        else if (dist >= fadeEnd) alpha = 1f;
        else alpha = (float) ((dist - fadeStart) / (fadeEnd - fadeStart));

        double inv = 1.0 / dist;
        float dirX = (float) (dx * inv);
        float dirY = (float) (dy * inv);
        float dirZ = (float) (dz * inv);

        // Angular size — real, clamped to per-body minimum
        float minAngular = MIN_ANGULAR.getOrDefault(id, 0.015f);
        float trueAngular = (float) (2.0 * Math.atan(bodyRadius / dist));
        float angular = Math.max(trueAngular, minAngular);
        float halfSize = SHELL_RADIUS * (float) Math.tan(angular * 0.5);

        // Build camera-facing basis
        Vector3f dir = new Vector3f(dirX, dirY, dirZ);
        Vector3f worldUp = new Vector3f(0f, 1f, 0f);
        Vector3f right = new Vector3f(worldUp).cross(dir);
        if (right.lengthSquared() < 1e-6f) {
            right.set(1f, 0f, 0f);
        }
        right.normalize();
        Vector3f up = new Vector3f(dir).cross(right).normalize();

        float cx = dirX * SHELL_RADIUS;
        float cy = dirY * SHELL_RADIUS;
        float cz = dirZ * SHELL_RADIUS;

        return new BillboardPlan(cx, cy, cz, right, up, halfSize, alpha);
    }

    private static void emitQuad(VertexConsumer vc, Matrix4f pose, BillboardPlan plan,
                                  float alpha, float scaleMul) {
        float s = plan.halfSize * scaleMul;
        float rx = plan.right.x * s, ry = plan.right.y * s, rz = plan.right.z * s;
        float ux = plan.up.x * s,    uy = plan.up.y * s,    uz = plan.up.z * s;
        int argb = packAlpha(alpha);
        int light = 0x00F000F0;
        int overlay = OverlayTexture.NO_OVERLAY;

        // Normal points back toward camera (opposite of view direction into scene)
        float nx = -plan.right.y * plan.up.z + plan.right.z * plan.up.y;
        float ny = -plan.right.z * plan.up.x + plan.right.x * plan.up.z;
        float nz = -plan.right.x * plan.up.y + plan.right.y * plan.up.x;

        // BL (0,1), BR (1,1), TR (1,0), TL (0,0)
        quadVertex(vc, pose, plan.cx - rx - ux, plan.cy - ry - uy, plan.cz - rz - uz, argb, 0f, 1f, overlay, light, nx, ny, nz);
        quadVertex(vc, pose, plan.cx + rx - ux, plan.cy + ry - uy, plan.cz + rz - uz, argb, 1f, 1f, overlay, light, nx, ny, nz);
        quadVertex(vc, pose, plan.cx + rx + ux, plan.cy + ry + uy, plan.cz + rz + uz, argb, 1f, 0f, overlay, light, nx, ny, nz);
        quadVertex(vc, pose, plan.cx - rx + ux, plan.cy - ry + uy, plan.cz - rz + uz, argb, 0f, 0f, overlay, light, nx, ny, nz);
    }

    private static void quadVertex(VertexConsumer vc, Matrix4f pose,
                                    float x, float y, float z, int argb,
                                    float u, float v, int overlay, int light,
                                    float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }

    private static int packAlpha(float alpha) {
        int ai = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (ai << 24) | 0x00FFFFFF;
    }
}
