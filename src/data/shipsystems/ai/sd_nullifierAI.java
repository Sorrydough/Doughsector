package data.shipsystems.ai;

public class sd_nullifierAI extends sd_hackingsuiteAI {
}
    // top priority: any ship that's got improved timeflow
    // we want to target the ship with the highest baseline timeflow that's available
    // second priority: the ship with the lowest remaining PPT as a percentage of its total
    // third prioity: the ship with the lowest remaining CR
    // final priority: choose the highest DP ship out of the above
    // additional criteria: if one of our ships is already targeting something, we also want to target it too so we can pile on the effect
