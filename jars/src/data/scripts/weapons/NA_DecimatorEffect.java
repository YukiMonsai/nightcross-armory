package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

public class NA_DecimatorEffect implements BeamEffectPlugin {

	public final int SHOCK_COUNT = 4;
	public final float DAMAGE_AMOUNT = 250;



	private static class BeamEffect {
		private IntervalUtil shockInterval = new IntervalUtil(0.07f, 0.12f);
		private int shocksLeft = 0;
	}

	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		CombatEntityAPI target = beam.getDamageTarget();

		WeaponAPI weapon = beam.getWeapon();
		if (weapon == null) return;
		ShipAPI ship = weapon.getShip();

		String key = "na_decomatorbeam" + (weapon.getSlot() != null ?
				((int) weapon.getSlot().getLocation().x) + "," + ((int) weapon.getSlot().getLocation().y)
				: weapon.getId());



		if (target instanceof ShipAPI && beam.getBrightness() >= 1f
			&& ((ShipAPI) target).getHullSize() != ShipAPI.HullSize.FIGHTER) {

			boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getTo());

			if (!hitShield) {

				Vector2f point = beam.getRayEndPrevFrame();
				float dam = DAMAGE_AMOUNT;

				BeamEffect data = (BeamEffect) ship.getCustomData().get(key);

				if (data != null && data.shocksLeft > 0) {
					if (data.shockInterval.intervalElapsed()) {
						data.shockInterval.randomize();

						data.shocksLeft -= 1;
						engine.spawnEmpArcPierceShields(
								beam.getSource(), point, beam.getDamageTarget(), beam.getDamageTarget(),
								DamageType.ENERGY,
								dam, // damage
								dam, // emp
								155f, // max range
								"tachyon_lance_emp_impact",
								beam.getWidth() + 20f,
								beam.getFringeColor(),
								beam.getCoreColor()
						);

					} else {
						data.shockInterval.advance(amount);
					}
				} else if (data != null){
					ship.removeCustomData(key);
				}
			}



		} else if (beam.getBrightness() < 1) {
			BeamEffect data = (BeamEffect) ship.getCustomData().get(key);
			if (data == null) {
				data = new BeamEffect();
				ship.setCustomData(key, data);
			}


			if (data.shocksLeft < SHOCK_COUNT) {
				data.shockInterval = new IntervalUtil(0.1f, 0.2f);
			}
			data.shocksLeft = SHOCK_COUNT;
		}
//			Global.getSoundPlayer().playLoop("system_emp_emitter_loop", 
//											 beam.getDamageTarget(), 1.5f, beam.getBrightness() * 0.5f,
//											 beam.getTo(), new Vector2f());
	}
}
