package hu.balygaby.projects.cyclepower;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sleepycat.je.DatabaseException;

import hu.balygaby.projects.cyclepower.database.ByteConverter;
import hu.balygaby.projects.cyclepower.database.ManageDb;
import hu.balygaby.projects.cyclepower.database.objects.WorkoutEntry;
import hu.balygaby.projects.cyclepower.settings.SettingsActivity;


public class MainActivity extends Activity implements ServiceCallbacks{

    public static final String KEY_IS_SERVICE_ON = "is_service_on";
    private static final String INITIAL_LATITUDE = "initial_latitude";
    private static final String INITIAL_LONGITUDE = "initial_longitude";
    /**
     * The service instance.
     */
    WorkoutService workoutService;
    /**
     * The service runs independently from the workoutData. In this variable
     * the service status is stored for reference purposes.
     */
    boolean isServiceOn;
    /**
     * Is the service bound by the IBinder.
     */
    private boolean isServiceBound = false;

    TextView tvElapsedTime, tvSpeed, tvCadence, tvDistance, tvGearRatio, tvAcceleration,
            tvPower, tvTorque, tvWork, tvSteepness, tvElevation, tvLatitude, tvLongitude, tvErrors;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO everything in a service, which is synced with the app whenever it's in foreground
        //calculate acceleration, power, torque
        //calculate optimal speed to get to a point

        tvElapsedTime = (TextView)findViewById(R.id.tv_elapsed_time);
        tvSpeed = (TextView)findViewById(R.id.tv_speed);
        tvCadence = (TextView)findViewById(R.id.tv_cadence);
        tvDistance = (TextView)findViewById(R.id.tv_distance);
        tvGearRatio = (TextView)findViewById(R.id.tv_gear_ratio);
        tvAcceleration = (TextView)findViewById(R.id.tv_acceleration);
        tvPower = (TextView)findViewById(R.id.tv_power);
        tvTorque = (TextView)findViewById(R.id.tv_torque);
        tvWork = (TextView)findViewById(R.id.tv_work);
        tvSteepness = (TextView)findViewById(R.id.tv_steepness);
        tvElevation = (TextView)findViewById(R.id.tv_elevation);
        tvLatitude = (TextView)findViewById(R.id.tv_latitude);
        tvLongitude = (TextView)findViewById(R.id.tv_longitude);
        tvErrors = (TextView)findViewById(R.id.tv_errors);

        /*Log.d("debug","bytearray: "+ Arrays.toString(ByteConverter.convertDataToByteArray(10000, 10000, 30.56, 70.44, 121.11, 55.65, 47.5034524624, 19.10232442)));
        Log.d("debug",""+ByteConverter.getDistanceFromBytes(ByteConverter.convertDataToByteArray(10000, 10000, 30.56, 70.44, 121.11, 55.65, 47.5034524624, 19.10232442)));
        Log.d("debug",""+ByteConverter.getWorkFromBytes(ByteConverter.convertDataToByteArray(10000, 10000, 30.56, 70.44, 121.11, 55.65, 47.5034524624, 19.10232442)));
        Log.d("debug",""+ByteConverter.getSpeedFromBytes(ByteConverter.convertDataToByteArray(10000, 10000, 30.56, 70.44, 121.11, 55.65, 47.5034524624, 19.10232442)));
        Log.d("debug", "" + ByteConverter.getCadenceFromBytes(ByteConverter.convertDataToByteArray(10000, 10000, 30.56, 70.44, 121.11, 55.65, 47.5034524624, 19.10232442)));
        Log.d("debug",""+ByteConverter.getPowerFromBytes(ByteConverter.convertDataToByteArray(10000, 10000, 30.56, 70.44, 121.11, 55.65, 47.5034524624, 19.10232442)));
        Log.d("debug",""+ByteConverter.getTorqueFromBytes(ByteConverter.convertDataToByteArray(10000, 10000, 30.56, 70.44, 121.11, 55.65, 47.5034524624, 19.10232442)));
        Log.d("debug",""+ByteConverter.getLatitudeFromBytes(ByteConverter.convertDataToByteArray(10000, 10000, 30.56, 70.44, 121.11, 55.65, 47.5034524624, 19.10232442)));
        Log.d("debug",""+ByteConverter.getLongitudeFromBytes(ByteConverter.convertDataToByteArray(10000, 10000, 30.56, 70.44, 121.11, 55.65, 47.5034524624, 19.10232442)));

        long time = System.currentTimeMillis();
        Log.d("debug",""+time);
        Log.d("debug", "" + Arrays.toString(ByteConverter.convertKeyToByteArray(time)));
        Log.d("debug",""+ ByteConverter.getTimeFromBytes(ByteConverter.convertKeyToByteArray(time)));

        byte[] firstRecord = ByteConverter.convertFirstRecordHeaderToByteArray(new byte[0], System.currentTimeMillis());
        Log.d("debug",""+ Arrays.toString(firstRecord));
        firstRecord = ByteConverter.convertFirstRecordSummaryToByteArray(firstRecord, System.currentTimeMillis(),20000,30000);
        Log.d("debug",""+ Arrays.toString(firstRecord));

        Log.d("debug",""+ Arrays.toString(ByteConverter.getFirstRecordKey()));

        Log.d("debug",""+ ByteConverter.getWorkoutsFromBytes(firstRecord).get(0).getStartTime());
        Log.d("debug",""+ ByteConverter.getWorkoutsFromBytes(firstRecord).get(0).getEndTime());
        Log.d("debug",""+ ByteConverter.getWorkoutsFromBytes(firstRecord).get(0).getDistance());
        Log.d("debug",""+ ByteConverter.getWorkoutsFromBytes(firstRecord).get(0).getWork());
        */


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        isServiceOn = sharedPreferences.getBoolean(KEY_IS_SERVICE_ON, false);
        if (isServiceOn){
            Intent serviceIntent = new Intent(MainActivity.this, WorkoutService.class);
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }

