package data.scripts.campaign.enc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ghosts.BaseSensorGhostCreator;
import com.fs.starfarer.api.impl.campaign.ghosts.GhostFrequencies;
import com.fs.starfarer.api.impl.campaign.ghosts.SensorGhost;
import com.fs.starfarer.api.impl.campaign.ghosts.SensorGhostManager;

import java.util.ArrayList;
import java.util.List;

public class NA_StargazerGhostCreator extends BaseSensorGhostCreator {

	public static ArrayList<StargazerGhostEncounterGenerationParams> genList = new ArrayList<>();
	public static  boolean generateAngry = false;

	public boolean canSpawnWhilePlayerInAbyss() {return true;}


	public static class StargazerGhostEncounterGenerationParams {

		protected boolean angry = false;
		public StargazerGhostEncounterGenerationParams(boolean angry) {
			this.angry = angry;
		}
	}


	@Override
	public List<SensorGhost> createGhost(SensorGhostManager manager) {
		if (genList.isEmpty()) return null;
		if (!Global.getSector().getCurrentLocation().isHyperspace()) return null;
		
		CampaignFleetAPI pf = Global.getSector().getPlayerFleet();
		
		List<SensorGhost> result = new ArrayList<SensorGhost>();
		SensorGhost g = new NA_StargazerGhost(manager, pf, genList.get(0));
		if (!g.isCreationFailed()) {
			result.add(g);
			genList.remove(0);
		}
		return result;
	}

	@Override
	public float getFrequency(SensorGhostManager manager) {

		return genList.isEmpty() ? 0 : 10000f;
		//return 10000f;
	}
	
	@Override
	public float getTimeoutDaysOnSuccessfulCreate(SensorGhostManager manager) {
		return .1f + manager.getRandom().nextFloat() * .1f;
	}

	
}



