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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.phasmoware.down_but_not_out.util.DownedUtility.isDowned;

public class ReviveTimer {
    // per reviver input tracking; progress itself is shared
    private static final class ReviverState {
        long ticksSinceInteract;
        long invalidTicks;
        boolean slowed;
    }

    @NotNull
    private final ServerPlayer downed;
    private final Map<ServerPlayer, ReviverState> revivers = new LinkedHashMap<>();
    private boolean interactionActive = false;
    // shared progress from 0.0 to 1.0; revivers joining or leaving only change the speed, never the progress
    private double progress;
    private int lastReportedStep = -1;

    public ReviveTimer(@Nullable ServerPlayer reviver, @NotNull ServerPlayer downed) {
        this.downed = downed;
        if (reviver != null) {
            continueRevive(reviver);
        }
    }


    public void tick() {
        if (revivers.isEmpty()) {
            interactionActive = false;
            decrementReviveProgress();
            return;
        }
        int counting = 0;
        boolean released = false;
        Iterator<Map.Entry<ServerPlayer, ReviverState>> it = revivers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ServerPlayer, ReviverState> entry = it.next();
            ServerPlayer reviver = entry.getKey();
            ReviverState state = entry.getValue();
            if (++state.ticksSinceInteract > getHoldWindowTicks()) {
                // no interact packet for too long: use was released
                ReviveUtility.notifyHoldReleased(reviver, downed);
                released = true;
                removeSlowness(reviver, state);
                it.remove();
            } else if (ReviveUtility.checkValidReviver(reviver, downed, false)) {
                state.invalidTicks = 0;
                applySlowness(reviver, state);
                counting++;
            } else if (isDowned(downed) && state.invalidTicks < ModConfig.INSTANCE.REVIVE_GRACE_TICKS) {
                // Bedrock (Geyser) sends sparse interact packets and aim jitters, so
                // tolerate a short run of invalid ticks: keep the reviver, pause their contribution
                state.invalidTicks++;
            } else {
                ReviveUtility.checkValidReviver(reviver, downed, true);
                removeSlowness(reviver, state);
                it.remove();
            }
        }
        if (revivers.isEmpty()) {
            interactionActive = false;
            if (released) {
                progress = 0;
                lastReportedStep = -1;
            }
            return;
        }
        interactionActive = true;
        if (counting == 0) {
            return;
        }
        TeamUtility.updateRevivingTeamColor(downed);
        progress += 1.0 / getEffectiveDurationTicks(counting);
        if (progress >= 1.0) {
            List<ServerPlayer> completed = new ArrayList<>(revivers.keySet());
            progress = 0;
            lastReportedStep = -1;
            stopReviveInteraction();
            StateManager.onReviveComplete(downed, completed);
        } else {
            updateReviveProgress();
        }
    }

    // each extra simultaneous reviver cuts a percentage of the base duration, down to a floor
    private static double getEffectiveDurationTicks(int revivers) {
        double reduction = ModConfig.INSTANCE.MULTI_REVIVER_REDUCTION_PERCENT * (revivers - 1);
        double percent = Math.max(100.0 - reduction, ModConfig.INSTANCE.MULTI_REVIVER_MIN_DURATION_PERCENT);
        return Math.max(1.0, ModConfig.INSTANCE.REVIVE_DURATION_TICKS * percent / 100.0);
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
        for (ServerPlayer reviver : revivers.keySet()) {
            MessageHandler.sendThrottledUpdateMessage(msgToReviver, reviver);
        }
        Component msgToDowned = Component.literal("Reviving: " + getCurrentProgressPercent() + "%").withStyle(ChatFormatting.AQUA);
        MessageHandler.sendThrottledUpdateMessage(msgToDowned, downed);
    }

    private void decrementReviveProgress() {
        if (progress > 0) {
            progress = Math.max(0, progress - 1.0 / Math.max(1, ModConfig.INSTANCE.REVIVE_DURATION_TICKS));
        }
    }

    // Slowness is short (refreshed while holding) so it can never leak if the reviver disconnects or dies
    private static void applySlowness(ServerPlayer reviver, ReviverState state) {
        if (!ModConfig.INSTANCE.REVIVER_SLOWNESS_ENABLED) {
            return;
        }
        if (!state.slowed) {
            // don't clobber slowness the player has for another reason
            if (reviver.hasEffect(MobEffects.SLOWNESS)) {
                return;
            }
            state.slowed = true;
        } else if (reviver.tickCount % Constants.REVIVER_SLOWNESS_REFRESH_TICKS != 0) {
            return;
        }
        reviver.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Constants.REVIVER_SLOWNESS_DURATION_TICKS,
                ModConfig.INSTANCE.REVIVER_SLOWNESS_AMPLIFIER, false, false, false));
    }

    private static void removeSlowness(ServerPlayer reviver, ReviverState state) {
        if (state.slowed) {
            state.slowed = false;
            reviver.removeEffect(MobEffects.SLOWNESS);
        }
    }

    public void stopReviveInteraction() {
        revivers.forEach((reviver, state) -> removeSlowness(reviver, state));
        revivers.clear();
        interactionActive = false;
    }

    public void continueRevive(ServerPlayer reviver) {
        ReviverState state = revivers.computeIfAbsent(reviver, r -> new ReviverState());
        state.ticksSinceInteract = 0;
        state.invalidTicks = 0;
        interactionActive = true;
    }

    public boolean isInteractionActive() {
        return interactionActive;
    }

    public int getCurrentProgressPercent() {
        if (ModConfig.INSTANCE.REVIVE_DURATION_TICKS <= 1) {
            return 100;
        }
        return (int) (progress * 100);
    }
}
