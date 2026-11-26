package com.phasmoware.down_but_not_out.mixin;

import com.mojang.authlib.GameProfile;
import com.phasmoware.down_but_not_out.config.ModConfig;
import com.phasmoware.down_but_not_out.mixinterface.ServerPlayerDuck;
import com.phasmoware.down_but_not_out.timer.BleedOutTimer;
import com.phasmoware.down_but_not_out.timer.ReviveTimer;
import com.phasmoware.down_but_not_out.util.ServerCrawlUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.phasmoware.down_but_not_out.util.DownedUtility.isDowned;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerEntityMixin extends Player implements ServerPlayerDuck {

    @Unique
    private final BleedOutTimer bleedOutTimer = new BleedOutTimer((ServerPlayer) (Object) this);

    @Unique
    private final ReviveTimer reviveTimer = new ReviveTimer(null, (ServerPlayer) (Object) this);
    @Unique
    public long ticksSinceLastUpdate;
    @Unique
    private Shulker invisibleShulkerEntity;
    @Unique
    private ArmorStand invisibleArmorStandEntity;
    @Unique
    private Component lastUpdateText;
    @Unique
    private Vec3 downedAnchor;

    public ServerPlayerEntityMixin(Level world, GameProfile profile) {
        super(world, profile);
    }

    @Shadow
    public abstract ServerLevel level();

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void injectTickPlayer(CallbackInfo ci) {
        ticksSinceLastUpdate++;
        reviveTimer.tick();
        bleedOutTimer.tick();
        if (isDowned(this)) {
            ServerCrawlUtility.forceCrawlPose(this);
            if (ModConfig.INSTANCE.DOWNED_LOCK_POSITION) {
                ServerCrawlUtility.lockDownedPosition(this);
            }
        }
    }

    // downed players must not be shoved around (keeps revive aim stable)
    @Override
    public boolean isPushable() {
        return !isDowned(this) && super.isPushable();
    }

    @Override
    public BleedOutTimer dbno$getBleedOutTimer() {
        return this.bleedOutTimer;
    }

    @Override
    public ReviveTimer dbno$getReviveTimer() {
        return this.reviveTimer;
    }

    @Override
    public Shulker dbno$getInvisibleShulkerEntity() {
        return this.invisibleShulkerEntity;
    }

    @Override
    public void dbno$setInvisibleShulkerEntity(Shulker shulkerEntity) {
        this.invisibleShulkerEntity = shulkerEntity;
    }

    @Override
    public ArmorStand dbno$getInvisibleArmorStandEntity() {
        return this.invisibleArmorStandEntity;
    }

    @Override
    public void dbno$setInvisibleArmorStandEntity(ArmorStand armorStandEntity) {
        this.invisibleArmorStandEntity = armorStandEntity;
    }

    @Override
    public Component dbno$getLastUpdateText() {
        return this.lastUpdateText;
    }

    @Override
    public void dbno$setLastUpdateText(Component lastText) {
        this.lastUpdateText = lastText;
    }

    @Override
    public Vec3 dbno$getDownedAnchor() {
        return this.downedAnchor;
    }

    @Override
    public void dbno$setDownedAnchor(Vec3 anchor) {
        this.downedAnchor = anchor;
    }

    @Override
    public long dbno$getTicksSinceLastUpdate() {
        return this.ticksSinceLastUpdate;
    }

    @Override
    public void dbno$setTicksSinceLastUpdate(long ticksSinceLastUpdate) {
        this.ticksSinceLastUpdate = ticksSinceLastUpdate;
    }
}
