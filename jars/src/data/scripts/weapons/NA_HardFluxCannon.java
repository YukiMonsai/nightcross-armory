package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class NA_HardFluxCannon implements EveryFrameWeaponEffectPlugin {

    public final float PCNT_PER_CHARGE = 0.082f;
    public final float MAX_CHARGE = 8;

    public int last_charges = 0;
    public static final String CHARGE_SOUND = "na_hardflux_charge";
    public static final Color CHARGE_COLOR = new Color(175, 50, 225);

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship != null) {
            float hardflux = ship.getFluxTracker().getHardFlux()/Math.max(1, ship.getFluxTracker().getMaxFlux());
            int charges = (int) Math.max(1, Math.min(MAX_CHARGE, hardflux/PCNT_PER_CHARGE));
            int currentCharges = weapon.getAmmoTracker().getAmmo();
            if (charges >= currentCharges) {
                weapon.getAmmoTracker().setMaxAmmo(charges);
            }

            if (currentCharges > last_charges && currentCharges > 1) {
                // do a fancy effect
                if (currentCharges == MAX_CHARGE) {
                    Global.getSoundPlayer().playSound(
                            CHARGE_SOUND, 0.75f, 1.3f, weapon.getLocation(), Misc.ZERO);
                    Global.getCombatEngine().addSwirlyNebulaParticle(
                            weapon.getLocation(), weapon.getShip() != null ? weapon.getShip().getVelocity() : Misc.ZERO,
                            100f, 0.1f, 0.05f, 0.2f,
                            1.5f,
                            CHARGE_COLOR, true
                    );
                } else {
                    Global.getSoundPlayer().playSound(
                            CHARGE_SOUND, 0.99f, 0.8f, weapon.getLocation(), Misc.ZERO);
                    Global.getCombatEngine().addSwirlyNebulaParticle(
                            weapon.getLocation(), weapon.getShip() != null ? weapon.getShip().getVelocity() : Misc.ZERO,
                            45f, 0.1f, 0.05f, 0.2f,
                            1.5f,
                            CHARGE_COLOR, true
                    );
                }

            }

            last_charges = currentCharges;

        }
    }
}
