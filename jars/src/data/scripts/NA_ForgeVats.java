package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.magiclib.util.MagicFakeBeam;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NA_ForgeVats extends BaseShipSystemScript {
    public static final Object KEY_JITTER = new Object();
    public static final Color JITTER_COLOR = new Color(100,165,255,155);


    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }


        if (effectLevel > 0) {
            float jitterLevel = effectLevel;

            boolean firstTime = false;
            final String fightersKey = ship.getId() + "_na_forgevats";
            List<WeaponAPI> weapons = null;
            if (!Global.getCombatEngine().getCustomData().containsKey(fightersKey)) {
                weapons = getWeapons(ship);
                Global.getCombatEngine().getCustomData().put(fightersKey, weapons);
                firstTime = true;
            } else {
                weapons = (List<WeaponAPI>) Global.getCombatEngine().getCustomData().get(fightersKey);
            }
            if (weapons == null) { // shouldn't be possible, but still
                weapons = new ArrayList<WeaponAPI>();
            }

            for (WeaponAPI weapon : weapons) {

                float maxRangeBonus = 10f;
                if (weapon.getSpec().getSize() == WeaponAPI.WeaponSize.LARGE) maxRangeBonus = 50;
                else if (weapon.getSpec().getSize() == WeaponAPI.WeaponSize.MEDIUM) maxRangeBonus = 25;
                float jitterRangeBonus = 5f + jitterLevel * maxRangeBonus;

                if (firstTime) {
                    Global.getSoundPlayer().playSound("system_forgevats", 1f, 0.5f, weapon.getLocation(), ship.getVelocity());
                    ship.setJitter(KEY_JITTER, JITTER_COLOR, jitterLevel, 10, 0f, jitterRangeBonus);
                }

                if (effectLevel == 1) {
                    int max = weapon.getAmmoTracker().getMaxAmmo();
                    float percent = weapon.getSpec().getMountType() == WeaponAPI.WeaponType.SYNERGY ? 0.5f : 0.2f;
                    float reload = (float) Math.ceil(max*percent);
                    weapon.getAmmoTracker().setAmmo(
                            (int) Math.min(max, Math.max(0, weapon.getAmmoTracker().getAmmo() + reload))
                    );

                }
            }
        }
    }

    // Get all synergy weapons and large missiles
    public static List<WeaponAPI> getWeapons(ShipAPI carrier) {
        List<WeaponAPI> result = new ArrayList<WeaponAPI>();

        for (WeaponAPI weapon : carrier.getAllWeapons()) {
            if (weapon.getSpec().getMountType() == WeaponAPI.WeaponType.SYNERGY
            || weapon.getSpec().getMountType() == WeaponAPI.WeaponType.MISSILE) {
                if (weapon.getSpec().getMountType() == WeaponAPI.WeaponType.SYNERGY
                || weapon.getSpec().getSize() == WeaponAPI.WeaponSize.LARGE) {
                    result.add(weapon);
                }
            }
        }

        return result;
    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        final String fightersKey = ship.getId() + "_na_forgevats";
        Global.getCombatEngine().getCustomData().remove(fightersKey);

//		for (ShipAPI fighter : getFighters(ship)) {
//			fighter.setPhased(false);
//			fighter.setCopyLocation(null, 1f, fighter.getFacing());
//		}
    }


    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }


}
















