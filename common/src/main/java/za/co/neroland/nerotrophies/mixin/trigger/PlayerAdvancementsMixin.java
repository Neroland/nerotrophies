package za.co.neroland.nerotrophies.mixin.trigger;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import za.co.neroland.nerotrophies.trigger.AdvancementQueue;

/**
 * The live {@code advancement} trigger. {@code PlayerAdvancements#award(AdvancementHolder, String)} is vanilla's single
 * choke point for granting a criterion on every loader and on every MC line this mod builds for (the signature is the
 * same from 1.21.1 to 26.3), and Fabric has no advancement event. When a criterion is newly granted, the advancement
 * is now complete and some loaded trophy names it, the completion is queued; the trigger tick awards it after the
 * vanilla call has returned, never inside it.
 */
@Mixin(PlayerAdvancements.class)
abstract class PlayerAdvancementsMixin {

    @Shadow
    private ServerPlayer player;

    @Inject(method = "award", at = @At("RETURN"))
    private void nerotrophies$queueCompletedAdvancement(AdvancementHolder holder, String criterion,
            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || player == null || holder == null || !AdvancementQueue.wants(holder.id())) {
            return;
        }
        if (((PlayerAdvancements) (Object) this).getOrStartProgress(holder).isDone()) {
            AdvancementQueue.offer(player.getUUID(), holder.id());
        }
    }
}
