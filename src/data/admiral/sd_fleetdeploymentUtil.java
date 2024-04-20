package data.admiral;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.combat.CombatEngine;
import com.fs.starfarer.combat.CombatFleetManager;
import com.fs.starfarer.combat.ai.C;
import com.fs.starfarer.settings.StarfarerSettings;
import com.fs.starfarer.combat.CombatFleetManager;

import java.util.*;

public class sd_fleetdeploymentUtil {
    public static class DeploymentSpec {
        private final ArrayList<FleetMemberAPI> membersToDeploy = new ArrayList<>();
        private int totalDeploymentCost = 0;

        public DeploymentSpec() {
        }

        public void sortMembersToDeploy() {
            Collections.sort(membersToDeploy, new Comparator<FleetMemberAPI>() {
                public int compare(FleetMemberAPI memberToDeploy1, FleetMemberAPI memberToDeploy2) {
                    return memberToDeploy1.getHullSpec().getHullSize().ordinal() - memberToDeploy2.getHullSpec().getHullSize().ordinal();
                }
            });
        }
        public void addShipToDeploy(FleetMemberAPI newShipToDeploy) {
            membersToDeploy.add(newShipToDeploy);
            totalDeploymentCost += newShipToDeploy.getDeploymentPointsCost();
        }
        public void clearShipsToDeploy() {
            membersToDeploy.clear();
            totalDeploymentCost = 0;
        }
        public boolean areAllShipsDeployed() {
            return membersToDeploy.isEmpty();
        }
        public boolean isShipToBeDeployed(FleetMemberAPI member) {
            for (FleetMemberAPI memberToDeploy : membersToDeploy) {
                if (memberToDeploy == member) {
                    return true;
                }
            }
            return false;
        }

        public void removeMemberFromDeploymentSpec(FleetMemberAPI member) {
            for (FleetMemberAPI memberToDeploy : membersToDeploy) {
                if (memberToDeploy == member) {
                    totalDeploymentCost -= member.getDeploymentPointsCost();
                    membersToDeploy.remove(member);
                }
            }
        }

        public int getCostOfMembersToDeploy() {
            return this.totalDeploymentCost;
        }

