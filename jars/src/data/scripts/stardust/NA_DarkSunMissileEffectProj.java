package data.scripts.stardust;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;

public class NA_DarkSunMissileEffectProj implements OnFireEffectPlugin {
    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        String key = "" + projectile.hashCode();
        Object targetDataObj = Global.getCombatEngine().getCustomData().get(key);
        if (targetDataObj == null) {
            Global.getCombatEngine().getCustomData().put(key, new NA_DarkSunListener(projectile, key, projectile.getSource()));
        }
    }
}