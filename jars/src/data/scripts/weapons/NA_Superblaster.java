package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.entities.Ship;
import data.scripts.everyframe.Nightcross_Homing;
import data.scripts.everyframe.Nightcross_Trails;

import java.awt.*;

public class NA_Superblaster implements OnFireEffectPlugin {

    protected String ID = "na_superblaster_shot";
    protected float DMG_MULT = 0.5f;
    protected float MAX_FLUX = 0.7f;

    public static final Color CHARGE_COLOR = new Color(21, 238, 238);

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        Nightcross_Trails.createIfNeeded();

        DamageAPI dmg = projectile.getDamage();
        ShipAPI source = weapon.getShip();
        if (dmg != null && source != null) {
            float flevel = source.getFluxLevel();
            dmg.getModifier().modifyMult(ID, 1f + Math.min(flevel, MAX_FLUX)/MAX_FLUX*DMG_MULT);

            if (flevel > 0.1f) {
                // visual feedback
                Global.getCombatEngine().addSwirlyNebulaParticle(
                        weapon.getLocation(), Misc.ZERO,
                        100f, 0.1f, 0.05f, 0.1f + 0.5f * flevel,
                        0.5f + 1.0f * flevel,
                        CHARGE_COLOR, true
                );
            }
            if (flevel >= MAX_FLUX) {
                Global.getSoundPlayer().playSound(
                        "na_superblaster_impact", 1f, 0.8f, projectile.getLocation(), Misc.ZERO);

            }
        }
    }
}
