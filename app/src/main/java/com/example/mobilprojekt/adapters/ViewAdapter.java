package com.example.mobilprojekt.adapters;

import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobilprojekt.MapActivity;
import com.example.mobilprojekt.R;
import com.example.mobilprojekt.models.PlaceInfo;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPhotoResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

import static androidx.core.content.ContextCompat.startActivity;

public class ViewAdapter extends RecyclerView.Adapter<ViewAdapter.MyViewHolder> {

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_layout, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: " +  data.get(position).getAddress());
        holder.placeName.setText(data.get(position).getName());
        holder.placeAddress.setText(data.get(position).getAddress());

    }

    private static final String TAG = "ViewAdapter";
    List<PlaceInfo> data;
    public ViewAdapter(List<PlaceInfo> data) {
        this.data = data;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public PlaceInfo getListItemAtPosition(int position){
        return data.get(position);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder{

        TextView placeAddress;
        TextView placeName;
        CircleImageView placePic;
        Button goToMaps;


        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            placeName = itemView.findViewById(R.id.name);
            placeAddress = itemView.findViewById(R.id.address);
            placePic = (CircleImageView) itemView.findViewById(R.id.place_image);
            goToMaps = (Button) itemView.findViewById(R.id.mapButton);
            GoogleSignInAccount signInAccount;

            signInAccount = GoogleSignIn.getLastSignedInAccount(itemView.getContext());
            new DownloadImageTask(placePic).execute(Objects.requireNonNull(signInAccount.getPhotoUrl()).toString());
            goToMaps.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(view.getContext(), MapActivity.class);
                    intent.putExtra("longitude", data.get(getLayoutPosition()).getLatLng().longitude);
                    intent.putExtra("latitude", data.get(getLayoutPosition()).getLatLng().latitude);
                    startActivity(view.getContext(), intent, null);
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });


        }
    }
}