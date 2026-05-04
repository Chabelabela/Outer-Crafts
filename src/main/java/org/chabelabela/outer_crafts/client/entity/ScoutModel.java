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
 * Little Scout probe model.
 * <p>
 * Composed of:
 *   • Body     — 8×8×8 cube
 *   • Lens     — 4×4×2 camera eye on front face
 *   • Antennae — 2× 1×3×1 prongs on top
 * <p>
 * Matching atlas: 32×32 {@code textures/entity/scout.png}.
 */
public class ScoutModel extends EntityModel<EntityRenderState> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "scout"), "main");

    public ScoutModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // ── Body (8×8×8) ──────────────────────────────────────────────
        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4f, -4f, -4f, 8, 8, 8),
                PartPose.ZERO);

        // ── Camera lens (4×4×2), mounted on front (-Z) face ──────────
        root.addOrReplaceChild("lens",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2f, -2f, -6f, 4, 4, 2),
                PartPose.ZERO);

        // ── Antennae (1×3×1) × 2 on top ──────────────────────────────
        root.addOrReplaceChild("antennaL",
                CubeListBuilder.create()
                        .texOffs(12, 16)
                        .addBox(-3f, -7f, -1f, 1, 3, 1),
                PartPose.ZERO);
        root.addOrReplaceChild("antennaR",
                CubeListBuilder.create()
                        .texOffs(18, 16)
                        .addBox(2f, -7f, -1f, 1, 3, 1),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 32);
    }
}
