package data.scripts.campaign.enc;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ghosts.SensorGhost;
import com.fs.starfarer.api.impl.campaign.ghosts.SensorGhostCreator;
import com.fs.starfarer.api.impl.campaign.ghosts.SensorGhostManager;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.TimeoutTracker;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.world.nightcross.NA_StargazerFleets;
import data.scripts.world.nightcross.NA_StargazerGen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class NA_StargazerSpawner implements EveryFrameScript {


	float timeoutRemaining = 5;
	float timePerCheck = 5;


	@Override
	public boolean isDone() {
		return false;
	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}

	public void advance(float amount) {
		if (amount == 0) return;
		if (Global.getSector() == null) return;
		if (Global.getSector().getClock() == null) return;

		timeoutRemaining -= amount * Global.getSector().getClock().convertToDays(amount);

		if (timeoutRemaining <= 0) {
			timeoutRemaining = timePerCheck;
			var numGazers = NA_StargazerGen.countWanderers(Global.getSector());

			if (numGazers < NA_StargazerGen.MIN_STARGAZER_WANDERERS) {
				NA_StargazerGen.createWanderers(Global.getSector(), 1);
			}
		}

	}


}
