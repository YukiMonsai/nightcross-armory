package data.scripts;



import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
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
}
