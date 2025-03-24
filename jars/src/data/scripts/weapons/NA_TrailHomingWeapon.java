package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.everyframe.Nightcross_Homing;
import data.scripts.everyframe.Nightcross_Trails;

public class NA_TrailHomingWeapon implements OnFireEffectPlugin {

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        Nightcross_Trails.createIfNeeded();
        Nightcross_Homing.createIfNeeded();
    }
}
