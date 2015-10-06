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

import hu.balygaby.projects.cyclepower.WorkoutService;

/**
 * Fetches weather data of a geolocation from the internet.
 * Call {@link #requestWeatherData} and implement the ASyncResponse interface.
 * <p>
 * IMPORTANT: it is advised to always instantiate the class before requesting data,
 * since it is used infrequently.
 * </p>
 */
public class FetchWeatherData {
    public static final String WEATHER_API_KEY = "96d339c398d973bd4ba90d3b0b2538a9";
    private static final String OPEN_WEATHER_MAP_API =
            "http://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&units=metric&APPID=%s";
    private static final String HTTP_FAILURE = "http_failure";
    private static final String KEY_COD = "cod";
    private static final int COD_OK = 200;
    private static final String KEY_WIND = "wind";
    private static final String KEY_SPEED = "speed";
    private static final String KEY_DEG = "deg";

    private WorkoutService workoutService;
    private int processStatus;
    /**
     * Wind speed in m/s.
     */
    private double windSpeed;
    /**
     * Wind angle in degrees.
     */
    private double windAngle;

    /**
     * See {@link FetchWeatherData}.
     * @param workoutService WorkoutService instance.
     */
    public FetchWeatherData(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    /**
     * Acquires weather data of the specified geolocation from the
     * OpenWeatherMap API.
     * <p>
     * Returns a boolean which indicates the network availability.
     * If it's false, no data is acquired.
     * </p>
     *
     * @param  latitude  latitude of the geolocation
     * @param  longitude longitude of the geolocation
     * @return      the network availability
     */
    public boolean requestWeatherData(double latitude, double longitude){
        processStatus = WorkoutService.AWAITING_DATA_REQUEST;
        String stringUrl = String.format(Locale.ENGLISH,OPEN_WEATHER_MAP_API, latitude, longitude, WEATHER_API_KEY);
        ConnectivityManager connMgr = (ConnectivityManager)
                workoutService.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new HttpTask().execute(stringUrl);
            return true;
        } else {
            return false;
        }
    }

    private int processRawWeatherData(String result){
        this.windSpeed = 0.0; this.windAngle = 0.0;
        if (result.length() == 0 || result.equals(HTTP_FAILURE)) return WorkoutService.RESULT_HTTP_FAILURE;
        JSONObject weatherJSON;
        try {
            weatherJSON = new JSONObject(result);
            if (weatherJSON.getInt(KEY_COD) != COD_OK) return WorkoutService.RESULT_WEATHER_NOT_OK;
            this.windSpeed = weatherJSON.getJSONObject(KEY_WIND).getDouble(KEY_SPEED);
            this.windAngle = weatherJSON.getJSONObject(KEY_WIND).getInt(KEY_DEG);
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
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            processStatus = processRawWeatherData(result);
            asyncResponse.weatherProcessFinish(processStatus, windSpeed, windAngle);
        }
    }

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


    public interface AsyncResponse {

        /**
         * Callback when the weather request is finished and the wind values
         * are fetched.
         * @param processStatus Result of the http request and the JSON processing.
         * <p>
         *     Elevation is only valid, if the status is {@link WorkoutService#RESULT_JSON_OK}.
         *                      Possible values:
         *                      <br>{@link WorkoutService#AWAITING_DATA_REQUEST}
         *                      <br>{@link WorkoutService#RESULT_JSON_OK}
         *                      <br>{@link WorkoutService#RESULT_HTTP_FAILURE}
         *                      <br>{@link WorkoutService#RESULT_INVALID_JSON}
         *                      <br>{@link WorkoutService#RESULT_WEATHER_NOT_OK}
         * </p>
         * @param windSpeed The resulting wind speed in m/s. In the case of failure, the
         *                  value is 0.
         * @param windAngle The resulting wind angle in degrees when north is 0 deg.
         *                  The angle indicates where FROM the wind blows. In the case of failure, the
         *                  value is 0.
         */
        void weatherProcessFinish(int processStatus, double windSpeed, double windAngle);
    }
}