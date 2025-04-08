package data.scripts.campaign.station;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

public class na_battlestationaria extends OrbitalStation {

    @Override
    protected void spawnStation() {
        FleetParamsV3 fParams = new FleetParamsV3(null, null,
                "nightcross",
                1f,
                FleetTypes.PATROL_SMALL,
                0,
                0, 0, 0, 0, 0, 0);
        fParams.allWeapons = true;
        fParams.qualityMod = 2f;

        removeStationEntityAndFleetIfNeeded();

//		if (market.getId().equals("jangala")) {
//			System.out.println("wefwefew");
//		}

        stationFleet = FleetFactoryV3.createFleet(fParams);

        stationFleet.setFaction(market.getFactionId(), true);
        //stationFleet.setName(getCurrentName());
        stationFleet.setNoFactionInName(true);


        stationFleet.setStationMode(true);

        //stationFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);

        // needed for AI fleets to engage it, as they engage the hidden station fleet, unlike
        // the player that interacts with the stationEntity
        stationFleet.clearAbilities();
        stationFleet.addAbility(Abilities.TRANSPONDER);
        stationFleet.getAbility(Abilities.TRANSPONDER).activate();
        stationFleet.getDetectedRangeMod().modifyFlat("gen", 10000f);

        stationFleet.setAI(null);
        stationFleet.addEventListener(this);


        ensureStationEntityIsSetOrCreated();

        if (stationEntity instanceof CustomCampaignEntityAPI) {
            if (!usingExistingStation || stationEntity.hasTag(Tags.USE_STATION_VISUAL)) {
                ((CustomCampaignEntityAPI)stationEntity).setFleetForVisual(stationFleet);
            }
        }

        stationFleet.setCircularOrbit(stationEntity, 0, 0, 100);
        stationFleet.getMemoryWithoutUpdate().set(MemFlags.STATION_MARKET, market);
        stationFleet.setHidden(true);


        matchStationAndCommanderToCurrentIndustry();
    }


    @Override
    public boolean canDowngrade() {
        return false;
    }

    @Override
    public boolean canUpgrade() {
        return false;
    }

    @Override
    public boolean showWhenUnavailable() {
        return false;
    }

    @Override
    public boolean isAvailableToBuild() {
        return false;
    }

    @Override
    public boolean isHidden() {
        return true;
    }
}
