package data.scripts.world.nightcross;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.enc.NA_StargazerNebulaScript;
import data.scripts.campaign.ids.NightcrossID;
import data.scripts.campaign.plugins.NAGhostCorePlugin;
import data.scripts.hullmods.NA_ProjectGhost;
import data.scripts.stardust.NA_StargazerFIDConfig;
import org.lazywizard.lazylib.MathUtils;

import java.util.*;

import static data.scripts.campaign.ids.NightcrossPeople.*;

public class NA_StargazerFleets {

    public static WeightedRandomPicker<String> STARGAZER_WANDERER_NAMES = new WeightedRandomPicker<String>();
    public static WeightedRandomPicker<String> STARGAZER_WANDERER_NAMES1 = new WeightedRandomPicker<String>();
    public static WeightedRandomPicker<String> STARGAZER_WANDERER_NAMES2 = new WeightedRandomPicker<String>();
    public static WeightedRandomPicker<String> STARGAZER_DEFENDER_NAMES1 = new WeightedRandomPicker<String>();
    public static WeightedRandomPicker<String> STARGAZER_DEFENDER_NAMES2 = new WeightedRandomPicker<String>();

    static {
        STARGAZER_WANDERER_NAMES.add("Wanderers", 10f);
        STARGAZER_WANDERER_NAMES.add("Travelers", 10f);
        STARGAZER_WANDERER_NAMES.add("Observers", 10f);
        STARGAZER_WANDERER_NAMES.add("Stargazers", 30f);

        STARGAZER_WANDERER_NAMES1.add("Void", 10f);
        STARGAZER_WANDERER_NAMES1.add("Aeon", 10f);
        STARGAZER_WANDERER_NAMES1.add("Star", 10f);
        STARGAZER_WANDERER_NAMES1.add("Abyss", 10f);

        STARGAZER_WANDERER_NAMES2.add("Wanderers", 20f);
        STARGAZER_WANDERER_NAMES2.add("Travelers", 10f);
        STARGAZER_WANDERER_NAMES2.add("Watchers", 10f);
        STARGAZER_WANDERER_NAMES2.add("Gazers", 10f);
    }
    static {
        STARGAZER_DEFENDER_NAMES1.add("Abyss", 10f);
        STARGAZER_DEFENDER_NAMES1.add("Void", 10f);
        STARGAZER_DEFENDER_NAMES1.add("Nebula", 10f);
        STARGAZER_DEFENDER_NAMES1.add("Crypt", 10f);
        STARGAZER_DEFENDER_NAMES1.add("Remnant", 10f);
        STARGAZER_DEFENDER_NAMES2.add("Guardians", 10f);
        STARGAZER_DEFENDER_NAMES2.add("Watchers", 10f);
        STARGAZER_DEFENDER_NAMES2.add("Stalkers", 10f);
        STARGAZER_DEFENDER_NAMES2.add("Wardens", 10f);
    }


    public static CampaignFleetAPI createStargazerFleet(FleetParamsV3 params, Random random, StargazerFleetType type) {


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


        f.setName(STARGAZER_WANDERER_NAMES.pick());
        modifyStargazerFleet(f, random, type);



        return f;
    }

    public enum StargazerFleetType {
        PURE, MIXED, LOST_ONES, NIGHTCROSS
    }

    public static  void modifyStargazerFleet(CampaignFleetAPI f, Random random, StargazerFleetType type) {

        if (f.getMemoryWithoutUpdate().contains("$naai_fleettypeset")) {
            return;
        } else {
            f.getMemoryWithoutUpdate().set("$naai_fleettypeset", true);
        }
        inflateStargazerFleet(f, random, type);
        f.getFleetData().sort();

        if (!f.hasScriptOfClass(NA_StargazerNebulaScript.class))
            f.addScript(new NA_StargazerNebulaScript(f, 0.05f));

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

        if (f.getFlagship() != null && f.getFlagship().getCaptain() != null) {

            f.getFlagship().getCaptain().getStats().setSkillLevel(Skills.FLUX_REGULATION, 2);
            f.getFlagship().getCaptain().getStats().setSkillLevel(Skills.COORDINATED_MANEUVERS, 2);
            f.getFlagship().getCaptain().getStats().setSkillLevel(Skills.ELECTRONIC_WARFARE, 2);

            if ((random != null && random.nextBoolean()) || (random == null && Math.random() < 0.5f))
                f.getFlagship().getCaptain().getStats().setSkillLevel(Skills.WOLFPACK_TACTICS, 2);
            else f.getFlagship().getCaptain().getStats().setSkillLevel(Skills.TACTICAL_DRILLS, 2);

        }
        editStargazerFleetAICores(f, random);

        if (type == StargazerFleetType.LOST_ONES)
            f.setName("Lost Ones");
    }



