package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.fleet.FleetMember;
import data.scripts.NAUtils;
import org.dark.shaders.distortion.WaveDistortion;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class NA_ProjectGhost extends BaseHullMod {


	public static float COMBAT_READINESS_PEN = -30f;
	public static float REPAIR_PEN = 50f;
	public static float EMP_PEN = 25f;
	public static float ACCELERATION = 50f;
	public static float AUTOFIRE = 1f;
	public static float RANGE = 10f;
	public static float RELOAD_TIME = 25f;
	public static float MAG_CAP = 50f;
	public static float MISSILE_CAP = 50f;


	public static String ID = "na_fulldive";
	public static String TAG_IMMUNE_TO_PENALTY = "na_fulldive_immunity";
	public static String TAG_NOPENALTY_SET = "na_fulldive_nopenalty_set";
	public static String CAPTAIN_TAG = "na_fulldive_captain";

	public static Color NIGHTCROSS_BLUE = new Color(30, 110, 225);

	static void log(final String message) {
		Global.getLogger(NA_ProjectGhost.class).info(message);
	}
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) COMBAT_READINESS_PEN + "%";
		return null;
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		FleetMemberAPI member = stats.getFleetMember();
		stats.getCombatEngineRepairTimeMult().modifyPercent(ID, REPAIR_PEN);
		stats.getCombatWeaponRepairTimeMult().modifyPercent(ID, REPAIR_PEN);
		stats.getAcceleration().modifyPercent(ID, ACCELERATION);
		stats.getDeceleration().modifyPercent(ID, ACCELERATION);
		stats.getTurnAcceleration().modifyPercent(ID, ACCELERATION);
		stats.getAutofireAimAccuracy().modifyFlat(ID, AUTOFIRE);
		stats.getBallisticWeaponRangeBonus().modifyPercent(ID, RANGE);
		stats.getEnergyWeaponRangeBonus().modifyPercent(ID, RANGE);
		stats.getBallisticAmmoBonus().modifyPercent(ID, MAG_CAP);
		stats.getBallisticAmmoRegenMult().modifyPercent(ID, -RELOAD_TIME);
		stats.getMissileAmmoBonus().modifyPercent(ID, MISSILE_CAP);
		if (member == null || !member.getVariant().hasTag(NA_ProjectGhost.TAG_IMMUNE_TO_PENALTY)) {
			stats.getMaxCombatReadiness().modifyPercent(ID, COMBAT_READINESS_PEN);
			if (member != null && (member.getCaptain() == null || !member.getCaptain().hasTag(NA_ProjectGhost.CAPTAIN_TAG))) {
				if (member.getVariant().hasTag(NA_ProjectGhost.TAG_IMMUNE_TO_PENALTY)) {
					member.getVariant().removeTag(NA_ProjectGhost.TAG_IMMUNE_TO_PENALTY);
				}
			}

			if (member != null && member.getVariant().hasTag(NA_ProjectGhost.TAG_NOPENALTY_SET)) {
				member.getVariant().removeTag(NA_ProjectGhost.TAG_NOPENALTY_SET);
				member.getVariant().removeTag(Tags.TAG_AUTOMATED_NO_PENALTY);
			}
		}

	}

	public static boolean isSModded(MutableShipStatsAPI stats, String id) {
		if (stats == null || stats.getVariant() == null) return false;
		return stats.getVariant().getSMods().contains(id) ||
				stats.getVariant().getSModdedBuiltIns().contains(id);
	}

	public static boolean isSModded(ShipAPI ship, String id) {
		if (ship == null || ship.getVariant() == null) return false;
		return ship.getVariant().getSMods().contains(id) ||
				ship.getVariant().getSModdedBuiltIns().contains(id);
	}

	@Override
	public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}
	public float getTooltipWidth() {
		return super.getTooltipWidth();
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 3f;
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		Color bad = Misc.getNegativeHighlightColor();
		Color t = Misc.getTextColor();
		Color g = Misc.getGrayColor();

		tooltip.addPara("This ship is fitted to an unconventional automation standard, different from Tri-Tachyon droneships."
				+ "\nNotably, the ship possesses life support facilities for a single human pilot and an interface for an AI core of nonstandard make.", NIGHTCROSS_BLUE, opad);

		tooltip.addSectionHeading("Campaign", Alignment.MID, opad);
		tooltip.addPara("It is possible to assign a standard AI core to captain this ship, but the outdated protocols and nonstandard interface results in a %s Combat Readiness penalty.", opad, h,
				"" + (int) Math.round(- COMBAT_READINESS_PEN) + "%");

		tooltip.addSectionHeading("Combat", Alignment.MID, opad);
		tooltip.addPara("The vessel's primitive automation architecture results in different combat performance compared to vessels of standard make."
						+ "\n- Weapon and engine repair takes %s longer."
						+ "\n- Turning and acceleration improved by %s due to inertial dampening suite."
						+ "\n- Direct targeting uplink increases autofire accuracy by %s and ballistic/energy weapon range by %s."
						+ "\n- Rugged, low-maintenance autoloaders reload ballistic weapons %s more slowly but increases capacity by %s."
						+ "\n- Space savings increase missile weapon capacity by %s.",
				opad, h,
				"" + (int) Math.round((REPAIR_PEN)) + "%",
				"" + (int) Math.round((ACCELERATION)) + "%",
				"" + (int) Math.round((AUTOFIRE) * 100f) + "%",
				"" + (int) Math.round((RANGE)) + "%",
				"" + (int) Math.round((RELOAD_TIME) * 1f) + "%",
				"" + (int) Math.round((MAG_CAP)) + "%",
				"" + (int) Math.round((MISSILE_CAP)) + "%");


	}

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
		if (!ship.isAlive()) return;
	}
}
