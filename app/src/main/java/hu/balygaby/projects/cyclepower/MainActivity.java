package hu.balygaby.projects.cyclepower;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.sleepycat.je.DatabaseException;

import java.text.DecimalFormat;
import java.util.ArrayList;

import hu.balygaby.projects.cyclepower.calculation.math_aid.DecimalToFraction;
import hu.balygaby.projects.cyclepower.connectivity.LocationService;
import hu.balygaby.projects.cyclepower.database.ByteConverter;
import hu.balygaby.projects.cyclepower.database.ManageDb;
import hu.balygaby.projects.cyclepower.database.objects.WorkoutEntry;
import hu.balygaby.projects.cyclepower.serving_activities.AppointPathActivity;
import hu.balygaby.projects.cyclepower.serving_activities.SettingsActivity;
import hu.balygaby.projects.cyclepower.serving_activities.WorkoutsOverviewActivity;

public class MainActivity extends FragmentActivity implements ServiceCallbacks, LocationService.LastLocationData {

    // SHA1: 66:03:8B:66:B4:5E:EA:EB:14:0A:18:B2:70:40:A2:70:29:E5:D0:77

    //<editor-fold desc="FIELDS">
    public static final String MAPS_API_KEY = "AIzaSyDAEX_wDgtyWtcNhvz2oaoNyJFoDTfc-tk";
    public static final String MAPS_ANDROID_KEY = "AIzaSyDfOrVAYVGQL9bTixqE23hBaewjfG5OOKM";
    public static final String KEY_IS_SERVICE_ON = "is_service_on";
    private static final int REQUEST_APPOINT_PATH = 2015;
    public static final String LAST_LOCATION = "last_location";
    public static final String KEY_POLYLINE = "polyline";
    public static final String KEY_ARRIVAL_TIME_MINUTES = "arrival_time_minutes";
    /**
     * The service instance.
     */
    private WorkoutService workoutService;
    /**
     * The service runs independently from the workoutData. In this variable
     * the service status is stored for reference purposes.
     */
    /**
     * The
     */
    private boolean isServiceOn;
    /**
     * Is the service bound by the IBinder.
     */
    private boolean isServiceBound = false;

    private GoogleMap googleMap;

    private TextView tvElapsedTime, tvSpeed, tvCadence, tvDistance, tvGearRatio, tvAcceleration,
            tvPower, tvTorque, tvWork, tvSteepness, tvElevation, tvErrors;
    private Location lastLocation;
    /**
     * LatLng points of the desired path.
     */
    private ArrayList<LatLng> pathPoints;
    /**
     * Desired arrival time from the service start in minutes.
     */
    private int arrivalMinutes;
    private Polyline desiredPath;
    //</editor-fold>

