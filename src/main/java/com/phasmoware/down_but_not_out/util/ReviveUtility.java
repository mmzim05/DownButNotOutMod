package com.phasmoware.down_but_not_out.util;

import com.phasmoware.down_but_not_out.config.ModConfig;
import com.phasmoware.down_but_not_out.handler.MessageHandler;
import com.phasmoware.down_but_not_out.mixinterface.ServerPlayerDuck;
import com.phasmoware.down_but_not_out.timer.BleedOutTimer;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static com.phasmoware.down_but_not_out.util.DownedUtility.isDowned;

public class ReviveUtility {
    public static void applyRevivedPenalty(ServerPlayerDuck player) {
        BleedOutTimer timer = player.dbno$getBleedOutTimer();
        if (ModConfig.INSTANCE.BLEEDING_OUT_DURATION_TICKS > 0 && timer.getTicksUntilBleedOut() > 1) {
            player.dbno$getBleedOutTimer().setReviveCooldownTicks(ModConfig.INSTANCE.REVIVE_PENALTY_COOLDOWN_TICKS);
            timer.setTicksUntilBleedOut(timer.getTicksUntilBleedOut() / ModConfig.INSTANCE.REVIVE_PENALTY_MULTIPLIER);
        }
    }

    public static boolean isLookingAtPlayer(ServerPlayer viewer, ServerPlayer target) {
        if (viewer == target) {
            return false;
        }
        Vec3 eyePos = viewer.getEyePosition(Constants.MIN_TICK_PROGRESS);
        Vec3 lookDir = viewer.getViewVector(Constants.MIN_TICK_PROGRESS);
        Vec3 rayEnd = eyePos.add(lookDir.scale(viewer.entityInteractionRange()));
        // expand by a bit for stability (the downed hitbox is small and Bedrock aim jitters)
        AABB targetBox = target.getBoundingBox().inflate(ModConfig.INSTANCE.REVIVE_AIM_TOLERANCE);
        Optional<Vec3> hit = targetBox.clip(eyePos, rayEnd);
        return hit.isPresent();
    }

    /**
     * @param notify whether to tell the players why the revive is invalid
     */
    public static boolean checkValidReviver(ServerPlayer reviver, ServerPlayer downed, boolean notify) {
        if (reviver == null || downed == null) {
            return false;
        }
        if (reviver == downed) {
            return false;
        }
        if (isDowned(reviver)) {
            return false;
        }
        if (!isDowned(downed)) {
            return false;
        }
        if (ModConfig.INSTANCE.RESTRICT_REVIVE_TO_TEAMMATES_OR_TEAMLESS) {
            if ((reviver.getTeam() != null && !TeamUtility.isOnTempDownedTeam(downed)) && (!reviver.getTeam().equals(downed.getTeam()))) {
                if (notify) {
                    MessageHandler.onPlayerRevivingFromDifferentTeam(reviver, downed);
                }
                return false;
            }
        }
        if ((!reviver.getMainHandItem().isEmpty() && ModConfig.INSTANCE.REVIVING_REQUIRES_EMPTY_HAND)) {
            if (notify) {
                MessageHandler.onPlayerRevivingWithoutEmptyHand(reviver, downed);
            }
            return false;
        }
        double range = reviver.entityInteractionRange();
        if (reviver.distanceToSqr(downed) > range * range) {
            if (notify) {
                MessageHandler.onPlayerRevivingTooFarAway(reviver, downed);
            }
            return false;
        }
        if (!ReviveUtility.isLookingAtPlayer(reviver, downed)) {
            if (notify) {
                MessageHandler.onPlayerLookingAwayWhileReviving(reviver, downed);
            }
            return false;
        }
        return true;
    }
}
