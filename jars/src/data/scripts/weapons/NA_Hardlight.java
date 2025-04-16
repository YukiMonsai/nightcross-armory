package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.util.Misc;
import data.scripts.NAUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;


public class NA_Hardlight implements EveryFrameWeaponEffectPlugin {

    // multiplier to armor for flat reduction
    public final float ARMOR_SCALE = 7f;
    // determines the size of the 'sweet spot' along the ship's centerline
    public final float CENTER_SCALE = 1.33f;




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
            if (dmglistener == null) {
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
                return 300f;
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
                        if (shieldHit) {
                            damage.setSoftFlux(true);
                        } else if (target instanceof ShipAPI) {
                            float armor = Math.max(0f, NAUtils.getArmorAtPoint((ShipAPI) target, point) - 0.15f * ((ShipAPI) target).getArmorGrid().getArmorRating());
                            float orig = Math.max(1f, projectile.getWeapon().getDamage().getDamage());
                            float dmg = orig;
                            dmg = Math.max(100f, dmg - ARMOR_SCALE*armor);
                            damage.setDamage(50f);

                            // needs to pass thru center of the ship
                            Vector2f center = target.getLocation();
                            Vector2f closestPass = MathUtils.getNearestPointOnLine(center, point,
                                    MathUtils.getPointOnCircumference(point, 1000f, projectile.getFacing()));

                            float dist = MathUtils.getDistance(closestPass, center);

                            float distScale = Math.max(0f, Math.min(1f, CENTER_SCALE - CENTER_SCALE*dist/target.getCollisionRadius()));


                            dmg = Math.max(100f, dmg*distScale);

                            target.setHitpoints(Math.max(0, target.getHitpoints() - dmg));


                            if (dmg > 100) {
                                Global.getSoundPlayer().playSound("atomicdriver_impact", 1f, 0.15f + 0.25f * dmg/orig, point, Misc.ZERO);
                                Global.getCombatEngine().addSwirlyNebulaParticle(
                                        point, Misc.ZERO,
                                        80f + 120f* dmg/orig, 1.5f, 0.35f, 0.5f,
                                        3.5f,
                                        new Color(200, 250, 255), true
                                );
                            }
                        }
                    }
                }
            }
            return null;
        }
    }
}
