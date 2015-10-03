package hu.balygaby.projects.cyclepower.connectivity;

import android.location.Location;

public interface InputData {
    int INPUT_VALID = 1;

    /**
     * Callback when a new Bluetooth message arrives.
     * @param validity Validity of the Bluetooth data.
     * @param wheelRpm Current wheel rpm.
     * @param pedalRpm Current pedal rpm.
     * @param wheelRotation Current count of wheel rotations. This value can only increase with time.
     */
    void transmitBicycleData(int validity, double wheelRpm, double pedalRpm, long wheelRotation);

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
