package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BreachOnHitEffect;
import com.fs.starfarer.api.impl.combat.DisintegratorEffect;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicAnim;

import java.awt.*;

public class NA_Pyrolance implements BeamEffectPlugin {

	public static final float DMG_LARGE = 500f;
	public static final float DMG_MED = 150f;
	public static final float DMG_SMALL = 75f;
	public static final int COLOR_R = 225;
	public static final int COLOR_G = 75;
	public static final int COLOR_B = 15;


	private static class BeamEffect {
		private IntervalUtil shockInterval = new IntervalUtil(0.1f, 0.2f);
	}

	public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
		CombatEntityAPI target = beam.getDamageTarget();

		WeaponAPI weapon = beam.getWeapon();
		if (weapon == null) return;
		ShipAPI ship = weapon.getShip();

		float sizemult = 0.6f;
		if (weapon.getSize() == WeaponAPI.WeaponSize.LARGE) sizemult = 1.5f;
		else
		if (weapon.getSize() == WeaponAPI.WeaponSize.MEDIUM) sizemult = 1.0f;

		String key = "na_pyrolance_" + (weapon.getSlot() != null ?
				((int) weapon.getSlot().getLocation().x) + "," + ((int) weapon.getSlot().getLocation().y)
				: weapon.getId());

		BeamEffect data = (BeamEffect) ship.getCustomData().get(key);
		if (data == null) {
			data = new BeamEffect();
			ship.setCustomData(key, data);
		}

		if (beam.getBrightness() >= 1f) {
			//beam.setCoreColor(new Color(COLOR_R, COLOR_G, COLOR_B));


			if (amount > 0 && target != null) {
				if (target instanceof ShipAPI && (((ShipAPI) target).getShield() == null || ((ShipAPI) target).getShield().isOff() || ((ShipAPI) target).getShield().isWithinArc(beam.getTo()))) {
					// do the damage
					float dmg = DMG_SMALL;
					if (weapon.getId().equals("na_pyrolance_drill")) dmg = DMG_LARGE;
					else
					if (weapon.getSize() == WeaponAPI.WeaponSize.LARGE) dmg = DMG_LARGE;
					else
					if (weapon.getSize() == WeaponAPI.WeaponSize.MEDIUM) dmg = DMG_MED;
					dealArmorDamage(beam, (ShipAPI) target, beam.getTo(),
							dmg*amount);
				}

				Global.getCombatEngine().addSwirlyNebulaParticle(
						beam.getTo(), Misc.ZERO, 25f, 1.5f, amount, amount*2f, amount*4f,
						new Color(238, 161, 19), true
				);
			}


		}

		if (beam.getBrightness() > 0) {
			//beam.setCoreColor(new Color(COLOR_R, COLOR_G, COLOR_B, (int) (100 * beam.getBrightness())));

			// vfx
			if (data.shockInterval.intervalElapsed()) {
				data.shockInterval.randomize();

				EmpArcEntityAPI.EmpArcParams params = new EmpArcEntityAPI.EmpArcParams();
				params.maxZigZagMult = 0.25f;
				params.flickerRateMult = 0.25f;
				params.glowSizeMult *= sizemult;

				if (weapon.getCooldownRemaining() == 0 && beam.getBrightness() < 1f) {
					EmpArcEntityAPI arc = engine.spawnEmpArcVisual(
							MathUtils.getRandomPointInCone(beam.getFrom(),
									250f, weapon.getCurrAngle()-45f,
									weapon.getCurrAngle()+45f),
							null,
							beam.getFrom(), beam.getSource(),
							beam.getWidth()*0.5f * sizemult,
							new Color(225, 125, 0),
							beam.getCoreColor(), params
					);
					arc.setSingleFlickerMode(true);
					arc.setRenderGlowAtStart(false);
				} else {
					EmpArcEntityAPI arc = engine.spawnEmpArcVisual(
							beam.getFrom(), null,
							MathUtils.getRandomPointInCone(beam.getFrom(),
									MathUtils.getRandomNumberInRange(Math.max(50f, beam.getLength()/5f), Math.max(300f, beam.getLength()/4f)),
									weapon.getCurrAngle()-10f,
									weapon.getCurrAngle()+10f),
							null,
							beam.getWidth()*0.5f * sizemult,
							new Color(225, 125, 0),
							beam.getCoreColor(), params
					);
					arc.setSingleFlickerMode(true);
					arc.setRenderGlowAtEnd(false);
				}
			} else {
				data.shockInterval.advance(amount);
			}
		}
	}


	public static void dealArmorDamage(BeamAPI beam, ShipAPI target, Vector2f point, float armorDamage) {
		CombatEngineAPI engine = Global.getCombatEngine();

		ArmorGridAPI grid = target.getArmorGrid();
		int[] cell = grid.getCellAtLocation(point);
		if (cell == null) return;

		int gridWidth = grid.getGrid().length;
		int gridHeight = grid.getGrid()[0].length;

		float damageTypeMult = DisintegratorEffect.getDamageTypeMult(beam.getSource(), target);

		float damageDealt = 0f;
		for (int i = -2; i <= 2; i++) {
			for (int j = -2; j <= 2; j++) {
				if ((i == 2 || i == -2) && (j == 2 || j == -2)) continue; // skip corners

				int cx = cell[0] + i;
				int cy = cell[1] + j;

				if (cx < 0 || cx >= gridWidth || cy < 0 || cy >= gridHeight) continue;

				float damMult = 1/30f;
				if (i == 0 && j == 0) {
					damMult = 1/15f;
				} else if (i <= 1 && i >= -1 && j <= 1 && j >= -1) { // S hits
					damMult = 1/15f;
				} else { // T hits
					damMult = 1/30f;
				}

				float armorInCell = grid.getArmorValue(cx, cy);
				float damage = armorDamage * damMult * damageTypeMult;
				damage = Math.min(damage, armorInCell);
				if (damage <= 0) continue;

				target.getArmorGrid().setArmorValue(cx, cy, Math.max(0, armorInCell - damage));
				damageDealt += damage;
			}
		}

		if (damageDealt > 0) {
			//if (Misc.shouldShowDamageFloaty(beam.getSource(), target)) {
				engine.addFloatingDamageText(point, damageDealt, Misc.FLOATY_ARMOR_DAMAGE_COLOR, target, beam.getSource());
			//}
			target.syncWithArmorGridState();
		}
	}
}
