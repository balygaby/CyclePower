package hu.balygaby.projects.cyclepower;

public interface ServiceCallbacks {

    static enum ErrorList{WEATHER, ELEVATION, LOCATION, NETWORK, BLUETOOTH};

    /**
     * Fields to display on the activity screen.
     * Fields: see {@link #DisplayData(long, double, double, double, double, double, double, double, double, double, double, double, double, int[])}
     */
    class DisplayData {
        long time;
        double speed;
        double cadence;
        double gearRatio;
        double acceleration;
        double distance;
        double power;
        double work;
        double torque;
        double latitude;
        double longitude;
        double elevation;
        double steepness;
        int[] errors;

        /**
         * Data to display.
         *
         * @param time Elapsed time in ms.
         * @param speed Current speed in km/h.
         * @param cadence Current cadence in rpm
         * @param gearRatio Gear ratio.
         * @param acceleration Current acceleration in m/s/s.
         * @param distance Distance until now in m.
         * @param power Current power in W.
         * @param work Work until now in J.
         * @param torque Current torque in Nm.
         * @param latitude Latitude; 0.0 if no location.
         * @param longitude Longitude; 0.0 if no location.
         * @param elevation Elevation above sea level in m.
         * @param steepness Steepness in % [m/100m].
         * @param errors List of errors.
         */
        public DisplayData(long time,
                           double speed, double cadence,
                           double gearRatio,
                           double acceleration,  double distance,
                           double power, double work, double torque,
                           double latitude, double longitude,
                           double elevation, double steepness,
                           int[] errors) {
            this.time = time;
            this.speed = speed;
            this.cadence = cadence;
            this.gearRatio = gearRatio;
            this.acceleration = acceleration;
            this.distance = distance;
            this.power = power;
            this.work = work;
            this.torque = torque;
            this.latitude = latitude;
            this.longitude = longitude;
            this.elevation = elevation;
            this. steepness = steepness;
            this.errors = errors;
        }

        public double getLongitude() {
            return longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getTorque() {
            return torque;
        }

        public double getWork() {
            return work;
        }

        public double getPower() {
            return power;
        }

        public double getDistance() {
            return distance;
        }

        public double getAcceleration() {
            return acceleration;
        }

        public double getCadence() {
            return cadence;
        }

        public double getSpeed() {
            return speed;
        }

        public long getTime() {
            return time;
        }

        public double getGearRatio() {
            return gearRatio;
        }

        public double getElevation() {
            return elevation;
        }

        public double getSteepness() {
            return steepness;
        }

        public int[] getErrors() {
            return errors;
        }
    }

    /**
     * Sends values back to the main activity for display.
     *
         * @param displayData The {@link hu.balygaby.projects.cyclepower.ServiceCallbacks.DisplayData}
         *                    object to transmit.
         */
        void transmitBasicData(DisplayData displayData);

    //TODO: not basic data: optimalPace
}
