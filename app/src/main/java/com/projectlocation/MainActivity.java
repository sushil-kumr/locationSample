package com.projectlocation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "sushildlh";
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;


    public static final int LOCATION_PERMISSION = 1;
    public static final String LOCATION_TEXT = "LOCATION";

    private final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setTitle("Location");

        mSettingsClient = LocationServices.getSettingsClient(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        requestLocationPermissions();

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getLastLocationNewMethod();
            }
        });

    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                Toast.makeText(MainActivity.this, location.getLongitude() + "," + location.getLatitude(), Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }


    private void getLastLocationNewMethod() {
        if (isLocationOn(this)) {
            Toast.makeText(this, "Turn On Location", Toast.LENGTH_SHORT).show();
        } else {
            getPackageManager().checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, getPackageName());
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // GPS location can be null if GPS is switched off
                            if (location != null) {
                                Toast.makeText(MainActivity.this, location.getLongitude() + "," + location.getLatitude(), Toast.LENGTH_SHORT).show();

                                if (mFusedLocationClient != null) {
                                    mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                                }

                            } else {
                                Toast.makeText(MainActivity.this, "Location searching ...", Toast.LENGTH_SHORT).show();

                                startLocationUpdates();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivity.this, "Error trying to get last GPS location", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    });
        }
    }


    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        //noinspection MissingPermission
                        getPackageManager().checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, getPackageName());
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());
                        getLastLocationNewMethod();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, 0x1);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                //  mRequestingLocationUpdates = false;
                        }
                        getLastLocationNewMethod();
                    }
                });
    }

    private void requestLocationPermissions() {
        if (!hasStoragePermission(this)) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION);


            } else if (getSharePrefernceBoolean(this, LOCATION_TEXT)) {
                Toast.makeText(this, "Need Location Permission", Toast.LENGTH_SHORT).show();
            } else {
                //just request the permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION);

            }
            setSharePrefernceBoolean(this, LOCATION_TEXT, true);

        } else {
            getLastLocationNewMethod();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {

            case LOCATION_PERMISSION:
                if (validateGrantedPermissions(grantResults)) {
                    getLastLocationNewMethod();
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    public static boolean hasStoragePermission(Context context) {
        int finePermissionCheck = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int courePermissionCheck = ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && (finePermissionCheck == PackageManager.PERMISSION_DENIED
                || courePermissionCheck == PackageManager.PERMISSION_DENIED));
    }

    public static boolean validateGrantedPermissions(int[] grantResults) {
        boolean isGranted = true;
        if (grantResults != null && grantResults.length > 0) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    isGranted = false;
                    break;
                }
            }
            return isGranted;
        } else {
            isGranted = false;
            return isGranted;
        }
    }

    public static boolean isLocationOn(Context context) {

        LocationManager mLocationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        boolean gps_enabled = false, network_enabled = false;

        try {
            gps_enabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        try {
            network_enabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        return !gps_enabled && !network_enabled;
    }

    public static void setSharePrefernceBoolean(Context ctx, String name, boolean flag) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ctx).edit();
        editor.putBoolean(name, flag);
        editor.commit();
    }

    public static boolean getSharePrefernceBoolean(Context ctx, String name) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(name, true);
    }

}
