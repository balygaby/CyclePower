package hu.balygaby.projects.cyclepower;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import hu.balygaby.projects.cyclepower.calculation.BasicCalculations;
import hu.balygaby.projects.cyclepower.connectivity.InputData;
import hu.balygaby.projects.cyclepower.connectivity.LocationService;
import hu.balygaby.projects.cyclepower.connectivity.internet_data.AsyncResponse;
import hu.balygaby.projects.cyclepower.connectivity.internet_data.FetchElevationData;
import hu.balygaby.projects.cyclepower.connectivity.internet_data.FetchWeatherData;

public class WorkoutService extends Service implements AsyncResponse, InputData{

    //<editor-fold desc="FIELDS">
    private static final String BICYCLE_WEIGHT = "bicycle_weight";
    private static final String YOUR_WEIGHT = "your_weight";
    private static final String WHEEL_PERIMETER = "wheel_perimeter";
    private static final int NOTIFICATION_ID = 20152016;
    private static final int NETWORK_PROBLEM = 0;
    private static final int NETWORK_OK = 1;
    private int bicycleWeight, yourWeight, wheelPerimeter;
    private LocationService locationService;
    private double wheelRpm, cadence;
    /**
     * Speed in km/h.
     */
    private double speed;
    /**
     * Starting time of the service to calculate elapsed time.
     */
    private double gearRatio;
    /**
     * Acceleration in m/s/s.
     */
    private double acceleration;
    /**
     * Distance in m.
     */
    private double distance;
    /**
     * Power in W.
     */
    private double power;
    /**
     * Work in J.
     */
    private double work;
    /**
     * Torque in Nm.
     */
    private double torque;
    private Location location, lastLocation, lastElevationLocation;
    /**
     * List of errors.
     *
     * <p>0: Weather</p>
     * <p>1: Elevation</p>
     * <p>2: Location</p>
     * <p>3: Network</p>
     * <p>4: Bluetooth</p>
     */
    private int errors[] = {RESULT_JSON_OK,RESULT_JSON_OK,1,NETWORK_OK,1};
    /**
     * Optimal Pace in km/h.
     */
    private double optimalPace;
    /**
     * Wind speed in km/h.
     */
    private double windSpeed;
    /**
     * Wind angle in degrees.
     */
    private double windAngle;
    /**
     * Elevation in m.
     */
    private double elevation;
    /**
     * Steepness in %.
     */
    private double steepness;

    private long startTime;
    /**
     * Updating activity user interface.
     */
    private Timer timerActivityConnection;
    private TimerTask timerTaskActivityConnection;
    /**
     * Getting weather data.
     */
    private Timer timerFetchData;
    private TimerTask timerTaskFetchData;
    /**
     * Doing the calculations.
     */
    private Timer timerDoCalculations;
    private TimerTask timerTaskDoCalculations;
    private FetchElevationData fetchElevationData;
    //TODO timer for watch connection
    /**
     * Service callback interface.
     */
    private ServiceCallbacks workoutData;
    private final IBinder mBinder = new LocalBinder();
    //</editor-fold>

