package com.example.mobilprojekt.models;

import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.model.OpeningHours;
import com.google.android.libraries.places.api.model.PhotoMetadata;

public class PlaceInfo {
    private String name;
    private String address;
    private String ID;
    private LatLng latLng;
    private String phoneNumber;
    private double rating;
    private OpeningHours openingHours;
    private PhotoMetadata photoMetadata;

    public PlaceInfo(String name, String address, String ID, LatLng latLng, String phoneNumber, double rating, OpeningHours openingHours, PhotoMetadata photoMetadata) {
        this.name = name;
        this.address = address;
        this.ID = ID;
        this.latLng = latLng;
        this.phoneNumber = phoneNumber;
        this.rating = rating;
        this.openingHours = openingHours;
        this.photoMetadata = photoMetadata;
    }


    public PlaceInfo() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getID() {
        return ID;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public OpeningHours getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(OpeningHours openingHours) {
        this.openingHours = openingHours;
    }

    public PhotoMetadata getPhotoMetadata() {
       return photoMetadata;
    }

    public void setPhotoMetadata(PhotoMetadata photoMetadata) {
        this.photoMetadata = photoMetadata;
    }

    @Override
    public String toString() {
        return "PlaceInfo{" +
                "name='" + name + '\'' +
                ", addres=" + address +
                ", ID='" + ID + '\'' +
                ", latLng=" + latLng +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", rating=" + rating +
                ", openingHours=" + openingHours +
                ", photoMetadata=" + photoMetadata +
                '}';
    }
}
