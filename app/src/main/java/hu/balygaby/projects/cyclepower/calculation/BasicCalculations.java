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
     * Calculates direction (bearing) between the two last locations.
     * <p>
     *     IMPORTANT: use this if the location's {@link Location#hasBearing()} is false.
     * </p>
     * @param location This location.
     * @param lastLocation The last location before this.
     * @return Bearing from the last location to this.
     */
    public static double calculateDirection(Location location, Location lastLocation){
        //TODO
        return 0;
    }

    public static double calculateAcceleration(){
        //TODO
        return 0;
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
