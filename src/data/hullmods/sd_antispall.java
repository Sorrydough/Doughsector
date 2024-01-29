package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

public class sd_antispall extends BaseHullMod {
    final float SPALL_BONUS = 0.9f;
    final float FLAT_BONUS = 10;
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getCrewLossMult().modifyMult(id, SPALL_BONUS);
    }
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new sd_antispallListener());
    }
    public static class sd_antispallListener implements DamageTakenModifier {
        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (damage.getType() == DamageType.KINETIC || damage.getType() == DamageType.FRAGMENTATION && !shieldHit) {
                damage.getModifier().modifyFlat("sd_antispall", -Math.max(0, damage.computeDamageDealt(Global.getCombatEngine().getElapsedInLastFrame()) - 10));
            }
            return null;
        }
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("Kinetic and fragmentation damage to armor and hull are reduced by a flat "+ Math.round(FLAT_BONUS) +" points.", 5f,
                Misc.getHighlightColor(), Math.round(FLAT_BONUS) +"");
        tooltip.addPara("Crew casualties in combat are also reduced by "+ Math.round((1 - SPALL_BONUS) * 100) +"%%.", 5f,
                Misc.getHighlightColor(), Math.round((1 - SPALL_BONUS) * 100) +"%");
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }
}