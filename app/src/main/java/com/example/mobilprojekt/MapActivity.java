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
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Objects;

public class MapActivity extends AppCompatActivity {

    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final String TAG = "MapActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final float DEFAULT_ZOOM = 15f;
    private Boolean mLocationPermissionsGranted = false;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private GoogleSignInAccount signInAccount;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private AutocompleteSupportFragment mAutocompleteSupportFragment;
    private PlaceInfo mPlace = new PlaceInfo();
    private Marker mMarker;

    private PlacesClient placesClient = null;

    //widgets
    private ImageView mGPS;
    private ImageView mInfo;
    private ImageView mAdd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        mGPS = (ImageView) findViewById(R.id.ic_gps);
        mInfo = (ImageView) findViewById(R.id.place_info);
        mAdd = (ImageView) findViewById(R.id.place_add);
        signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        getLocationPermission();
        setUpAutocomplete();
    }

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

    private void getLocationPermission() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(), COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                Toast.makeText(getApplicationContext(), "Map is Ready", Toast.LENGTH_SHORT).show();
                mMap = googleMap;
                if (mLocationPermissionsGranted) {
                    getDeviceLocation();
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setMyLocationButtonEnabled(false);
                    mMap.getUiSettings().setScrollGesturesEnabled(true);
                    mMap.getUiSettings().setZoomControlsEnabled(true);
                }
            }
        });
        mGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked gps icon");
                getDeviceLocation();
            }
        });
        mInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked info icon");
                try{
                    if(mMarker.isInfoWindowShown()){
                        mMarker.hideInfoWindow();
                    }else{
                        mMarker.showInfoWindow();
                    }
                }catch (NullPointerException e){
                    Log.d(TAG, "onClick: NullPointerException" + e.getMessage());
                }
            }
        });
        mAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isServicesOK()){
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
    }

    private void setUpAutocomplete(){
        if(!Places.isInitialized()){
            Places.initialize(getApplicationContext(),getString(R.string.google_maps_api_key));
        }
        mAutocompleteSupportFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
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

    private void geoLocate(PlaceInfo place){
        Log.d(TAG, "geoLocate: Searching" + place);
        Geocoder geocoder = new Geocoder(MapActivity.this);
        if(place != null && place.getLatLng() != null) {
            moveCamera(new LatLng(place.getLatLng().latitude, place.getLatLng().longitude), DEFAULT_ZOOM, place);
        }
    }


    private void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation()");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
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
        Log.d(TAG, photoRequest.toString());
        placesClient = Places.createClient(this);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionsGranted = false;
        switch (requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for (int i = 0; i < grantResults.length; i++){
                        if (grantResults[i]!=PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionsGranted = false;
                            return;
                        }
                    }
                    mLocationPermissionsGranted = true;
                    initMap();
                }
            }
        }
    }

    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
}