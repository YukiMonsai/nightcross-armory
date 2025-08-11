package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.magiclib.plugins.MagicTrailPlugin;

import java.awt.*;


public class NA_SystemLight implements EveryFrameWeaponEffectPlugin {


    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship != null) {
            if (ship.getSystem() != null) {
                float effLevel = ship.getSystem().getEffectLevel();
                float effectiveEffLevel = 0.02f;
                float effLevelWindow = 0.33f;

                if (weapon.getSpec().getWeaponId().endsWith("_med")) effectiveEffLevel += effLevelWindow;
                else if (weapon.getSpec().getWeaponId().endsWith("_hi")) effectiveEffLevel += 2f*effLevelWindow;

                boolean on = effLevel >= effectiveEffLevel && effLevel <= effLevelWindow + effectiveEffLevel;

                if (on) {
                    weapon.getAnimation().setFrame(0);
                } else {
                    weapon.getAnimation().setFrame(1);
                }

            }

        }
    }
}
