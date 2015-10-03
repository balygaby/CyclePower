package hu.balygaby.projects.cyclepower;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.sleepycat.je.DatabaseException;

import java.util.Timer;
import java.util.TimerTask;

import hu.balygaby.projects.cyclepower.calculation.BasicCalculations;
import hu.balygaby.projects.cyclepower.calculation.CalculateDynamics;
import hu.balygaby.projects.cyclepower.connectivity.InputData;
import hu.balygaby.projects.cyclepower.connectivity.LocationService;
import hu.balygaby.projects.cyclepower.connectivity.internet_data.AsyncResponse;
import hu.balygaby.projects.cyclepower.connectivity.internet_data.FetchElevationData;
import hu.balygaby.projects.cyclepower.connectivity.internet_data.FetchWeatherData;
import hu.balygaby.projects.cyclepower.database.ByteConverter;
import hu.balygaby.projects.cyclepower.database.ManageDb;
import hu.balygaby.projects.cyclepower.database.objects.WholeWorkout;
import hu.balygaby.projects.cyclepower.database.objects.WorkoutEntry;

public class WorkoutService extends Service implements AsyncResponse, InputData{

    //<editor-fold desc="FIELDS">
    private static final String BICYCLE_WEIGHT = "bicycle_weight";
    private static final String YOUR_WEIGHT = "your_weight";
    private static final String WHEEL_PERIMETER = "wheel_perimeter";
    private static final int NOTIFICATION_ID = 20152016;
    private static final int NETWORK_PROBLEM = 0;
    private static final int NETWORK_OK = 1;
    private static final int NETWORK_TOO_SLOW = -1;
    private static final int LOCATION_OK = 1;
    private static final int BLUETOOTH_OK = 1;
    public static final int DATABASE_OK = 1;
    public static final int DATABASE_WRITE_PROBLEM = 0;
    public static final int DATABASE_NULL = -1;
    public static final int DATABASE_CLOSE_ERROR = -2;
    private static final long PERIOD_OF_WEATHER_QUERY = 120000;
    public static final long PERIOD_OF_CALCULATION = 1000;
    private static final long PERIOD_OF_DATABASE = 1000;

    private int wheelPerimeter;
    private double bicycleWeight, yourWeight;
    private LocationService locationService;
    private double wheelRpm, cadence;
    /**
     * Speed in km/h.
     */
    private double speed;
    /**
     * Time of the {@link #speed} field.
     */
    private long timeOfSpeed;
    /**
     * The wheel rotation count at the start of the workout session. Distance calculation works
     * with the increment from this variable.
     */
    private long startingWheelRotation;//todo
    /**
     * The distance at the time of an unexpected service restart.
     */
    private double startingDistance;
    /**
     * The work at the time of an unexpected service restart.
     */
    private double startingWork;
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
    private double work = 0;
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
    private int errors[] = {RESULT_JSON_OK,RESULT_JSON_OK,LOCATION_OK,NETWORK_OK,BLUETOOTH_OK,DATABASE_OK};
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
    /**
     * Bearing in degrees (North is 0).
     */
    private double direction;

    private long startTime;
    /**
     * Updating activity user interface.
     */
    private static Timer timerActivityConnection;
    private static TimerTask timerTaskActivityConnection;
    private static Timer timer;
    private static TimerTask timerTaskFetchData,timerTaskDoCalculations,timerTaskWriteDatabase;
    private FetchElevationData fetchElevationData;
    //TODO timer for watch connection
    /**
     * Service callback interface.
     */
    private ServiceCallbacks workoutData;
    private ManageDb manageDb;
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
            bicycleWeight = Double.parseDouble(sBicycleWeight);
        }else {bicycleWeight = 8;}
        if (sYourWeight.length() > 0 && !sYourWeight.equals("0")){
            yourWeight = Double.parseDouble(sYourWeight);
        }else {bicycleWeight = 70;}
        if (sWheelPerimeter.length() > 0 && !sWheelPerimeter.equals("0")){
            wheelPerimeter = Integer.parseInt(sWheelPerimeter);
        }else {bicycleWeight = 2133;}
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(MainActivity.KEY_IS_SERVICE_ON, true);
        editor.apply();
        //</editor-fold>

        //<editor-fold desc="BUILDING NOTIFICATION">
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(getResources().getString(R.string.app_name))
                        .setContentText(getResources().getString(R.string.session_on))
                        .setOngoing(true);
        int mNotificationId = NOTIFICATION_ID;
// Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
// Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
        //</editor-fold>

        //<editor-fold desc="SETTING UP DATABASE">
        manageDb = ManageDb.getInstance(this);
        try {
            manageDb.setupDb(false);
            if (manageDb.isWorkoutInProgress()){//service restarted without user interaction
                try {
                    WholeWorkout lastWholeWorkout;
                    lastWholeWorkout = manageDb.getLastWorkout();
                    this.startTime = lastWholeWorkout.getStartTime();//other fields are then 0
                    WorkoutEntry lastWorkoutEntry = manageDb.getLastEntry();
                    this.startingDistance = lastWorkoutEntry.getDistance();
                    this.startingWork = lastWorkoutEntry.getWork();
                } catch (Exception e) {
                    Log.d("WorkoutService","error getting last workout: "+e);
                }
                //the other fields are temporary (always changing), so are recovered in a second.
            }else{
                startTime = System.currentTimeMillis();
                errors[ServiceCallbacks.ErrorList.NETWORK.ordinal()] =
                        manageDb.writeFirstRecordHeader(startTime);//new first record header;
                //we can access the oncoming entries by moving the cursor to the record (with searchKeyRange), the time of
                //which is closest to the start time, and is after the start time.
            }
        } catch (DatabaseException | IllegalArgumentException e) {
            Log.d("WorkoutService", "error setting up database: " + e);
        }
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
        errors[ServiceCallbacks.ErrorList.DATABASE.ordinal()] = manageDb.closeDb(); //close database
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
        timer = new Timer();
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
        timer.schedule(timerTaskFetchData,500,PERIOD_OF_WEATHER_QUERY);

        timerTaskDoCalculations = new TimerTask() {
            @Override
            public void run() {
                //todo do calculations
                double totalResistance = CalculateDynamics.calculateTotalResistance(speed, direction, windSpeed, windAngle, bicycleWeight, yourWeight, steepness, acceleration);
                power = CalculateDynamics.calculatePower(speed, totalResistance);
                torque = CalculateDynamics.calculateTorque(cadence, totalResistance, wheelPerimeter, gearRatio);
                work += BasicCalculations.calculateWorkIncrement(power);
            }
        };
        //calculations in every 250 ms todo write back to 250 maybe
        timer.schedule(timerTaskDoCalculations,500,PERIOD_OF_CALCULATION);

        timerTaskWriteDatabase = new TimerTask() {
            @Override
            public void run() {
                double latitude = 0; double longitude = 0;
                if (location != null){ latitude = location.getLatitude(); longitude = location.getLongitude();}
                errors[ServiceCallbacks.ErrorList.DATABASE.ordinal()] =
                        manageDb.writeEntry(ByteConverter.convertKeyToByteArray(System.currentTimeMillis()),
                                ByteConverter.convertDataToByteArray(distance, work + startingWork, speed, cadence, power, torque, latitude, longitude));
                if (errors[ServiceCallbacks.ErrorList.DATABASE.ordinal()] == DATABASE_NULL){//if it closes for some reason
                    Log.d("WorkoutService","Database is null");
                    manageDb = ManageDb.getInstance(WorkoutService.this);
                    manageDb.closeDb();
                    try {
                        manageDb.setupDb(false);
                        Log.d("WorkoutService", "Setting up closed db again");
                    } catch (DatabaseException | IllegalArgumentException e) {
                        Log.d("WorkoutService", "error setting up database: " + e);
                    }
                }
            }
        };
        timer.schedule(timerTaskWriteDatabase,500,PERIOD_OF_DATABASE);
    }

    private void stopTimers(){
        if (timerTaskFetchData != null) timerTaskFetchData.cancel();
        if (timerTaskDoCalculations != null) timerTaskDoCalculations.cancel();
        if (timerTaskWriteDatabase != null) timerTaskWriteDatabase.cancel();
        if (timer != null){
            timer.cancel();
            timer.purge();
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
                        new ServiceCallbacks.DisplayData(time, speed, cadence, gearRatio, acceleration, distance, power, work + startingWork, torque, latitude, longitude, elevation, steepness, errors);
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
        if (processStatus == RESULT_JSON_OK){
            this.elevation = elevation;
            Location elevationLocation = this.location;
            elevationLocation.setLatitude(latitude); elevationLocation.setLongitude(longitude);elevationLocation.setAltitude(elevation);
            if (lastElevationLocation == null) {//on first run avoid passing null
                lastElevationLocation = this.location;
                lastElevationLocation.setLatitude(latitude); lastElevationLocation.setLongitude(longitude);lastElevationLocation.setAltitude(elevation);
            }
            if (elevationLocation.distanceTo(this.location)>50){//too old data
                this.steepness = 0;
                this.errors[ServiceCallbacks.ErrorList.NETWORK.ordinal()] = NETWORK_TOO_SLOW;
            }else {
                this.steepness = BasicCalculations.calculateSteepness(elevationLocation, lastElevationLocation);
                this.errors[ServiceCallbacks.ErrorList.NETWORK.ordinal()] = NETWORK_OK;
            }
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
    public void transmitBicycleData(int validity, double wheelRpm, double pedalRpm, long wheelRotation) {
        //TODO
        if (startingWheelRotation == 0) startingWheelRotation = wheelRotation; //assigning a value when the service starts;
        // since wheelRotation only increases, the service start is the only point when startingWheelRotation can be 0

        long currentTime = System.currentTimeMillis();
        double currentSpeed = BasicCalculations.calculateSpeed(wheelRpm,wheelPerimeter);
        this.acceleration = BasicCalculations.calculateAcceleration(currentTime, currentSpeed, timeOfSpeed, speed);
        this.speed = currentSpeed;
        this.timeOfSpeed = currentTime;
        this.gearRatio = BasicCalculations.calculateGearRatio(wheelRpm, pedalRpm);
        this.wheelRpm = wheelRpm;
        this.cadence = pedalRpm;
        this.distance = BasicCalculations.calculateDistance(this.startingDistance, this.startingWheelRotation, wheelRotation, wheelPerimeter);

    }

    @Override
    public void transmitLocationData(int validity, Location location) {
        if (validity == InputData.INPUT_VALID) {
            lastLocation = this.location;
            this.location = location;
            //getting elevation for the new location
            if (fetchElevationData != null){
                 boolean isNetworkOk = fetchElevationData.requestElevationData(location.getLatitude(),location.getLongitude());
                if (!isNetworkOk) {
                    errors[ServiceCallbacks.ErrorList.NETWORK.ordinal()] = NETWORK_PROBLEM;
                } else {
                    errors[ServiceCallbacks.ErrorList.NETWORK.ordinal()] = NETWORK_OK;
                }
            }
            this.direction = BasicCalculations.calculateDirection(this.location, this.lastLocation);
            Log.d("service", "new location, direction: " + this.direction);
        }
        //There cannot be any problems with the location, besides when it comes not frequently enough.
        //But also then we cannot really do anything, but use the new location when it arrives.
        errors[ServiceCallbacks.ErrorList.LOCATION.ordinal()] = validity;

    }
    //</editor-fold>

}