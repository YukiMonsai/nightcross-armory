package data.scripts.combat.plugins;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import org.lwjgl.util.vector.Vector2f;

public class NA_CombatDamageListener implements HullDamageAboutToBeTakenListener {

    public NA_CombatDamageListener() {

    }

    @Override
    public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {
        if (ship != null && ship.getOwner() == 1) {
            if (NA_CombatPlugin.musicPhase == -1) NA_CombatPlugin.musicPhase++;
        }
        return false;
    }
}
