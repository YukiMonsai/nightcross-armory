package data.scripts.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.IntervalUtil;
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


	public static String ID = "na_fulldive";
	public static String TAG_IMMUNE_TO_PENALTY = "na_fulldive_immunity";
	public static String TAG_NOPENALTY_SET = "na_fulldive_nopenalty_set";
	public static String CAPTAIN_TAG = "na_fulldive_captain";


	static void log(final String message) {
		Global.getLogger(NA_ProjectGhost.class).info(message);
	}
	public String getDescriptionParam(int index, HullSize hullSize) {
		if (index == 0) return "" + (int) COMBAT_READINESS_PEN + "%";
		return null;
	}

	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		FleetMemberAPI member = stats.getFleetMember();
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
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
		if (!ship.isAlive()) return;
	}
}
