package data.scripts.campaign.missions;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.CheapCommodityMission;
import com.fs.starfarer.api.impl.campaign.missions.DelayedFleetEncounter;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Map;

public class NCAProcurementMission extends HubMissionWithBarEvent {

	public static float PROB_COMPLICATIONS = 0.5f;
	
	public static float MISSION_DAYS = 90f;
	
	public static float MIN_BASE_VALUE = 10000;
	public static float MAX_BASE_VALUE = 100000;
	public static float BASE_PRICE_MULT = 1.5f;
	
	public static float PROB_REMOTE = 0.7f;

	
	
	public static enum Stage {
		TALK_TO_PERSON,
		COMPLETED,
		FAILED,
	}
	public static enum Variation {
		LOCAL,
		REMOTE,
	}
	
	protected String commodityId;
	protected int quantity;
	protected int pricePerUnit;
	
	protected Variation variation;
	protected MarketAPI deliveryMarket;
	protected PersonAPI deliveryContact;
	
	
	@Override
	protected boolean create(MarketAPI createdAt, boolean barEvent) {
		if (barEvent && !(Factions.INDEPENDENT.equals(createdAt.getFaction().getId())
				|| "nightcross".equals(createdAt.getFaction().getId()))) {
			return false;
		}
		//genRandom = Misc.random;
		if (barEvent) {
			setGiverRank(Ranks.CITIZEN);
			String post = pickOne(Ranks.POST_TRADER, Ranks.POST_COMMODITIES_AGENT,
								  Ranks.POST_MERCHANT, Ranks.POST_INVESTOR,
								  Ranks.POST_EXECUTIVE, Ranks.POST_SENIOR_EXECUTIVE,
								  Ranks.POST_PORTMASTER);
			setGiverPost(post);
			setGiverFaction("nightcross");
			if (post.equals(Ranks.POST_SENIOR_EXECUTIVE)) {
				setGiverImportance(pickHighImportance());
			} else {
				setGiverImportance(pickImportance());
			}
			setGiverTags(Tags.CONTACT_TRADE);
			findOrCreateGiver(createdAt, false, false);
		}


		PersonAPI person = getPerson();
		if (!barEvent && person != null && person.getFaction() != null && !person.getFaction().getId().equals("nightcross")) return false;
		if (person == null) return false;
		MarketAPI market = person.getMarket();
		if (market == null) return false;
		
		if (!setPersonMissionRef(person, "$ncaproCom_ref")) {
			return false;
		}
		
		if (barEvent) {
			setGiverIsPotentialContactOnSuccess(1f);
		}
		
		PersonImportance importance = person.getImportance();
		boolean canOfferRemote = importance.ordinal() >= PersonImportance.MEDIUM.ordinal();
		boolean preferExpensive = getQuality() >= PersonImportance.HIGH.getValue();
		variation = Variation.LOCAL;
		if (canOfferRemote && rollProbability(PROB_REMOTE)) {
			variation = Variation.REMOTE;
		}
		if (CheapCommodityMission.SAME_CONTACT_DEBUG) {
			variation = Variation.REMOTE;
		}

		CommodityOnMarketAPI com = null;
		if (variation == Variation.LOCAL) {
			requireMarketIs(market);
			requireCommodityIsNotPersonnel();
			requireCommodityLegal();
			requireCommodityDemandAtLeast(1);
			requireCommoditySurplusAtMost(0);
			requireCommodityDeficitAtLeast(1);
			if (preferExpensive) {
				preferCommodityTags(ReqMode.ALL, Commodities.TAG_EXPENSIVE);
			}
			com = pickCommodity();
		} 
		
		if (com == null && canOfferRemote) {
			variation = Variation.REMOTE;
		}
		
		if (variation == Variation.REMOTE) {
			if (CheapCommodityMission.SAME_CONTACT_DEBUG) {
				requireMarketIs("jangala");
			} else {
				requireMarketIsNot(market);
			}
			requireMarketFaction(market.getFactionId());
			requireMarketNotHidden();
			requireMarketLocationNot(createdAt.getContainingLocation());
			requireCommodityIsNotPersonnel();
			requireMarketFactionNotHostileTo(Factions.PLAYER);
			requireCommodityLegal();
			requireCommodityDemandAtLeast(1);
			requireCommoditySurplusAtMost(0);
			requireCommodityDeficitAtLeast(1);
			if (preferExpensive) {
				preferCommodityTags(ReqMode.ALL, Commodities.TAG_EXPENSIVE);
			}
			com = pickCommodity();
		}
		
		if (com == null) return false;
		
		deliveryMarket = com.getMarket();
		
		commodityId = com.getId();
		
		float value = MIN_BASE_VALUE + (MAX_BASE_VALUE - MIN_BASE_VALUE) * getQuality();
		quantity = getRoundNumber(value / com.getCommodity().getBasePrice());
		
		if (quantity < 10) quantity = 10;
		pricePerUnit = (int) (com.getMarket().getSupplyPrice(com.getId(), quantity, true) / (float) quantity * 
							  BASE_PRICE_MULT / getRewardMult());
		pricePerUnit = getRoundNumber(pricePerUnit);
		if (pricePerUnit < 2) pricePerUnit = 2;
		
		
		if (variation == Variation.REMOTE) {
			if (com.isIllegal()) {
				deliveryContact = findOrCreateCriminal(deliveryMarket, true);
			} else {
				deliveryContact = findOrCreateTrader(deliveryMarket.getFactionId(), deliveryMarket, true);
			}
		} else {
			deliveryContact = person;
		}
		ensurePersonIsInCommDirectory(deliveryMarket, deliveryContact);
		//setPersonIsPotentialContactOnSuccess(deliveryContact);
		
		if (deliveryContact == null ||
				(variation == Variation.REMOTE && !setPersonMissionRef(deliveryContact, "$ncaproCom_ref"))) {
			return false;
		}
		setPersonDoGenericPortAuthorityCheck(deliveryContact);
		makeImportant(deliveryContact, "$ncaproCom_needsCommodity", Stage.TALK_TO_PERSON);
		
		setStartingStage(Stage.TALK_TO_PERSON);
		setSuccessStage(Stage.COMPLETED);
		setFailureStage(Stage.FAILED);
		
		setStageOnMemoryFlag(Stage.COMPLETED, deliveryContact, "$ncaproCom_completed");
		setTimeLimit(Stage.FAILED, MISSION_DAYS, null);
		
		
		if (getQuality() < 0.5f) {
			setRepFactionChangesVeryLow();
		} else {
			setRepFactionChangesLow();
		}
		setRepPersonChangesMedium();
		
		return true;
	}
	
