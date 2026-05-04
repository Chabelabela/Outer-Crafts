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
import org.chabelabela.outer_crafts.client.entity.ScoutModel;
import org.chabelabela.outer_crafts.entity.ScoutEntity;

/**
 * Renders the Little Scout probe as a small textured cube.
 */
public class ScoutRenderer extends EntityRenderer<ScoutEntity, EntityRenderState> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(OuterCrafts.MODID, "textures/entity/scout.png");

    private final ScoutModel model;

    public ScoutRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ScoutModel(context.bakeLayer(ScoutModel.LAYER));
        this.shadowRadius = 0.2f;
    }

    @Override
    public void submit(EntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.scale(0.5f, 0.5f, 0.5f);
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
