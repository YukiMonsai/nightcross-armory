package data.scripts.combat.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SoundAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.combat.ai.system.V;
import data.scripts.campaign.plugins.NAModPlugin;
import data.scripts.campaign.plugins.NA_SettingsListener;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicRenderPlugin;
import org.magiclib.util.MagicRender;
import org.magiclib.util.MagicUI;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class NA_CombatPlugin implements EveryFrameCombatPlugin {

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (musicPhases > 0)
            doMusic(amount);
    }

    public static float[] musicVolume = {0};
    public static SoundAPI[] music = {null};
    public static float[] musicVolumeVocals = {0};

    public static String customBattleMusic = null;

    public static int musicPhase = 0;
    public static int musicPhases = 1;

    public static IntervalUtil musicTimer = new IntervalUtil(.1f, .1f);
    public static float adjustVolRate = 0.4f; // in fractions of second


    public void doMusic(float amount) {
        /*musicTimer.advance(amount);
        if (musicTimer.intervalElapsed()) {
            musicTimer = new IntervalUtil(0.1f, 0.1f);

        }*/

        // advance music up if it is the primary, down if it is not
        float delta = Global.getCombatEngine().getElapsedInLastFrame();
        boolean fadeOut = false;
        for (int i = 0; i < musicPhases; i++) {
            float orig = musicVolume[i];
            if (i != musicPhase) {
                musicVolume[i] = Math.max(0f, musicVolume[i] - adjustVolRate * delta);
                fadeOut = true;
            }
            if (orig != musicVolume[i]) {
                if (music[i] != null) {
                    if (musicVolume[i] == 0) {
                        music[i].stop();
                        music[i] = null;
                    }
                    else music[i].setVolume(musicVolume[i]);
                }
            }
        }
        if (!fadeOut)
            for (int i = 0; i < musicPhases; i++) {
                float orig = musicVolume[i];
                if (i == musicPhase) {
                    musicVolume[i] = Math.min(1f, musicVolume[i] + adjustVolRate * delta);
                }
                if (orig != musicVolume[i]) {
                    if (music[i] != null) music[i].setVolume(musicVolume[i]);
                    if ((orig == 0 || music[i] == null) && musicVolume[i] > 0) {
                        if (music[i] != null) {
                            music[i].stop();
                            music[i] = null;
                        }
                        String song = customBattleMusic != null ? customBattleMusic : "mekaloton_Red_Maskq";
                        if (Global.getCombatEngine() != null && customBattleMusic == null) {
                            CombatFleetManagerAPI manager = Global.getCombatEngine().getFleetManager(1);
                            if (manager != null && manager.getFleetCommander() != null && manager.getFleetCommander().getFleet() != null) {
                                CampaignFleetAPI fleet = manager.getFleetCommander().getFleet();

                                if (fleet != null) {
                                    MemoryAPI mem = fleet.getMemoryWithoutUpdate();
                                    if (mem.contains("$na_customCombatMusic" + (musicPhase + 1))) {
                                        song = mem.getString("$na_customCombatMusic" + (musicPhase + 1));
                                    }
                                }

                            }
                        }
                        music[i] = Global.getSoundPlayer().playUISound(
                                song, 1, musicVolume[i]
                        );
                    }
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

        if (Global.getCombatEngine() != null) {
            CombatFleetManagerAPI manager = Global.getCombatEngine().getFleetManager(1);
            if (manager != null && manager.getFleetCommander() != null && manager.getFleetCommander().getFleet() != null) {
                CampaignFleetAPI fleet = manager.getFleetCommander().getFleet();

                if (fleet != null) {
                    MemoryAPI mem = fleet.getMemoryWithoutUpdate();
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

        musicVolume = new float[musicPhases];
        musicVolumeVocals = new float[musicPhases];
        music = new SoundAPI[musicPhases];
        musicPhase = 0;

        musicTimer.advance(0.1f);
    }
}
