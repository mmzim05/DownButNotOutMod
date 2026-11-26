package com.phasmoware.down_but_not_out.mixinterface;

import com.phasmoware.down_but_not_out.timer.BleedOutTimer;
import com.phasmoware.down_but_not_out.timer.ReviveTimer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.phys.Vec3;

public interface ServerPlayerDuck {

    BleedOutTimer dbno$getBleedOutTimer();

    ReviveTimer dbno$getReviveTimer();

    Shulker dbno$getInvisibleShulkerEntity();

    void dbno$setInvisibleShulkerEntity(Shulker shulkerEntity);

    ArmorStand dbno$getInvisibleArmorStandEntity();

    void dbno$setInvisibleArmorStandEntity(ArmorStand armorStandEntity);

    Component dbno$getLastUpdateText();

    void dbno$setLastUpdateText(Component lastUpdateText);

    Vec3 dbno$getDownedAnchor();

    void dbno$setDownedAnchor(Vec3 anchor);

    long dbno$getTicksSinceLastUpdate();

    void dbno$setTicksSinceLastUpdate(long ticksSinceLastUpdate);
}
