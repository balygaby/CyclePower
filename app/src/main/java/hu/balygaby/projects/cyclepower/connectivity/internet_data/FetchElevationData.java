package hu.balygaby.projects.cyclepower.connectivity.internet_data;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import hu.balygaby.projects.cyclepower.MainActivity;
import hu.balygaby.projects.cyclepower.WorkoutService;

/**
 * Fetches elevation data of a geolocation from the internet.
 * Call {@link #requestElevationData} and implement the ASyncResponse interface.
 */
public class FetchElevationData {
    //https://maps.googleapis.com/maps/api/elevation/json?locations=%f,%f&key=%s , lat, long, apikey


    private static final String GOOGLE_MAP_ELEVATION_API =
            "https://maps.googleapis.com/maps/api/elevation/json?locations=%f,%f&key=%s";
    private static final String HTTP_FAILURE = "http_failure";
    private static final String KEY_STATUS = "status";
    private static final String OK = "OK";
    private static final String KEY_RESULTS = "results";
    private static final String KEY_ELEVATION = "elevation";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_LATITUDE = "lat";
    private static final String KEY_LONGITUDE = "lng";


    private WorkoutService workoutService;
    private int processStatus;
    private double elevation, latitude, longitude;

    /**
     * See {@link FetchElevationData}.
     * @param workoutService WorkoutService instance.
     */
    public FetchElevationData(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    /**
     * Acquires elevation data of the specified geolocation from the
     * Google elevation API.
     * <p>
     * Returns a boolean which indicates the network availability.
     * If it's false, no data is acquired.
     * </p>
     * @param  latitude  latitude of the geolocation
     * @param  longitude longitude of the geolocation
     * @return      the network availability
     */
    public boolean requestElevationData(double latitude, double longitude){
        processStatus = WorkoutService.AWAITING_DATA_REQUEST;
        String stringUrl = String.format(Locale.ENGLISH,GOOGLE_MAP_ELEVATION_API, latitude, longitude, MainActivity.MAPS_API_KEY);
        ConnectivityManager connMgr = (ConnectivityManager)
                workoutService.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new HttpTask().execute(stringUrl);
            return true;
        } else {
            return false; // no network connection
        }
    }

    private int processRawElevation(String result){
        this.elevation = 0;
        if (result.length() == 0 || result.equals(HTTP_FAILURE)) return WorkoutService.RESULT_HTTP_FAILURE;
        JSONObject elevationJSON;
        try {
            elevationJSON = new JSONObject(result);
            if (!elevationJSON.getString(KEY_STATUS).equals(OK)) return WorkoutService.RESULT_ELEVATION_NOT_OK;
            this.elevation = elevationJSON.getJSONArray(KEY_RESULTS).getJSONObject(0).getDouble(KEY_ELEVATION);
            this.latitude = elevationJSON.getJSONArray(KEY_RESULTS).getJSONObject(0).getJSONObject(KEY_LOCATION).getDouble(KEY_LATITUDE);
            this.longitude = elevationJSON.getJSONArray(KEY_RESULTS).getJSONObject(0).getJSONObject(KEY_LOCATION).getDouble(KEY_LONGITUDE);
        } catch (JSONException e) {
            return WorkoutService.RESULT_INVALID_JSON;
        }
        return WorkoutService.RESULT_JSON_OK;
    }

    private class HttpTask extends AsyncTask<String, Void, String> {
        public AsyncResponse asyncResponse = workoutService;
        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                return downloadUrl(urls[0]);
            } catch (IOException e) {
                return HTTP_FAILURE;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            processStatus = processRawElevation(result);
            asyncResponse.elevationProcessFinish(processStatus, elevation, latitude, longitude);

        }
    }

    //<editor-fold desc="NETWORK STUFF">
    // Given a URL, establishes an HttpUrlConnection and retrieves
// the web page content as a InputStream, which it returns as
// a string.
    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        // Only display the first 500 characters of the retrieved
        // web page content.
        int len = 500;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //conn.setReadTimeout(10000 /* milliseconds */);
            //conn.setConnectTimeout(15000 /* milliseconds */);
            //conn.setRequestMethod("GET");
            //conn.setDoInput(true);
            // Starts the query
            conn.connect();
            is = conn.getInputStream();

            // Convert the InputStream into a string
            return readIt(is, len);

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    // Reads an InputStream and converts it to a String.
    public String readIt(InputStream stream, int len) throws IOException {
        Reader reader;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }
    //</editor-fold>


    public interface AsyncResponse {

        /**
         * Callback when the elevation request is finished and the elevation value
         * is fetched.
         *
         * @param processStatus Result of the http request and the JSON processing.
         * <p>
         *     Elevation is only valid, if the status is {@link WorkoutService#RESULT_JSON_OK}.
         *                      Possible values:
         *                      <br>{@link WorkoutService#AWAITING_DATA_REQUEST}
         *                      <br>{@link WorkoutService#RESULT_JSON_OK}
         *                      <br>{@link WorkoutService#RESULT_HTTP_FAILURE}
         *                      <br>{@link WorkoutService#RESULT_INVALID_JSON}
         *                      <br>{@link WorkoutService#RESULT_ELEVATION_NOT_OK}
         * </p>
         * @param elevation The resulting elevation. In the case of failure, the
         *                  value is 0.
         * @param latitude Latitude regarding the location.
         * @param longitude Longitude regarding the location.
         */
        void elevationProcessFinish(int processStatus, double elevation, double latitude, double longitude);
    }
}