	protected void updateInteractionDataImpl() {
		set("$ncaproCom_barEvent", isBarEvent());
		
		set("$ncaproCom_commodityId", commodityId);
		set("$ncaproCom_underworld", getPerson().hasTag(Tags.CONTACT_UNDERWORLD));
		set("$ncaproCom_playerHasEnough", playerHasEnough(commodityId, quantity));
		set("$ncaproCom_commodityName", getSpec().getLowerCaseName());
		set("$ncaproCom_quantity", Misc.getWithDGS(quantity));
		set("$ncaproCom_pricePerUnit", Misc.getDGSCredits(pricePerUnit));
		set("$ncaproCom_totalPrice", Misc.getDGSCredits(pricePerUnit * quantity));
		set("$ncaproCom_variation", variation);
		set("$ncaproCom_manOrWoman", getPerson().getManOrWoman());
		set("$ncaproCom_hisOrHer", getPerson().getHisOrHer());
		//set("$ncaproCom_heOrShe", getPerson().getHeOrShe());
		//set("$ncaproCom_HeOrShe", getPerson().getHeOrShe().substring(0, 1).toUpperCase() + getPerson().getHeOrShe().substring(1));
		
		
		if (variation == Variation.REMOTE) {
			set("$ncaproCom_personName", deliveryContact.getNameString());
			set("$ncaproCom_personPost", deliveryContact.getPost().toLowerCase());
			set("$ncaproCom_PersonPost", Misc.ucFirst(deliveryContact.getPost()));
			set("$ncaproCom_marketName", deliveryMarket.getName());
			set("$ncaproCom_marketOnOrAt", deliveryMarket.getOnOrAt());
			set("$ncaproCom_dist", getDistanceLY(deliveryMarket));
		}
	}
	