    //<editor-fold desc="LIFECYCLE">
    @Override
    public void onCreate() {//Called only once when the service is started
        super.onCreate();
        Log.d("WorkoutService", "onCreate");

        //<editor-fold desc="GETTING SHARED PREFERENCE VALUES">
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String sBicycleWeight = ""+sharedPreferences.getString(BICYCLE_WEIGHT, "8");
        String sYourWeight = ""+sharedPreferences.getString(YOUR_WEIGHT, "70");
        String sWheelPerimeter = ""+sharedPreferences.getString(WHEEL_PERIMETER, "2133");
        if (sBicycleWeight.length() > 0 && !sBicycleWeight.equals("0")){
            bicycleWeight = Integer.parseInt(sBicycleWeight);
        }else {bicycleWeight = 8;}
        if (sYourWeight.length() > 0 && !sYourWeight.equals("0")){
            yourWeight = Integer.parseInt(sYourWeight);
        }else {bicycleWeight = 70;}
        if (sWheelPerimeter.length() > 0 && !sWheelPerimeter.equals("0")){
            wheelPerimeter = Integer.parseInt(sWheelPerimeter);
        }else {bicycleWeight = 2133;}
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(MainActivity.KEY_IS_SERVICE_ON, true);
        editor.apply();
        //</editor-fold>

        //<editor-fold desc="BUILDING NOTIFICATION">
        Intent startMainIntent = new Intent(this, MainActivity.class);
// Because clicking the notification opens a new ("special") activity, there's
// no need to create an artificial back stack.
        PendingIntent startMainPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        startMainIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(getResources().getString(R.string.app_name))
                        .setContentText(getResources().getString(R.string.session_on))
                        .setContentIntent(startMainPendingIntent)
                        .setOngoing(true);
        int mNotificationId = NOTIFICATION_ID;
// Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
// Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
        //</editor-fold>

        startTimers();
        locationService = new LocationService(this);
        fetchElevationData = new FetchElevationData(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("WorkoutService", "onStartCommand");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("WorkoutService","onBind");
        return mBinder;
    }

    @Override
    public void onDestroy() {
        Log.d("WorkoutService", "onDestroy");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(MainActivity.KEY_IS_SERVICE_ON, false);
        editor.apply();
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(NOTIFICATION_ID);
        stopCallbacks();
        locationService.stopLocationUpdates();
        locationService.collapseGoogleApiClient();
        stopTimers();
        super.onDestroy();
    }
    //</editor-fold>

    //<editor-fold desc="ACTIVITY COMMUNICATION">
    /**
     * Returns the instance of the service.
     */
    public class LocalBinder extends Binder {
        public WorkoutService getServiceInstance(){
            return WorkoutService.this;
        }
    }

    /**
     * Registers the activity to the service as ServiceCallbacks client.
     */
    public void registerClient(Activity activity){
        this.workoutData = (ServiceCallbacks) activity;
    }

    public void unregisterClients(){
        this.workoutData = null;
    }

    /**
     * Request from the activity for data callbacks via the {@link ServiceCallbacks}
     * interface. Call after binding to the service.
     */
    public void startCallbacks(){
        startActivityTimer();
    }

    /**
     * Stop data callbacks. Call after unbinding from the service.
     */
    public void stopCallbacks(){
        stopActivityTimer();

    }

    //</editor-fold>

    //<editor-fold desc="TIMER">

    private void startTimers() {
        startTime = System.currentTimeMillis();
        timerFetchData = new Timer();
        timerTaskFetchData = new TimerTask() {
            @Override
            public void run() {
                boolean isNetworkOk;
                FetchWeatherData fetchWeatherData = new FetchWeatherData(WorkoutService.this);
                do {
                    if (location != null) {
                        Log.d("Workout", "Trying to fetch weather data");
                        do {
                            Log.d("Workout", "Fetching weather data");
                            isNetworkOk = fetchWeatherData.requestWeatherData(location.getLatitude(), location.getLongitude());
                            if (!isNetworkOk) {
                                errors[ServiceCallbacks.ErrorList.NETWORK.ordinal()] = NETWORK_PROBLEM;
                            } else {
                                errors[ServiceCallbacks.ErrorList.NETWORK.ordinal()] = NETWORK_OK;
                            }
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException ignored) {}
                        } while (!isNetworkOk);
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {}
                }while (location == null);
            }
        };
        //getting weather data every 2 minutes; starting from 1 second after service start, so to get location
        timerFetchData.schedule(timerTaskFetchData,500,120000);

        timerDoCalculations = new Timer();
        timerTaskDoCalculations = new TimerTask() {
            @Override
            public void run() {
                //todo do calculations
            }
        };
        //calculations in every 250 ms
        timerDoCalculations.schedule(timerTaskDoCalculations,1000,250);
    }

    private void stopTimers(){
        if (timerTaskFetchData != null) timerTaskFetchData.cancel();
        if (timerFetchData != null){
            timerFetchData.cancel();
            timerFetchData.purge();
        }
        if (timerTaskDoCalculations != null) timerTaskDoCalculations.cancel();
        if (timerDoCalculations != null){
            timerDoCalculations.cancel();
            timerDoCalculations.purge();
        }
    }

    /**
     * This timerActivityConnection transmits data to the main activity.
     */
    private void startActivityTimer(){
        timerActivityConnection = new Timer();
        timerTaskActivityConnection = new TimerTask() {
            @Override
            public void run() {
                long time = System.currentTimeMillis() - startTime;
                double longitude = 0.0, latitude = 0.0;
                if (location != null){longitude = location.getLongitude(); latitude = location.getLatitude();}
                ServiceCallbacks.DisplayData displayData =
                        new ServiceCallbacks.DisplayData(time, speed, cadence, gearRatio, acceleration, distance, power, work, torque, latitude, longitude, elevation, steepness, errors);
                //unregisterClients wasn't called
                if (workoutData != null) workoutData.transmitBasicData(displayData);
            }
        };
        //schedule the timerActivityConnection, after the first 0 ms the TimerTask will run every 500 ms
        timerActivityConnection.schedule(timerTaskActivityConnection, 0, 500);
    }

    private void stopActivityTimer(){
        if (timerTaskActivityConnection != null) timerTaskActivityConnection.cancel();
        if (timerActivityConnection != null){
            timerActivityConnection.cancel();
            timerActivityConnection.purge();
        }
    }

    //</editor-fold>

    //<editor-fold desc="INTERFACE CALLBACKS">
    @Override
    public void elevationProcessFinish(int processStatus, double elevation, double latitude, double longitude) {
        //TODO see how recent the data are
        if (processStatus == RESULT_JSON_OK){
            this.elevation = elevation;
            Location elevationLocation = this.location;
            elevationLocation.setLatitude(latitude); elevationLocation.setLongitude(longitude);elevationLocation.setAltitude(elevation);
            if (lastElevationLocation == null) {
                lastElevationLocation = this.location;
                lastElevationLocation.setLatitude(latitude); lastElevationLocation.setLongitude(longitude);lastElevationLocation.setAltitude(elevation);
            }
            this.steepness = BasicCalculations.calculateSteepness(elevationLocation, lastElevationLocation);
            lastElevationLocation = elevationLocation;
        }
        errors[ServiceCallbacks.ErrorList.ELEVATION.ordinal()] = processStatus;
        Log.d("service","Elevation: "+elevation+" Steepness: "+steepness);
    }

    @Override
    public void weatherProcessFinish(int processStatus, double windSpeed, double windAngle) {
        if (processStatus == RESULT_JSON_OK) {
            this.windSpeed = windSpeed * 3.6; //m/s to km/h
            this.windAngle = windAngle;
        }
        errors[ServiceCallbacks.ErrorList.WEATHER.ordinal()] = processStatus;
        Log.d("service","WSpeed: "+this.windSpeed+" WAngle: "+this.windAngle);
    }

    @Override
    public void transmitBicycleData(int validity, double wheelRpm, double pedalRpm) {
        //TODO
    }

    @Override
    public void transmitLocationData(int validity, Location location) {
        if (validity == InputData.INPUT_VALID) {
            lastLocation = this.location;
            this.location = location;
            Log.d("service","new location");
            //TODO check validity
            //todo in the timer see how recent the location object is
            //getting elevation for the new location
            if (fetchElevationData != null){
                fetchElevationData.requestElevationData(location.getLatitude(),location.getLongitude());
            }
            //todo bearing
        }
        errors[ServiceCallbacks.ErrorList.LOCATION.ordinal()] = validity;

    }
    //</editor-fold>

}