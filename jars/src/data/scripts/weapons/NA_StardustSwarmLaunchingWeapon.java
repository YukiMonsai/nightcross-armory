package data.scripts.weapons;

import com.fs.starfarer.api.combat.WeaponAPI;


/**
 * A weapon that can not be manually controlled and launches swarms automatically.
 *
 */
public interface NA_StardustSwarmLaunchingWeapon {
    int getPreferredNumFragmentsToFire(WeaponAPI weapon);
}
