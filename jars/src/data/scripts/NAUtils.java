package data.scripts;



import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.DisintegratorEffect;
import com.fs.starfarer.combat.entities.Missile;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import org.lazywizard.lazylib.FastTrig;

import java.util.ArrayList;
import java.util.List;

public class NAUtils {
    public static Vector2f lengthdir(float radius, float ang) {
        return new Vector2f(
                radius * (float) FastTrig.cos((float) (Math.random() * Math.PI * 2f)),
                radius * (float) FastTrig.sin((float) (Math.random() * Math.PI * 2f)));
    }


    public static float getFriendlyWeight(ShipAPI ship, Vector2f point, float range) {

        float friendlyWeight = NAUtils.shipSize(ship);

        List<ShipAPI> friendsNearby = NAUtils.getFriendlyShipsWithinRange(ship, point, range, true);
        for (ShipAPI shp: friendsNearby) {
            friendlyWeight += NAUtils.shipSize(shp);
        }

        return friendlyWeight;
    }
    public static float getEnemyWeight(ShipAPI ship, Vector2f point, float range) {
        List<ShipAPI> enemiesNearby = NAUtils.getEnemyShipsWithinRange(ship, point, range, true);

        float enemyWeight = 0;
        for (ShipAPI shp: enemiesNearby) {
            if (!shp.getFluxTracker().isOverloadedOrVenting())
                enemyWeight += NAUtils.shipSize(shp);
        }

        return enemyWeight;
    }


    public static float getArmorAtPoint(ShipAPI target, Vector2f point) {
        ArmorGridAPI grid = target.getArmorGrid();
        int[] cell = grid.getCellAtLocation(point);
        if (cell == null) return 0f;

        int gridWidth = grid.getGrid().length;
        int gridHeight = grid.getGrid()[0].length;

        float totalArmor = 0f;
        float armorCells = 0f;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if ((i == 2 || i == -2) && (j == 2 || j == -2)) continue; // skip corners

                int cx = cell[0] + i;
                int cy = cell[1] + j;

                if (cx < 0 || cx >= gridWidth || cy < 0 || cy >= gridHeight) continue;


                float armorInCell = grid.getArmorFraction(cx, cy);
                totalArmor += armorInCell;
                armorCells++;
            }
        }

        float armorRating = target.getArmorGrid().getArmorRating();
        return armorRating*totalArmor / Math.max(1, armorCells);

        /*
        int[] cellloc = target.getArmorGrid().getCellAtLocation(point);
        float armorRating = target.getArmorGrid().getArmorRating();
        return armorRating * target.getArmorGrid().getArmorFraction(cellloc[0], cellloc[1]);*/
    }

    public static float shipSize(ShipAPI ship) {
        switch (ship.getHullSize()) {
            case CAPITAL_SHIP: return 4f;
            case CRUISER: return 3f;
            case DESTROYER: return 2f;
            case FRIGATE: return 1f;
            case FIGHTER: return 0.1f;
        }
        return 0;
    }

    // Does AOE damage
    public static void doDamage(Vector2f point, float radius, float dmg, float emp, DamageType damageType, boolean bypassShields, boolean softflux, Object source, boolean sound) {
        List<CombatEntityAPI> entities = getEntitiesWithinRange(point, radius);

        for (CombatEntityAPI e:entities) {
            if (e instanceof ShipAPI || e instanceof CombatAsteroidAPI) {
                Vector2f closest = MathUtils.getPointOnCircumference(
                        e.getLocation(), e.getCollisionRadius()*0.5f,
                        (float) (180f / Math.PI * Math.atan2(point.y - e.getLocation().y, point.x - e.getLocation().x))
                );
                Global.getCombatEngine().applyDamage(
                        e, closest,
                        dmg,
                        damageType,
                        emp,
                        bypassShields, softflux,
                        source, sound
                );
            }
        }
    }

    public static List<CombatEntityAPI> getEntitiesWithinRange(Vector2f location, float range) {
        List<CombatEntityAPI> entities = new ArrayList<>();

        for (CombatEntityAPI tmp : Global.getCombatEngine().getShips()) {
            if (MathUtils.isWithinRange(tmp, location, range)) {
                entities.add(tmp);
            }
        }

        // This also includes missiles
        for (CombatEntityAPI tmp : Global.getCombatEngine().getProjectiles()) {
            if (MathUtils.isWithinRange(tmp, location, range)) {
                entities.add(tmp);
            }
        }

        for (CombatEntityAPI tmp : Global.getCombatEngine().getAsteroids()) {
            if (MathUtils.isWithinRange(tmp, location, range)) {
                entities.add(tmp);
            }
        }

        return entities;
    }


    public static List<MissileAPI> getMissilesWithinRange(Vector2f location, float range) {
        List<MissileAPI> entities = new ArrayList<>();

        for (MissileAPI tmp : Global.getCombatEngine().getMissiles()) {
            if (MathUtils.isWithinRange(tmp, location, range)) {
                entities.add(tmp);
            }
        }

        return entities;
    }
    public static List<DamagingProjectileAPI> getProjectilesWithinRange(Vector2f location, float range) {
        List<DamagingProjectileAPI> entities = new ArrayList<>();

        // This also includes missiles
        for (DamagingProjectileAPI tmp : Global.getCombatEngine().getProjectiles()) {
            if (MathUtils.isWithinRange(tmp, location, range)) {
                entities.add(tmp);
            }
        }

        return entities;
    }





    public static List<ShipAPI> getShipsWithinRange(Vector2f location, float range) {
        List<ShipAPI> entities = new ArrayList<>();

        for (ShipAPI tmp : Global.getCombatEngine().getShips()) {
            if (MathUtils.isWithinRange(tmp, location, range)) {
                entities.add(tmp);
            }
        }


        return entities;
    }
    public static List<ShipAPI> getFriendlyShipsWithinRange(ShipAPI ship, Vector2f location, float range, boolean allowFighters) {
        List<ShipAPI> entities = new ArrayList<>();

        for (ShipAPI tmp : Global.getCombatEngine().getShips()) {
            if (MathUtils.isWithinRange(tmp, location, range)
                    && tmp.isAlive()
                    && (allowFighters || !tmp.isFighter())
                    && tmp.getOwner() == ship.getOwner()) {
                entities.add(tmp);
            }
        }


        return entities;
    }
    public static List<ShipAPI> getEnemyShipsWithinRange(ShipAPI ship, Vector2f location, float range, boolean allowFighters) {
        List<ShipAPI> entities = new ArrayList<>();

        for (ShipAPI tmp : Global.getCombatEngine().getShips()) {
            if (MathUtils.isWithinRange(tmp, location, range)
                    && tmp.isAlive()
                    && (allowFighters || !tmp.isFighter())
                    && tmp.getOwner() != ship.getOwner()) {
                entities.add(tmp);
            }
        }


        return entities;
    }
}
