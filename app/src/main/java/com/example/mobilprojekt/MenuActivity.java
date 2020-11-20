package com.example.mobilprojekt;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mobilprojekt.adapters.DownloadImageTask;
import com.example.mobilprojekt.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import org.w3c.dom.Text;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MenuActivity extends AppCompatActivity {

    //UI elements
    private Button logoutButton;
    private Button mapButton;
    private Button placesButton;
    private TextView profileName;
    private CircleImageView profilePic;
    private TextInputEditText emailEditText;
    private TextInputEditText tokenEditText;

    //Constants
    private static final String TAG = "MainActivity";
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final String KEY_EMAIL = "email";

    //Database
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    //Google Account
    private GoogleSignInAccount signInAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int i) {
                if((i & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(uiOptions);
                }
            }
        });

        //Getting Design Components
        logoutButton = (Button) findViewById(R.id.logoutButton);
        mapButton = (Button) findViewById(R.id.mapButton);
        placesButton = (Button) findViewById(R.id.placesButton);
        profileName = (TextView) findViewById(R.id.profile_name);
        profilePic = (CircleImageView) findViewById(R.id.profile_image);
        emailEditText = (TextInputEditText) findViewById(R.id.email_edit_text);
        tokenEditText = (TextInputEditText) findViewById(R.id.token_edit_text);

        //Getting Google Account information
        signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (isServicesOK()){
            init();
        }
        displayGoogleData();
    }

    @Override
    protected void onStart() {
        super.onStart();
        db.collection("Users").document(Objects.requireNonNull(signInAccount.getEmail())).addSnapshotListener(this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null){
                    Log.d(TAG, error.toString());
                    return;
                }
                if(value.exists()){
                    //Do something if change happened
                }else{
                    //Do nothing
                }
            }
        });
    }

    private void init(){
        User loggedInUser = new User(signInAccount.getEmail());
        db.collection("Users").document(Objects.requireNonNull(signInAccount.getEmail())).set(loggedInUser).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(getApplicationContext(), "User " + signInAccount.getDisplayName() +" logged in!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(getApplicationContext(), "Could not log in user: " + signInAccount.getDisplayName(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public boolean isServicesOK(){
        Log.d(TAG,"isServicesOK: checking google services version");
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MenuActivity.this);
        if (available == ConnectionResult.SUCCESS){
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MenuActivity.this, available, ERROR_DIALOG_REQUEST);
            assert dialog != null;
            dialog.show();
        }else{
            Toast.makeText(this, "You cant make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    public void displayGoogleData(){
        if(signInAccount !=null){
            new DownloadImageTask(profilePic).execute(Objects.requireNonNull(signInAccount.getPhotoUrl()).toString());
            profileName.setText(signInAccount.getDisplayName());
            emailEditText.setText(signInAccount.getEmail());
            tokenEditText.setText(signInAccount.getId());
            tokenEditText.setFocusable(false);
            tokenEditText.setFocusableInTouchMode(false);
            emailEditText.setFocusable(false);
            emailEditText.setFocusableInTouchMode(false);
        }
    }


    public void logOutUser(View view) {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public void goToMap(View view) {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    public void goToSavedPlaces(View view) {
        Intent intent = new Intent(this, PlacesActivity.class);
        startActivity(intent);
    }
}