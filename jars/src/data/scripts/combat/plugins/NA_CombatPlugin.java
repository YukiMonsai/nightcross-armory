package data.scripts.combat.plugins;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
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

    // TODO add optional display for other side (for tournaments maybe?)

    public static Color TEXT_COLOR_OFF = new Color(255, 255, 255, 115);
    public static Color TEXT_COLOR_ON = new Color(255, 255, 255, 255);
    public static Color TEXT_COLOR_HIGHLIGHT = new Color(255, 255, 255, 180);

    enum CommandMode {
        RETREAT_COMMAND,
        ESCORT_COMMAND,
        SEARCHANDDESTROY_COMMAND,
        NONE,
    }

    public static CommandMode commandMode = CommandMode.NONE;

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {
        if (NAModPlugin.hasLunaLib && NA_SettingsListener.na_combatui_enable && !NA_SettingsListener.na_combatui_nocontrol) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine.isUIShowingHUD() && (!NA_SettingsListener.na_combatui_pause
                    || engine.isPaused()
            )) {
                for (InputEventAPI e: events) {
                    if (e.isMouseDownEvent()) {
                        if (drawNightcrossTactical(true, e, events)) return;

                        // any clicks outside will reset the command mode
                        commandMode = CommandMode.NONE;
                    }

                }

            }
        }
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {

    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {


    }

    public boolean drawNightcrossTactical(boolean input, InputEventAPI e, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();

        if (!engine.isInCampaignSim() && !engine.isInCampaign()) return false;
        if (e != null && e.isConsumed()) return false;

        // get
        List<DeployedFleetMemberAPI> members = engine.getFleetManager(0).getDeployedCopyDFM();
        List<DeployedFleetMemberAPI> capitals = new ArrayList<DeployedFleetMemberAPI>();
        List<DeployedFleetMemberAPI> cruisers = new ArrayList<DeployedFleetMemberAPI>();
        List<DeployedFleetMemberAPI> destroyers = new ArrayList<DeployedFleetMemberAPI>();
        List<DeployedFleetMemberAPI> frigates = new ArrayList<DeployedFleetMemberAPI>();

        // filter
        for (DeployedFleetMemberAPI member : members) {
            if (member.getMember().getHullSpec().getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) capitals.add(member);
            else if (member.getMember().getHullSpec().getHullSize() == ShipAPI.HullSize.CRUISER) cruisers.add(member);
            else if (member.getMember().getHullSpec().getHullSize() == ShipAPI.HullSize.DESTROYER) destroyers.add(member);
            else if (member.getMember().getHullSpec().getHullSize() == ShipAPI.HullSize.FRIGATE) frigates.add(member);
        }

        // build the render list
        List<List<DeployedFleetMemberAPI>> display = new ArrayList<List<DeployedFleetMemberAPI>>();
        display.add(capitals);
        display.add(cruisers);
        display.add(destroyers);
        display.add(frigates);

        // sort by DP
        for (List<DeployedFleetMemberAPI> list : display) {
            list.sort(new Comparator<DeployedFleetMemberAPI>() {
                @Override
                public int compare(DeployedFleetMemberAPI o1, DeployedFleetMemberAPI o2) {
                    boolean retreating1 = Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(o1.getShip()) != null
                            && Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(o1.getShip()).getType().equals(CombatAssignmentType.RETREAT);
                    boolean retreating2 = Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(o2.getShip()) != null
                            && Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(o2.getShip()).getType().equals(CombatAssignmentType.RETREAT);
                    if (retreating1 && !retreating2) return 1;
                    if (!retreating1 && retreating2) return -1;
                    return (int) (o1.getMember().getDeploymentPointsCost() - o2.getMember().getDeploymentPointsCost());
                }
            });
        }

        HashMap<String, CombatFleetManagerAPI.AssignmentInfo> escortList = new HashMap<>();



        /*for (List<DeployedFleetMemberAPI> list : display) {
            for (DeployedFleetMemberAPI member : list) {

                boolean retreating = Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()) != null
                        && Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()).getType().equals(CombatAssignmentType.RETREAT);

                boolean escort = !retreating && Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()) != null
                        && (
                        Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()).getType().equals(CombatAssignmentType.LIGHT_ESCORT)
                                || Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()).getType().equals(CombatAssignmentType.MEDIUM_ESCORT)
                                || Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()).getType().equals(CombatAssignmentType.HEAVY_ESCORT)
                );
                if (escort) {
                    if (Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()).getType().equals(CombatAssignmentType.LIGHT_ESCORT)
                            || Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()).getType().equals(CombatAssignmentType.MEDIUM_ESCORT)
                            || Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()).getType().equals(CombatAssignmentType.HEAVY_ESCORT)) {
                        AssignmentTargetAPI at = Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentTargetFor(member.getShip());
                        if (at != null)
                            escortList.put(((DeployedFleetMemberAPI) at).getShip().getId(), Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()));
                    }
                }




                XX += Xspacing;
            }
            XX = XXstart;
            YY += Yspacing;
        }*/

        List<CombatFleetManagerAPI.AssignmentInfo> assignments = Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAllAssignments();
        for (CombatFleetManagerAPI.AssignmentInfo info : assignments) {
            if (info.getType().equals(CombatAssignmentType.LIGHT_ESCORT)
            || info.getType().equals(CombatAssignmentType.MEDIUM_ESCORT)
            || info.getType().equals(CombatAssignmentType.HEAVY_ESCORT)) escortList.put(((DeployedFleetMemberAPI) info.getTarget()).getShip().getId(), info);
        }




        // render
        float YY = Global.getSettings().getScreenHeightPixels() - NA_SettingsListener.tacticalRenderHeightOffset;
        float XXstart = NA_SettingsListener.tacticalRenderSideOffset;
        float XX = XXstart;
        float Xspacing = 85;
        float Yspacing = -90;
        float w = 80;
        float h = 60;
        if (NA_SettingsListener.na_combatui_compact) {
            w = 60f;
            h = 40f;
            Xspacing = 65f;
            Yspacing = -60f;
        }
        float TEXTHEIGHT = 20;
        float textSpacing = 100;
        float TEXTOFF = 20 + h;
        double sineAmt = Math.sin(9f * engine.getTotalElapsedTime(true) % (2*Math.PI));

        if (!NA_SettingsListener.na_combatui_nocontrol) {
            if (input) {
                if (e.getX() > XX && e.getX() < XX + textSpacing
                        && e.getY() > YY + TEXTOFF - TEXTHEIGHT && e.getY() < YY + TEXTOFF) {
                    commandMode = CommandMode.RETREAT_COMMAND;
                    Global.getSoundPlayer().playUISound("ui_button_full_retreat", 1f, 1f);
                    e.consume();
                    return true;
                } else if (e.getX() > XX + textSpacing && e.getX() < XX + 2 * textSpacing
                        && e.getY() > YY + TEXTOFF - TEXTHEIGHT && e.getY() < YY + TEXTOFF) {
                    commandMode = CommandMode.ESCORT_COMMAND;
                    Global.getSoundPlayer().playUISound("ui_button_full_retreat", 1f, 1f);
                    e.consume();
                    return true;
                } else if (e.getX() > XX + 2*textSpacing && e.getX() < XX + 3 * textSpacing
                        && e.getY() > YY + TEXTOFF - TEXTHEIGHT && e.getY() < YY + TEXTOFF) {
                    commandMode = CommandMode.SEARCHANDDESTROY_COMMAND;
                    Global.getSoundPlayer().playUISound("ui_button_patrol", 1f, 1f);
                    e.consume();
                    return true;
                }
            } else {
                Color textColor_OFF = TEXT_COLOR_OFF;
                Color textColor_ON = TEXT_COLOR_ON;
                Color textColor_HL = TEXT_COLOR_HIGHLIGHT;
                if (Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getCommandPointsLeft() == 0) textColor_ON = textColor_OFF;
                boolean hl_ret = false;
                boolean hl_esc = false;
                boolean hl_snd = false;

                if (Global.getSettings().getMouseX() > XX && Global.getSettings().getMouseX() < XX + textSpacing
                        && Global.getSettings().getMouseY() > YY + TEXTOFF - TEXTHEIGHT && Global.getSettings().getMouseY() < YY + TEXTOFF) {
                    hl_ret = true;
                } else if (Global.getSettings().getMouseX() > XX + textSpacing && Global.getSettings().getMouseX() < XX + 2 * textSpacing
                        && Global.getSettings().getMouseY() > YY + TEXTOFF - TEXTHEIGHT && Global.getSettings().getMouseY() < YY + TEXTOFF) {
                    hl_esc = true;
                } else if (Global.getSettings().getMouseX() > XX + 2*textSpacing && Global.getSettings().getMouseX() < XX + 3 * textSpacing
                        && Global.getSettings().getMouseY() > YY + TEXTOFF - TEXTHEIGHT && Global.getSettings().getMouseY() < YY + TEXTOFF) {
                    hl_snd = true;
                }

                if (!NA_SettingsListener.na_combatui_copyright)
                    MagicUI.addText(Global.getCombatEngine().getPlayerShip(), "Nightcross Tactical Display", textColor_OFF, new Vector2f(XX+20, YY + TEXTOFF + TEXTHEIGHT), false);
                MagicUI.addText(Global.getCombatEngine().getPlayerShip(), "Retreat", commandMode == CommandMode.RETREAT_COMMAND ? textColor_ON : hl_ret ? textColor_HL : textColor_OFF, new Vector2f(XX, YY + TEXTOFF), false);
                MagicUI.addText(Global.getCombatEngine().getPlayerShip(), "Escort", commandMode == CommandMode.ESCORT_COMMAND ? textColor_ON : hl_esc ? textColor_HL : textColor_OFF, new Vector2f(XX + textSpacing, YY + TEXTOFF), false);
                MagicUI.addText(Global.getCombatEngine().getPlayerShip(), "S&D", commandMode == CommandMode.SEARCHANDDESTROY_COMMAND ? textColor_ON : hl_snd ? textColor_HL : textColor_OFF, new Vector2f(XX + 2 * textSpacing, YY + TEXTOFF), false);

            }
        }



        for (List<DeployedFleetMemberAPI> list : display) {
            for (DeployedFleetMemberAPI member : list) {
                if (input) {
                    if ((member.getShip() != engine.getPlayerShip() || commandMode == CommandMode.ESCORT_COMMAND)
                            && e.getX() > XX && e.getX() < XX + w
                            && e.getY() > YY && e.getY() < YY + h) {
                        if (commandMode == CommandMode.ESCORT_COMMAND) {
                            CombatAssignmentType escortType = CombatAssignmentType.LIGHT_ESCORT;
                            if (member.getShip().getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) escortType = CombatAssignmentType.HEAVY_ESCORT;
                            else if (member.getShip().getHullSize() == ShipAPI.HullSize.CRUISER) escortType = CombatAssignmentType.MEDIUM_ESCORT;

                            if (!escortList.containsKey(member.getShip().getId())) {

                                if (Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()) != null) {
                                    Global.getCombatEngine().getFleetManager(0).getTaskManager(false).removeAssignment(
                                            Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()));
                                }


                                Global.getCombatEngine().getFleetManager(0).getTaskManager(false).createAssignment(escortType, member, true);
                                Global.getSoundPlayer().playUISound("ui_button_full_retreat", 1f, 1f);
                            } else if (escortList.containsKey(member.getShip().getId())) {

                                Global.getCombatEngine().getFleetManager(0).getTaskManager(false).removeAssignment(
                                        escortList.get(member.getShip().getId()));
                                Global.getSoundPlayer().playUISound("ui_button_full_retreat", 1f, 1f);
                            }
                        } else if (commandMode == CommandMode.SEARCHANDDESTROY_COMMAND) {
                            //List<CombatFleetManagerAPI.AssignmentInfo> assignments = Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAllAssignments();
                            if (Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()) == null
                                    || !Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()).getType().equals(CombatAssignmentType.SEARCH_AND_DESTROY)) {

                                Global.getCombatEngine().getFleetManager(0).getTaskManager(false).orderSearchAndDestroy(member, true);
                                Global.getSoundPlayer().playUISound("ui_button_full_retreat", 1f, 1f);
                            }
                        } else if (commandMode == CommandMode.RETREAT_COMMAND) {
                            if (Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()) == null
                                    || !Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()).getType().equals(CombatAssignmentType.RETREAT)) {

                                if (Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()) != null) {
                                    Global.getCombatEngine().getFleetManager(0).getTaskManager(false).removeAssignment(
                                            Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()));
                                }


                                Global.getCombatEngine().getFleetManager(0).getTaskManager(false).orderRetreat(member, true, true);
                                Global.getSoundPlayer().playUISound("ui_button_full_retreat", 1f, 1f);
                            }
                        }



                        e.consume();
                        return true;
                    }
                } else {
                    // icon
                    SpriteAPI sprite = Global.getSettings().getSprite(member.getMember().getHullSpec().getSpriteName());
                    float hp = member.getShip().getHullLevel();
                    float colorScale = 200;

                    Color color = NA_SettingsListener.na_combatui_colorblind ?
                            new Color(250 - (int)(colorScale * 0.8 * hp), 50 + (int)(0.7f * colorScale * hp), 50 + (int)(colorScale * hp), 220)
                            : new Color(250 - (int)(colorScale * 0.9 * hp), 50 + (int)(colorScale * hp), 50, 220);
                    float scalex = (sprite.getWidth() > 0) ? w / sprite.getWidth() : 1f;
                    float scaley = (sprite.getHeight() > 0) ? h/ sprite.getHeight() : 1f;
                    float scale = Math.min(scalex, scaley);

                    sprite.setSize(scale * sprite.getWidth(), scale * sprite.getHeight());
                    sprite.setColor(color);
                    sprite.setCenter(sprite.getWidth(), sprite.getHeight());


                    boolean retreating = Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()) != null
                            && Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()).getType().equals(CombatAssignmentType.RETREAT);

                    boolean escort = !NA_SettingsListener.na_combatui_info && !retreating && Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()) != null
                            && (
                            Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()).getType().equals(CombatAssignmentType.LIGHT_ESCORT)
                                    || Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()).getType().equals(CombatAssignmentType.MEDIUM_ESCORT)
                                    || Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()).getType().equals(CombatAssignmentType.HEAVY_ESCORT)
                    );
                    boolean snd = !NA_SettingsListener.na_combatui_info && Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()) != null
                            && Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()).getType().equals(CombatAssignmentType.SEARCH_AND_DESTROY);

                    if (retreating) {
                        sprite.setAlphaMult(0.6f + 0.39f * (float)sineAmt);
                    }

                    sprite.render(XX + w * 0.5f, YY + h * 0.5f);
                    sprite.setAdditiveBlend();
                    sprite.render(XX + w * 0.5f, YY + h * 0.5f);
                    sprite.setNormalBlend();

                    float yyy = 0;
                    if (snd) {
                        MagicUI.addText(member.getShip(), "S&D", new Color(250, 39, 190), new Vector2f(XX + 6, YY + h + yyy), false);
                        yyy -= 6;
                    }
                    if (!NA_SettingsListener.na_combatui_info && escortList.containsKey(member.getShip().getId())) {
                        if (escortList.get(member.getShip().getId()).getAssignedMembers().isEmpty()) {
                            MagicUI.addText(member.getShip(), "-", new Color(100, 255, 100), new Vector2f(XX + 6, YY + h + yyy), false);
                        } else
                        if (escortList.get(member.getShip().getId()).getType().equals(CombatAssignmentType.MEDIUM_ESCORT))
                            MagicUI.addText(member.getShip(), "M", new Color(100, 255, 100), new Vector2f(XX + 6, YY + h + yyy), false);
                        else if (escortList.get(member.getShip().getId()).getType().equals(CombatAssignmentType.HEAVY_ESCORT))
                            MagicUI.addText(member.getShip(), "H", new Color(100, 255, 100), new Vector2f(XX + 6, YY + h + yyy), false);
                        else if (escortList.get(member.getShip().getId()).getType().equals(CombatAssignmentType.LIGHT_ESCORT))
                            MagicUI.addText(member.getShip(), "L", new Color(100, 255, 100), new Vector2f(XX + 6, YY + h + yyy), false);
                        yyy -= 6;
                    }
                    if (!snd && escort) {


                        if (Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()).getType().equals(CombatAssignmentType.LIGHT_ESCORT)
                                || Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()).getType().equals(CombatAssignmentType.MEDIUM_ESCORT)
                                || Global.getCombatEngine().getFleetManager(0).getTaskManager(false).getAssignmentFor(member.getShip()).getType().equals(CombatAssignmentType.HEAVY_ESCORT)) {
                            MagicUI.addText(member.getShip(), "esc", new Color(46, 207, 46), new Vector2f(XX + 6, YY + h + yyy), false);
                        }
                    }

                    if (!NA_SettingsListener.na_combatui_info && retreating) {
                        MagicUI.addText(member.getShip(), "retreat", new Color(216, 237, 26), new Vector2f(XX + 6, YY + h + yyy), false);
                        yyy -= 6;
                    }

                    // bars
                    if (!NA_SettingsListener.na_combatui_bars) {
                        Color fill = new Color(210, member.getShip().getFluxTracker().isOverloaded() ? 150 : 82, 237, member.getShip().getFluxTracker().isOverloadedOrVenting() ? (int) (200 + 50 * sineAmt) : 255);
                        Color border = new Color(175, 134, 227, 180);
                        //if (NA_SettingsListener.na_combatui_compact)
                        MagicUI.addBar(member.getShip(), member.getShip().getFluxLevel(), fill, border, member.getShip().getHardFluxLevel(), new Vector2f(XX + w * 0.125f, YY - 4), 6, w*0.75f, true);
                        //else MagicUI.addInterfaceStatusBar(member.getShip(), new Vector2f(XX, YY - 4), member.getShip().getFluxLevel(), fill, border, member.getShip().getHardFluxLevel());
                        Color fill2 = new Color(202, 197, 197, member.getShip().getPeakTimeRemaining() < 1f ? (int) (200 + 50 * sineAmt) : 255);
                        Color border2 = new Color(169, 232, 8, 180);

                        if (!retreating) {
                            float crTimeFrac = member.getShip().getPeakTimeRemaining()/
                                    (1f + member.getMember().getStats().getPeakCRDuration().computeEffective(member.getShip().getHullSpec().getNoCRLossTime()));
                            //if (NA_SettingsListener.na_combatui_compact) {
                            MagicUI.addBar(member.getShip(), crTimeFrac, fill2, border, member.getShip().getCurrentCR(), new Vector2f(XX + w * 0.125f, YY - 12), 6, w*0.75f, true);
                        }
                    }


                    //MagicUI.drawSystemBar(member.getShip(), new Vector2f(XX + w/2, YY - 4), crTimeFrac <= 0.1f ? border2 : fill2, crTimeFrac <= 0.1f ? crTimeFrac : member.getShip().getCurrentCR(), 0);
                    //}
                    //else MagicUI.addInterfaceStatusBar(member.getShip(), new Vector2f(XX, YY - 15), member.getShip().getCurrentCR(), fill2, border2, crTimeFrac);
                }


                XX += Xspacing;
            }
            XX = XXstart;
            YY += Yspacing;
        }
        return false;
    }

    @Override
    public void renderInUICoords(ViewportAPI viewport) {
        if (NAModPlugin.hasLunaLib && NA_SettingsListener.na_combatui_enable) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine.isUIShowingHUD() && (!NA_SettingsListener.na_combatui_pause
                    || engine.isPaused()
            )) {
                drawNightcrossTactical(false, null, null);

            }
        }
    }

    @Override
    public void init(CombatEngineAPI engine) {

    }
}
