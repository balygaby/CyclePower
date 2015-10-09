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

    /**
     * Calculates the climb resistance force.
     * @param bicycleWeight Rider weight in kg.
     * @param yourWeight Bicycle weight in kg.
     * @param steepness Slope steepness in %.
     * @return The resistance force in Newtons.
     */
    public static double calculateClimbResistance(double bicycleWeight, double yourWeight, double steepness){
        //steepness is in % gradient, tg(x) by value - x is the slope angle
        //we need sin(x) for the formula F = M*g*sin(x)
        //sin(x) = tg(x) * sqrt(1 / ((tg(x))^2 + 1) )
        double tangentOfSlope = steepness / 100;
        double sineOfSlope = tangentOfSlope * Math.sqrt(1 / (Math.pow(tangentOfSlope,2) + 1) );
        return (bicycleWeight + yourWeight) * GRAVITATIONAL_ACCELERATION * sineOfSlope;
    }

    /**
     * Calculates the inertial resistance.
     * @param bicycleWeight Bicycle weight in kg.
     * @param yourWeight Rider weight in kg.
     * @param acceleration Bicycle acceleration is m/s/s.
     * @return The resistance force in N.
     */
    public static double calculateInertialResistance(double bicycleWeight, double yourWeight, double acceleration){
        return acceleration * (bicycleWeight + yourWeight);
    }

    /**
     * Calculates the total resistance. Pass the result to the {@link #calculatePower(double, double)} )} method.
     * @param speed Bicycle speed in km/h.
     * @param direction Bicycle heading direction in degrees, when north is 0.
     * @param windSpeed Wind speed in km/h.
     * @param windAngle Wind source direction in degrees, when north is 0.
     * @param bicycleWeight Rider weight in kg.
     * @param yourWeight Bicycle weight in kg.
     * @param steepness Slope steepness in %.
     * @param acceleration Bicycle acceleration is m/s/s.
     * @return Total resistance in Newtons.
     */
    public static double calculateTotalResistance(double speed, double direction, double windSpeed, double windAngle,
                                                  double bicycleWeight, double yourWeight,
                                                  double steepness,
                                                  double acceleration){
        return calculateAirDrag(speed, direction, windSpeed, windAngle)+
                calculateRollingResistance(bicycleWeight, yourWeight)+
                calculateClimbResistance(bicycleWeight, yourWeight, steepness)+
                calculateInertialResistance(bicycleWeight, yourWeight, acceleration);
    }

    /**
     * Calculates power from resistance forces.
     * @param speed Current bicycle speed in km/h.
     * @param totalResistance The total of resistance forces acting on the bicycle in Newtons.
     * @return The current drive power in Watts.
     */
    public static double calculatePower(double speed, double totalResistance){
        return speed/*km/h*/ / 3.6 * totalResistance/*N*/;
    }

    /**
     * Calculates the current torque.
     * @param cadence Pedal rpm.
     * @param totalResistance The total of resistance forces acting on the bicycle in Newtons.
     * @param wheelPerimeter Perimeter in mm.
     * @param gearRatio Gear ratio, wheel rpm / pedal rpm.
     * @return The current torque in Nm.
     */
    public static double calculateTorque(double totalResistance, double wheelPerimeter, double gearRatio){
        //M1 = i * F * K / (2pi)
        return totalResistance/*N*/ * wheelPerimeter/*mm*/ / 1000 * gearRatio / 2 / Math.PI;
    }
}