        public ArrayList<FleetMemberAPI> getMembersToDeploy() {
            return membersToDeploy;
        }
    }
//    public void deploy(DeploymentSpec deploymentSpec, int owner) {
//        deploymentSpec.sortMembersToDeploy();
//        if (!deploymentSpec.getMembersToDeploy().isEmpty()) {
//            LinkedList<String> var2 = new LinkedList<>();
//            var2.add("Incoming vessels: ");
//
//            for (FleetMemberAPI var3 : deploymentSpec.getMembersToDeploy()) {
//                var2.add(var3.getShipName());
//            }
//
//            if ((owner == CombatEngine.getInstance().getPlayerId() || StarfarerSettings.õÒ0000()) && CombatEngine.getInstance().getCombatUI() != null && CombatEngine.getInstance().getCombatUI().getMessageWidget() != null) {
//                CombatEngine.getInstance().getCombatUI().getMessageWidget().o00000(1, var2.toArray());
//            }
//        }
//
//        com.fs.starfarer.combat.A.B var26 = CombatEngine.getInstance().getCombatMap();
//        float var27 = 500.0F;
//
//        for (FleetMemberAPI fleetMemberAPI : deploymentSpec.getMembersToDeploy()) {
//            CombatFleetManager.oo var28 = (CombatFleetManager.oo) fleetMemberAPI;
//            float var6 = var28.Ò00000().getVariant().getHullSpec().getSpriteSpec().Ô00000() + 500.0F;
//            if (var6 > var27) {
//                var27 = var6;
//            }
//        }
//
//        float var29 = var26.Õ00000() - var27 - this.ÖÔÒo00;
//        float var30 = 1.0F;
//        byte var31 = 0;
//        if (owner == 0) {
//            var31 = 1;
//        }
//
//        boolean var7 = false;
//        if (CombatEngine.getInstance().getFleetManager(var31).getGoal() == FleetGoal.ESCAPE) {
//            var7 = true;
//        }
//
//        if (owner == 1 && this.ÔÖÒo00 != FleetGoal.ESCAPE && !var7) {
//            var29 = var26.Ø00000() + var27 + this.ÖÔÒo00;
//            var30 = -1.0F;
//        }
//
//        float var8;
//        if (this.while.Object$while) {
//            var8 = CombatEngine.getInstance().getMapHeight();
//            float var9 = CombatEngine.getInstance().getCombatMap().Õ00000();
//            float var10 = CombatEngine.getInstance().getStandoffRange();
//            CombatFleetManager var11 = CombatEngine.getInstance().getEnemyFleetManager(this.getOwner());
//            if (var11 != null && var11.getBiggestStation() != null) {
//                var10 += 5000.0F;
//            }
//
//            if (this.getBiggestStation() != null) {
//                var10 += 2000.0F;
//            }
//
//            if (owner == 0) {
//                var29 = var9 + var8 * 0.5F - var10 * 0.5F;
//            } else {
//                var29 = var9 + var8 * 0.5F + var10 * 0.5F;
//            }
//        }
//
//        var8 = (var26.return() + var26.Ò00000()) / 2.0F;
//        Collections.sort(deploymentSpec.getMembersToDeploy(), new Comparator<FleetMemberAPI>() {
//            public int compare(FleetMemberAPI var1, FleetMemberAPI var2) {
//                if (var1.Ò00000().isFighterWing() && var2.Ò00000().isFighterWing()) {
//                    return (int)Math.signum(var2.Ò00000().getStats().getMaxSpeed().getModifiedValue() - var1.Ò00000().getStats().getMaxSpeed().getModifiedValue());
//                } else if (var1.Ò00000().isFighterWing() && !var2.Ò00000().isFighterWing()) {
//                    return -1;
//                } else {
//                    return !var1.Ò00000().isFighterWing() && var2.Ò00000().isFighterWing() ? 1 : (int)Math.signum(var2.Ò00000().getStats().getMaxSpeed().getModifiedValue() + var2.Ò00000().getStats().getZeroFluxSpeedBoost().getModifiedValue() - var1.Ò00000().getStats().getMaxSpeed().getModifiedValue() - var1.Ò00000().getStats().getZeroFluxSpeedBoost().getModifiedValue());
//                }
//            }
//        });
//
//        for(int var32 = 0; var32 < deploymentSpec.getMembersToDeploy().size(); var32 += 5) {
//            ArrayList var35 = new ArrayList();
//
//            int var34;
//            for(var34 = var32; var34 < var32 + 5 && var34 < deploymentSpec.getMembersToDeploy().size(); ++var34) {
//                var35.add(deploymentSpec.getMembersToDeploy().get(var34));
//            }
//
//            Collections.shuffle(var35);
//            Collections.sort(var35, new Comparator<FleetMemberAPI>() {
//                public int compare(FleetMemberAPI var1, FleetMemberAPI var2) {
//                    ShipAPI.HullSize var3 = var1.Ò00000().getHullSpec().getHullSize();
//                    ShipAPI.HullSize var4 = var2.Ò00000().getHullSpec().getHullSize();
//                    return var3 == var4 ? 0 : (int)Math.signum((float)(var3.ordinal() - var4.ordinal()));
//                }
//            });
//
//            for(var34 = var32; var34 < var32 + 5 && var34 < deploymentSpec.getMembersToDeploy().size(); ++var34) {
//                var1.String().set(var34, (CombatFleetManager.oo)var35.get(var34 - var32));
//            }
//        }
//
//        byte var33 = 5;
//        float[] var36 = new float[]{-2.0F, 2.0F, -1.0F, 1.0F, 0.0F};
//        float var37 = 1000.0F;
//        int var12 = 0;
//        int var13 = 0;
//        int var14 = 0;
//        Iterator var16 = deploymentSpec.getMembersToDeploy().iterator();
//
//        while(var16.hasNext()) {
//            CombatFleetManager.oo var15 = (CombatFleetManager.oo)var16.next();
//            if (var15.Ó00000 == null) {
//                ++var12;
//            } else if (var15.Ó00000 == com.fs.starfarer.combat.A.B.Oo.new) {
//                ++var13;
//            } else if (var15.Ó00000 == com.fs.starfarer.combat.A.B.Oo.Ó00000) {
//                ++var14;
//            }
//        }
//
//        int var38 = (int)Math.ceil((double)((float)var12 / (float)var33));
//        if (this.ÔÖÒo00 == FleetGoal.ESCAPE) {
//            var29 += var30 * var27 * (float)(Math.max(var38, 1) - 1);
//        }
//
//        int var17;
//        int var18;
//        int var19;
//        int var20;
//        float var21;
//        int var22;
//        CombatFleetManager.oo var23;
//        CombatFleetManager.O0 var24;
//        boolean var25;
//        int var39;
//        for(var39 = 0; var39 < var38; ++var39) {
//            var17 = var39 * var33;
//            var18 = Math.min(var39 * var33 + var33, var12);
//
//            for(var19 = var18 - 1; var19 >= var17; --var19) {
//                var20 = var33 - (var18 - var17);
//                var21 = 0.0F;
//                if (var20 % 2 == 1) {
//                    var21 = var37 / 2.0F;
//                }
//
//                var22 = var19 - var17 + var20;
//                var23 = this.o00000(var1.String(), var19, (com.fs.starfarer.combat.A.B.Oo)null);
//                if (var23.Ó00000 == null) {
//                    var24 = this.Ò00000(var23, var8 + var36[var22] * var37 - var21, var29);
//                    var25 = var24.getMember().isCivilian();
//                    if (this.ÔÖÒo00 == FleetGoal.ESCAPE && var24 != null && (var25 || var24.getMember().isCarrier())) {
//                        if (var24.isAlly()) {
//                            this.ÔÔÒo00.giveDirectOrder(var24, new C(com.fs.starfarer.combat.tasks.C.o.String, var25), true);
//                        } else {
//                            this.ÓÔÒo00.giveDirectOrder(var24, new C(com.fs.starfarer.combat.tasks.C.o.String, var25), true);
//                        }
//                    }
//                }
//            }
//
//            var29 -= var30 * var27;
//            if (!this.suspendYOffsetIncr) {
//                this.ÖÔÒo00 += var27;
//                if (this.ÖÔÒo00 > 10000.0F) {
//                    this.ÖÔÒo00 = 10000.0F;
//                }
//            }
//        }
//
//        var29 = var26.Õ00000() + CombatEngine.getInstance().getContext().getFlankDeploymentDistance();
//        var8 = var26.Ò00000() - this.OÕÒo00 - 1000.0F;
//        var33 = 3;
//        var36 = new float[]{-1.0F, 1.0F, 0.0F};
//        var38 = (int)Math.ceil((double)((float)var13 / (float)var33));
//
//        for(var39 = 0; var39 < var38; ++var39) {
//            var17 = var39 * var33;
//            var18 = Math.min(var39 * var33 + var33, var13);
//
//            for(var19 = var18 - 1; var19 >= var17; --var19) {
//                var20 = var33 - (var18 - var17);
//                var21 = 0.0F;
//                if (var20 % 2 == 0) {
//                    var21 = -var37 / 2.0F;
//                }
//
//                var22 = var19 - var17 + var20;
//                var23 = this.o00000(var1.String(), var19, com.fs.starfarer.combat.A.B.Oo.new);
//                if (var23.Ó00000 == com.fs.starfarer.combat.A.B.Oo.new) {
//                    var24 = this.Ò00000(var23, var8, var29 + var36[var22] * var37 - var21);
//                    var25 = var24.getMember().isCivilian();
//                    if (this.ÔÖÒo00 == FleetGoal.ESCAPE && var24 != null && (var25 || var24.getMember().isCarrier())) {
//                        if (var24.isAlly()) {
//                            this.ÔÔÒo00.giveDirectOrder(var24, new C(com.fs.starfarer.combat.tasks.C.o.String, var25), true);
//                        } else {
//                            this.ÓÔÒo00.giveDirectOrder(var24, new C(com.fs.starfarer.combat.tasks.C.o.String, var25), true);
//                        }
//                    }
//                }
//            }
//
//            var8 -= var27;
//            if (!this.suspendYOffsetIncr) {
//                this.OÕÒo00 -= var27;
//            }
//        }
//
//        var8 = var26.return() + this.ØÔÒo00 + 1000.0F;
//        var33 = 3;
//        var36 = new float[]{-1.0F, 1.0F, 0.0F};
//        var38 = (int)Math.ceil((double)((float)var14 / (float)var33));
//
//        for(var39 = 0; var39 < var38; ++var39) {
//            var17 = var39 * var33;
//            var18 = Math.min(var39 * var33 + var33, var14);
//
//            for(var19 = var18 - 1; var19 >= var17; --var19) {
//                var20 = var33 - (var18 - var17);
//                var21 = 0.0F;
//                if (var20 % 2 == 0) {
//                    var21 = -var37 / 2.0F;
//                }
//
//                var22 = var19 - var17 + var20;
//                var23 = this.o00000(var1.String(), var19, com.fs.starfarer.combat.A.B.Oo.Ó00000);
//                if (var23.Ó00000 == com.fs.starfarer.combat.A.B.Oo.Ó00000) {
//                    var24 = this.Ò00000(var23, var8, var29 + var36[var22] * var37 - var21);
//                    var25 = var24.getMember().isCivilian();
//                    if (this.ÔÖÒo00 == FleetGoal.ESCAPE && var24 != null && (var25 || var24.getMember().isCarrier())) {
//                        if (var24.isAlly()) {
//                            this.ÔÔÒo00.giveDirectOrder(var24, new C(com.fs.starfarer.combat.tasks.C.o.String, var25), true);
//                        } else {
//                            this.ÓÔÒo00.giveDirectOrder(var24, new C(com.fs.starfarer.combat.tasks.C.o.String, var25), true);
//                        }
//                    }
//                }
//            }
//
//            var8 += var27;
//            if (!this.suspendYOffsetIncr) {
//                this.ØÔÒo00 += var27;
//            }
//        }
//
//        this.ÓÔÒo00.reassignPlayerShipsOnly();
//        this.while.Object$while = false;
//    }
}
