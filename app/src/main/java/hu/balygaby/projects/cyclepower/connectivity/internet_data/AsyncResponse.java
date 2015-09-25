package hu.balygaby.projects.cyclepower.connectivity.internet_data;

public interface AsyncResponse {
    int RESULT_HTTP_FAILURE = -4;
    int RESULT_INVALID_JSON = -1;
    int RESULT_ELEVATION_NOT_OK = -2;
    int RESULT_WEATHER_NOT_OK = -3;
    int RESULT_JSON_OK = 1;
    int AWAITING_DATA_REQUEST = 0;

    /**
     * Callback when the elevation request is finished and the elevation value
     * is fetched.
     *
     * @param processStatus Result of the http request and the JSON processing.
     * <p>
     *     Elevation is only valid, if the status is {@link #RESULT_JSON_OK}.
     *                      Possible values:
     *                      <br>{@link #AWAITING_DATA_REQUEST}
     *                      <br>{@link #RESULT_JSON_OK}
     *                      <br>{@link #RESULT_HTTP_FAILURE}
     *                      <br>{@link #RESULT_INVALID_JSON}
     *                      <br>{@link #RESULT_ELEVATION_NOT_OK}
     * </p>
     * @param elevation The resulting elevation. In the case of failure, the
     *                  value is 0.
     * @param latitude Latitude regarding the location.
     * @param longitude Longitude regarding the location.
     */
    void elevationProcessFinish(int processStatus, double elevation, double latitude, double longitude);

    /**
     * Callback when the weather request is finished and the wind values
     * are fetched.
     * @param processStatus Result of the http request and the JSON processing.
     * <p>
     *     Elevation is only valid, if the status is {@link #RESULT_JSON_OK}.
     *                      Possible values:
     *                      <br>{@link #AWAITING_DATA_REQUEST}
     *                      <br>{@link #RESULT_JSON_OK}
     *                      <br>{@link #RESULT_HTTP_FAILURE}
     *                      <br>{@link #RESULT_INVALID_JSON}
     *                      <br>{@link #RESULT_WEATHER_NOT_OK}
     * </p>
     * @param windSpeed The resulting wind speed in m/s. In the case of failure, the
     *                  value is 0.
     * @param windAngle The resulting wind angle. In the case of failure, the
     *                  value is 0.
     */
    void weatherProcessFinish(int processStatus, double windSpeed, double windAngle);
}
