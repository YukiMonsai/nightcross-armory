package data.scripts.weapons;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import org.lwjgl.util.vector.Vector2f;


public class NA_SynergyWeapon implements EveryFrameWeaponEffectPlugin {

    private boolean inited = false;
    public final float MAG_SIZE_BONUS = 1.0f;
    private int magSizeBefore = 1;
    private int magSizeTracker = 1;
    private int magSizeAfter = 1;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI w) {
        if (!inited) {
            inited = true;
            WeaponAPI.WeaponType slotType = w.getSlot().getWeaponType();
            if (slotType != WeaponAPI.WeaponType.ENERGY
                    && slotType != WeaponAPI.WeaponType.BALLISTIC) {
                if (w.getShip() != null) {
                    magSizeTracker = (int) w.getShip().getMutableStats().getEnergyAmmoBonus().computeEffective(w.getSpec().getMaxAmmo());
                } else {
                    magSizeTracker = w.getSpec().getMaxAmmo();
                }
                magSizeAfter = (int) Math.ceil(magSizeTracker + w.getSpec().getMaxAmmo() * MAG_SIZE_BONUS);
            }
        }

        if (inited) {
            int tracker = 1;
            if (w.getShip() != null) {
                tracker = (int) w.getShip().getMutableStats().getEnergyAmmoBonus().computeEffective(w.getSpec().getMaxAmmo());
            } else {
                tracker = w.getSpec().getMaxAmmo();
            }
            if (tracker != magSizeTracker) {
                inited = false;
            }

            if (magSizeAfter > 1f) {
                w.getAmmoTracker().setMaxAmmo(magSizeAfter);
            }
        }



    }
}
