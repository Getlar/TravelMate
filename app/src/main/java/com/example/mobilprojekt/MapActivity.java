package com.example.mobilprojekt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.mobilprojekt.adapters.CustomWindowAdapter;
import com.example.mobilprojekt.models.PlaceInfo;
import com.example.mobilprojekt.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.firestore.FirebaseFirestore;

import android.Manifest;
import android.app.ActionBar;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class MapActivity extends AppCompatActivity implements SensorEventListener {

    //CONSTANTS

    //Google Service Error Check Dialog Request
    private static final int ERROR_DIALOG_REQUEST = 9001;
    //Activity TAG
    private static final String TAG = "MapActivity";
    //Permission Constants for Google Maps
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    //Default Zoom value after search
    private static final float DEFAULT_ZOOM = 15f;
    //Default Zoom value after phone shake
    private static final float RANDOM_DEFAULT_ZOOM = 1f;
    //Location Services permission code
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    //DB Instance
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    //Google Account
    private GoogleSignInAccount signInAccount;
    //Google Maps and Places
    private GoogleMap mMap;
    private Boolean mLocationPermissionsGranted = false;
    private final PlaceInfo mPlace = new PlaceInfo();
    private Marker mMarker;

    //Sensor Variables
    private SensorManager sensorManager;
    private long mLastShakeTime;
    private static final int MIN_TIME_BETWEEN_SHAKES_MILLISECS = 1000;
    private static final float SHAKE_THRESHOLD = 2.0f;
    private Sensor accelerometer;

    //Widgets for ImageViews
    private ImageView mGPS;
    private ImageView mInfo;
    private ImageView mAdd;
    private ImageView mBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        //Disable NavBar
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        //Instantiate Widgets
        mGPS = (ImageView) findViewById(R.id.ic_gps);
        mInfo = (ImageView) findViewById(R.id.place_info);
        mAdd = (ImageView) findViewById(R.id.place_add);
        mBack = (ImageView) findViewById(R.id.place_return);

        //Get Signed in Google User
        signInAccount = GoogleSignIn.getLastSignedInAccount(this);

        //Set up Accelerometer Sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(accelerometer != null){
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
        Log.d(TAG, "onCreate: Initialization is complete.");
        getLocationPermission();

    }

    // Get Location Permission
    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: Getting Location Permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
                Log.d(TAG, "getLocationPermission: Location Permission already granted.");
                initMap();
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    //On Location Permission Result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: Asking for Location Permissions.");
        mLocationPermissionsGranted = false;
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                for (int grantResult : grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "onRequestPermissionsResult: Location Permission denied.");
                        mLocationPermissionsGranted = false;
                        return;
                    }
                }
                mLocationPermissionsGranted = true;
                //If granted permission initialize map
                Log.d(TAG, "onRequestPermissionsResult: Location Permissions granted.");
                initMap();
            }
        }
    }

    private void initMap() {
        Log.d(TAG, "initMap: Initializing Map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                Log.d(TAG, "onMapReady: Map is Ready");
                Toast.makeText(MapActivity.this, "Map is Ready to Use", Toast.LENGTH_SHORT).show();
                mMap = googleMap;
                if (mLocationPermissionsGranted) {
                    getDeviceLocation();
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setMyLocationButtonEnabled(false);
                    mMap.getUiSettings().setZoomControlsEnabled(true);
                }
            }
        });
        //Set up Widget Listeners for Events
        mGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick (mGPS) : Clicked gps icon");
                getDeviceLocation();
            }
        });
        mInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick (mInfo) : Clicked info icon");
                try{
                    if(mMarker.isInfoWindowShown()){
                        mMarker.hideInfoWindow();
                    }else{
                        mMarker.showInfoWindow();
                    }
                }catch (NullPointerException e){
                    Log.d(TAG, "onClick (mInfo) : NullPointerException" + e.getMessage());
                }
            }
        });
        mAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: (mAdd) : Clicked add to DB button");
                if(isServicesOK() && mPlace!=null){
                    db.collection("Users").document(Objects.requireNonNull(signInAccount.getEmail())).collection("Places").document(mPlace.getName()).set(mPlace).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(getApplicationContext(), "User with email: " + signInAccount.getEmail() +" added", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(), "User with email: " + signInAccount.getEmail() +" not added", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MapActivity.this, MenuActivity.class);
                startActivity(intent);
            }
        });

        //Setup Search Environment
        setUpAutocomplete();
    }

    private void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation()");
        FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try{
            if(mLocationPermissionsGranted){
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "Found Location");
                            Location currentLocation = (Location) task.getResult();
                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),DEFAULT_ZOOM, "My Location");
                        }else{
                            Log.d(TAG, "Location is null");
                            Toast.makeText(MapActivity.this, "unable to get location",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }catch (SecurityException e){
            Log.d(TAG, "Security Exception" + e.getMessage());
        }
    }

    //Check if Google Play Service is OK
    public boolean isServicesOK(){
        Log.d(TAG,"isServicesOK: checking google services version");
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MapActivity.this);
        if (available == ConnectionResult.SUCCESS){
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MapActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "You cant make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    //Set up Autocomplete Fragment and listen for Change
    private void setUpAutocomplete(){
        if(!Places.isInitialized()){
            Places.initialize(getApplicationContext(),getString(R.string.google_maps_api_key));
        }
        AutocompleteSupportFragment mAutocompleteSupportFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        assert mAutocompleteSupportFragment != null;
        mAutocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.PHONE_NUMBER, Place.Field.OPENING_HOURS, Place.Field.PHOTO_METADATAS, Place.Field.PRICE_LEVEL, Place.Field.RATING));
        mAutocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                Log.w(TAG, "Place:"+place);
                try {
                    mPlace.setID(place.getId());
                    mPlace.setAddres(place.getAddress());
                    mPlace.setLatLng(place.getLatLng());
                    mPlace.setName(place.getName());
                    mPlace.setOpeningHours(place.getOpeningHours());
                    mPlace.setPhoneNumber(place.getPhoneNumber());
                    if (place.getRating() != null) {
                        mPlace.setRating(place.getRating());
                    }
                    if (place.getPhotoMetadatas().size() > 0) {
                        mPlace.setPhotoMetadata(place.getPhotoMetadatas().get(0));
                    }else{
                        mPlace.setPhotoMetadata(null);
                    }
                }catch (NullPointerException e){
                    Log.d(TAG,e.getMessage());
                }
                geoLocate(mPlace);
            }
            @Override
            public void onError(@NonNull Status status) {
                Log.e(TAG, "AutocompleteSupportFragment - Ocorreu um erro: " + status);
                getDeviceLocation();
            }
        });
    }

    //Geolocate the place
    private void geoLocate(PlaceInfo place){
        Log.d(TAG, "geoLocate: Searching" + place);
        Geocoder geocoder = new Geocoder(MapActivity.this);
        if(place != null && place.getLatLng() != null) {
            moveCamera(new LatLng(place.getLatLng().latitude, place.getLatLng().longitude), DEFAULT_ZOOM, place);
        }
    }

    //Move camera to the correct place
    private void moveCamera(LatLng latLng, float zoom, String title){
        Log.d(TAG, "Moving the camera");
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        if(!title.equals("My Location")) {
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            mMap.addMarker(options);
        }
        hideSoftKeyboard();
    }
    private void moveCamera(LatLng latLng, float zoom, PlaceInfo placeInfo){
        Log.d(TAG, "Moving the camera NOT MYSELF");

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        mMap.clear();
        final String attributions = placeInfo.getPhotoMetadata().getAttributions();
        final FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(placeInfo.getPhotoMetadata())
                .setMaxHeight(800)
                .setMaxWidth(600)
                .build();
        PlacesClient placesClient = Places.createClient(this);
        placesClient.fetchPhoto(photoRequest).addOnSuccessListener((fetchPhotoRespone) -> {
            Bitmap bitmap = fetchPhotoRespone.getBitmap();
            mMap.setInfoWindowAdapter(new CustomWindowAdapter(MapActivity.this,bitmap));
        }).addOnFailureListener((exception)->{
            final ApiException apiException = (ApiException) exception;
            Log.d(TAG, "renderWindowText: " + exception.getMessage());
            final int statusCode = apiException.getStatusCode();
        });

        try{
            Log.d(TAG, "Ass");
            StringBuilder sb = new StringBuilder();
            String snippet = "Address: " + placeInfo.getAddres() + "\n" +
                    "Phone Number: " + placeInfo.getPhoneNumber() + "\n" +
                    "Rating: " + placeInfo.getRating() + '\n';
            sb.append(snippet);
            if(placeInfo.getOpeningHours() == null){
                sb.append("This place has no opening hours!");
            }else{
                sb.append("Opening Hours: " + '\n');
                for (String s : placeInfo.getOpeningHours().getWeekdayText()
                ) {
                    sb.append(s).append('\n');
                }
            }
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(placeInfo.getName())
                    .snippet(sb.toString());
            mMarker = mMap.addMarker(options);
            mMap.addMarker(options);
            Log.d(TAG, "ADDED MARKER");
        }catch (NullPointerException e){
            Log.d(TAG, e.getMessage());
        }
        hideSoftKeyboard();
    }


    //Hide Keyboard Popup
    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    //Listen for accelerometer changes, if reaches threshold go to random location
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            long curTime = System.currentTimeMillis();
            if((curTime - mLastShakeTime) > MIN_TIME_BETWEEN_SHAKES_MILLISECS){
                float x = sensorEvent.values[0];
                float y = sensorEvent.values[1];
                float z = sensorEvent.values[2];
                double acceleration = Math.sqrt(Math.pow(x,2) + Math.pow(y,2) + Math.pow(z,2)) - SensorManager.GRAVITY_EARTH;
                if(acceleration > SHAKE_THRESHOLD){
                    mLastShakeTime = curTime;
                    Log.d(TAG, "onSensorChanged: Shake, Rattle, and Roll");
                    double highLat = 90;
                    double highLong = 180;
                    Random r = new Random();
                    double longitude = r.nextDouble()*2*highLong - highLong;
                    double latitude = r.nextDouble()*2*highLat - highLat;
                    LatLng latLng = new LatLng(latitude, longitude);
                    Geocoder geocoder = new Geocoder(this);
                    List<Address> addressList = new ArrayList<>();

                    try {
                        addressList = geocoder.getFromLocation(latitude, longitude, 1);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(addressList.size() > 0) {
                        Log.d(TAG, "GeoLocated location is: " + addressList.get(0).getAddressLine(0));
                        moveCamera(latLng, RANDOM_DEFAULT_ZOOM, "Random Place Generated: " + addressList.get(0).getAddressLine(0));
                        mPlace.setName(addressList.get(0).getCountryName());
                        mPlace.setLatLng(latLng);
                        mPlace.setAddres(addressList.get(0).getAddressLine(0));
                    }
                }
            }
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }
}