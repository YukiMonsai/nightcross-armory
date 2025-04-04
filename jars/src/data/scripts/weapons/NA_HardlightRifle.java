package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;


public class NA_HardlightRifle implements EveryFrameWeaponEffectPlugin {
    private final float BOOST_PER_HIT = 50f;
    private final float BOOST_MAX = 200f;
    public float currentBoost = 0f;
    private final float TIMER = 2.0f;
    private IntervalUtil lastHitTimer = new IntervalUtil(TIMER, TIMER);
    private ShipAPI lastTarget;

    private boolean inited = false;
    private NA_HardlightRifleBoost dmglistener;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (!inited) {
            inited = true;
            if (dmglistener == null) {
                dmglistener = new NA_HardlightRifleBoost(weapon, this);
                weapon.getShip().addListener(dmglistener);
            }
        }
        if (lastHitTimer.intervalElapsed()) {
            currentBoost = 0;
            lastHitTimer = new IntervalUtil(TIMER, TIMER);
        } else {
            lastHitTimer.advance(amount);
        }
    }

    public void doHit(ShipAPI target) {
        if (!target.isFighter()) {
            if (target != lastTarget) currentBoost -= BOOST_PER_HIT;
            else currentBoost += BOOST_PER_HIT;
            lastTarget = target;

            if (currentBoost > BOOST_MAX) currentBoost = BOOST_MAX;
            else if (currentBoost < 0) currentBoost = 0;

            lastHitTimer = new IntervalUtil(TIMER, TIMER);
        }

    }


    public class NA_HardlightRifleBoost implements DamageDealtModifier {
        public WeaponAPI weapon;
        public NA_HardlightRifle script;
        public float mult;
        public float effectLevel;

        public NA_HardlightRifleBoost(WeaponAPI weapon, NA_HardlightRifle script) {
            this.weapon = weapon;
            this.script = script;
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (param != null && !shieldHit && target instanceof ShipAPI && ((ShipAPI) target).isAlive()) {
                if (param instanceof DamagingProjectileAPI) {
                    DamagingProjectileAPI projectile = (DamagingProjectileAPI) param;
                    if (projectile.getWeapon() == this.weapon) {
                        float bonus = script.currentBoost;

                        damage.getModifier().modifyPercent("na_hardlightriflesucchit", bonus);
                        Global.getSoundPlayer().playSound("hardlightrifle_impact", 1f, 0.15f + 0.2f * (bonus)/100f, point, Misc.ZERO);

                        script.doHit((ShipAPI) target);
                    }
                }
            }
            return null;
        }
    }
}
