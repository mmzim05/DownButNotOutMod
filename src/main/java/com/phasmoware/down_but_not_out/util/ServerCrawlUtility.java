package com.phasmoware.down_but_not_out.util;

import com.phasmoware.down_but_not_out.config.ModConfig;
import com.phasmoware.down_but_not_out.mixin.ArmorStandEntityAccessor;
import com.phasmoware.down_but_not_out.mixinterface.ServerPlayerDuck;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.phys.Vec3;

public class ServerCrawlUtility {

    public static ArmorStand spawnInvisibleArmorStand(ServerPlayer player) {
        ArmorStand armorStand = new ArmorStand(EntityTypes.ARMOR_STAND, player.level());
        armorStand.setInvisible(true);
        armorStand.setNoGravity(true);
        armorStand.setPermanentlyInvulnerable(true);
        armorStand.setSilent(true);
        armorStand.setPos(player.position());
        armorStand.addTag(Constants.DOWNED_TAG);
        if (ModConfig.INSTANCE.SHOW_REVIVE_TAG_ABOVE_PLAYER) {
            armorStand.setCustomNameVisible(true);
            armorStand.setCustomName(Component.literal(Constants.CUSTOM_REVIVE_TAG_ABOVE_NAME));

        } else {
            armorStand.setCustomNameVisible(false);
        }
        AttributeInstance armorStandScale = armorStand.getAttribute(Attributes.SCALE);
        if (armorStandScale != null) {
            armorStandScale.setBaseValue(Constants.MIN_ENTITY_SCALE);
        }
        ArmorStandEntityAccessor accessor = (ArmorStandEntityAccessor) armorStand;
        accessor.invokeSetMarker(true);
        accessor.invokeSetSmall(true);
        player.level().addFreshEntity(armorStand);
        return armorStand;
    }

    public static Shulker spawnInvisibleShulker(ServerPlayer player) {
        Shulker shulkerEntity = new Shulker(EntityTypes.SHULKER, player.level());
        shulkerEntity.setPos(player.position());
        shulkerEntity.setPermanentlyInvulnerable(true);
        shulkerEntity.setNoGravity(true);
        shulkerEntity.setNoAi(true);
        shulkerEntity.setSilent(true);
        shulkerEntity.setCustomNameVisible(false);
        shulkerEntity.addTag(Constants.DOWNED_TAG);
        // for compatability with https://modrinth.com/datapack/random-mob-sizes-dp
        shulkerEntity.addTag(Constants.IGNORE_TAG);

        AttributeInstance attributeInstance = shulkerEntity.getAttribute(Attributes.SCALE);
        if (attributeInstance != null) {
            attributeInstance.setBaseValue(Constants.MIN_ENTITY_SCALE);
        }
        shulkerEntity.setInvisible(true);
        MobEffectInstance instance = new MobEffectInstance(MobEffects.INVISIBILITY, -1, 0, false, false);
        shulkerEntity.addEffect(instance);
        player.level().addFreshEntity(shulkerEntity);

        return shulkerEntity;
    }

    public static void setInvisibleShulkerArmorStandRider(ServerPlayerDuck player) {
        // spawns and saves invisible Shulker (passenger)
        player.dbno$setInvisibleShulkerEntity(spawnInvisibleShulker((ServerPlayer) player));
        // spawns and saves ArmorStand (vehicle)
        player.dbno$setInvisibleArmorStandEntity(spawnInvisibleArmorStand((ServerPlayer) player));
        // spawn and mount Shulker on top of ArmorStand
        player.dbno$getInvisibleShulkerEntity().startRiding(player.dbno$getInvisibleArmorStandEntity(), true, true);
    }

    public static void cleanUpForceCrawlEntities(ServerPlayerDuck player) {
        player.dbno$setDownedAnchor(null);
        if (player.dbno$getInvisibleShulkerEntity() != null) {
            player.dbno$getInvisibleShulkerEntity().remove(Entity.RemovalReason.DISCARDED);
            player.dbno$setInvisibleShulkerEntity(null);
        }
        if (player.dbno$getInvisibleArmorStandEntity() != null) {
            player.dbno$getInvisibleArmorStandEntity().remove(Entity.RemovalReason.DISCARDED);
            player.dbno$setInvisibleArmorStandEntity(null);
        }
    }

    public static void forceCrawlPose(ServerPlayerDuck serverPlayer) {
        ServerPlayer player = (ServerPlayer) serverPlayer;
        Vec3 headPosition = player.position().relative(Direction.UP, Constants.Y_OFFSET);
        if (serverPlayer.dbno$getInvisibleShulkerEntity() != null && serverPlayer.dbno$getInvisibleShulkerEntity().position().distanceToSqr(headPosition) > 0.01) {
            if (serverPlayer.dbno$getInvisibleArmorStandEntity() != null && !serverPlayer.dbno$getInvisibleArmorStandEntity().isRemoved()) {
                serverPlayer.dbno$getInvisibleArmorStandEntity().setPos(headPosition.x, headPosition.y, headPosition.z);
            } else if (serverPlayer.dbno$getInvisibleShulkerEntity() == null || serverPlayer.dbno$getInvisibleShulkerEntity().isRemoved()) {
                setInvisibleShulkerArmorStandRider(serverPlayer);
                TeamUtility.assignShulkerAndArmorStandToTempDownedTeam(player);
            }
        }
    }

    /**
     * Keeps a downed player where they went down. Bedrock (Geyser) clients can slide
     * while crawling even with no input, so pull the player back to the anchor once
     * horizontal drift exceeds the tolerance. Vertical movement is left alone.
     */
    public static void lockDownedPosition(ServerPlayerDuck serverPlayer) {
        ServerPlayer player = (ServerPlayer) serverPlayer;
        Vec3 anchor = serverPlayer.dbno$getDownedAnchor();
        if (anchor == null) {
            serverPlayer.dbno$setDownedAnchor(player.position());
            return;
        }
        double dx = player.getX() - anchor.x;
        double dz = player.getZ() - anchor.z;
        double distSq = dx * dx + dz * dz;
        if (distSq > Constants.DOWNED_LOCK_MAX_SNAP_DISTANCE * Constants.DOWNED_LOCK_MAX_SNAP_DISTANCE) {
            serverPlayer.dbno$setDownedAnchor(player.position());
            return;
        }
        player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
        if (distSq > Constants.DOWNED_LOCK_TOLERANCE * Constants.DOWNED_LOCK_TOLERANCE) {
            // absolute position, zero velocity, keep the client's current look direction
            player.connection.teleport(new PositionMoveRotation(new Vec3(anchor.x, player.getY(), anchor.z), Vec3.ZERO, 0.0F, 0.0F), Relative.ROTATION);
        }
    }
}
