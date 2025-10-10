package data.scripts.stardust;

import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ProximityExplosionEffect;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.impl.combat.dweller.RiftLightningEffect;

import java.awt.*;

public class NA_RageLightningExplosion implements ProximityExplosionEffect {

    public void onExplosion(DamagingProjectileAPI explosion, DamagingProjectileAPI originalProjectile) {
        //System.out.println("EXPLOSION");
        Color color = RiftLightningEffect.RIFT_LIGHTNING_COLOR;
        color = new Color(156, 29, 92, 144);
        NegativeExplosionVisual.NEParams p = RiftCascadeMineExplosion.createStandardRiftParams(
                color, 20f);
        p.fadeOut = 0.5f;
        p.hitGlowSizeMult = 0.3f;
        //p.invertForDarkening = NSProjEffect.STANDARD_RIFT_COLOR;
        p.thickness = 35f;


//		p.hitGlowSizeMult = 0.5f;
//		p.thickness = 25f;
//		p.fadeOut = 0.25f;

        RiftCascadeMineExplosion.spawnStandardRift(explosion, p);

    }
}
