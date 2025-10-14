package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.GenericPluginManagerAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.enc.EncounterManager;
import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SectorThemeGenerator;
import data.scripts.campaign.enc.NA_StargazerBH;
import data.scripts.campaign.ids.NightcrossID;
import data.scripts.campaign.ids.NightcrossPeople;
import data.scripts.campaign.plugins.NACampaignPlugin;
import data.scripts.weapons.NA_PyrowispAutofireAI;
import data.scripts.weapons.ai.NA_HomingLaserAI;
import data.scripts.weapons.ai.NA_RKKVAI;
import data.scripts.weapons.ai.NA_corrosionmoteai;
import data.scripts.world.MarketHelpers;
import data.scripts.world.nightcross.*;
import exerelin.campaign.SectorManager;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;
import org.dark.shaders.light.LightData;
import org.magiclib.ai.MagicMissileAI;
import org.magiclib.util.MagicVariables;

public class NAModPlugin extends BaseModPlugin {

    public static boolean isExerelin = false;
    public static boolean hasLazyLib = false;
    public static boolean hasGraphicsLib = false;
    public static boolean hasMagicLib = false;

    public static final String MEMKEY_VERSION = "$nightcross_version";
    public static final String MEMKEY_INTIALIZED = "$nightcross_initialized";
    public static final String MEMKEY_INTIALIZEDSG = "$nightcross_stargazer_initialized";
    public static final String MEMKEY_PLACED_MARE_CRISIUM = "$nightcross_placed_mare_crisium2";
    public static final String MEMKEY_PLACED_STRINGOFPEARLS = "$nightcross_placed_sop2";
    public static final String MEMKEY_IBB_INITIALIZED = "$nightcross_ibb_initialized";
    public static final String MEMKEY_NCA_PERSON_ADMIN = "$nightcross_nca_person_admin";


    static {
        //generators.add(new SpecialThemeGenerator());
        SectorThemeGenerator.generators.add(new NA_NightcrossThemeGenerator());
    }


    private static void initNA() {

        ProcgenUsedNames.notifyUsed("Pascal");
        ProcgenUsedNames.notifyUsed("Nightcross");

        NightcrossPeople.create();

        NAGen gen = new NAGen();
        gen.generate(Global.getSector());
        Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_INTIALIZED, true);


        NA_StargazerGen gen2 = new NA_StargazerGen();
        gen2.init(Global.getSector());
        Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_INTIALIZEDSG, true);


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

    @Override
    public void onApplicationLoad() {

        EncounterManager.CREATORS.add(new NA_StargazerBH());

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
    }

    protected void addToOngoingGame() {
        if (!isExerelin || SectorManager.getManager().isCorvusMode()) {


            NAGen gen = new NAGen();
            if (!Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_INTIALIZED)) {
                gen.generate(Global.getSector());
                Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_INTIALIZED, true);
            }
            if (!Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_PLACED_MARE_CRISIUM)) {
                gen.place_mare(Global.getSector());
                Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_PLACED_MARE_CRISIUM, true);
            }
            if (!Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_PLACED_STRINGOFPEARLS)) {
                gen.place_sop(Global.getSector());
                Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_PLACED_STRINGOFPEARLS, true);
            }


            NA_StargazerGen gensg = new NA_StargazerGen();
            if (!Global.getSector().getMemoryWithoutUpdate().contains(MEMKEY_INTIALIZEDSG)) {
                gensg.init(Global.getSector());
                gensg.generate(Global.getSector());
                Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_INTIALIZEDSG, true);
            }

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

        NAGen gen = new NAGen();
        gen.place_mare(Global.getSector());
        Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_PLACED_MARE_CRISIUM, true);
        gen.place_sop(Global.getSector());
        Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_PLACED_STRINGOFPEARLS, true);


        NA_StargazerGen gensg = new NA_StargazerGen();
        gensg.generate(Global.getSector());
        Global.getSector().getMemoryWithoutUpdate().set(MEMKEY_INTIALIZEDSG, true);
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

