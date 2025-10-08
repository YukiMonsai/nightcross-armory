package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class NA_PyrowispAutofireAI implements AutofireAIPlugin {

    private final WeaponAPI weapon;
    private boolean shouldFire;
    private ShipAPI ship;
    private CombatEntityAPI target = null;

    public NA_PyrowispAutofireAI(WeaponAPI weapon) {
        this.weapon = weapon;
    }

    @Override
    public void advance(float amount) {
        if (Global.getCombatEngine() == null) return;
        if (weapon.getShip() == null) return;
        this.ship = weapon.getShip();
        float firingRange = weapon.getRange();

        boolean player = (ship == Global.getCombatEngine().getPlayerShip()) && (ship.getShipAI() == null);
        ShipAPI bestTarget;
        if (!player && (ship.getAIFlags() != null) && ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET) instanceof ShipAPI) {
            bestTarget = (ShipAPI) ship.getAIFlags().getCustom(ShipwideAIFlags.AIFlags.MANEUVER_TARGET);
        } else {
            bestTarget = ship.getShipTarget();
        }
        if ((bestTarget != null) && (bestTarget.isFighter() || bestTarget.isDrone())) {
            bestTarget = null;
        }
        if ((bestTarget != null) && (bestTarget.getCollisionClass() != CollisionClass.SHIP) && (bestTarget.getCollisionClass() != CollisionClass.FIGHTER)) {
            bestTarget = null;
        }
        if ((bestTarget != null) && ((bestTarget.getOwner() == ship.getOwner()) || (bestTarget.getOwner() == 100))) {
            bestTarget = null;
        }
        if ((bestTarget != null) && !bestTarget.isAlive()) {
            bestTarget = null;
        }

        if (bestTarget != null) {
            target = bestTarget;
            shouldFire = MathUtils.isWithinRange(target.getLocation(), weapon.getFirePoint(0), firingRange + target.getCollisionRadius());
            return;
        }

        List<ShipAPI> enemies = AIUtils.getEnemiesOnMap(ship);
        for (ShipAPI enemy : enemies) {
            if (MathUtils.isWithinRange(enemy.getLocation(), weapon.getFirePoint(0), firingRange + enemy.getCollisionRadius())) {
                shouldFire = true;
                target = enemy;
                return;
            }
        }
        shouldFire = false;
    }

    @Override
    public boolean shouldFire() {

        return shouldFire;
    }

    @Override
    public void forceOff() {
        shouldFire = false;
    }

    @Override
    public Vector2f getTarget() {
        if (target == null) {
            return null;
        } else {
            return target.getLocation();
        }
    }

    @Override
    public MissileAPI getTargetMissile() {
        return null;
    }

    @Override
    public ShipAPI getTargetShip() {
        if (target instanceof ShipAPI) {
            return (ShipAPI) target;
        }
        return null;
    }

    @Override
    public WeaponAPI getWeapon() {
        return this.weapon;
    }
}
