package org.chabelabela.outer_crafts.client.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import org.chabelabela.outer_crafts.OuterCrafts;

/**
 * Outer Wilds ship entity model.
 * <p>
 * Composed of:
 *   • Main hull       — 24×16×32 box at origin
 *   • Cockpit window  — 8×6×8 box on top-front
 *   • Landing legs    — 4× 1×6×1 posts at hull corners
 *   • Thrusters       — 2× 3×3×5 nozzles at rear
 * <p>
 * Matching atlas: 128×128 {@code textures/entity/ship.png}.
 */
public class ShipModel extends EntityModel<EntityRenderState> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "ship"), "main");

    public ShipModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── Main hull (24w × 16h × 32d) ───────────────────────────────
        root.addOrReplaceChild("hull",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-12f, -8f, -16f, 24, 16, 32),
                PartPose.ZERO);

        // ── Cockpit window (8w × 6h × 8d) ─────────────────────────────
        root.addOrReplaceChild("cockpit",
                CubeListBuilder.create()
                        .texOffs(0, 50)
                        .addBox(-4f, -14f, -10f, 8, 6, 8),
                PartPose.ZERO);

        // ── Landing legs (1w × 6h × 1d) × 4 ──────────────────────────
        root.addOrReplaceChild("legFL",
                CubeListBuilder.create().texOffs(0, 70)
                        .addBox(-11f, 8f, -15f, 1, 6, 1),
                PartPose.ZERO);
        root.addOrReplaceChild("legFR",
                CubeListBuilder.create().texOffs(0, 70)
                        .addBox(10f, 8f, -15f, 1, 6, 1),
                PartPose.ZERO);
        root.addOrReplaceChild("legBL",
                CubeListBuilder.create().texOffs(0, 70)
                        .addBox(-11f, 8f, 14f, 1, 6, 1),
                PartPose.ZERO);
        root.addOrReplaceChild("legBR",
                CubeListBuilder.create().texOffs(0, 70)
                        .addBox(10f, 8f, 14f, 1, 6, 1),
                PartPose.ZERO);

        // ── Thrusters (3w × 3h × 5d) × 2 ─────────────────────────────
        root.addOrReplaceChild("thrusterL",
                CubeListBuilder.create().texOffs(8, 70)
                        .addBox(-10f, -1.5f, 16f, 3, 3, 5),
                PartPose.ZERO);
        root.addOrReplaceChild("thrusterR",
                CubeListBuilder.create().texOffs(26, 70)
                        .addBox(7f, -1.5f, 16f, 3, 3, 5),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 128);
    }
}
