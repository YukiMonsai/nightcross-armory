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

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {

    }

    @Override
    public void renderInUICoords(ViewportAPI viewport) {
    }

    @Override
    public void init(CombatEngineAPI engine) {

    }
}
