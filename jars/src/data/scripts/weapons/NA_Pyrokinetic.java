package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import data.scripts.NA_FastCaps;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;


public class NA_Pyrokinetic implements EveryFrameWeaponEffectPlugin {


    private boolean inited = false;
    private NA_PyrokineticRangeModifier rnglistener;
    private NA_PyrokineticDmgBoost dmglistener;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (!inited) {
            inited = true;
            if (rnglistener == null && weapon.getSlot().getWeaponType() != WeaponAPI.WeaponType.ENERGY) {
                rnglistener = new NA_PyrokineticRangeModifier(weapon);
                weapon.getShip().addListener(rnglistener);
            }
            if (dmglistener == null && weapon.getSlot().getWeaponType() != WeaponAPI.WeaponType.BALLISTIC) {
                dmglistener = new NA_PyrokineticDmgBoost(weapon);
                weapon.getShip().addListener(dmglistener);
            }
        }
    }




    public static class NA_PyrokineticRangeModifier implements WeaponBaseRangeModifier {
        public float mult;
        public float effectLevel;
        public WeaponAPI weapon;
        public NA_PyrokineticRangeModifier(WeaponAPI weapon) {
            this.weapon = weapon;
        }

        public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
            return 0;
        }
        public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
            //if (weaponEligible(weapon)) return 1f + mult * effectLevel;
            return 1f;
        }
        public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
            if (weapon == this.weapon)
                return 200f;
            return 0f;
        }
    }


    public class NA_PyrokineticDmgBoost implements DamageDealtModifier {
        public WeaponAPI weapon;
        public float mult;
        public float effectLevel;

        public NA_PyrokineticDmgBoost(WeaponAPI weapon) {
           this.weapon = weapon;
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (param != null && !shieldHit && target instanceof ShipAPI && ((ShipAPI) target).isAlive()) {

                if (param instanceof DamagingProjectileAPI) {
                    DamagingProjectileAPI projectile = (DamagingProjectileAPI) param;
                    if (projectile.getWeapon() == this.weapon) {
                        if (MathUtils.getRandomNumberInRange(0, 100) < 20) {
                            float emp = projectile.getDamageAmount();
                            float dam = 1.5f * projectile.getDamageAmount();

                            Global.getCombatEngine().spawnEmpArc(projectile.getSource(), point, target, target,
                                    DamageType.ENERGY,
                                    dam,
                                    emp, // emp
                                    100000f, // max range
                                    "tachyon_lance_emp_impact",
                                    20f, // thickness
                                    new Color(25, 100, 155, 255),
                                    new Color(255, 255, 255, 255)
                            );
                        }
                    }
                }
            }
            return null;
        }
    }
}
