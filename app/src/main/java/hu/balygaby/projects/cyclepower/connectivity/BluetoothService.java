package hu.balygaby.projects.cyclepower.connectivity;

public class BluetoothService {



    public interface InputData {

        /**
         * Callback when a new Bluetooth message arrives.
         * @param validity Validity of the Bluetooth data.
         * @param wheelRpm Current wheel rpm.
         * @param pedalRpm Current pedal rpm.
         * @param wheelRotation Current count of wheel rotations. This value can only increase with time.
         */
        void transmitBicycleData(int validity, double wheelRpm, double pedalRpm, long wheelRotation);
    }
}
