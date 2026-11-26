package com.phasmoware.down_but_not_out.timer;

import com.phasmoware.down_but_not_out.config.ModConfig;
import com.phasmoware.down_but_not_out.handler.MessageHandler;
import com.phasmoware.down_but_not_out.StateManager;
import com.phasmoware.down_but_not_out.util.Constants;
import com.phasmoware.down_but_not_out.util.ReviveUtility;
import com.phasmoware.down_but_not_out.util.TeamUtility;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.phasmoware.down_but_not_out.util.DownedUtility.isDowned;

public class ReviveTimer {
    @NotNull
    private final ServerPlayer downed;
    private boolean interactionActive = false;
    private long reviveProgressTicks;
    private long invalidTicks;
    private long ticksSinceInteract;
    private int lastReportedStep = -1;
    @Nullable
    private ServerPlayer reviver;

    public ReviveTimer(@Nullable ServerPlayer reviver, @NotNull ServerPlayer downed) {
        this.reviver = reviver;
        this.downed = downed;
    }


    public void tick() {
        if (reviver != null && ++ticksSinceInteract > getHoldWindowTicks()) {
            // no interact packet for too long: use was released, so start over
            ReviveUtility.notifyHoldReleased(reviver, downed);
            reviveProgressTicks = 0;
            lastReportedStep = -1;
            stopReviveInteraction();
            return;
        }
        if (ReviveUtility.checkValidReviver(reviver, downed, false)) {
            invalidTicks = 0;
            TeamUtility.updateRevivingTeamColor(downed);
            interactionActive = true;
            reviveProgressTicks++;
            updateReviveProgress();
            if (this.reviveProgressTicks >= ModConfig.INSTANCE.REVIVE_DURATION_TICKS) {
                reviveProgressTicks = 0;
                StateManager.onReviveComplete(downed, reviver);
                stopReviveInteraction();
            }
        } else if (reviver != null && isDowned(downed) && invalidTicks < ModConfig.INSTANCE.REVIVE_GRACE_TICKS) {
            // Bedrock (Geyser) sends sparse interact packets and aim jitters, so
            // tolerate a short run of invalid ticks: keep the reviver, pause progress
            invalidTicks++;
        } else if (interactionActive || reviver != null) {
            ReviveUtility.checkValidReviver(reviver, downed, true);
            stopReviveInteraction();
        } else {
            stopReviveInteraction();
            decrementReviveProgress();
        }
    }


    // Java re-sends the interact packet every ~4 ticks while use is held, Bedrock (Geyser) at a similar rate
    private static long getHoldWindowTicks() {
        return Math.max(ModConfig.INSTANCE.REVIVE_GRACE_TICKS, Constants.MIN_REVIVE_HOLD_WINDOW_TICKS);
    }

    private void updateReviveProgress() {
        // Bedrock redraws the overlay on every message, so only send when the shown value changes
        int step = getCurrentProgressPercent() / Constants.REVIVE_PROGRESS_MESSAGE_STEP_PERCENT;
        if (step == lastReportedStep) {
            return;
        }
        lastReportedStep = step;
        Component msgToReviver = Component.literal("Hold to Revive: " + getCurrentProgressPercent() + "%").withStyle(ChatFormatting.AQUA);
        MessageHandler.sendThrottledUpdateMessage(msgToReviver, reviver);
        Component msgToDowned = Component.literal("Reviving: " + getCurrentProgressPercent() + "%").withStyle(ChatFormatting.AQUA);
        MessageHandler.sendThrottledUpdateMessage(msgToDowned, downed);
    }

    private void decrementReviveProgress() {
        if (reviveProgressTicks > 0) {
            reviveProgressTicks--;
        }
    }

    public void stopReviveInteraction() {
        this.invalidTicks = 0;
        this.ticksSinceInteract = 0;
        if (this.reviver != null) {
            this.reviver = null;
        }
        if (this.interactionActive) {
            this.interactionActive = false;
        }
    }

    public void continueRevive(ServerPlayer reviver) {
        this.invalidTicks = 0;
        this.ticksSinceInteract = 0;
        if (this.reviver != reviver) {
            this.reviver = reviver;
        }
        if (!interactionActive) {
            interactionActive = true;
        }
    }

    public boolean isInteractionActive() {
        return interactionActive;
    }

    public int getCurrentProgressPercent() {
        if (ModConfig.INSTANCE.REVIVE_DURATION_TICKS <= 1) {
            return 100;
        }
        return (int) ((((float) this.reviveProgressTicks / (float) ModConfig.INSTANCE.REVIVE_DURATION_TICKS)) * 100);
    }
}
