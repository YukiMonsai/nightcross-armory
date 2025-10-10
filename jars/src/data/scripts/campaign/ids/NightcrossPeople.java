package data.scripts.campaign.ids;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseMissionHub;
import com.fs.starfarer.api.loading.Description;
import com.fs.starfarer.api.util.Misc;
import data.scripts.hullmods.NA_ProjectGhost;

import java.util.Random;

import static com.fs.starfarer.api.campaign.AICoreOfficerPlugin.AUTOMATED_POINTS_MULT;
import static com.fs.starfarer.api.campaign.AICoreOfficerPlugin.AUTOMATED_POINTS_VALUE;


public class NightcrossPeople {

    // Kasane Teto (placeholder
    public static String TETO = "na_teto";
    public static String GHOST_CORE = "na_ghost_core";
    public static String BLACKCAT = "na_blackcat";
    public static float TETO_POINTS = 3.5f;
    public static float GHOST_POINTS = 2.5f;

    public static PersonAPI getPerson(String id) {
        return Global.getSector().getImportantPeople().getPerson(id);
    }


    public static PersonAPI createAIPerson(String aiCoreId, String factionId, Random random) {
        if (random == null) random = new Random();

        PersonAPI person = Global.getFactory().createPerson();
        person.setFaction(factionId);
        person.setAICoreId(aiCoreId);

        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(aiCoreId);
        boolean ghost = GHOST_CORE.equals(aiCoreId);

        person.getStats().setSkipRefresh(true);

        person.setName(new FullName(spec.getName(), "", Gender.ANY));
        int points = 0;
        float mult = 1f;
        if (ghost) {
            person.setId(GHOST_CORE);
            person.setName(new FullName("Ghost", "Core", Gender.FEMALE));
            person.setGender(Gender.ANY);

            person.getMemoryWithoutUpdate().set(AUTOMATED_POINTS_MULT, GHOST_POINTS);

            person.getStats().setLevel(6);
            person.getStats().setSkillLevel(NightcrossID.SKILL_FULLDIVE, 2); // character

            person.addTag(NA_ProjectGhost.CAPTAIN_TAG);

            person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
            person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
            person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
            person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
            person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
            person.setPortraitSprite(Global.getSettings().getSpriteName("na_characters", "ghostcore"));
            person.getMemoryWithoutUpdate().set("$chatterChar", "robotic");

            points = 0;
            mult = GHOST_POINTS;
        }
        if (points != 0) {
            person.getMemoryWithoutUpdate().set(AUTOMATED_POINTS_VALUE, points);
        }
        person.getMemoryWithoutUpdate().set(AUTOMATED_POINTS_MULT, mult);

        person.setPersonality(Personalities.RECKLESS);
        person.setRankId(Ranks.SPACE_CAPTAIN);
        person.setPostId(null);

        person.getStats().setSkipRefresh(false);

        return person;
    }


    public static void create() {
        createCharacters();
    }

    public static void createCharacters() {
        ImportantPeopleAPI ip = Global.getSector().getImportantPeople();

        // Kasane Teto, placeholder until actual plot relevant stuff
        if (getPerson(TETO) == null) {
            PersonAPI person = Global.getFactory().createPerson();
            person.setId(TETO);
            person.setName(new FullName("Teto", "Kasane", Gender.FEMALE));
            person.setAICoreId(NightcrossID.TETO_CORE);
            person.setFaction(Factions.INDEPENDENT);
            person.setGender(Gender.FEMALE);
            person.setRankId(Ranks.SPACE_COMMANDER);
            person.setPostId(Ranks.POST_OFFICER);
            person.setImportance(PersonImportance.MEDIUM);
            person.setPersonality(Personalities.AGGRESSIVE);

            person.getMemoryWithoutUpdate().set(AUTOMATED_POINTS_MULT, TETO_POINTS);

            person.getStats().setLevel(7);
            person.getStats().setSkillLevel(NightcrossID.SKILL_FULLDIVE, 2); // character

            person.addTag(NA_ProjectGhost.CAPTAIN_TAG);

            person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
            person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
            person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
            person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
            person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
            person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
            person.setPortraitSprite(Global.getSettings().getSpriteName("na_characters", "teto"));
            //person.getMemoryWithoutUpdate().set("$chatterChar", "robotic");
            ip.addPerson(person);
        }

        // This is what they get for giving an AI emotions
        if (getPerson(BLACKCAT) == null) {
            PersonAPI person = Global.getFactory().createPerson();
            person.setId(BLACKCAT);
            person.setName(new FullName("Black", "Cat", Gender.ANY));
            person.setFaction(NightcrossID.NIGHTCROSS_ARMORY);
            person.setGender(Gender.ANY);
            person.setRankId(Ranks.POST_UNKNOWN);
            person.setPostId(Ranks.POST_STATION_COMMANDER);
            person.setImportance(PersonImportance.MEDIUM);
            person.setPersonality(Personalities.CAUTIOUS);

            person.getStats().setLevel(7);

            person.getStats().setSkillLevel(Skills.HELMSMANSHIP, 2);
            person.getStats().setSkillLevel(Skills.COMBAT_ENDURANCE, 2);
            person.getStats().setSkillLevel(Skills.ENERGY_WEAPON_MASTERY, 2);
            person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
            person.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
            person.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
            person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);

            person.setPortraitSprite(Global.getSettings().getSpriteName("na_characters", "blackcat"));
            person.getMemoryWithoutUpdate().set("$chatterChar", "robotic");
            ip.addPerson(person);
        }
    }
}
