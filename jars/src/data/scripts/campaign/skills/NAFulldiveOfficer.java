package data.scripts.campaign.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.*;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.hullmods.NA_ProjectGhost;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class NAFulldiveOfficer {
    public static float CR_PENALTY = -30f;
    public static float AUTO_MULT_GHOST = 1f;


    public static String TAG_SET_TO_NO_PENALTY = "$na_fulldive_settonopen";

    public static class Level1 implements ShipSkillEffect {
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            FleetMemberAPI member = stats.getFleetMember();
            boolean isGhost = member.getVariant().hasHullMod(NA_ProjectGhost.ID);
            if (!isGhost) {
                stats.getMaxCombatReadiness().modifyFlat(id, CR_PENALTY * 0.01f, "Project: GHOST");

                if (member.getVariant().hasTag(NA_ProjectGhost.TAG_NOPENALTY_SET)) {
                    member.getVariant().removeTag(NA_ProjectGhost.TAG_NOPENALTY_SET);
                    member.getVariant().removeTag(Tags.TAG_AUTOMATED_NO_PENALTY);
                }

            }
            if (!member.getVariant().hasTag(NA_ProjectGhost.TAG_IMMUNE_TO_PENALTY)) {
                member.getVariant().addTag(NA_ProjectGhost.TAG_IMMUNE_TO_PENALTY);
            }

            if (member.getCaptain() != null && member.getCaptain().isAICore()) {
                MemoryAPI captainMemory = member.getCaptain().getMemoryWithoutUpdate();
                if (!captainMemory.contains("$na_fulldive_origmult") && captainMemory.contains(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT)) {
                    captainMemory.set("$na_fulldive_origmult",
                            captainMemory.get(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT));
                    float newMult = isGhost ? AUTO_MULT_GHOST : (float) captainMemory.get("$na_fulldive_origmult");

                    member.getCaptain().getMemoryWithoutUpdate().set(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT, newMult);
                } else if (captainMemory.contains("$na_fulldive_origmult")) {
                    float newMult = isGhost ? AUTO_MULT_GHOST : (float) captainMemory.get("$na_fulldive_origmult");
                    member.getCaptain().getMemoryWithoutUpdate().set(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT, newMult);
                }
                if (!captainMemory.contains("$na_fulldive_origmult") && captainMemory.contains(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT)) {
                    captainMemory.set("$na_fulldive_origmult",
                            captainMemory.get(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT));
                    float newMult = isGhost ? AUTO_MULT_GHOST : (float) captainMemory.get("$na_fulldive_origmult");

                    member.getCaptain().getMemoryWithoutUpdate().set(AICoreOfficerPlugin.AUTOMATED_POINTS_MULT, newMult);
                }
                if (isGhost && !member.getVariant().hasTag(Tags.TAG_AUTOMATED_NO_PENALTY)) {
                    if (!member.getVariant().hasTag(NA_ProjectGhost.TAG_NOPENALTY_SET)) {
                        member.getVariant().addTag(NA_ProjectGhost.TAG_NOPENALTY_SET);
                        member.getVariant().addTag(Tags.TAG_AUTOMATED_NO_PENALTY);
                        captainMemory.set(TAG_SET_TO_NO_PENALTY, true);
                    }
                }
            }
        }

        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            stats.getMaxCombatReadiness().unmodify(id);
            FleetMemberAPI member = stats.getFleetMember();
            if (member.getVariant().hasTag(NA_ProjectGhost.TAG_IMMUNE_TO_PENALTY)) {
                member.getVariant().removeTag(NA_ProjectGhost.TAG_IMMUNE_TO_PENALTY);
            }
            if (member.getVariant().hasTag(NA_ProjectGhost.TAG_NOPENALTY_SET)) {
                member.getVariant().removeTag(NA_ProjectGhost.TAG_NOPENALTY_SET);
                member.getVariant().removeTag(Tags.TAG_AUTOMATED_NO_PENALTY);

            }
        }

        public String getEffectDescription(float level) {
            return "*Can pilot automated ships equipped with Project: GHOST interfaces without penalty. " + (int) CR_PENALTY + "% Combat Readiness otherwise.";
        }

        public String getEffectPerLevelDescription() {
            return null;
        }

        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }
}

