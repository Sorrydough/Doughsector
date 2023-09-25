package data.scripts;

public class sd_util {
    public static boolean isNumberWithinRange(float numberA, float numberB, float deviationPercent) {
        float lowerBound = numberB - (numberB * (deviationPercent / 100));
        float upperBound = numberB + (numberB * (deviationPercent / 100));
        return numberA <= upperBound && numberA >= lowerBound;
    }
}