    public static String getVariant(String hullId, Random random) {
        List<String> list = Global.getSettings().getHullIdToVariantListMap().get(hullId);
        WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>(random);
        picker.addAll(list);
        String variantId = picker.pick();
        if (variantId == null) {
            return null;
        }
        return variantId;
    }


    public static String pickVariant(String factionId, Random random, Object ... shipRoles) {
        if (random == null) random = new Random();

        FactionAPI faction = Global.getSector().getFaction(factionId);

        WeightedRandomPicker<String> picker = new WeightedRandomPicker<String>(random);
        for (int i = 0; i < shipRoles.length; i += 2) {
            String role = (String) shipRoles[i];
            Float weight = (Float) shipRoles[i + 1];
            picker.add(role, weight);
        }

        Set<String> variantsForRole = new HashSet<String>();
        while (variantsForRole.isEmpty() && !picker.isEmpty()) {
            String role = picker.pickAndRemove();
            if (role == null) return null;

            variantsForRole = faction.getVariantsForRole(role);
        }

        picker.clear();
        picker.addAll(variantsForRole);
        String variantId = picker.pick();

        return variantId;
    }


    public static String pickSmallVariantId(String factionId, Random random) {
        String variantId = pickVariant(factionId, random,
                ShipRoles.COMBAT_SMALL, 10f,
                ShipRoles.COMBAT_SMALL, 3f
        );
        return variantId;
    }

    public static String pickMediumVariantId(String factionId, Random random) {
        String variantId = pickVariant(factionId, random,
                ShipRoles.COMBAT_MEDIUM, 10f,
                ShipRoles.CARRIER_SMALL, 1f
        );
        return variantId;
    }

    public static String pickLargeVariantId(String factionId, Random random) {
        String variantId = pickVariant(factionId, random,
                ShipRoles.COMBAT_LARGE, 10f,
                ShipRoles.CARRIER_MEDIUM, 1f,
                ShipRoles.CARRIER_LARGE, 1f
        );
        return variantId;
    }


    public static DerelictShipEntityPlugin.DerelictType pickDerelictType(Random random, float medWeight, float largeWeight) {
        if (random == null) random = new Random();
        WeightedRandomPicker<DerelictShipEntityPlugin.DerelictType> picker = new WeightedRandomPicker<DerelictShipEntityPlugin.DerelictType>(random);

        picker.add(DerelictShipEntityPlugin.DerelictType.LARGE, largeWeight);
        picker.add(DerelictShipEntityPlugin.DerelictType.MEDIUM, medWeight);
        picker.add(DerelictShipEntityPlugin.DerelictType.SMALL, 100f);

        return picker.pick();
    }

