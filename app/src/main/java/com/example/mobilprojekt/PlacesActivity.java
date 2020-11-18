package com.example.mobilprojekt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mobilprojekt.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PlacesActivity extends AppCompatActivity {

    private TextView textView;
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final String TAG = "PlacesActivity";
    private static final String KEY_EMAIL = "email";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private GoogleSignInAccount signInAccount;
    private DocumentReference noteref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places);
        signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        textView = (TextView)findViewById(R.id.textView);
        if (isServicesOK()) {
            init();
        }
    }

    public boolean isServicesOK(){
        Log.d(TAG,"isServicesOK: checking google services version");
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(PlacesActivity.this);
        if (available == ConnectionResult.SUCCESS){
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(PlacesActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "You cant make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void init(){
        noteref = db.collection("Users").document(Objects.requireNonNull(signInAccount.getEmail()));
        noteref.get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(documentSnapshot.exists()){
                            User user = documentSnapshot.toObject(User.class);
                            assert user != null;
                            textView.setText(user.getEmail());
                        }else{
                            Toast.makeText(PlacesActivity.this, "Document does not exist!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(PlacesActivity.this, "Failed to Load Database!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void deletePlaces(View view) {
        noteref.delete();
    }
}