    //<editor-fold desc="LIFECYLE">
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "onCreate");

        tvElapsedTime = (TextView) findViewById(R.id.tv_elapsed_time);
        tvSpeed = (TextView) findViewById(R.id.tv_speed);
        tvCadence = (TextView) findViewById(R.id.tv_cadence);
        tvDistance = (TextView) findViewById(R.id.tv_distance);
        tvGearRatio = (TextView) findViewById(R.id.tv_gear_ratio);
        tvAcceleration = (TextView) findViewById(R.id.tv_acceleration);
        tvPower = (TextView) findViewById(R.id.tv_power);
        tvTorque = (TextView) findViewById(R.id.tv_torque);
        tvWork = (TextView) findViewById(R.id.tv_work);
        tvSteepness = (TextView) findViewById(R.id.tv_steepness);
        tvElevation = (TextView) findViewById(R.id.tv_elevation);
        tvErrors = (TextView) findViewById(R.id.tv_errors);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        isServiceOn = sharedPreferences.getBoolean(KEY_IS_SERVICE_ON, false);

        setUpMapIfNeeded();

        Button btnStartWorkout = (Button) findViewById(R.id.btn_start_workout);
        btnStartWorkout.setText((!isServiceOn) ? getResources().getString(R.string.start_workout) : getResources().getString(R.string.stop_workout));
        btnStartWorkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startStop();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MainActivity", "onResume");

        if (isServiceOn) {
            Intent serviceIntent = new Intent(MainActivity.this, WorkoutService.class);
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
        setUpMapIfNeeded();
    }

    @Override
    protected void onPause() {
        Log.d("MainActivity", "onPause");
        if (isServiceBound && isServiceOn) unbindService(serviceConnection);
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("MainActivity", "onStart");
    }

    @Override
    protected void onStop() {
        Log.d("MainActivity", "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d("MainActivity", "onDestroy");
        super.onDestroy();
    }

    //</editor-fold>

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. T
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (googleMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.main_activity_map_container);
            mapFragment.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap gm) {
                    googleMap = gm;
                    googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                        @Override
                        public void onMapClick(LatLng latLng) {
                            bakeToast("click");
                            if (pathPoints == null || arrivalMinutes == 0){
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setCancelable(true);
                                builder.setItems(R.array.main_map_actions_array_no_path, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (isServiceOn) {
                                            bakeToast(getResources().getString(R.string.stop_workout_in_progress));
                                            dialog.cancel();
                                        }else {
                                            Intent intent = new Intent(MainActivity.this, AppointPathActivity.class);
                                            if (lastLocation != null) {
                                                intent.putExtra(LAST_LOCATION, lastLocation);
                                                startActivityForResult(intent, REQUEST_APPOINT_PATH);
                                            } else {
                                                bakeToast(getResources().getString(R.string.last_location_is_null));
                                            }
                                        }
                                        dialog.cancel();
                                    }
                                });
                                AlertDialog dialog = builder.create();
                                dialog.show();

                            }else{
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setCancelable(true);
                                builder.setItems(R.array.main_map_actions_array_path_ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (desiredPath != null){
                                            desiredPath.remove();
                                            desiredPath = null;
                                        }
                                        arrivalMinutes = 0;
                                        pathPoints = null;
                                    }
                                });
                                AlertDialog dialog = builder.create();
                                dialog.show();

                            }
                        }
                    });
                    //this returns the last location in the callback
                    new LocationService(MainActivity.this);
                    //only after we have a map do we acquire and mark our position
                }
            });
        }
    }

    //<editor-fold desc="SERVICE CONNECTION">
    private void startStop() {
        Intent serviceIntent = new Intent(MainActivity.this, WorkoutService.class);
        Button btnStartWorkout = (Button) findViewById(R.id.btn_start_workout);
        LocationManager service = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean enabled = service
                .isProviderEnabled(LocationManager.GPS_PROVIDER);
        if ((!isServiceOn) && (enabled)) {//S T A R T
            startService(serviceIntent); //Starting the service
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE); //Binding to the service!

            bakeToast(getResources().getString(R.string.session_started));
            isServiceOn = true;
            btnStartWorkout.setText(getResources().getString(R.string.stop_workout));
        } else if (!isServiceOn) {//not enabled
            //if GPS is not enabled, we navigate to the GPS on screen in the device settings
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        } else {//S T O P

            if (workoutService != null) {
                workoutService.stopCallbacks();
                workoutService.unregisterClients();
            }
            if (isServiceBound) unbindService(serviceConnection);
            stopService(serviceIntent);
            bakeToast(getResources().getString(R.string.session_ended));
            isServiceOn = false;
            isServiceBound = false;
            btnStartWorkout.setText(getResources().getString(R.string.start_workout));

            //<editor-fold desc="CLOSING WORKOUT IN DATABASE">
            Thread threadCloseDb = new Thread() {
                public void run() {
                    ManageDb manageDb = ManageDb.getInstance(MainActivity.this);
                    while (manageDb.isDatabaseSet()){
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignored) {
                        }
                    }
                    try {
                        manageDb.setupDb(false);
                        //getting the summary data from the last entry record
                        WorkoutEntry lastEntry;
                        double wholeDistance = 0.0;
                        double wholeWork = 0.0;
                        try {
                            lastEntry = manageDb.getLastEntry();
                            if (lastEntry.getTimeInMillis() >= manageDb.getLastWorkout().getStartTime()) {//there's at least one entry in the workout
                                wholeDistance = lastEntry.getDistance();
                                wholeWork = lastEntry.getWork();
                            }
                        } catch (Exception e) {
                            Log.d("MainActivity", "error getting last workout: " + e);
                        }
                        long currentTime = System.currentTimeMillis(); //There has to be one variable like this, so the following two database
                        //writes will get the same ending time
                        //writing summary + one ending record with the currentTime
                        int status = manageDb.writeEntry(ByteConverter.convertKeyToByteArray(currentTime), ByteConverter.getClosingRecordData());
                        if (status != WorkoutService.DATABASE_OK)
                            Log.d("MainActivity", "Error writing closing entry " + status);
                        status = manageDb.writeFirstRecordSummary(currentTime, wholeDistance, wholeWork);
                        if (status != WorkoutService.DATABASE_OK)
                            Log.d("MainActivity", "Error writing summary " + status);
                        manageDb.closeDb();
                    } catch (DatabaseException | IllegalArgumentException e) {
                        Log.d("MainActivity", "error closing workout in database");
                    }
                }
            };

            threadCloseDb.start();

            //</editor-fold>
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            WorkoutService.LocalBinder binder = (WorkoutService.LocalBinder) service;
            workoutService = binder.getServiceInstance(); //Get instance of your service!
            isServiceBound = true;
            workoutService.registerClient(MainActivity.this); //Activity register in the service as client for callbacks
            workoutService.startCallbacks();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            workoutService.stopCallbacks();
            workoutService.unregisterClients();
            isServiceBound = false;
            bakeToast(getResources().getString(R.string.workout_stopped));
        }
    };
    //</editor-fold>

    //<editor-fold desc="CALLBACKS">
    @Override
    public void transmitBasicData(final DisplayData data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvElapsedTime.setText(data.getTime() / 60000 + ":" + ((((data.getTime() / 1000) % 60) < 10) ? ("0" + (data.getTime() / 1000) % 60) : ((data.getTime() / 1000) % 60)));
                DecimalFormat decimalFormat = new DecimalFormat("0.000");
                tvDistance.setText(decimalFormat.format(data.getDistance() / 1000) + " km");
                tvWork.setText(decimalFormat.format(data.getWork() / 1000) + " kJ");
                tvAcceleration.setText(decimalFormat.format(data.getAcceleration()) + " m/s/s");
                decimalFormat = new DecimalFormat("0.0");
                tvSpeed.setText(decimalFormat.format(data.getSpeed()) + " km/h");
                tvCadence.setText(decimalFormat.format(data.getCadence()) + " rpm");
                tvPower.setText(decimalFormat.format(data.getPower()) + " W");
                tvGearRatio.setText((DecimalToFraction.convertToString(data.getGearRatio()).length() > 6) ?
                        DecimalToFraction.convertToString(data.getGearRatio()).substring(0, 6) :
                        DecimalToFraction.convertToString(data.getGearRatio()));
                decimalFormat = new DecimalFormat("0.00");
                tvTorque.setText(decimalFormat.format(data.getTorque()) + " Nm");
                tvSteepness.setText(decimalFormat.format(data.getSteepness()) + " %");
                tvElevation.setText(decimalFormat.format(data.getElevation()) + " m");
                tvErrors.setText(data.getErrors()[0] + ";" + data.getErrors()[1] + ";" + data.getErrors()[2] + ";" + data.getErrors()[3] + ";" + data.getErrors()[4] + ";" + data.getErrors()[5]);
                googleMap.clear();
                googleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(data.getLatitude(), data.getLongitude())));
                //todo register errors
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_APPOINT_PATH && resultCode == RESULT_OK) {
            pathPoints = data.getParcelableArrayListExtra(KEY_POLYLINE);
            arrivalMinutes = data.getIntExtra(KEY_ARRIVAL_TIME_MINUTES, 0);
            PolylineOptions polylineOptions = new PolylineOptions();
            for (int i=0; i<pathPoints.size();i++){
                polylineOptions.add(pathPoints.get(i));
            }
            desiredPath = googleMap.addPolyline(polylineOptions);
            //todo draw on map
            //todo service intent putExtra path

        }
    }

    @Override
    public void transmitLocationData(int validity, Location location) {
        if (validity == WorkoutService.LOCATION_OK) {
            this.lastLocation = location;
            if (googleMap != null){
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude()) ;
                googleMap.addMarker(new MarkerOptions().position(latLng));
                CameraUpdate cameraUpdate  = CameraUpdateFactory.newLatLngZoom(latLng, 15);
                googleMap.moveCamera(cameraUpdate);
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="OPTIONS">
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent workoutData in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_browse_workouts) {
            if (isServiceOn) {
                bakeToast(getResources().getString(R.string.stop_workout_in_progress));
                return true;
            }
            startActivity(new Intent(MainActivity.this, WorkoutsOverviewActivity.class));
            return true;
        } else if (id == R.id.action_appoint_path) {
            if (isServiceOn) {
                bakeToast(getResources().getString(R.string.stop_workout_in_progress));
                return true;
            }
            Intent intent = new Intent(MainActivity.this, AppointPathActivity.class);
            if (lastLocation != null) {
                intent.putExtra(LAST_LOCATION, lastLocation);
                startActivityForResult(intent, REQUEST_APPOINT_PATH);
            }else bakeToast(getResources().getString(R.string.last_location_is_null));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //</editor-fold>

    void bakeToast(String text) {
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
    }
}
