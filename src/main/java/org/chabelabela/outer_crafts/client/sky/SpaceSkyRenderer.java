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
import org.joml.Vector4f;

import java.util.Map;

/**
 * Renders every celestial body as a real 3D textured sphere at its actual
 * world position, drawn in the {@code AFTER_SKY} render stage so it sits
 * behind world geometry but in front of the void/star backdrop.
 *
 * <p>Replaces the previous flat-billboard approach which broke at distance
 * (no parallax, fixed shell radius made planets look stuck to the camera,
 * Z-fought with translucent water/glass).
 *
 * <p>Each draw:
 * <ul>
 *   <li>The icosphere from {@link PlanetSphereMesh} is scaled by the body's
 *       radius and translated to the camera-relative position of the body.</li>
 *   <li>The body's per-id texture is sampled equirectangularly via UVs
 *       computed from each vertex's normalized position.</li>
 *   <li>A distance fade prevents the sphere mesh from clipping inside the
 *       actual world blocks when the player gets close.</li>
 *   <li>The sun gets a uniform-bright treatment (it's a light source) plus
 *       an additive {@code sun_glow} halo for OW-style corona.</li>
 * </ul>
 */
public final class SpaceSkyRenderer {

    /** Inside this distance from a planet's centre, the sphere mesh is fully invisible
     *  (the player has reached the actual world blocks). */
    private static final float FADE_FULL_HIDE = 30f;
    /** Past this distance, the sphere is fully opaque. Linear fade between. */
    private static final float FADE_FULL_SHOW = 60f;