	@Override
	public void addDescriptionForNonEndStage(TooltipMakerAPI info, float width, float height) {
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.TALK_TO_PERSON) {
			TooltipMakerAPI text = info.beginImageWithText(deliveryContact.getPortraitSprite(), 48f);
			text.addPara("Deliver %s units of " + getSpec().getLowerCaseName() + " to " + deliveryContact.getNameString() + " " +
					deliveryMarket.getOnOrAt() + " " + deliveryMarket.getName() + ". You will be paid %s per unit, or " + 
					"%s total.", 0f, h,
					Misc.getWithDGS(quantity),
					Misc.getDGSCredits(pricePerUnit), 
					Misc.getDGSCredits(pricePerUnit * quantity));
			info.addImageWithText(opad);
			if (playerHasEnough(commodityId, quantity)) {
				info.addPara("You have enough " + getSpec().getLowerCaseName() + " in your cargo holds to complete " +
						"the delivery.", opad);
			} else {
				info.addPara("You do not have enough " + getSpec().getLowerCaseName() + " in your cargo holds to complete " +
						"the delivery.", opad);
			}
		}
	}

//	need to mention that need to acquire the item
//	check whether player has enough and chance desc/bullet point depending?
	
	@Override
	public boolean addNextStepText(TooltipMakerAPI info, Color tc, float pad) {
		Color h = Misc.getHighlightColor();
		if (currentStage == Stage.TALK_TO_PERSON) {
			if (playerHasEnough(commodityId, quantity)) {
				info.addPara("Go to " + deliveryMarket.getName() + " and contact " + deliveryContact.getNameString() + " to arrange delivery", tc, pad);
			} else {
				String name = getSpec().getLowerCaseName();
				info.addPara("Acquire %s units of " + name, pad, tc, h, "" + (int) quantity);				
			}
			return true;
		}
		return false;
	}	
	
	@Override
	public String getBaseName() {
		return getSpec().getName() + " Procurement";
	}
	
	@Override
	public void accept(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
		super.accept(dialog, memoryMap);
		
		if (variation == Variation.REMOTE && rollProbability(PROB_COMPLICATIONS)) {
			String faction = Factions.PIRATES;
			String hail = "PROCOMPirateHail";
			if (Math.random() < 0.75f) {
				// competition!!!
				if (Math.random() < 0.5f) {
					faction = Factions.MERCENARY;
					hail = "NCAPROCOMMercHail";
				} else {
					faction = Factions.TRITACHYON;
					hail = "NCAPROCOMTTHail";
				}
			}
			DelayedFleetEncounter e = new DelayedFleetEncounter(genRandom, getMissionId());
			e.setDelay(10f);
			e.setLocationCoreOnly(true, faction);
			e.setEncounterInHyper();
			e.beginCreate();
			e.triggerCreateFleet(FleetSize.MEDIUM, FleetQuality.DEFAULT, faction, FleetTypes.PATROL_MEDIUM, new Vector2f());
			e.triggerSetAdjustStrengthBasedOnQuality(true, getQuality());
			e.triggerSetStandardAggroPirateFlags();
			e.triggerSetStandardAggroInterceptFlags();
			e.triggerSetFleetMemoryValue("$ncaproCom_commodityName", getSpec().getLowerCaseName());
			e.triggerSetFleetGenericHailPermanent(hail);
			e.endCreate();
		}
	}
	
	
	
	protected transient CommoditySpecAPI spec;
	protected CommoditySpecAPI getSpec() {
		if (spec == null) {
			spec = Global.getSettings().getCommoditySpec(commodityId);
		}
		return spec;
	}
}

