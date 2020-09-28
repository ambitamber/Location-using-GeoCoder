package com.example.location;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, LocationListener {

    Button button;
    TextView textView;
    public static GoogleApiClient client;
    public static FusedLocationProviderClient fusedLocationProviderClient;
    public ArrayList<String> permissionsToRequest;
    public ArrayList<String> permissions = new ArrayList<>();
    public ArrayList<String> permissionsRejected = new ArrayList<>();


    public Boolean obtainedAddress=false;
    public static final long UPDATE_INTERVAL=5000;
    private LocationRequest locationRequest;
    private static final int ALL_PERMISSIONS_RESULT = 1111;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.getbutton);
        textView = findViewById(R.id.TV);

        client = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addOnConnectionFailedListener(MainActivity.this)
                .build();
        fusedLocationProviderClient =LocationServices.getFusedLocationProviderClient(MainActivity.this);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissionsToRequest = permissionsToRequestMtd(permissions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0) {
                requestPermissions(permissionsToRequest.toArray(
                        new String[permissionsToRequest.size()]),
                        ALL_PERMISSIONS_RESULT
                );
            }
        }
        if (obtainedAddress){
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            client.disconnect();
        }else {
            startLocationUpdates();
        }

    }

    private void startLocationUpdates() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("LOCATION","NO PERMISSION+++++++++++++++++++++++++++++++++");
        }
        //request location update
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Log.d("LOCATION","in onLocationResult CALLED FUSED LCCATIONNNNNN");
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    Log.d("LOCATION","in onLocationResult after locationresult!=null, CALLED FUSED LCCATIONNNNNN"+ location.getLatitude()+ location.getLongitude()+location.getTime());

                    Geocoder geocoder;
                    List<Address> addresses;
                    geocoder = new Geocoder(MainActivity.this, Locale.getDefault());

                    try {
                        addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                        String address = addresses.get(0).getLocality() + " "+ addresses.get(0).getAdminArea(); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()

                        Log.d("LOCATION","Here is the address: "+ address);
                        textView.setText(address);
                        obtainedAddress=true;
                        Log.d("LOCATION","obtainedAddress is "+obtainedAddress);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
            }
        };
        LocationServices.getFusedLocationProviderClient(this)
                .requestLocationUpdates(locationRequest, locationCallback, null)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                    }
                });
    }


    private ArrayList<String> permissionsToRequestMtd(ArrayList<String> permissions) {
        ArrayList<String> results=new ArrayList<>();
        for (String permission : permissions){
            //check if already permitted
            if (!hasPermission(permission)){
                results.add(permission);
            }
        }
        return results;
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT >=Build.VERSION_CODES.M){
            return checkSelfPermission(permission)== PackageManager.PERMISSION_GRANTED;
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case ALL_PERMISSIONS_RESULT:
                for (String perm : permissionsToRequest) {
                    if (!hasPermission(perm)) {
                        permissionsRejected.add(perm);
                    }
                }
                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setMessage("These permissions are mandatory to get location")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            requestPermissions(permissionsRejected.toArray(
                                                    new String[permissionsRejected.size()]),
                                                    ALL_PERMISSIONS_RESULT);
                                        }
                                    }).setNegativeButton("Cancel", null).create().show();
                        }
                    }
                }else {
                    if (client != null) {
                        client.connect();
                    }
                }
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (client != null){
            client.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (obtainedAddress){
            startLocationUpdates();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        client.disconnect();
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("Main", "onPause is called");
       client.disconnect();
       fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //invoke fusedlocationproviderclient and listener
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener( new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        //get last known location
                        if (location!=null){
                            Log.d("LOCATION","in onConnected--------------------"+location.getLatitude()+"----------------");
                        }
                    }
                });
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}