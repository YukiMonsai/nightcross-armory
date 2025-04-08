package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

public class NA_DecimatorEffect implements BeamEffectPlugin {

	private IntervalUtil shockInterval = new IntervalUtil(0.1f, 0.2f);
	private int shocksLeft = 0;
	public final int SHOCK_COUNT = 4;
	public final float DAMAGE_AMOUNT = 250;

	
	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		CombatEntityAPI target = beam.getDamageTarget();
		if (target instanceof ShipAPI && beam.getBrightness() >= 1f
			&& ((ShipAPI) target).getHullSize() != ShipAPI.HullSize.FIGHTER) {

			boolean hitShield = target.getShield() != null && target.getShield().isWithinArc(beam.getTo());

			if (!hitShield) {

				Vector2f point = beam.getRayEndPrevFrame();
				float dam = DAMAGE_AMOUNT;



				if (shocksLeft > 0) {
					if (shockInterval.intervalElapsed()) {
						shockInterval.randomize();

						shocksLeft -= 1;
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
						shockInterval.advance(amount);
					}
				}
			}



		} else if (beam.getBrightness() < 1) {
			if (shocksLeft < SHOCK_COUNT) {
				shockInterval = new IntervalUtil(0.1f, 0.2f);
			}
			shocksLeft = SHOCK_COUNT;
		}
//			Global.getSoundPlayer().playLoop("system_emp_emitter_loop", 
//											 beam.getDamageTarget(), 1.5f, beam.getBrightness() * 0.5f,
//											 beam.getTo(), new Vector2f());
	}
}
