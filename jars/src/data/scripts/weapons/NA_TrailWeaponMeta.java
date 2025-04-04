package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.everyframe.Nightcross_Trails;

public class NA_TrailWeaponMeta implements OnFireEffectPlugin {

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        Nightcross_Trails.createIfNeeded();

        if (weapon != null && weapon.getSlot().getWeaponType() != WeaponAPI.WeaponType.BALLISTIC) {
            if (projectile.getCustomData() == null || !projectile.getCustomData().containsKey("na_energypowerup"))
                projectile.setCustomData("na_energypowerup", true);
        }
    }
}
