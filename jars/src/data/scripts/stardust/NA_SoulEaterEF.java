package data.scripts.stardust;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.magiclib.plugins.MagicTrailPlugin;

import java.awt.*;

public class NA_SoulEaterEF implements EveryFrameWeaponEffectPlugin {

	public static final Color COLOR_SHOCK = new Color(198, 2, 48);
	public static final Color COLOR_SHOCK_CORE = new Color(255, 244, 246);

	private static class BeamEffect {
		private IntervalUtil shockInterval = new IntervalUtil(0.1f, 0.2f);
		public float hash = (float) (Math.random());
		boolean fired = false;
	}

	public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
		if (weapon == null) return;
		ShipAPI ship = weapon.getShip();

		float sizemult = 0.6f;
		if (weapon.getSize() == WeaponAPI.WeaponSize.LARGE) sizemult = 2.0f;
		else
		if (weapon.getSize() == WeaponAPI.WeaponSize.MEDIUM) sizemult = 1.5f;

		String key = "naai_hil_ef_" + (weapon.getSlot() != null ?
				((int) weapon.getSlot().getLocation().x) + "," + ((int) weapon.getSlot().getLocation().y)
				: weapon.getId());

		BeamEffect data = (BeamEffect) ship.getCustomData().get(key);
		if (data == null) {
			data = new BeamEffect();
			ship.setCustomData(key, data);
		}



		if (weapon.isFiring() && weapon.getChargeLevel() < 1f) {
			if (!data.fired) {
				Global.getSoundPlayer().playSound("na_bassdestroyer_charge", 1f, 1, weapon.getFirePoint(0), Misc.ZERO);

			}
			data.fired = true;

			// vfx
			if (data.shockInterval.intervalElapsed()) {
				data.shockInterval.randomize();

				EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
				params.maxZigZagMult = 0.25f;
				params.flickerRateMult = 0.25f;
				params.glowSizeMult *= 0.5f*sizemult;

				EmpArcEntityAPI arc = engine.spawnEmpArcVisual(
						MathUtils.getRandomPointInCone(weapon.getFirePoint(0),
								250f, weapon.getCurrAngle()-180f,
								weapon.getCurrAngle()+180f),
						null,
						weapon.getFirePoint(0), weapon.getShip(),
						20*0.5f * sizemult,
						COLOR_SHOCK,
						COLOR_SHOCK_CORE, params
				);
				arc.setSingleFlickerMode(true);
				arc.setRenderGlowAtStart(false);

			} else {
				data.shockInterval.advance(amount);
			}
		} else if (weapon.getChargeLevel() <= 0) {

			data.fired = false;
		}
	}



}
