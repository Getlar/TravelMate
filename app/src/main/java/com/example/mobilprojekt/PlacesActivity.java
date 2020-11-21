package com.example.mobilprojekt;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mobilprojekt.adapters.CustomWindowAdapter;
import com.example.mobilprojekt.adapters.ViewAdapter;
import com.example.mobilprojekt.models.PlaceInfo;
import com.example.mobilprojekt.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPhotoResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PlacesActivity extends AppCompatActivity {

    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final String TAG = "PlacesActivity";
    private static final String KEY_EMAIL = "email";
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private GoogleSignInAccount signInAccount;
    private CollectionReference collref;
    private RecyclerView mRecycleView;
    private DocumentReference documentReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places);
        signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        mRecycleView = (RecyclerView)findViewById(R.id.my_recycler_view);
        mRecycleView.setLayoutManager(new LinearLayoutManager(this));

        /*
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
        */

        if (isServicesOK()) {
            init();
        }

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                recyclerView.getAdapter().notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                final PlaceInfo placeInfo = ((ViewAdapter)mRecycleView.getAdapter()).getListItemAtPosition(viewHolder.getAdapterPosition());
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        documentReference = db.collection("Users").document(Objects.requireNonNull(signInAccount.getEmail())).collection("Places").document(placeInfo.getName());
                        documentReference.delete();
                    }
                }).start();
            }
        });
        itemTouchHelper.attachToRecyclerView(mRecycleView);

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

        collref = db.collection("Users").document(Objects.requireNonNull(signInAccount.getEmail())).collection("Places");
        collref.get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            List<PlaceInfo> placeInfos = new ArrayList<>();
                            for (QueryDocumentSnapshot document: task.getResult()
                            ) {
                                String address = (String) document.getData().get("address");
                                Map<String, Double> latika = (Map) document.getData().get("latLng");
                                LatLng latLng = new LatLng(latika.get("latitude"), latika.get("longitude"));
                                String name = (String) document.getData().get("name");
                                Map<String, Object> metadata = (Map<String, Object>)document.getData().get("photoMetadata");
                                PhotoMetadata photoMetadata;
                                if(metadata != null) {
                                    String attributions = metadata.get("attributions").toString();
                                    int heightInt = Integer.parseInt(metadata.get("height").toString());
                                    int widthInt = Integer.parseInt(metadata.get("width").toString());
                                    String[] split = attributions.split("\"");
                                    PhotoMetadata.Builder pm = PhotoMetadata.builder(split[1]).setAttributions(attributions).setHeight(heightInt).setWidth(widthInt);
                                    photoMetadata = pm.build();
                                }else{
                                    photoMetadata = null;
                                }
                                placeInfos.add(new PlaceInfo(name,address,null, latLng, null, 0, null, photoMetadata));
                            }
                            mRecycleView.setAdapter(new ViewAdapter(placeInfos));
                        }else{
                            Toast.makeText(PlacesActivity.this, "You have no places so far!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(PlacesActivity.this, "Failed to connect to the database!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void clearList(View view) {
        collref = db.collection("Users").document(Objects.requireNonNull(signInAccount.getEmail())).collection("Places");
        collref.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                for (QueryDocumentSnapshot document: task.getResult()){
                    document.getReference().delete();
                }
                ArrayList<PlaceInfo> empty = new ArrayList<>();
                mRecycleView.setAdapter(new ViewAdapter(empty));
            }
        });
    }

}