    public static void inflateStargazerFleet(CampaignFleetAPI f, Random random, StargazerFleetType type) {
        if (random == null) random = new Random();


        float w = Global.getSettings().getFloat("sectorWidth");
        float h = Global.getSettings().getFloat("sectorHeight");
        float u = Global.getSettings().getFloat("unitsPerLightYear");

        var inflater =
                f.getInflater();
        if (inflater != null) {
            if (inflater.getParams() instanceof DefaultFleetInflaterParams) {

                DefaultFleetInflaterParams params = (DefaultFleetInflaterParams) inflater.getParams();
                params.allWeapons = true;
            }
        }


        float numMidShips = 0;
        float largeNum = 0f;
        float medNum = 0f;
        float spookChance = 0.15f;
        float fails = 1000;

        for (FleetMemberAPI curr : f.getFleetData().getMembersListCopy()) {
            if ((curr.getHullSpec() != null && curr.getHullSpec().hasTag("stargazer_hull"))
                    && !(curr.getHullSpec() != null && curr.getHullSpec().getBaseHull() != null
                    && curr.getHullSpec().getBaseHull().hasTag("stargazer_hull"))) {
                numMidShips += 1;
                if ((curr.getHullSpec() != null && curr.getHullSpec().getHullSize() == ShipAPI.HullSize.DESTROYER)
                    || (curr.getHullSpec().getBaseHull() != null && curr.getHullSpec().getBaseHull().getHullSize() == ShipAPI.HullSize.DESTROYER)) {
                    medNum += 0.5f;
                } else
                if ((curr.getHullSpec() != null && curr.getHullSpec().getHullSize() == ShipAPI.HullSize.CRUISER)
                        || (curr.getHullSpec().getBaseHull() != null && curr.getHullSpec().getBaseHull().getHullSize() == ShipAPI.HullSize.CRUISER)) {
                    medNum += 1f;
                    largeNum += 0.5f;
                } else
                if ((curr.getHullSpec() != null && curr.getHullSpec().getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP)
                        || (curr.getHullSpec().getBaseHull() != null && curr.getHullSpec().getBaseHull().getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP)) {
                    largeNum += 1f;
                    medNum += 2f;
                    numMidShips += 1;
                }
            }


        }

        if (type.equals(StargazerFleetType.LOST_ONES)) {
            numMidShips *= 2;
            medNum *= 2;
            largeNum *= 2;

            for (FleetMemberAPI curr : f.getFleetData().getMembersListCopy()) {
                if (!curr.isFlagship()) {
                    f.getFleetData().removeFleetMember(curr);
                }
            }
        } else if (type.equals(StargazerFleetType.MIXED)) {
            numMidShips *= 2;
            medNum *= 2;
            largeNum *= 2;

            spookChance = 0.35f;

            for (FleetMemberAPI curr : f.getFleetData().getMembersListCopy()) {
                if (!curr.isFlagship()) {
                    if ((curr.getHullSpec() != null && curr.getHullSpec().getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP)
                            || (curr.getHullSpec().getBaseHull() != null && curr.getHullSpec().getBaseHull().getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP)) {
                        // nothing
                    } else {
                        if ((curr.getHullSpec() != null && curr.getHullSpec().getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP)
                                || (curr.getHullSpec().getBaseHull() != null && curr.getHullSpec().getBaseHull().getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP)) {
                            if (random.nextFloat() < 0.9f) {
                                f.getFleetData().removeFleetMember(curr);
                            }
                        } else {
                            if (random.nextFloat() < 0.4f) {
                                f.getFleetData().removeFleetMember(curr);
                            }
                        }

                    }
                }
            }
        }



        if (numMidShips > 0 && !type.equals(StargazerFleetType.PURE)) {

            var factions = type.equals(StargazerFleetType.NIGHTCROSS) ? new WeightedRandomPicker<String>() : SalvageSpecialAssigner.getNearbyFactions(random, MathUtils.getPointOnCircumference(Misc.ZERO, 15f, random.nextFloat() * 360),
                    15f, 10f, 0f);

            if (type.equals(StargazerFleetType.NIGHTCROSS)) {
                factions.add("nightcross", 1.0f);
            }
            int i = 0;
            while(i < numMidShips) {
                String factionId = factions.pick();
                DerelictShipEntityPlugin.DerelictType dtype = pickDerelictType(random, 100 * medNum / numMidShips, 100 * largeNum / numMidShips);
                String variantId = null;
                boolean spooky = random.nextFloat() < spookChance;
                switch (dtype) {
                    case LARGE: variantId = pickLargeVariantId(factionId, random); break;
                    case MEDIUM: variantId = pickMediumVariantId(factionId, random); break;
                    case SMALL: variantId = pickSmallVariantId(factionId, random); break;
                }

                if (variantId != null) {

                    if (Global.getSettings().getVariant(variantId).getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP
                        || !Global.getSettings().getVariant(variantId).getHullSpec().getBuiltInWings().isEmpty()
                        || (!spooky && Global.getSettings().getVariant(variantId).getHullSpec().isPhase())) {
                        if (fails++ < 1000) {
                            continue;
                        } else {
                            i++;
                            continue;
                        }
                    }

                    i++;
                    var member = f.getFleetData().addFleetMember(variantId);
                    var variant = member.getVariant();
                    if (variant != null) {
                        variant = variant.clone();

                        variant.setSource(VariantSource.REFIT);
                        variant.setHullVariantId(Misc.genUID());
                        member.setVariant(variant, false, true);


                        float numberOfWings = member.getVariant().getWings().size();
                        for (int ii = 0; ii < numberOfWings; ii++) {
                            member.getVariant().setWingId(ii, "naai_locust_wing");
                        }

                        if (spooky) {

                            //member.setVariant(variant, true, true);

                            AICoreOfficerPlugin plugin = Misc.getAICoreOfficerPlugin(NightcrossID.GHOST_CORE_ID);
                            PersonAPI person = plugin.createPerson(NightcrossID.GHOST_CORE_ID, NightcrossID.FACTION_STARGAZER, new Random());
                            member.setCaptain(person);

                            member.getVariant().addPermaMod("na_stargazerstars");


                            int numCorr = 0;

                            for (String ws : member.getVariant().getFittedWeaponSlots()) {
                                WeaponSlotAPI slot = variant.getSlot(ws);

                                if (slot != null) {
                                    if ((slot.getWeaponType() == WeaponAPI.WeaponType.MISSILE
                                            && (slot.getSlotSize() == WeaponAPI.WeaponSize.SMALL || slot.getSlotSize() == WeaponAPI.WeaponSize.MEDIUM))
                                            || slot.getWeaponType() == WeaponAPI.WeaponType.SYNERGY && slot.getSlotSize() == WeaponAPI.WeaponSize.SMALL) {
                                        if (numCorr == 0 || random.nextFloat() < 0.4f) {
                                            numCorr++;
                                            member.getVariant().addWeapon(ws, "naai_corrosionmote");
                                        }
                                    }
                                }
                            }
                        } else {
                            //
                        }


                        member.getVariant().addPermaMod(HullMods.AUTOMATED);
                        member.getVariant().setVariantDisplayName("Automated");
                        member.getVariant().addTag(Tags.TAG_NO_AUTOFIT);
                        member.getVariant().addPermaMod("automated");
                        member.getVariant().addPermaMod("na_fulldive");
                        member.getVariant().addPermaMod(HullMods.FAULTY_GRID);
                        member.getVariant().addPermaMod(HullMods.COMP_ARMOR);
                        member.getVariant().addPermaMod(HullMods.DEGRADED_ENGINES);
                        member.getVariant().addPermaMod(HullMods.GLITCHED_SENSORS);
                        member.getVariant().addPermaMod(HullMods.INCREASED_MAINTENANCE);
                    }


                } else if (fails++ >= 1000) {
                    i++;
                }
            }
        }






        for (FleetMemberAPI curr : f.getFleetData().getMembersListCopy()) {
            if (!(curr.getHullSpec() != null && curr.getHullSpec().hasTag("stargazer_hull"))
                    && !(curr.getHullSpec() != null && curr.getHullSpec().getBaseHull() != null
                    && curr.getHullSpec().getBaseHull().hasTag("stargazer_hull"))) continue;



            boolean keepPortrait = (curr.isFlagship()) ?
                    Math.random() < 0.75f :
                    Math.random() < 0.25f;

            float chance_matrix = 0;
            float chance_grid = 0.33f;
            float chance_ghost = 1.1f;

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
                chance_grid = .25f;
            }


            if (random.nextFloat() < chance_matrix) {
                setStargazerAICore(curr, NightcrossID.GHOST_MATRIX_ID, keepPortrait, random, true);
            } else if (random.nextFloat() < chance_grid) {
                setStargazerAICore(curr, NightcrossID.GHOST_GRID_ID, keepPortrait, random, true);
            } else if (random.nextFloat() < chance_ghost) {
                setStargazerAICore(curr, NightcrossID.GHOST_CORE_ID, keepPortrait, random, false);
            }
        }
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
            float chance_ghost = 1.1f;

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
                chance_grid = .25f;
            }


            if (random.nextFloat() < chance_matrix) {
                setStargazerAICore(curr, NightcrossID.GHOST_MATRIX_ID, keepPortrait, random, true);
            } else if (random.nextFloat() < chance_grid) {
                setStargazerAICore(curr, NightcrossID.GHOST_GRID_ID, keepPortrait, random, true);
            } else if (random.nextFloat() < chance_ghost) {
                setStargazerAICore(curr, NightcrossID.GHOST_CORE_ID, keepPortrait, random, false);
            }
        }
    }


    public static void setStargazerAICore(FleetMemberAPI curr, String aiCoreID, boolean keepPortrait, Random random, boolean addDrops) {
        //if (curr.getCaptain() == null) {
        AICoreOfficerPlugin plugin = new NAGhostCorePlugin();
        //PersonAPI person = OfficerManagerEvent.createOfficer(fleet.getFaction(), 20, true, SkillPickPreference.NON_CARRIER, random);
        PersonAPI person = plugin.createPerson(aiCoreID, curr.getFleetData().getFleet().getFaction().getId(), random);
        curr.setCaptain(person);
        //}

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
                curr.getCaptain().getMemoryWithoutUpdate().set("$chatterChar", chatter_ghost_matrix.pick());
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

                curr.getCaptain().getMemoryWithoutUpdate().set("$chatterChar", chatter_ghost_core.pick());
                break;
            case NightcrossID.GHOST_GRID_ID:
                if (!keepPortrait) {
                    curr.getCaptain().setPortraitSprite(Global.getSettings().getSpriteName("na_characters", "stargazergrid"));
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
                curr.getCaptain().getMemoryWithoutUpdate().set("$chatterChar", chatter_ghost_grid.pick());
                break;
        }
    }
}
