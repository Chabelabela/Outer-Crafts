package org.chabelabela.outer_crafts.mixin.client;

import net.minecraft.world.entity.player.Player;
import org.chabelabela.outer_crafts.ship.ShipEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents the player from dismounting the ship via Shift key.
 * <p>
 * In the ship, Shift is used for downward thrust, not dismounting.
 * The player must right-click on the ship while inside to exit (future),
 * or use a dedicated exit key.
 * <p>
 * The method {@code wantsToStopRiding()} lives on Player (not LocalPlayer).
 */
@Mixin(Player.class)
public abstract class ShipDismountMixin {

    @Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
    private void outerCrafts$preventShipDismount(CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (self.getVehicle() instanceof ShipEntity) {
            cir.setReturnValue(false);
        }
    }
}
