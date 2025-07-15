package data.scripts.weapons;

import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;

public class NA_RKKVEffect implements OnFireEffectPlugin, DamageDealtModifier {

    public static float DAMAGE_MULT_PER_SPEED = 1f/1300f;
    public static float DAMAGE_MAX_MULT = 1.25f;
    public static float DAMAGE_MIN = 0.1f;

    protected String weaponId = null;

    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        ShipAPI ship = weapon.getShip();
        if (!ship.hasListenerOfClass(NA_RKKVEffect.class)) {
            ship.addListener(this);
            weaponId = weapon.getId();
        }
    }

    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (param instanceof MissileAPI) {
            MissileAPI m = (MissileAPI) param;
            if (m.getWeaponSpec() != null && m.getWeaponSpec().getWeaponId().equals(weaponId)) {
                float base = damage.getBaseDamage();
                float relvel = m.getMoveSpeed();
                if (target.getVelocity() != null) {
                    relvel = MathUtils.getDistance(m.getVelocity(), target.getVelocity());
                }
                damage.setDamage(base * Math.min(DAMAGE_MAX_MULT, Math.max(DAMAGE_MIN, 0.1f + DAMAGE_MULT_PER_SPEED * relvel)));
                return "na_rkkv";
            }
        }
        return null;
    }


}
