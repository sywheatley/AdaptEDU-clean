package procrastination_alg;

public class main_alg {
    private static final double EFFORT_SLOPE = 1.5000;
    private static final double EFFORT_INTERCEPT_HOURS = 0.0000;

    public static double getRealisticTimeInMinutes(double estimatedTimeInMinutes) {
        if (estimatedTimeInMinutes <= 0) {
            return 0;
        }

        double estimatedTimeInHours = estimatedTimeInMinutes / 60.0;
        double realisticTimeInHours = (estimatedTimeInHours * EFFORT_SLOPE) + EFFORT_INTERCEPT_HOURS;
        double realisticTimeInMinutes = realisticTimeInHours * 60.0;

        return Math.max(estimatedTimeInMinutes, realisticTimeInMinutes);
    }
}
