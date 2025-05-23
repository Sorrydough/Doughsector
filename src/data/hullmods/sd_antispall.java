package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

public class sd_antispall extends BaseHullMod {
    static final float SPALL_BONUS = 0.1f;
    static final float FLAT_BONUS = 10;
    static final float CREW_BONUS = 0.9f;
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getCrewLossMult().modifyMult(id, CREW_BONUS);
    }
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new sd_antispallListener());
    }
    public static class sd_antispallListener implements DamageTakenModifier {
        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (!shieldHit && (damage.getType() == DamageType.KINETIC || damage.getType() == DamageType.FRAGMENTATION)) {
                if (damage.getModifier().getFlatStatMod("sd_antispall") == null ) { // makes sure we don't modify the same damage twice... idk how this could happen but the modiverse is a wild place
                    damage.getModifier().modifyFlat("sd_antispall", -Math.min(FLAT_BONUS, damage.getDamage() * SPALL_BONUS));
                }
            }
            return null;
        }
    }
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addPara("Kinetic and fragmentation damage to armor and hull is reduced by the smaller of "+ Math.round((1 - SPALL_BONUS) * 100) +"%% or a flat "+ Math.round(FLAT_BONUS) +" points.", 5f,
                Misc.getHighlightColor(), Math.round((1 - SPALL_BONUS) * 100) +"%", Math.round(FLAT_BONUS) +"");
        tooltip.addPara("Crew casualties in combat are also reduced by "+ Math.round((1 - CREW_BONUS) * 100) +"%%.", 5f,
                Misc.getHighlightColor(), Math.round((1 - CREW_BONUS) * 100) +"%");
    }
    @Override
    public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
        return false;
    }
}