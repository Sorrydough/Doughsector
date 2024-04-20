package data.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.procgen.PlanetConditionGenerator;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.Console;

import java.util.Objects;
import java.util.Random;

public class sd_addplanetCommand implements BaseCommand {
    @Override @SuppressWarnings("")
    public CommandResult runCommand(@NotNull String args, @NotNull CommandContext context) {
        if (Global.getSector().getPlayerFleet().getContainingLocation() instanceof StarSystemAPI && !args.isEmpty()) {
            boolean exists = false;
            for (PlanetSpecAPI spec : Global.getSettings().getAllPlanetSpecs())
                if (Objects.equals(spec.getName(), args)) {
                    exists = true;
                    break;
                }
            if (!exists)
                return CommandResult.BAD_SYNTAX;
            StarSystemAPI system = Global.getSector().getPlayerFleet().getStarSystem();
            PlanetAPI planet = system.addPlanet(this.toString(), system.getStar(), "Your Mother", args, new Random().nextInt(360), 50,
                    system.getStar().getRadius() * 5, system.getStar().getRadius());
            PlanetConditionGenerator.generateConditionsForPlanet(planet, system.getAge());
            return CommandResult.SUCCESS;
        } else {
            if (args.isEmpty())
                Console.showMessage("Command needs a planet type ID!");
            else
                Console.showMessage("Command must be run in a star system!");
            return CommandResult.WRONG_CONTEXT;
        }
    }
}