    private static final Identifier SUN_GLOW_TEX =
            Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "textures/sky/sun_glow.png");

    private SpaceSkyRenderer() {}

    private static Identifier bodyTex(String id) {
        return Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "textures/sky/" + id + ".png");
    }

    /**
     * Called from {@code OuterCrafts.ClientForgeEvents.onRenderLevelStage}.
     * Active in the solar-system dimension only. Fired during {@code AFTER_SKY}
     * so spheres render between vanilla sky and world geometry.
     */
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!mc.level.dimension().equals(OuterCraftsDimensions.SOLAR_SYSTEM_LEVEL)) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        long tick = mc.level.getGameTime();
        Map<String, Vec3> positions = CelestialBodyRegistry.resolveAllPositions(tick);
        Vec3 sunPos = positions.get("sun");

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // Pass 1: solid spheres for every body.
        for (Map.Entry<String, Vec3> entry : positions.entrySet()) {
            String id = entry.getKey();
            if ("twin_center".equals(id)) continue; // virtual orbital anchor, no body
            CelestialBody body = CelestialBodyRegistry.get(id);
            if (body == null || body.radius() <= 0) continue;

            Vec3 planetCenter = entry.getValue();
            float dx = (float) (planetCenter.x - camPos.x);
            float dy = (float) (planetCenter.y - camPos.y);
            float dz = (float) (planetCenter.z - camPos.z);
            float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

            float surfaceDist = dist - (float) body.radius();
            if (surfaceDist < FADE_FULL_HIDE) continue; // inside the planet → blocks own this view
            float alpha = surfaceDist >= FADE_FULL_SHOW
                    ? 1f
                    : (surfaceDist - FADE_FULL_HIDE) / (FADE_FULL_SHOW - FADE_FULL_HIDE);

            // Sun-direction in body-local space, for Lambert shading on non-sun bodies.
            Vector3f sunDirLocal;
            if ("sun".equals(id) || sunPos == null) {
                sunDirLocal = null; // emissive — no shading
            } else {
                double sx = sunPos.x - planetCenter.x;
                double sy = sunPos.y - planetCenter.y;
                double sz = sunPos.z - planetCenter.z;
                double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
                if (len < 1e-6) sunDirLocal = null;
                else sunDirLocal = new Vector3f((float)(sx / len), (float)(sy / len), (float)(sz / len));
            }

            // Use the dynamic supernova-driven sun radius so we visually see it
            // expand during warning/supernova phases. Otherwise use base radius.
            float bodyRadius;
            if ("sun".equals(id)) {
                float liveRadius = (float) TimeLoopClientState.getSunRadius();
                bodyRadius = Math.max(liveRadius, (float) body.radius());
            } else {
                bodyRadius = (float) body.radius();
            }

            renderSphere(poseStack, bufferSource, bodyTex(id),
                    dx, dy, dz, bodyRadius, alpha, sunDirLocal);
        }

        // Pass 2: additive sun-glow halo (slightly larger than the sun itself).
        if (sunPos != null) {
            CelestialBody sun = CelestialBodyRegistry.get("sun");
            if (sun != null) {
                float dx = (float) (sunPos.x - camPos.x);
                float dy = (float) (sunPos.y - camPos.y);
                float dz = (float) (sunPos.z - camPos.z);
                float liveRadius = (float) TimeLoopClientState.getSunRadius();
                float bodyRadius = Math.max(liveRadius, (float) sun.radius());
                renderSphere(poseStack, bufferSource, SUN_GLOW_TEX,
                        dx, dy, dz, bodyRadius * 1.6f, 0.55f, null);
            }
        }

        bufferSource.endBatch();
    }

    /**
     * Emit the icosphere as a textured 3D ball at the given camera-relative
     * offset, scaled to {@code radius}. {@code sunDirLocal} non-null applies
     * a soft Lambert shading using that direction in body-local space.
     */
    private static void renderSphere(PoseStack poseStack,
                                      MultiBufferSource.BufferSource bufferSource,
                                      Identifier texture,
                                      float dx, float dy, float dz,
                                      float radius,
                                      float alpha,
                                      Vector3f sunDirLocal) {
        if (alpha <= 0f) return;
        VertexConsumer vc = bufferSource.getBuffer(RenderTypes.entityTranslucentEmissive(texture));

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        Matrix4f pose = poseStack.last().pose();

        int alphaInt = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
        int light = 0x00F000F0;
        int overlay = OverlayTexture.NO_OVERLAY;

        int triCount = PlanetSphereMesh.triangleCount();
        for (int t = 0; t < triCount; t++) {
            int ia = PlanetSphereMesh.triA(t);
            int ib = PlanetSphereMesh.triB(t);
            int ic = PlanetSphereMesh.triC(t);

            float ax = PlanetSphereMesh.vx(ia), ay = PlanetSphereMesh.vy(ia), az = PlanetSphereMesh.vz(ia);
            float bx = PlanetSphereMesh.vx(ib), by = PlanetSphereMesh.vy(ib), bz = PlanetSphereMesh.vz(ib);
            float cx = PlanetSphereMesh.vx(ic), cy = PlanetSphereMesh.vy(ic), cz = PlanetSphereMesh.vz(ic);

            float au = PlanetSphereMesh.uvU(ax, az), av = PlanetSphereMesh.uvV(ay);
            float bu = PlanetSphereMesh.uvU(bx, bz), bv = PlanetSphereMesh.uvV(by);
            float cu = PlanetSphereMesh.uvU(cx, cz), cv = PlanetSphereMesh.uvV(cy);

            // Detect equirectangular seam crossing: if the three U coords span
            // more than half the texture, one of the verts has wrapped. Shift
            // the wrapped one(s) by ±1 so the triangle stays in a consistent
            // U-band and the texture doesn't snap across the whole planet.
            float uMin = Math.min(au, Math.min(bu, cu));
            float uMax = Math.max(au, Math.max(bu, cu));
            if (uMax - uMin > 0.5f) {
                if (au < 0.5f) au += 1f;
                if (bu < 0.5f) bu += 1f;
                if (cu < 0.5f) cu += 1f;
            }

            int colorA = shadedColor(alphaInt, ax, ay, az, sunDirLocal);
            int colorB = shadedColor(alphaInt, bx, by, bz, sunDirLocal);
            int colorC = shadedColor(alphaInt, cx, cy, cz, sunDirLocal);

            // Emit triangle as a degenerate quad (vc's render type expects quads).
            putVertex(vc, pose, ax * radius, ay * radius, az * radius, colorA, au, av, light, overlay, ax, ay, az);
            putVertex(vc, pose, bx * radius, by * radius, bz * radius, colorB, bu, bv, light, overlay, bx, by, bz);
            putVertex(vc, pose, cx * radius, cy * radius, cz * radius, colorC, cu, cv, light, overlay, cx, cy, cz);
            // Degenerate fourth vertex = repeat C
            putVertex(vc, pose, cx * radius, cy * radius, cz * radius, colorC, cu, cv, light, overlay, cx, cy, cz);
        }

        poseStack.popPose();
    }

    /**
     * Lambert shading. Returns ARGB.
     * - sunDirLocal == null: emissive white (full brightness).
     * - else: ambient 0.35 + 0.65 * max(0, n·l), so the night side stays visible.
     */
    private static int shadedColor(int alphaInt, float nx, float ny, float nz, Vector3f sunDirLocal) {
        float brightness;
        if (sunDirLocal == null) {
            brightness = 1f;
        } else {
            float lambert = nx * sunDirLocal.x + ny * sunDirLocal.y + nz * sunDirLocal.z;
            if (lambert < 0f) lambert = 0f;
            brightness = 0.35f + 0.65f * lambert;
        }
        int rgb = Math.round(brightness * 255f);
        return (alphaInt << 24) | (rgb << 16) | (rgb << 8) | rgb;
    }

    @SuppressWarnings("unused") // Vector4f import retained for future Lambert tweaks
    private static void putVertex(VertexConsumer vc, Matrix4f pose,
                                   float x, float y, float z, int argb,
                                   float u, float v, int light, int overlay,
                                   float nx, float ny, float nz) {
        vc.addVertex(pose, x, y, z)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }
}
