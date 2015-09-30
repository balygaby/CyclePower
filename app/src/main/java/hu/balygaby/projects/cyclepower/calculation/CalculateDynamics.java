package hu.balygaby.projects.cyclepower.calculation;

public class CalculateDynamics {
    //1/2 * c * ro * A = 0.5 * 0.9 * 1.2kg/m3 * 0.362m2
    private static final double AIR_DRAG_CONSTANT = 0.19548; //dimensionless
    private static final double ROLLING_RESISTANCE_CONSTANT = 0.004; //dimensionless
    private static final double GRAVITATIONAL_ACCELERATION = 9.80665; //m/s/s

    /**
     * Calculates the total air resistance acting on the bicycle.
     * @param speed Bicycle speed in km/h.
     * @param direction Bicycle heading direction in degrees, when north is 0.
     * @param windSpeed Wind speed in km/h.
     * @param windAngle Wind source direction in degrees, when north is 0.
     * @return The resulting air drag in Newtons.
     */
    public static double calculateAirDrag(double speed, double direction, double windSpeed, double windAngle){
        //every speed value in the parameters is in km/h, so they need to be converted to SI
        // if (direction < 0) -> dummy//no wind.
        if (direction < 0) return AIR_DRAG_CONSTANT * Math.pow((speed/*km/h*/ / 3.6),2);

        //windAngle indicates where FROM the wind blows
        //direction indicates where TO the bicycle goes
        //so the max. resistance is when the two directions are the same
        double windAngleComparedToDirection = BasicCalculations.calculateDifferenceBetweenAngles(direction, windAngle);
        //in headwind, additional speed is positive, because it is like going faster without wind
        double additionalSpeed = Math.cos(Math.toRadians(windAngleComparedToDirection)) * windSpeed; //still km/h

        return AIR_DRAG_CONSTANT * Math.pow(((speed/*km/h*/ + additionalSpeed/*km/h*/) / 3.6),2); //Newtons
    }

    /**
     * Calculates the rolling resistance acting on the bicycle.
     * @param bicycleWeight Bicycle weight in kg.
     * @param yourWeight Rider weight in kg.
     * @return Rolling resistance force in Newtons.
     */
    public static double calculateRollingResistance(double bicycleWeight, double yourWeight){
        return ROLLING_RESISTANCE_CONSTANT * GRAVITATIONAL_ACCELERATION * (yourWeight + bicycleWeight);
    }

    public static void calculateClimbResistance(){
        //TODO
    }
    public static void calculateInertialResistance(){
        //TODO
    }
    public static void calculateWork(double startingWork){
        //TODO
    }
}
