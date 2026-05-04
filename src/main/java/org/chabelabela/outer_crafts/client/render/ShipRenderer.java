package org.chabelabela.outer_crafts.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.chabelabela.outer_crafts.OuterCrafts;
import org.chabelabela.outer_crafts.client.entity.ShipModel;
import org.chabelabela.outer_crafts.ship.ShipEntity;

/**
 * Renders the Outer Wilds ship as a textured box model.
 */
public class ShipRenderer extends EntityRenderer<ShipEntity, EntityRenderState> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "textures/entity/ship.png");

    private final ShipModel model;

    public ShipRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ShipModel(context.bakeLayer(ShipModel.LAYER));
        this.shadowRadius = 1.5f;
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(0.15f, 0.15f, 0.15f);
        collector.submitModel(this.model, state, poseStack, TEXTURE,
                state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
