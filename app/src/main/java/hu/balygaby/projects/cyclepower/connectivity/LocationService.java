package hu.balygaby.projects.cyclepower.connectivity;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import hu.balygaby.projects.cyclepower.MainActivity;
import hu.balygaby.projects.cyclepower.R;
import hu.balygaby.projects.cyclepower.WorkoutService;

/**
 * <p>When instantiated with a workoutService parameter, starts a location request for 1 second location updates.
 * When an update arrives, calls the {@link InputData#transmitLocationData(int, Location)} callback.
 * To start updates, just call the constructor and implement the InputData interface.
 * To end updates, call {@link #stopLocationUpdates()} followed by {@link #collapseGoogleApiClient()}
 * to end it all.</p>
 * <p>When instantiated with a mainActivity parameter, automatically gets the last location data and returns
 * via the implementation of the {@link hu.balygaby.projects.cyclepower.connectivity.LocationService.LastLocationData}
 * interface. Automatically kills the GoogleApiClient.</p>
 */
public class LocationService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private boolean isReconnecting = false;
    private GoogleApiClient mGoogleApiClient;
    WorkoutService workoutService;
    MainActivity mainActivity;
    Context context;

    /**
     * See {@link LocationService}
     * @param workoutService WorkoutService instance.
     */
    public LocationService(WorkoutService workoutService) {
        this.workoutService = workoutService;
        this.context = workoutService.getBaseContext();
        buildGoogleApiClient();//set up connection
    }

    public LocationService(MainActivity mainActivity){
        this.mainActivity = mainActivity;
        this.context = mainActivity.getBaseContext();
        buildGoogleApiClient();//set up connection
    }

    protected synchronized void buildGoogleApiClient() {//runs automatically on startup
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();//Connecting to Google Play Services location provider
    }

    public void collapseGoogleApiClient(){
        if (mGoogleApiClient.isConnected()) mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {//callback from connecting
        if (mainActivity !=null) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                    mGoogleApiClient);
            if (lastLocation != null) {
                LastLocationData lastLocationData = mainActivity;
                lastLocationData.transmitLocationData(WorkoutService.LOCATION_OK, lastLocation);
            }
            collapseGoogleApiClient();//automatically stop
        } else if (workoutService !=null){
            startLocationUpdates();
            if (isReconnecting) {
                bakeToast(workoutService.getResources().getString(R.string.reconnected));
                isReconnecting = false;
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        bakeToast(workoutService.getResources().getString(R.string.connection_suspended));
        mGoogleApiClient.reconnect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        bakeToast(workoutService.getResources().getString(R.string.connection_failed));
        mGoogleApiClient.reconnect();
    }

    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        return mLocationRequest;
    }

    public void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, createLocationRequest(), this);
    }

    public void stopLocationUpdates() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        }
    }

    @Override
    public void onLocationChanged(Location location) {//this arrives due to the LocationRequest
        //when a position arrives, write it to file and also to the bottom of the screen
        InputData inputData = workoutService;
        inputData.transmitLocationData(WorkoutService.LOCATION_OK, location);
    }

    private void bakeToast(CharSequence sequence) {
        Toast.makeText(context, sequence, Toast.LENGTH_SHORT).show();
    }

    public interface InputData {

        /**
         * Callback when we have a new location.
         * @param validity Validity of the location object. Possible values:
         *                 <p>
         *                 {@link WorkoutService#LOCATION_OK}
         *                 </p>
         * @param location The raw location object.
         */
        void transmitLocationData(int validity, Location location);
    }

    public interface LastLocationData{
        /**
         * Callback when we have the last location.
         * @param validity Validity of the location object. Possible values:
         *                 <p>
         *                 {@link WorkoutService#LOCATION_OK}
         *                 </p>
         * @param location The raw location object.
         */
        void transmitLocationData(int validity, Location location);

    }
}
