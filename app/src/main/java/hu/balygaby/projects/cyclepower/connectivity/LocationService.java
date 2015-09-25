package hu.balygaby.projects.cyclepower.connectivity;

import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import hu.balygaby.projects.cyclepower.R;
import hu.balygaby.projects.cyclepower.WorkoutService;

/**
 * Starts a location request for 1 second location updates.
 * When an update arrives, calls the {@link InputData#transmitLocationData(int, Location)} callback.
 * To start updates, just call the constructor and implement the InputData interface.
 * To end updates, call {@link #stopLocationUpdates()} followed by {@link #collapseGoogleApiClient()}
 * to end it all.
 */
public class LocationService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private boolean isReconnecting = false;
    private boolean hasNoticedNoLocation = false;
    private GoogleApiClient mGoogleApiClient;
    WorkoutService workoutService;

    /**
     * See {@link LocationService}
     * @param workoutService WorkoutService instance.
     */
    public LocationService(WorkoutService workoutService) {
        this.workoutService = workoutService;
        buildGoogleApiClient();//set up connection
    }

    protected synchronized void buildGoogleApiClient() {//runs automatically on startup
        mGoogleApiClient = new GoogleApiClient.Builder(workoutService)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();//Connecting to Google Play Services location provider
    }

    public void collapseGoogleApiClient(){
        if (mGoogleApiClient.isConnected()) mGoogleApiClient.disconnect();
    }

    //todo call stopUpdates and collapse

    @Override
    public void onConnected(Bundle connectionHint) {//callback from connecting
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        startLocationUpdates();
        if (isReconnecting) {
            bakeToast(workoutService.getResources().getString(R.string.reconnected));
            isReconnecting = false;
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
        if (hasNoticedNoLocation) {
            hasNoticedNoLocation = false;
        }
//TODO when is it not valid?
        InputData inputData = workoutService;
        inputData.transmitLocationData(InputData.INPUT_VALID, location);

    }

    private void noLocation() {
        if (!hasNoticedNoLocation) {
            bakeToast(workoutService.getResources().getString(R.string.last_location_is_null));
            hasNoticedNoLocation = true;
        }
    }

    private void bakeToast(CharSequence sequence) {
        Toast.makeText(workoutService, sequence, Toast.LENGTH_SHORT).show();
    }
}
