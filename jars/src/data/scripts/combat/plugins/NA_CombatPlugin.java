package data.scripts.combat.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.ai.system.V;
import data.scripts.campaign.ids.NightcrossID;
import data.scripts.campaign.plugins.NAModPlugin;
import data.scripts.campaign.plugins.NAUtils;
import data.scripts.campaign.plugins.NA_SettingsListener;
import lunalib.LunaLibPlugin;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.ReflectionUtils;
import org.magiclib.plugins.MagicRenderPlugin;
import org.magiclib.util.MagicRender;
import org.magiclib.util.MagicUI;

import java.awt.*;
import java.util.*;
import java.util.List;

public class NA_CombatPlugin implements EveryFrameCombatPlugin {

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
         if (musicPhases > 0) {
             Global.getSoundPlayer().setSuspendDefaultMusicPlayback(true);
             musicalOverrideDone = true;
             doMusic(amount);
         }
    }


    public static String customBattleMusic = null;

    public static int musicPhase = 0;
    public static boolean musicalOverrideDone = false;
    public static int lastPhase = -1;
    public static int musicPhases = 1;


    public static String bossfight = null;

    public static String bossBuff = "flux";
    public static String bossfight_insert = null;

    public static IntervalUtil musicTimer = new IntervalUtil(.1f, .1f);
    public static float adjustVolRate = 0.4f; // in fractions of second


    public void doMusic(float amount) {
        musicTimer.advance(amount);

        if (musicPhase == -1) {
            List<DeployedFleetMemberAPI> members = Global.getCombatEngine().getFleetManager(1).getDeployedCopyDFM();
            for (DeployedFleetMemberAPI member: members) {
                if (member.getShip() != null && member.getShip().getHardFluxLevel() > 0) {
                    musicPhase++;
                    break;
                }
            }
        }

        boolean skipCheck = false;
        if (bossfight != null && musicPhase <= 0) {
            int destroyed = Global.getCombatEngine().getFleetManager(1).getDestroyedCopy().size();
            destroyed += Global.getCombatEngine().getFleetManager(1).getDisabledCopy().size();

            if (destroyed > Global.getCombatEngine().getFleetManager(1).getAllEverDeployedCopy().size() * 0.5f + Global.getCombatEngine().getFleetManager(1).getReservesCopy().size() * 0.9f) {

                if (musicPhase < 0) musicPhase = 0;
                musicPhase++;
                Global.getSoundPlayer().playCustomMusic(1, 1, bossfight_insert, false);
                lastPhase = musicPhase;
                skipCheck = true;


                if (Global.getCombatEngine() != null && customBattleMusic == null) {
                    CombatFleetManagerAPI manager = Global.getCombatEngine().getFleetManager(1);
                    if (manager != null) {
                        List<FleetMemberAPI> list = manager.getDeployedCopy();
                        if (list.isEmpty()) list = manager.getReservesCopy();
                        if (list.isEmpty()) list = manager.getDestroyedCopy();
                        if (list.isEmpty()) list = manager.getDisabledCopy();
                        if (list.isEmpty()) list = manager.getRetreatedCopy();
                        if (!list.isEmpty()) {
                            FleetDataAPI data = list.get(0).getFleetData();
                            if (data != null) {
                                CampaignFleetAPI fleet = data.getFleet();
                                BossFadeInPlugin plugin = new BossFadeInPlugin(bossfight, fleet, 0.25f, 3f, 270, "Existence", new Color(255, 55, 90, 70));
                                Global.getCombatEngine().addPlugin(plugin);
                                musicTimer = new IntervalUtil(5.0f, 5.0f);
                            }
                        }
                    }
                }

            }
        }

        if (musicTimer.intervalElapsed() && !skipCheck) {
            musicTimer = new IntervalUtil(1.0f, 1.0f);




            // advance music up if it is the primary, down if it is not
            float delta = Global.getCombatEngine().getElapsedInLastFrame();

            String song = (customBattleMusic != null && musicPhase >= 0) ? customBattleMusic : "na_silence_dummy";
            boolean unexcepted_track = false;
            if (Global.getCombatEngine() != null && customBattleMusic == null) {
                CombatFleetManagerAPI manager = Global.getCombatEngine().getFleetManager(1);
                if (manager != null) {
                    List<FleetMemberAPI> list = manager.getDeployedCopy();
                    if (list.isEmpty()) list = manager.getReservesCopy();
                    if (list.isEmpty()) list = manager.getDestroyedCopy();
                    if (list.isEmpty()) list = manager.getDisabledCopy();
                    if (list.isEmpty()) list = manager.getRetreatedCopy();
                    if (!list.isEmpty()) {
                        FleetDataAPI data = list.get(0).getFleetData();
                        if (data != null) {
                            CampaignFleetAPI fleet = data.getFleet();
                            if (fleet != null ) {
                                MemoryAPI mem = fleet.getMemoryWithoutUpdate();
                                if (musicPhase >= 0) {
                                    if (mem.contains("$na_customCombatMusic" + (musicPhase + 1))) {
                                        song = mem.getString("$na_customCombatMusic" + (musicPhase + 1));
                                    }
                                }

                                if (Global.getSoundPlayer().getCurrentMusicId() != null) {
                                    if (!Global.getSoundPlayer().getCurrentMusicId().equals(song + ".ogg") && !mem.contains("$na_customCombatMusicException_" + Global.getSoundPlayer().getCurrentMusicId())) {
                                         unexcepted_track = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (lastPhase != musicPhase || unexcepted_track) {
                lastPhase = musicPhase;



                Global.getSoundPlayer().playCustomMusic(1, 1, song, true);

            }
        }


    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {

    }

    @Override
    public void renderInUICoords(ViewportAPI viewport) {
    }

    @Override
    public void init(CombatEngineAPI engine) {
        musicPhases = 0;
        customBattleMusic = null;

        if (Global.getCombatEngine().getListenerManager().hasListenerOfClass(NA_CombatDamageListener.class)) {
            Global.getCombatEngine().getListenerManager().addListener(new NA_CombatDamageListener());
        }

        if (Global.getCombatEngine() != null) {
            CombatFleetManagerAPI manager = Global.getCombatEngine().getFleetManager(1);
            if (manager != null) {
                List<FleetMemberAPI> list = manager.getDeployedCopy();
                if (list.isEmpty()) list = manager.getReservesCopy();
                if (list.isEmpty()) list = manager.getDestroyedCopy();
                if (list.isEmpty()) list = manager.getDisabledCopy();
                if (list.isEmpty()) list = manager.getRetreatedCopy();
                if (!list.isEmpty()) {
                    FleetDataAPI data = list.get(0).getFleetData();
                    if (data != null) {
                        CampaignFleetAPI fleet = data.getFleet();
                        if (fleet != null ) {
                            MemoryAPI mem = fleet.getMemoryWithoutUpdate();
                            if (mem.contains("$na_maculafight")) {
                                bossfight = "na_macula_proto_stargazer";
                                if (mem.contains("$na_bossfightInsert")) {
                                    bossfight_insert = mem.getString("$na_bossfightInsert");
                                }
                            }
                            if (mem.contains("$na_customMusicPhases")) {
                                musicPhases = mem.getInt("$na_customMusicPhases");
                                Global.getSoundPlayer().playCustomMusic(1, 1, "na_silence_dummy", true);
                            } else if (fleet.getFaction().equals("na_earth")) {
                                Global.getSoundPlayer().playCustomMusic(1, 1, "na_silence_dummy", true);
                                musicPhases = 1;
                                customBattleMusic = "na_earth_battle";
                            }
                        }
                    }
                }
            }

        }

        musicPhase = -1;
        lastPhase = -1;

        musicTimer.advance(0.1f);
    }


    public static class BossFadeInPlugin extends BaseEveryFrameCombatPlugin {
        float elapsed = 0f;
        ShipAPI [] ships = null;
        CollisionClass collisionClass;

        String variantId;
        float delay;
        float fadeInTime;
        float angle;
        int owner = 1;
        Color color = new Color(255, 255, 255, 50);
        String name = "Boss";
        CampaignFleetAPI fleet = null;

        public BossFadeInPlugin(String variantId, CampaignFleetAPI fleet, float delay, float fadeInTime, float angle, String name, Color color) {
            this.variantId = variantId;
            this.delay = delay;
            this.fadeInTime = fadeInTime;
            this.angle = angle;
            this.fleet = fleet;
            this.name = name;
            this.color = color;

        }


        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (Global.getCombatEngine().isPaused()) return;

            elapsed += amount;
            if (elapsed < delay) return;

            CombatEngineAPI engine = Global.getCombatEngine();

            if (ships == null) {
                float facing = angle;
                boolean nearplayer = (Global.getCombatEngine().getPlayerShip() != null && Global.getCombatEngine().getPlayerShip().isAlive());
                Vector2f loc = nearplayer ?
                        MathUtils.getPointOnCircumference(Global.getCombatEngine().getPlayerShip().getLocation(), (float) (Math.random() * 200 + 500 + Global.getCombatEngine().getPlayerShip().getCollisionRadius()),
                                (float) (Math.random() * 360)) : Misc.ZERO;
                for (int i = 0; i < 10; i++) {
                    if (!NAUtils.getEntitiesWithinRange(loc, 400).isEmpty())
                        loc = nearplayer ?
                            MathUtils.getPointOnCircumference(Global.getCombatEngine().getPlayerShip().getLocation(), (float) (Math.random() * 200 + 500 + Global.getCombatEngine().getPlayerShip().getCollisionRadius()),
                                    (float) (Math.random() * 360)) : MathUtils.getPointOnCircumference(Misc.ZERO, 1000, (float) (Math.random() * 360));
                }
                CombatFleetManagerAPI fleetManager = engine.getFleetManager(owner);
                boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
                fleetManager.setSuppressDeploymentMessages(true);
                if (variantId.endsWith("_wing")) {
                    FighterWingSpecAPI spec = Global.getSettings().getFighterWingSpec(variantId);
                    ships = new ShipAPI[spec.getNumFighters()];

                    AICoreOfficerPlugin plugin = Misc.getAICoreOfficerPlugin(NightcrossID.GHOST_MATRIX_ID);
                    PersonAPI captain = plugin.createPerson(NightcrossID.GHOST_MATRIX_ID, fleet.getFaction().getId(), new Random());

                    captain.setPersonality(Personalities.RECKLESS); // doesn't matter for fighters
                    captain.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
                    captain.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
                    captain.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                    captain.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
                    captain.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
                    captain.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
                    captain.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
                    captain.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 2);
                    captain.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);

                    ShipAPI leader = engine.getFleetManager(owner).spawnShipOrWing(variantId, loc, facing, 0f, captain);
                    for (int i = 0; i < ships.length; i++) {
                        ships[i] = leader.getWing().getWingMembers().get(i);
                        ships[i].getLocation().set(loc);
                    }
                    collisionClass = ships[0].getCollisionClass();
                } else {
                    ships = new ShipAPI[1];


                    AICoreOfficerPlugin plugin = Misc.getAICoreOfficerPlugin(NightcrossID.GHOST_MATRIX_ID);
                    PersonAPI captain = plugin.createPerson(NightcrossID.GHOST_MATRIX_ID, fleet.getFaction().getId(), new Random());

                    captain.setPersonality(Personalities.RECKLESS); // doesn't matter for fighters
                    captain.getStats().setSkillLevel(Skills.POINT_DEFENSE, 2);
                    captain.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
                    captain.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
                    captain.getStats().setSkillLevel(Skills.FIELD_MODULATION, 2);
                    captain.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
                    captain.getStats().setSkillLevel(Skills.DAMAGE_CONTROL, 2);
                    captain.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
                    captain.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 2);
                    captain.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);

                    ships[0] = engine.getFleetManager(owner).spawnShipOrWing(variantId, loc, facing, 0f, captain);
                }
                ships[0].setName(name);
                fleetManager.setSuppressDeploymentMessages(wasSuppressed);
                collisionClass = ships[0].getCollisionClass();

            }



            float progress = (elapsed - delay) / fadeInTime;
            if (progress > 1f) progress = 1f;

            for (int i = 0; i < ships.length; i++) {
                ShipAPI ship = ships[i];
                ship.setAlphaMult(progress);

                if (progress < 0.5f) {
                    ship.blockCommandForOneFrame(ShipCommand.ACCELERATE);
                    ship.blockCommandForOneFrame(ShipCommand.TURN_LEFT);
                    ship.blockCommandForOneFrame(ShipCommand.TURN_RIGHT);
                    ship.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
                    ship.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
                }

                ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
                ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
                ship.blockCommandForOneFrame(ShipCommand.FIRE);
                ship.blockCommandForOneFrame(ShipCommand.PULL_BACK_FIGHTERS);
                ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
                ship.setHoldFireOneFrame(true);
                ship.setHoldFire(true);


                ship.setCollisionClass(CollisionClass.NONE);
                ship.getMutableStats().getHullDamageTakenMult().modifyMult("ShardSpawnerInvuln", 0f);
                if (progress < 0.5f) {
                    ship.getVelocity().set(Misc.ZERO);
                } else if (progress > 0.75f){
                    ship.setCollisionClass(collisionClass);
                    ship.getMutableStats().getHullDamageTakenMult().unmodifyMult("ShardSpawnerInvuln");
                }

//					Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(source.getLocation(), ship.getLocation()));
//					dir.scale(amount * 50f * progress);
//					Vector2f.add(ship.getLocation(), dir, ship.getLocation());


                float jitterLevel = progress;
                if (jitterLevel < 0.5f) {
                    jitterLevel *= 2f;
                } else {
                    jitterLevel = (1f - jitterLevel) * 2f;
                }

                float jitterRange = 1f - progress;
                float maxRangeBonus = 50f;
                float jitterRangeBonus = jitterRange * maxRangeBonus;

                ship.setJitter(this, color, jitterLevel, 25, 0f, jitterRangeBonus);
            }

            if (elapsed > fadeInTime) {
                for (int i = 0; i < ships.length; i++) {
                    ShipAPI ship = ships[i];
                    ship.setAlphaMult(1f);
                    ship.setHoldFire(false);
                    ship.setCollisionClass(collisionClass);
                    ship.getMutableStats().getHullDamageTakenMult().unmodifyMult("ShardSpawnerInvuln");
                    if (bossBuff.equals("flux")) {
                        ship.getMutableStats().getFluxDissipation().modifyPercent("NA_BossFight", 30);
                        ship.getMutableStats().getFluxCapacity().modifyPercent("NA_BossFight", 50);
                    }
                }
                engine.removePlugin(this);
            }
        }
    }
}
