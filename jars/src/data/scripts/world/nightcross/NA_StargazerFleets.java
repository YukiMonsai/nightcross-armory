package data.scripts.world.nightcross;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.ids.NightcrossID;
import data.scripts.campaign.plugins.NAGhostCorePlugin;
import data.scripts.hullmods.NA_ProjectGhost;
import data.scripts.stardust.NA_StargazerFIDConfig;

import java.util.Random;

public class NA_StargazerFleets {

    public static WeightedRandomPicker<String> STARGAZER_WANDERER_NAMES = new WeightedRandomPicker<String>();

    static {
        STARGAZER_WANDERER_NAMES.add("Wanderers", 10f);
        STARGAZER_WANDERER_NAMES.add("Travelers", 10f);
        STARGAZER_WANDERER_NAMES.add("Observers", 10f);
        STARGAZER_WANDERER_NAMES.add("Stargazers", 30f);
    }


    public static CampaignFleetAPI createStargazerFleet(FleetParamsV3 params, Random random) {


        CampaignFleetAPI f = FleetFactoryV3.createFleet(params);
        //f.setInflater(DefaultFleetInflater.);
        f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_FLEET_TYPE, params.fleetType);


        f.getFleetData().setSyncNeeded();
        f.getFleetData().syncIfNeeded();
        f.getFleetData().sort();



        FactionAPI faction = Global.getSector().getFaction(NightcrossID.FACTION_STARGAZER);
        f.setName(faction.getFleetTypeName(params.fleetType));

