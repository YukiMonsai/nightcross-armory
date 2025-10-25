package data.scripts.campaign.plugins;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.enc.EncounterManager;
import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SectorThemeGenerator;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.loading.LoadingUtils;
import data.scripts.campaign.enc.NA_StargazerBH;
import data.scripts.campaign.enc.NA_StargazerDrifter;
import data.scripts.campaign.enc.NA_StargazerGhostManager;
import data.scripts.campaign.ids.NightcrossID;
import data.scripts.campaign.ids.NightcrossPeople;
import data.scripts.weapons.NA_PyrowispAutofireAI;
import data.scripts.weapons.ai.NA_HomingLaserAI;
import data.scripts.weapons.ai.NA_RKKVAI;
import data.scripts.weapons.ai.NA_corrosionmoteai;
import data.scripts.world.nightcross.*;
import exerelin.campaign.SectorManager;
import org.apache.log4j.Logger;
import org.dark.shaders.util.TextureData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.lazylib.JSONUtils;
import org.magiclib.util.MagicVariables;

import lunalib.lunaSettings.LunaSettings;
import yukimonsai.sicnightcross.scripts.world.addXO;
import yukimonsai.sicnightcross.skills.HitAndRun;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class NAModPlugin extends BaseModPlugin {

    public static boolean isExerelin = false;
    public static boolean hasLazyLib = false;
    public static boolean hasGraphicsLib = false;
    public static boolean hasMagicLib = false;
    public static boolean hasLunaLib = false;
    public static boolean hasSiC = false;


    public static final String MEMKEY_VERSION = "$nightcross_version";
    public static final String MEMKEY_INTIALIZED = "$nightcross_initialized";
    public static final String MEMKEY_FACTION_INTIALIZED = "$nightcross_faction_initialized";
    public static final String MEMKEY_INTIALIZEDSG = "$nightcross_stargazer_initialized";
    public static final String MEMKEY_INTIALIZEDBC = "$nightcross_blackcat_initialized";

    public static final String MEMKEY_PLACED_MARE_CRISIUM = "$nightcross_placed_mare_crisium2";
    public static final String MEMKEY_PLACED_STRINGOFPEARLS = "$nightcross_placed_sop2";
    public static final String MEMKEY_IBB_INITIALIZED = "$nightcross_ibb_initialized";
    public static final String MEMKEY_NCA_PERSON_ADMIN = "$nightcross_nca_person_admin";




    public static final String WHITELIST_OFF = "data/config/systemwhitelist/offensive_whitelist.csv";
    public static final String WHITELIST_DEF = "data/config/systemwhitelist/defensive_whitelist.csv";
    public static final String WHITELIST_MOV = "data/config/systemwhitelist/movement_whitelist.csv";
    public static final String WHITELIST_UTI = "data/config/systemwhitelist/utility_whitelist.csv";
    static Exception failedLoad = null;
    public static Logger log = Global.getLogger(HitAndRun.class);
    public static HashMap<String, Boolean> system_whitelist_movement = new HashMap<>();
    public static HashMap<String, Boolean> system_whitelist_offensive = new HashMap<>();
    public static HashMap<String, Boolean> system_whitelist_defensive = new HashMap<>();
    public static HashMap<String, Boolean> system_whitelist_utility = new HashMap<>();
    static {
        try {

            log.info("Loading whitelisted factions");
            JSONArray systems_off = Global.getSettings().getMergedSpreadsheetDataForMod("id",
                    WHITELIST_OFF, "Nightcross");
            JSONArray systems_def = Global.getSettings().getMergedSpreadsheetDataForMod("id",
                    WHITELIST_DEF, "Nightcross");
            JSONArray systems_mov = Global.getSettings().getMergedSpreadsheetDataForMod("id",
                    WHITELIST_MOV, "Nightcross");
            JSONArray systems_uti = Global.getSettings().getMergedSpreadsheetDataForMod("id",
                    WHITELIST_UTI, "Nightcross");

            for(int i = 0; i < systems_off.length(); i++) {
                JSONObject row = systems_off.getJSONObject(i);
                system_whitelist_offensive.put(row.getString("id"), true);
                log.info("Added to offensive system whitelist: " + row.getString("id"));
            }
            for(int i = 0; i < systems_off.length(); i++) {
                JSONObject row = systems_off.getJSONObject(i);
                system_whitelist_defensive.put(row.getString("id"), true);
                log.info("Added to defensive system whitelist: " + row.getString("id"));
            }
            for(int i = 0; i < systems_off.length(); i++) {
                JSONObject row = systems_off.getJSONObject(i);
                system_whitelist_movement.put(row.getString("id"), true);
                log.info("Added to movement system whitelist: " + row.getString("id"));
            }
            for(int i = 0; i < systems_uti.length(); i++) {
                JSONObject row = systems_off.getJSONObject(i);
                system_whitelist_utility.put(row.getString("id"), true);
                log.info("Added to utility system whitelist: " + row.getString("id"));
            }

        } catch (JSONException | IOException ex) {
            log.error("Failed to load Nightcross Armory's ship system whitelist!!!", ex);
            // note: throwing an exception in static block causes class to fail to load with a NoClassDefFoundError
            // — and more critically, prevents logging anything useful
            // throw new RuntimeException();
            failedLoad = ex;
        }
    }

    static {
        //generators.add(new SpecialThemeGenerator());
        SectorThemeGenerator.generators.add(new NA_NightcrossThemeGenerator());
    }


    private static void initNA() {

        ProcgenUsedNames.notifyUsed("Pascal");
        ProcgenUsedNames.notifyUsed("Nightcross");

        NightcrossPeople.create();



        NAGen.initFactionRelationships(Global.getSector());
        Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_FACTION_INTIALIZED, true);

        if (!hasLunaLib || NA_SettingsListener.na_pascal_system) {
            NAGen gen = new NAGen();
            gen.generate(Global.getSector());
            Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_INTIALIZED, true);
        }

        if (!hasLunaLib || NA_SettingsListener.na_stargazer_spawn) {
            NA_StargazerGen gen2 = new NA_StargazerGen();
            gen2.init(Global.getSector());
            Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_INTIALIZEDSG, true);
        }


    }

    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        if (NightcrossID.HOMING_LASER_ID.contentEquals(missile.getProjectileSpecId())
            || NightcrossID.HOMING_LASER_MEDIUM_ID.contentEquals(missile.getProjectileSpecId())) {
            return new PluginPick<MissileAIPlugin>(new NA_HomingLaserAI(missile, launchingShip),
                    CampaignPlugin.PickPriority.MOD_SET);
        } else if (NightcrossID.RKKV_ID.contentEquals(missile.getProjectileSpecId())) {
            return new PluginPick<MissileAIPlugin>(new NA_RKKVAI(missile, launchingShip),
                    CampaignPlugin.PickPriority.MOD_SET);
        }else if ("na_corrosionbeambullet_shot".contentEquals(missile.getProjectileSpecId())) {
            return new PluginPick<MissileAIPlugin>(new NA_corrosionmoteai(missile, launchingShip),
                    CampaignPlugin.PickPriority.MOD_SET);
        }else if ("naai_starkiller_missile".contentEquals(missile.getProjectileSpecId())) {
            return new PluginPick<MissileAIPlugin>(new NA_corrosionmoteai(missile, launchingShip),
                    CampaignPlugin.PickPriority.MOD_SET);
        }

        return null;
    }

    @Override
    public PluginPick<AutofireAIPlugin> pickWeaponAutofireAI(WeaponAPI weapon) {
        if (weapon.getId().equals("na_pyrowisp") || weapon.getId().equals("na_pyrowisp_medium")) {
            return new PluginPick<AutofireAIPlugin>(new NA_PyrowispAutofireAI(weapon), CampaignPlugin.PickPriority.MOD_SPECIFIC);
        }
        return null;
    }

    public static final boolean HAVE_LUNALIB = Global.getSettings().getModManager().isModEnabled("lunalib");
    @Override
    public void onApplicationLoad() {


        EncounterManager.CREATORS.add(new NA_StargazerBH());
        EncounterManager.CREATORS.add(new NA_StargazerDrifter());

        isExerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        {
            hasLazyLib = Global.getSettings().getModManager().isModEnabled("lw_lazylib");
            if (!hasLazyLib) {
                throw new RuntimeException("Nightcross Armory requires LazyLib!" +
                        "\nGet it at http://fractalsoftworks.com/forum/index.php?topic=5444");
            }
            hasGraphicsLib = Global.getSettings().getModManager().isModEnabled("shaderLib");
            if (hasGraphicsLib) {
                org.dark.shaders.util.ShaderLib.init();
                org.dark.shaders.light.LightData.readLightDataCSV("data/lights/na_light_data.csv");
                TextureData.readTextureDataCSV("data/lights/na_texture_data.csv");
            }
            if (!hasGraphicsLib) {
                throw new RuntimeException("Nightcross Armory requires GraphicsLib!" +
                        "\nGet it at http://fractalsoftworks.com/forum/index.php?topic=10982");
            }
            hasLunaLib = Global.getSettings().getModManager().isModEnabled("lunalib");
            if (hasLunaLib) {
                LunaSettings.addSettingsListener(new NA_SettingsListener());
            }
            hasSiC = Global.getSettings().getModManager().isModEnabled("second_in_command");
            if (hasSiC) {
                //LunaSettings.addSettingsListener(new NA_SettingsListener());
            }
            hasMagicLib = Global.getSettings().getModManager().isModEnabled("MagicLib");
            if (!hasMagicLib) {
                throw new RuntimeException("Nightcross Armory requires MagicLib!" +
                        "\nGet it at http://fractalsoftworks.com/forum/index.php?topic=13718.0");
            }

        }


    }


    @Override
    public void onNewGame() {

        Global.getSector().registerPlugin(new NACampaignPlugin());
        initNA();
    }


    @Override
    public void onGameLoad(boolean newGame) {
        Global.getSector().registerPlugin(new NACampaignPlugin());

        NightcrossPeople.create();

        if (!isExerelin || SectorManager.getManager().isCorvusMode()) { // Till Nex
            addToOngoingGame();
        }

        if (!Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_IBB_INITIALIZED)) {
            addIBB();
            Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_IBB_INITIALIZED, true);
        }

        if (!Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_VERSION)) {
            Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_VERSION, 0.1);
        }


        NAUtils.NAGenPeople();


        if (!hasLunaLib || NA_SettingsListener.na_bounties) {
            Global.getSector().getMemoryWithoutUpdate().set("$na_enableaibounties", true);
        } else
            Global.getSector().getMemoryWithoutUpdate().unset("$na_enableaibounties");

        syncScripts();
    }

    private void syncScripts() {

        GenericPluginManagerAPI plugins = Global.getSector().getGenericPlugins();
        if (!plugins.hasPlugin(NA_NightcrossDefenderPlugin.class)) {
            plugins.addPlugin(new NA_NightcrossDefenderPlugin(), true);
        }
        if (!plugins.hasPlugin(NA_NightcrossHumanDefenderPlugin.class)) {
            plugins.addPlugin(new NA_NightcrossHumanDefenderPlugin(), true);
        }

        if(!Global.getSector().getListenerManager().hasListenerOfClass(addXO.class))
            Global.getSector().getListenerManager().addListener(new addXO(), false);

        SectorAPI sector = Global.getSector();
        if (!sector.hasScript(NA_StargazerGhostManager.class)) {
            sector.addScript(new NA_StargazerGhostManager());
        }

        // add no_drop_salvage tags to weapons
        // remove rare_bp from ships
        // make the blueprint packages hidden

        if (hasLunaLib && NA_SettingsListener.na_faction_gear) {
            List<WeaponSpecAPI> weapons = Global.getSettings().getAllWeaponSpecs();
            List<FighterWingSpecAPI> wings = Global.getSettings().getAllFighterWingSpecs();
            List<ShipHullSpecAPI> ships = Global.getSettings().getAllShipHullSpecs();
            List<SpecialItemSpecAPI> bps = Global.getSettings().getAllSpecialItemSpecs();
            for (WeaponSpecAPI weapon : weapons) {
                if (weapon.hasTag("nightcross_bp_rare")
                        || weapon.hasTag("nightcross_bp_fast")
                        || weapon.hasTag("nightcross_bp_heavy")) weapon.addTag("no_drop_salvage");
                if (NA_SettingsListener.na_faction_gearrare
                        && weapon.hasTag("nightcross_bp_restricted")) weapon.addTag("no_drop_salvage");

            }
            for (FighterWingSpecAPI wing : wings) {
                if (wing.hasTag("nightcross")) wing.addTag("no_drop_salvage");
                if (wing.hasTag("nightcross") && wing.hasTag("rare_bp")) wing.addTag("no_drop");
            }
            for (ShipHullSpecAPI ship : ships) {
                if (ship.hasTag("nightcross") && ship.hasTag("rare_bp")) ship.addTag("no_drop");
            }
            for (SpecialItemSpecAPI bp : bps) {
                if (bp.hasTag("nightcross")) bp.getTags().add("no_drop");
            }
        }
        if (hasLunaLib && NA_SettingsListener.na_faction_merc) {
            List<WeaponSpecAPI> weapons = Global.getSettings().getAllWeaponSpecs();
            List<FighterWingSpecAPI> wings = Global.getSettings().getAllFighterWingSpecs();
            List<ShipHullSpecAPI> ships = Global.getSettings().getAllShipHullSpecs();
            for (WeaponSpecAPI weapon : weapons) {
                if (weapon.hasTag("merc")) weapon.getTags().remove("merc");
            }
            for (FighterWingSpecAPI wing : wings) {
                if (wing.hasTag("merc")) wing.getTags().remove("merc");
            }
            for (ShipHullSpecAPI ship : ships) {
                if (ship.hasTag("merc")) ship.getTags().remove("merc");
            }
        }
    }

    protected void addToOngoingGame() {
        if (!isExerelin || SectorManager.getManager().isCorvusMode()) {


            NAGen gen = new NAGen();



            NAGen.initFactionRelationships(Global.getSector());
            Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_FACTION_INTIALIZED, true);

            if (!hasLunaLib || NA_SettingsListener.na_pascal_system) {
                if (!Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_INTIALIZED)) {
                    gen.generate(Global.getSector());
                    Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_INTIALIZED, true);
                }
            }



            if (!hasLunaLib || NA_SettingsListener.na_ships_spawn) {
                if (!Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_PLACED_MARE_CRISIUM)) {
                    gen.place_mare(Global.getSector());
                    Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_PLACED_MARE_CRISIUM, true);
                }
                if (!Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_PLACED_STRINGOFPEARLS)) {
                    gen.place_sop(Global.getSector());
                    Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_PLACED_STRINGOFPEARLS, true);
                }
            }



            if (!hasLunaLib || NA_SettingsListener.na_stargazer_spawn) {
                NA_StargazerGen gensg = new NA_StargazerGen();
                if (!Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_INTIALIZEDSG)) {
                    gensg.init(Global.getSector());
                    gensg.generate(Global.getSector());
                    Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_INTIALIZEDSG, true);
                }
            }
            //if (!hasLunaLib || NA_SettingsListener.na_stargazer_spawn) {
            NA_BlackcatGen genbc = new NA_BlackcatGen();
                if (!Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_INTIALIZEDBC)) {
                    genbc.init(Global.getSector());
                    genbc.generate(Global.getSector());
                    if (genbc.BlackcatGenerated)
                        Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_INTIALIZEDBC, true);
                }
            //}




            //MarketHelpers.generateMarketsFromEconJson("na_pascal");
        }
    }

    protected void addIBB() {
        // Placeholder
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        if (!MagicVariables.getIBB()) {
            // Do IBB in future
            addIBB();
        }
        Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_VERSION, 0.1);
        Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_IBB_INITIALIZED, 0.1);

        NAUtils.NAGenPeople();
    }


    @Override
    public void onNewGameAfterProcGen() {

        // any vanilla generated systems with black dwarf or wolf rayet gets custom music

        for (StarSystemAPI system : Global.getSector().getStarSystems()) {
            if (system.getStar() != null && system.getStar().getSpec() != null) {
                if (system.getStar().getSpec().getName().equals("na_blackdwarf2")
                    || system.getStar().getSpec().getName().equals("na_blackdwarf")) {
                    system.getMemoryWithoutUpdate().set("$musicSetId","kocaeli_blackhole");
                } else if (system.getStar().getSpec().getName().equals("star_rayet")) {
                    system.getMemoryWithoutUpdate().set("$musicSetId","nightcross_pascal_music");
                }
            }
        }

        NAGen gen = new NAGen();

        if (!hasLunaLib || NA_SettingsListener.na_ships_spawn) {
            gen.place_mare(Global.getSector());
            Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_PLACED_MARE_CRISIUM, true);
            gen.place_sop(Global.getSector());
            Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_PLACED_STRINGOFPEARLS, true);
        }




        if (!NAModPlugin.hasLunaLib
                || NA_SettingsListener.na_stargazer_spawn)
        {
            NA_StargazerGen gensg = new NA_StargazerGen();
            gensg.init(Global.getSector());
            gensg.generate(Global.getSector());
            Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_INTIALIZEDSG, true);
        }

        //if (!NAModPlugin.hasLunaLib
        //       || NA_SettingsListener.na_stargazer_spawn)
        //{
        NA_BlackcatGen genbc = new NA_BlackcatGen();
        genbc.init(Global.getSector());
        genbc.generate(Global.getSector());
        if (genbc.BlackcatGenerated)
            Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_INTIALIZEDBC, true);
        //}
    }

    @Override
    public PluginPick<ShipAIPlugin> pickShipAI(FleetMemberAPI member, ShipAPI ship) {

        boolean isSupportDrone = false;
        boolean isAttackDrone = false;

        switch (ship.getHullSpec().getBaseHullId()) {
            case NightcrossID.NA_SHIELDBATTERY_ID:
            case NightcrossID.NA_LUMINOUSDRILL_REPAIR:
                isSupportDrone = true;
                break;
            case NightcrossID.NA_LUMINOUSDRILL:
                isAttackDrone = true;
                break;
            default:
                isSupportDrone = false;
                isAttackDrone = false;
        }

        // TODO

        if (isSupportDrone) {
            return null;
            /*ShipAIConfig droneAI = new ShipAIConfig();
            droneAI.alwaysStrafeOffensively = false;
            droneAI.turnToFaceWithUndamagedArmor = false;
            droneAI.burnDriveIgnoreEnemies = true;
            droneAI.personalityOverride = "timid";

            return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, droneAI), CampaignPlugin.PickPriority.MOD_SET);*/
        } else if (isAttackDrone) {
            /*ShipAIConfig droneAI = new ShipAIConfig();
            droneAI.alwaysStrafeOffensively = true;
            droneAI.turnToFaceWithUndamagedArmor = false;
            droneAI.personalityOverride = "reckless";

            return new PluginPick<ShipAIPlugin>(Global.getSettings().createDefaultShipAI(ship, droneAI), CampaignPlugin.PickPriority.MOD_SET);*/
            return null;
        }
        return null;


    }
}

