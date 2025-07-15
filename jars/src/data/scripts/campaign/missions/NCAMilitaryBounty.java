package data.scripts.campaign.missions;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.cb.*;
import com.fs.starfarer.api.util.Misc;

import java.util.ArrayList;
import java.util.List;

public class NCAMilitaryBounty extends BaseCustomBounty {

	public static List<CustomBountyCreator> CREATORS = new ArrayList<CustomBountyCreator>();
	static {
		CREATORS.add(new CBPirate());
		CREATORS.add(new CBDeserter());
		CREATORS.add(new CBDerelict());
		CREATORS.add(new CBMerc());
		CREATORS.add(new NCACBThreat());
		CREATORS.add(new CBRemnant());
		CREATORS.add(new CBRemnantPlus());
		CREATORS.add(new CBRemnantStation());
		CREATORS.add(new CBEnemyStation());
	}
	
	@Override
	public List<CustomBountyCreator> getCreators() {
		return CREATORS;
	}

	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		PersonAPI person = getPerson();
		if (!barEvent && person != null && person.getFaction() != null && !person.getFaction().getId().equals("nightcross")) return false;
		if (!barEvent || Factions.INDEPENDENT.equals(createdAt.getFaction().getId())
			|| "nightcross".equals(createdAt.getFaction().getId())) {
				return super.create(createdAt, barEvent);
		}
		return false;
	}
	
	@Override
	protected void createBarGiver(MarketAPI createdAt) {
		List<String> posts = new ArrayList<String>();
		if (Misc.isMilitary(createdAt)) {
			posts.add(Ranks.POST_BASE_COMMANDER);
		}
		if (Misc.hasOrbitalStation(createdAt)) {
			posts.add(Ranks.POST_STATION_COMMANDER);
		}
		if (posts.isEmpty()) {
			posts.add(Ranks.POST_GENERIC_MILITARY);
		}
		String post = pickOne(posts);
		setGiverPost(post);
		setGiverFaction("nightcross");
		if (post.equals(Ranks.POST_GENERIC_MILITARY)) {
			setGiverRank(Ranks.SPACE_COMMANDER);
			setGiverImportance(pickImportance());
		} else if (post.equals(Ranks.POST_BASE_COMMANDER)) {
			setGiverRank(Ranks.GROUND_COLONEL);
			setGiverImportance(pickImportance());
		} else if (post.equals(Ranks.POST_STATION_COMMANDER)) {
			setGiverRank(Ranks.SPACE_CAPTAIN);
			setGiverImportance(pickHighImportance());
		}
		setGiverTags(Tags.CONTACT_MILITARY, Tags.CONTACT_TRADE);
		findOrCreateGiver(createdAt, false, false);
		setGiverIsPotentialContactOnSuccess();
	}



	
}