        Button btnStartWorkout = (Button) findViewById(R.id.btn_start_workout);
        btnStartWorkout.setText((!isServiceOn)?getResources().getString(R.string.start_workout):getResources().getString(R.string.stop_workout));
        btnStartWorkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startStop();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        if (isServiceBound && isServiceOn) unbindService(serviceConnection);
        super.onStop();
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
            serviceIntent.removeExtra(INITIAL_LONGITUDE); serviceIntent.removeExtra(INITIAL_LATITUDE);
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE); //Binding to the service!

            bakeToast(getResources().getString(R.string.session_started));
            isServiceOn = true;
            btnStartWorkout.setText(getResources().getString(R.string.stop_workout));
        } else if (!isServiceOn){//not enabled
            //if GPS is not enabled, we navigate to the GPS on screen in the device settings
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }else {//S T O P
            if (workoutService!=null) {
                workoutService.stopCallbacks();
                workoutService.unregisterClients();
            }
            if (isServiceBound) unbindService(serviceConnection);
            stopService(serviceIntent);
            bakeToast(getResources().getString(R.string.session_ended));
            isServiceOn = false; isServiceBound = false;
            btnStartWorkout.setText(getResources().getString(R.string.start_workout));

            //<editor-fold desc="CLOSING WORKOUT IN DATABASE">
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {//delayed, so that the database instance in the service will be closed
                @Override
                public void run() {
                    try {
                        ManageDb manageDb = ManageDb.getInstance(MainActivity.this);
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
                        int status = manageDb.writeEntry(ByteConverter.convertKeyToByteArray(currentTime),ByteConverter.getClosingRecordData());
                        if (status!=WorkoutService.DATABASE_OK)
                            Log.d("MainActivity","Error writing closing entry "+status);
                        status = manageDb.writeFirstRecordSummary(currentTime,wholeDistance,wholeWork);
                        if (status!=WorkoutService.DATABASE_OK)
                            Log.d("MainActivity","Error writing summary " + status);
                        manageDb.closeDb();
                    } catch (DatabaseException | IllegalArgumentException e) {
                        Log.d("MainActivity","error closing workout in database");
                    }
                }
            },500);//todo do we need this handler?

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

    @Override
    public void transmitBasicData(final DisplayData data) {
        //TODO display data
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvElapsedTime.setText(data.getTime()/60000 + ":" + (  (   ((data.getTime()/1000)%60)    <10)?  ("0"+(data.getTime()/1000)%60)  :   ((data.getTime()/1000)%60) ));
                tvSpeed.setText(data.getSpeed()+" km/h");
                tvCadence.setText(data.getCadence() + " rpm");
                tvDistance.setText(data.getDistance() + " m");
                tvGearRatio.setText(data.getGearRatio()+"");
                tvAcceleration.setText(data.getAcceleration() + " m/s/s");
                tvPower.setText(data.getPower() + " W");
                tvTorque.setText(data.getTorque() + " Nm");
                tvWork.setText(data.getWork() + "J");
                tvSteepness.setText(data.getSteepness() + " %");
                tvElevation.setText(data.getElevation() + " m");
                tvLatitude.setText(data.getLatitude() + "");
                tvLongitude.setText(data.getLongitude() +"");
                tvErrors.setText(data.getErrors()[0] + ";" + data.getErrors()[1] + ";" + data.getErrors()[2] + ";" + data.getErrors()[3] + ";" + data.getErrors()[4] + ";" + data.getErrors()[5]);

                //todo register errors
            }
        });
    }


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
        }

        return super.onOptionsItemSelected(item);
    }
    //</editor-fold>

    void bakeToast(String text){
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
    }

}
