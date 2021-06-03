package com.codenamegradient.gpsscanner;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.sql.Time;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    /**
     * Class properties
     */
    private boolean pause = false;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private final int PERMISSION_LOCATION = 0;
    private GoogleMap mMap;
    private int mapZoom = 16;
    private int mapTilt = 0;
    private final int mapAnimateSpeed = 250;

    /**
     * Our main method which starts the application for the first time
     * @param savedInstanceState saved instance
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startLocationUpdates();
    }

    /**
     * Handles the device resuming the application action
     */
    @Override
    protected void onResume() {
        super.onResume();

        if (!pause) {
            startLocationUpdates();
        }
    }

    /**
     * Handles the application being closed or device going to sleep action
     */
    @Override
    protected void onPause() {
        super.onPause();

        stopLocationUpdates(true);
    }

    /**
     * Start updating the UI with location requests
     */
    private void startLocationUpdates() {

        Button button = findViewById(R.id.pauseResume);

        pause = false;
        button.setBackgroundColor(getResources().getColor(R.color.colorButtonActive));
        button.setText(getString(R.string.pause));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
        } else {
            // create fused location client
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            // get the last known location and update the UI
            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        updateStrings(location);
                    }
                }
            });

            // create the location request that will get updates
            LocationRequest mLocationRequest = LocationRequest.create();
            mLocationRequest.setInterval(1000);
            mLocationRequest.setFastestInterval(1000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            // updates the map when a our location updates
            mLocationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    for (Location location : locationResult.getLocations()) {
                        // update current position on map
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        float bearing = location.getBearing();

                        CameraPosition cameraPosition = new CameraPosition.Builder()
                                .target(latLng)
                                .zoom(mapZoom)
                                .tilt(mapTilt)
                                .bearing(bearing)
                                .build();

                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), mapAnimateSpeed, null);

                        // update strings
                        if (!pause) {
                            updateStrings(location);
                        }
                    }
                }
            };
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);

            // register map callback
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }
    }

    /**
     * Prompt user to give us the permissions we need
     */
    private void requestPermissions() {
        // if we don't have location permissions, stop updates (for UI purposes mostly)
        stopLocationUpdates();

        // request permission from user
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION);
    }

    /**
     * When the user has paused updates, the device has switched applications, or the device has gone to sleep we want to stop updating to save battery
     *
     */
    private void stopLocationUpdates() {
        Button button = findViewById(R.id.pauseResume);

        pause = true;
        button.setBackgroundColor(getResources().getColor(R.color.colorButtonPaused));
        button.setText(getString(R.string.resume));

        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    /**
     * Overloaded method that does not change the pause/resume state (this is used when the app is resumed after switching from another app for example)
     * @param ignoreState ignore state
     */
    private void stopLocationUpdates(boolean ignoreState) {
        Button button = findViewById(R.id.pauseResume);

        if (!ignoreState) {
            pause = true;
        }
        button.setBackgroundColor(getResources().getColor(R.color.colorButtonPaused));
        button.setText(getString(R.string.resume));

        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    /**
     * Converts a decimal latitude or longitude into degree/minute/second format
     * @param dd (decimal degrees)
     * @return string degree, minute, seconds format
     * Note: this isn't currently being used. I'm not sure if I'll ever really implement this.
     */
    private String decimalToDMS(double dd) {
        // todo: ensure this is actually accurate, accounts for directions etc.
        // basically I just grabbed this of a page I found via Google and don't really know how correct it is
        int d = (int) dd;
        int m = (int) (dd - d) * 60;
        int s = (int) ((dd - d - m / 60) * 3600);

        return d + "\u00b0 " + m + "' " + s + "''";
    }

    /**
     * Update the various TextViews
     * @param location a standard location object
     */
    private void updateStrings(Location location) {
        DecimalFormat ultraPrecision = new DecimalFormat("###.#####");
        DecimalFormat highPrecision = new DecimalFormat("###.###");
        DecimalFormat standardPrecision = new DecimalFormat("###.#");

        // latitude
        TextView latitude = findViewById(R.id.latitude);
        double latitudeRaw = location.getLatitude();
        latitude.setText(getString(R.string.latitude, ultraPrecision.format(latitudeRaw)));

        // longitude
        TextView longitude = findViewById(R.id.longitude);
        double longitudeRaw = location.getLongitude();
        longitude.setText(getString(R.string.longitude, ultraPrecision.format(longitudeRaw)));

        // accuracy
        TextView accuracy = findViewById(R.id.accuracy);
        float accuracyRaw = location.getAccuracy();
        accuracy.setText(getString(R.string.accuracy, standardPrecision.format(accuracyRaw), standardPrecision.format(accuracyRaw * 3.28084)));

        // bearing
        TextView bearing = findViewById(R.id.bearing);
        bearing.setText(getString(R.string.bearing, standardPrecision.format(location.getBearing())));

        // altitude
        TextView altitude = findViewById(R.id.altitude);
        double altitudeRaw = location.getAltitude();
        altitude.setText(getString(R.string.altitude, standardPrecision.format(altitudeRaw), standardPrecision.format(altitudeRaw * 3.28084)));

        // speed
        TextView speed = findViewById(R.id.speed);
        TextView speedKilometers = findViewById(R.id.speedKilometers);
        TextView speedMiles = findViewById(R.id.speedMiles);
        TextView speedKnots = findViewById(R.id.speedKnots);
        float speedRaw = location.getSpeed();
        speed.setText(getString(R.string.speed, standardPrecision.format(speedRaw)));
        speedKilometers.setText(getString(R.string.speed_kmh, standardPrecision.format(speedRaw * 3.6)));
        speedMiles.setText(getString(R.string.speed_mph, standardPrecision.format(speedRaw * 2.23694)));
        speedKnots.setText(getString(R.string.speed_knots, standardPrecision.format(speedRaw * 1.94384)));

        // time
        TextView time = findViewById(R.id.time);
        TextView timeHuman = findViewById(R.id.timeHuman);
        long timeRaw = location.getTime();
        Time timeObj = new Time(timeRaw);
        time.setText(getString(R.string.time, timeRaw));
        timeHuman.setText(getString(R.string.time_human, timeObj.toString()));

        // provider
        TextView provider = findViewById(R.id.provider);
        provider.setText(getString(R.string.provider, location.getProvider()));
    }

    private void addMarker() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // get the last known location and drop pin
            mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

                        mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("Paused Position"));
                    }
                }
            });
        }
    }

    /**
     * Updates the camera position, this is done after changing a zoom/tilt value
     */
    private void updateMapCamera() {
        // gets the map current camera position
        CameraPosition current = mMap.getCameraPosition();
        LatLng cLatLng = current.target;
        float cBearing = current.bearing;

        // create a new camera position object with our new tilt and the other existing values
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(cLatLng)
                .zoom(mapZoom)
                .tilt(mapTilt)
                .bearing(cBearing)
                .build();

        // change the camera position
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), mapAnimateSpeed, null);
    }

    /**
     * Handles a permissions request result
     * @param requestCode request code
     * @param permissions permissions
     * @param grantResults grant results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                // user has denied location access, make some toast
                Context context = getApplicationContext();
                CharSequence text = "Location access required!";
                int duration = Toast.LENGTH_LONG;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        }
    }

    /**
     * Handles the Pause/Resume button action
     * @param view view
     */
    public void togglePause(View view) {
        if (pause) {
            mMap.clear();
            startLocationUpdates();
        } else {
            addMarker();
            stopLocationUpdates();
        }
    }

    /**
     * Creates map with our settings
     * @param googleMap map fragment
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // shows the current location blue dot
            mMap.setMyLocationEnabled(true);

            // disable most of the map interactions
            UiSettings settings = mMap.getUiSettings();
            settings.setAllGesturesEnabled(false);
            settings.setMyLocationButtonEnabled(false);
            settings.setCompassEnabled(false);
        }
    }

    /**
     * Changes the map type when the button is tapped
     * @param view view
     */
    public void mapTypePicker(View view) {
        int mapType = mMap.getMapType();
        Button button = findViewById(R.id.cycleMapType);

        if (mapType == GoogleMap.MAP_TYPE_NORMAL) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            button.setText(R.string.map_hybrid);
        } else if (mapType == GoogleMap.MAP_TYPE_HYBRID) {
            mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
            button.setText(R.string.map_terrain);
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            button.setText(R.string.map_normal);
        }
    }

    /**
     * Changes the map zoom on button tap
     */
    public void mapZoomPicker(View view) {
        Button button = findViewById(R.id.mapZoom);

        if (mapZoom == 5) {
            mapZoom = 10;
        } else if (mapZoom == 10) {
            mapZoom = 13;
        } else if (mapZoom == 13) {
            mapZoom = 16;
        } else if (mapZoom == 16) {
            mapZoom = 18;
        } else if (mapZoom == 18) {
            mapZoom = 20;
        } else {
            mapZoom = 5;
        }

        button.setText(getString(R.string.zoom, mapZoom));

        updateMapCamera();
    }

    /**
     * Changes the map tilt on button tap
     */
    public void mapTiltPicker(View view) {
        Button button = findViewById(R.id.mapTilt);

        if (mapTilt == 0) {
            mapTilt = 15;
        } else if (mapTilt == 15) {
            mapTilt = 30;
        } else if (mapTilt == 30) {
            mapTilt = 45;
        } else if (mapTilt == 45) {
            mapTilt = 60;
        } else {
            mapTilt = 0;
        }

        button.setText(getString(R.string.tilt, mapTilt));

        updateMapCamera();
    }
}
