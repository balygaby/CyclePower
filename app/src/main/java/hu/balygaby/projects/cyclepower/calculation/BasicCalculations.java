package hu.balygaby.projects.cyclepower.calculation;


import android.location.Location;

public class BasicCalculations {

    /**
     * Calculates gear ratio,
     * @param wheelRpm Speed in km/h.
     * @param pedalRpm Cadence in rpm.
     * @return Gear ratio.
     */
    public static double calculateGearRatio(double wheelRpm, double pedalRpm){
        return wheelRpm / pedalRpm;
    }

    /**
     * Speed from wheel rpm.
     * @param wheelRpm Rpm.
     * @param wheelPerimeter Perimeter in mm.
     * @return Speed in km/h.
     */
    public static double calculateSpeed(double wheelRpm, int wheelPerimeter){
        return (wheelRpm / 60) * ((double)wheelPerimeter) / 1000 * 3.6;
    }

    /**
     * Calculates distance until now.
     * @param startingDistance Starting distance in meters, in case the service restarted mid-session.
     * @param startingWheelRotation Rotation count on the beginning of the session.
     * @param wheelRotation Current wheel rotation.
     * @param wheelPerimeter Wheel perimeter specified in shared preferences.
     * @return Distance from the session start in meters.
     */
    public static double calculateDistance(double startingDistance, int startingWheelRotation, int wheelRotation, int wheelPerimeter){
        return ((double) (wheelRotation - startingWheelRotation)) * ((double) wheelPerimeter) / 1000 + startingDistance;
    }

    /**
     * Calculates direction (bearing) between the two last locations.
     * @param location This location.
     * @param lastLocation The last location before this.
     * @return The current bearing if exists, or the bearing from the last location to this
     * (in degrees, north is 0).
     * If neither exist, the dummy return value is -1. Therefore check validity by a
     * condition for greater than 0.
     */
    public static double calculateDirection(Location location, Location lastLocation){
        if ((location == null) || (lastLocation == null)) return -1;
        if (location.hasBearing()) return location.getBearing();
        else return lastLocation.bearingTo(location);
    }

    /**
     * Difference between two angles [0-360 deg].
     * @param angle1 Angle 1 in deg.
     * @param angle2 Angle 2 in deg.
     * @return Difference in degrees.
     */
    public static double calculateDifferenceBetweenAngles(double angle1, double angle2){
        return (angle1 - angle2 + 540) % 360 - 180;
    }

    /**
     * Calculates quasi the current acceleration.
     * <p>
     *     Can also be called if the last speed and its time is 0, because then the
     *     denominator will be so great that the acceleration will return 0.
     * </p>
     * @param currentTime Current time in milliseconds.
     * @param speed Current speed in km/h.
     * @param timeOfLastSpeed Time of the last speed value in milliseconds.
     * @param lastSpeed Last speed in km/h.
     * @return Acceleration in m/s/s.
     */
    public static double calculateAcceleration(long currentTime, double speed, long timeOfLastSpeed, double lastSpeed){
        long timeWindow = currentTime - timeOfLastSpeed;
        if (timeWindow == 0) return 0;
        //converting speed to m/s and ms to s
        else return (speed - lastSpeed) / 3.6 / ((double) timeWindow) * 1000;
    }

    /**
     * Calculates the steepness of a short segment of a road based on the two locations on each end.
     * IMPORTANT: the locations must have an altitude property.
     * @param elevationLocation End point of the segment.
     * @param lastElevationLocation Start point of the segment.
     * @return The steepness in % (from the latter to the former).
     */
    public static double calculateSteepness(Location elevationLocation, Location lastElevationLocation) {
        //TODO take into account that the data might be older because of some network delay
        double distance = lastElevationLocation.distanceTo(elevationLocation);
        if (distance == 0) return 0;
        double elevationDifference = elevationLocation.getAltitude() - lastElevationLocation.getAltitude();
        return elevationDifference / distance * 100;
    }
}
