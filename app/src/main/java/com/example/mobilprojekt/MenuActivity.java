package com.example.mobilprojekt;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mobilprojekt.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MenuActivity extends AppCompatActivity {

    Button logout;
    TextView name;
    private static final String TAG = "MainActivity";
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final String KEY_EMAIL = "email";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private GoogleSignInAccount signInAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        name = findViewById(R.id.googleName);
        signInAccount = GoogleSignIn.getLastSignedInAccount(this);

        if(signInAccount !=null){
            name.setText(signInAccount.getDisplayName());
        }
        logout = findViewById(R.id.googleSignInButton);

        if (isServicesOK()){
            init();
        }
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

    public void logOutUser(View view) {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public void goToMap(View view) {
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
    }

    private void init(){
        User loggedInUser = new User(signInAccount.getEmail());
        db.collection("Users").document(Objects.requireNonNull(signInAccount.getEmail())).set(loggedInUser).addOnSuccessListener(new OnSuccessListener<Void>() {
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


    public boolean isServicesOK(){
        Log.d(TAG,"isServicesOK: checking google services version");
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MenuActivity.this);
        if (available == ConnectionResult.SUCCESS){
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MenuActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "You cant make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    public void goToSavedPlaces(View view) {
        Intent intent = new Intent(this, PlacesActivity.class);
        startActivity(intent);
    }
}