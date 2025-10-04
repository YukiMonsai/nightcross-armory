package data.scripts.weapons;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

import data.scripts.stardust.NA_StargazerStardust;

public interface NA_StardustWeapon {
    public int getNumFragmentsToFire();


    default public void showNoFragmentSwarmWarning(WeaponAPI w, ShipAPI ship) {
        boolean playerShip = Global.getCurrentState() == GameState.COMBAT &&
                Global.getCombatEngine() != null && Global.getCombatEngine().getPlayerShip() == ship;

        if (playerShip) {
            NA_StargazerStardust swarm = NA_StargazerStardust.getSwarmFor(ship);
            if (swarm == null && ship.getFullTimeDeployed() > 0.1f) {
                Global.getCombatEngine().maintainStatusForPlayerShip(w.getSpec(),
                        Global.getSettings().getSpriteName("ui", "icon_tactical_fragment_swarm"),
                        w.getDisplayName(),
                        "REQ: STARDUST",
                        true);
            }
        }
    }
}
