package hu.balygaby.projects.cyclepower.connectivity;

import android.location.Location;

public interface InputData {
    int INPUT_VALID = 1;

    void transmitBicycleData(int validity, double wheelRpm, double pedalRpm, double wheelRotation);

    /**
     * Callback when we have a new location.
     * @param validity Validity of the location object. Possible values:
     *                 <p>
     *                 {@link #INPUT_VALID}
     *                 </p>
     * @param location The raw location object.
     */
    void transmitLocationData(int validity, Location location);
}
