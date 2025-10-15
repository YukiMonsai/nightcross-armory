package data.scripts.campaign.enc;

import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl;
import com.fs.starfarer.api.impl.campaign.fleets.AutoDespawnScript;
import com.fs.starfarer.api.impl.campaign.ghosts.*;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetQuality;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.FleetSize;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerNum;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers.OfficerQuality;
import com.fs.starfarer.api.impl.campaign.world.MoteParticleScript;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.ids.NightcrossID;
import data.scripts.world.NightcrossTags;
import data.scripts.world.nightcross.NA_StargazerFleets;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

public class NA_StargazerGhost extends BaseSensorGhost implements Script {

	protected Random random;
	protected NA_StargazerGhostCreator.StargazerGhostEncounterGenerationParams params;

	public NA_StargazerGhost(SensorGhostManager manager, CampaignFleetAPI fleet, NA_StargazerGhostCreator.StargazerGhostEncounterGenerationParams params) {
		super(manager, 0);
		this.params = params;
		
		random = Misc.getRandom(manager.getRandom().nextLong(), 5);

		int maxBurn = 7 + random.nextInt(3);;
		initEntity(genLargeSensorProfile(), genLargeRadius());
		
		if (!placeNearPlayer()) {
			setCreationFailed();
			return;
		}

		//entity.addScript(new NA_StargazerNebulaScript(entity, 0.1f));
		//entity.addTag(NightcrossTags.NEBULA_GHOST);
		setDespawnRange(-700f);
		
		float speed = Misc.getSpeedForBurnLevel(maxBurn);
		float accelMult = speed / Misc.getSpeedForBurnLevel(20f);
		if (accelMult < 0.1f) accelMult = 0.1f;
		setAccelMult(1f/ accelMult);
		
		addBehavior(new GBIntercept(fleet, 5f + random.nextFloat() * 2f, maxBurn, 450f, true));
		addBehavior(new GBCircle(fleet, 0.7f + random.nextFloat() * 0.5f, maxBurn / 2, 300f, random.nextBoolean() ? 1f : -1f));
		addBehavior(new GBStayInPlace(0.1f));
		addInterrupt(new GBIRunScript(0f, this, true));
	}


	public static WeightedRandomPicker<String> seekingTypes = new WeightedRandomPicker<>();
	static {
		seekingTypes.add("seeking", 10f);
		seekingTypes.add("scavenging", 10f);
		seekingTypes.add("hunting", 10f);
		seekingTypes.add("//I SEE YOU", 1f);
	}
	public static WeightedRandomPicker<String> driftingTypes = new WeightedRandomPicker<>();
	static {
		driftingTypes.add("drifting", 10f);
		driftingTypes.add("dormant", 10f);
		driftingTypes.add("//DYING", 2f);
		driftingTypes.add("//ERROR PLEASE TRY AGAIN", 2f);
	}


	public static WeightedRandomPicker<String> STARGAZER_WANDERER_NAMES_AGGRO = new WeightedRandomPicker<String>();

	static {
		STARGAZER_WANDERER_NAMES_AGGRO.add("Hunters", 10f);
		STARGAZER_WANDERER_NAMES_AGGRO.add("Scavengers", 10f);
		STARGAZER_WANDERER_NAMES_AGGRO.add("Stargazers", 10f);
	}

	public void run() {
		FleetCreatorMission m = new FleetCreatorMission(random);
		m.beginFleet();
		
		Vector2f loc = entity.getLocationInHyperspace();
		
		FleetSize size = FleetSize.MEDIUM;
		FleetQuality quality = FleetQuality.VERY_LOW;
		OfficerQuality oQuality = OfficerQuality.LOWER;
		OfficerNum oNum = OfficerNum.ALL_SHIPS;
		String type = FleetTypes.PATROL_MEDIUM;
		float r = random.nextFloat();
		if (r < 0.25f) {
			size = FleetSize.LARGE;
		} else if (r < 0.5f) {
			size = FleetSize.LARGER;
		}
		
		m.triggerCreateFleet(size, quality, NightcrossID.FACTION_STARGAZER, type, loc);
		m.getPreviousCreateFleetAction().fQualityMod = -10f;
		m.triggerSetFleetOfficers(oNum, oQuality);



		
		CampaignFleetAPI fleet = m.createFleet();
		if (fleet != null) {
			setVel(new Vector2f(0, 0));
			entity.getContainingLocation().addEntity(fleet);
			fleet.setLocation(entity.getLocation().x, entity.getLocation().y);
			fleet.addScript(new AutoDespawnScript(fleet));

			NA_StargazerFleets.modifyStargazerFleet(fleet, random);


			fleet.addScript(new NA_StargazerNebulaScript(fleet, 0.1f));
			fleet.addTag(NightcrossTags.NEBULA_GHOST);

			fleet.getMemoryWithoutUpdate().set("$fromGhost", true);
			fleet.getMemoryWithoutUpdate().set(MemFlags.MAY_GO_INTO_ABYSS, true);
			if (params.angry) {
				fleet.setName(STARGAZER_WANDERER_NAMES_AGGRO.pick());
				fleet.setTransponderOn(true);
				fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_LOW_REP_IMPACT, true);
				fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
				fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, true);
				fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
				fleet.setNullAIActionText(seekingTypes.pick());
				fleet.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "Kocaeli_Core");
			} else {
				fleet.setTransponderOn(true);
				fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
				fleet.setAI(null);
				fleet.setNullAIActionText(driftingTypes.pick());
				fleet.getMemoryWithoutUpdate().set(MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY, "kocaeli_nightcross_remnant");
			}
		}
	}


	
}







