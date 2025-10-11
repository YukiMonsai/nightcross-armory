package data.scripts.campaign.rulecmd.nca;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Map;

public class NA_PlayerFleetHasStargazerHulls extends BaseCommandPlugin {

	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params, Map<String, MemoryAPI> memoryMap) {
		if (dialog == null) return false;
		
		for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {

			ShipHullSpecAPI spec = member.getHullSpec();
			if (spec.hasTag("stargazer_hull")) return true;
			if (member.getHullSpec().getBaseHull() != null) spec = member.getHullSpec().getBaseHull();
			if (spec.hasTag("stargazer_hull")) return true;
		}
		
		return false;
	}

}