        f.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN,
                new NA_StargazerFIDConfig());
        f.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_ALLOW_LONG_PURSUIT, false);
        f.getMemoryWithoutUpdate().set(MemFlags.MAY_GO_INTO_ABYSS, true);


        modifyStargazerFleet(f, random);
        f.setName(STARGAZER_WANDERER_NAMES.pick());


        return f;
    }

    public static  void modifyStargazerFleet(CampaignFleetAPI f, Random random) {
        for (FleetMemberAPI curr : f.getFleetData().getMembersListCopy()) {
            if (!(curr.getHullSpec() != null && curr.getHullSpec().hasTag("stargazer_hull"))
                    && !(curr.getHullSpec() != null && curr.getHullSpec().getBaseHull() != null
                    && curr.getHullSpec().getBaseHull().hasTag("stargazer_hull"))) continue;


            curr.getRepairTracker().setCR(curr.getRepairTracker().getMaxCR());
            if (curr.getHullSpec() != null && curr.getHullSpec().getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) {
                f.addDropRandom("na_stargazer_drops_cap", 1);
            } else if (curr.getHullSpec() != null && curr.getHullSpec().getHullSize() == ShipAPI.HullSize.CRUISER) {
                f.addDropRandom("na_stargazer_drops_cru", 1);
            } else if (curr.getHullSpec() != null && curr.getHullSpec().getHullSize() != ShipAPI.HullSize.FIGHTER) {
                f.addDropRandom("na_stargazer_drops", 1);
            }

        }
        editStargazerFleetAICores(f, random);
    }

    public static void editStargazerFleetAICores(CampaignFleetAPI f, Random random) {
        if (random == null) random = new Random();


        for (FleetMemberAPI curr : f.getFleetData().getMembersListCopy()) {
            if (!(curr.getHullSpec() != null && curr.getHullSpec().hasTag("stargazer_hull"))
                    && !(curr.getHullSpec() != null && curr.getHullSpec().getBaseHull() != null
                    && curr.getHullSpec().getBaseHull().hasTag("stargazer_hull"))) continue;



            boolean keepPortrait = (curr.isFlagship()) ?
                    Math.random() < 0.75f :
                    Math.random() < 0.25f;

            float chance_matrix = 0;
            float chance_grid = 0.33f;
            float chance_ghost = 1f;

            if (curr.getHullSpec().getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) {
                chance_matrix = 0.33f;
                chance_grid = 0.5f;
            }
            if (curr.getHullSpec().getHullSize() == ShipAPI.HullSize.CRUISER) {
                chance_matrix = 0.25f;
                chance_grid = 0.6f;
            }
            if (curr.getHullSpec().getHullSize() == ShipAPI.HullSize.DESTROYER) {
                chance_matrix = 0.1f;
                chance_grid = 0.33f;
            }
            if (curr.getHullSpec().getHullSize() == ShipAPI.HullSize.FRIGATE) {
                chance_matrix = 0.05f;
                chance_grid = 25;
            }


            if (random.nextFloat() < chance_matrix) {
                setStargazerAICore(curr, NightcrossID.GHOST_MATRIX_ID, keepPortrait, random, true);
            } else if (random.nextFloat() < chance_grid) {
                setStargazerAICore(curr, NightcrossID.GHOST_CORE_ID, keepPortrait, random, true);
            } else if (random.nextFloat() < chance_ghost) {
                setStargazerAICore(curr, NightcrossID.GHOST_CORE_ID, keepPortrait, random, false);
            }
        }
    }


    public static void setStargazerAICore(FleetMemberAPI curr, String aiCoreID, boolean keepPortrait, Random random, boolean addDrops) {
        if (curr.getCaptain() == null) {
            AICoreOfficerPlugin plugin = new NAGhostCorePlugin();
            //PersonAPI person = OfficerManagerEvent.createOfficer(fleet.getFaction(), 20, true, SkillPickPreference.NON_CARRIER, random);
            PersonAPI person = plugin.createPerson(aiCoreID, curr.getFleetData().getFleet().getFaction().getId(), random);
            curr.setCaptain(person);
        }

        switch (aiCoreID) {
            case NightcrossID.TETO_CORE:
                if (!keepPortrait) {
                    curr.getCaptain().setPortraitSprite(Global.getSettings().getSpriteName("na_characters", "teto"));
                    curr.getCaptain().setName(new FullName("Teto", "Kasane", FullName.Gender.FEMALE));
                }

                curr.getCaptain().addTag(NA_ProjectGhost.CAPTAIN_TAG);

                curr.getCaptain().getStats().setLevel(6);
                curr.getCaptain().getStats().setSkillLevel(NightcrossID.SKILL_FULLDIVE_TETO, 2);

                curr.getCaptain().getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                break;
            case NightcrossID.GHOST_MATRIX_ID:
                if (!keepPortrait) {
                    curr.getCaptain().setPortraitSprite(Global.getSettings().getSpriteName("na_characters", "stargazermatrix"));
                    curr.getCaptain().setName(new FullName("Stargazer", "Matrix", FullName.Gender.ANY));
                }

                curr.getCaptain().addTag(NA_ProjectGhost.CAPTAIN_TAG);

                curr.getCaptain().getStats().setLevel(7);
                curr.getCaptain().getStats().setSkillLevel(NightcrossID.SKILL_FULLDIVE_MATRIX, 2);

                curr.getCaptain().getStats().setSkillLevel(random.nextFloat() < 0.5f ? Skills.GUNNERY_IMPLANTS : Skills.ORDNANCE_EXPERTISE, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                curr.getCaptain().getStats().setSkillLevel(random.nextFloat() < 0.5f ? Skills.FIELD_MODULATION : Skills.POLARIZED_ARMOR, 2);
                curr.getCaptain().getStats().setSkillLevel(random.nextFloat() < 0.5f ? Skills.DAMAGE_CONTROL : Skills.COMBAT_ENDURANCE, 2);

                if (addDrops) {
                    curr.getFleetData().getFleet().addDropRandom("na_stargazer_drops_matrix", 1);
                }
                break;
            case NightcrossID.GHOST_CORE_ID:
                if (!keepPortrait) {
                    curr.getCaptain().setPortraitSprite(Global.getSettings().getSpriteName("na_characters", "ghostcore"));
                    curr.getCaptain().setName(new FullName("Ghost", "Core", FullName.Gender.ANY));
                }

                curr.getCaptain().addTag(NA_ProjectGhost.CAPTAIN_TAG);

                curr.getCaptain().getStats().setLevel(5);
                curr.getCaptain().getStats().setSkillLevel(NightcrossID.SKILL_FULLDIVE_GHOST, 2);

                curr.getCaptain().getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                curr.getCaptain().getStats().setSkillLevel(random.nextFloat() < 0.5f ? Skills.FIELD_MODULATION : Skills.DAMAGE_CONTROL, 2);
                break;
            case NightcrossID.GHOST_GRID_ID:
                if (!keepPortrait) {
                    curr.getCaptain().setPortraitSprite(Global.getSettings().getSpriteName("na_characters", "ghostgrid"));
                    curr.getCaptain().setName(new FullName("Stargazer", "Grid", FullName.Gender.ANY));
                }

                curr.getCaptain().addTag(NA_ProjectGhost.CAPTAIN_TAG);

                curr.getCaptain().getStats().setLevel(6);
                curr.getCaptain().getStats().setSkillLevel(NightcrossID.SKILL_FULLDIVE_GRID, 2);

                curr.getCaptain().getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
                curr.getCaptain().getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
                curr.getCaptain().getStats().setSkillLevel(random.nextFloat() < 0.5f ? Skills.GUNNERY_IMPLANTS : Skills.ORDNANCE_EXPERTISE, 2);
                if (addDrops) {
                    curr.getFleetData().getFleet().addDropRandom("na_stargazer_drops_grid", 1);
                }
                break;
        }
    }
}
