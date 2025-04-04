package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;


public class NA_Metahelium implements EveryFrameWeaponEffectPlugin {


    private boolean inited = false;
    private NA_MetaheliumRangeModifier rnglistener;
    private NA_MetaheliumDmgBoost dmglistener;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (!inited) {
            inited = true;
            if (rnglistener == null && weapon.getSlot().getWeaponType() != WeaponAPI.WeaponType.ENERGY) {
                rnglistener = new NA_MetaheliumRangeModifier(weapon);
                weapon.getShip().addListener(rnglistener);
            }
            if (dmglistener == null && weapon.getSlot().getWeaponType() == WeaponAPI.WeaponType.BALLISTIC) {
                dmglistener = new NA_MetaheliumDmgBoost(weapon);
                weapon.getShip().addListener(dmglistener);
            }
        }
    }




    public static class NA_MetaheliumRangeModifier implements WeaponBaseRangeModifier {
        public float mult;
        public float effectLevel;
        public WeaponAPI weapon;
        public NA_MetaheliumRangeModifier(WeaponAPI weapon) {
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
                return 100f;
            return 0f;
        }
    }


    public class NA_MetaheliumDmgBoost implements DamageDealtModifier {
        public WeaponAPI weapon;
        public float mult;
        public float effectLevel;

        public NA_MetaheliumDmgBoost(WeaponAPI weapon) {
           this.weapon = weapon;
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (param != null && target instanceof ShipAPI && ((ShipAPI) target).isAlive()) {

                if (param instanceof DamagingProjectileAPI) {
                    DamagingProjectileAPI projectile = (DamagingProjectileAPI) param;
                    if (projectile.getWeapon() == this.weapon) {
                        damage.setSoftFlux(true);
                    }
                }
            }
            return null;
        }
    }
}
