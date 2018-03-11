package com.example.android.cleancity;

import android.*;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;

    private Boolean mLocationPermissionGranted = false;
    private static int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private GoogleMap mMap;

    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void markerAdder(GoogleMap googleMap, long id, long level, long area,
                             double longitude, double latitude){
        if(level == 1){
            Drawable dustbin = getResources().getDrawable(R.drawable.ic_dustbin_green);
            BitmapDescriptor markerIcon = getMarkerIconFromDrawable(dustbin);

            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng( longitude,latitude))
                    .title("Dustbin: " + id)
                    .snippet("Area: " + area + ", 50% or less filled")
                    .icon(markerIcon));
        }
        else if(level == 2){
            Drawable dustbin = getResources().getDrawable(R.drawable.ic_dustbin_yellow);
            BitmapDescriptor markerIcon = getMarkerIconFromDrawable(dustbin);

            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng( longitude,latitude))
                    .title("Dustbin: " + id)
                    .snippet("Area: " + area + ", 75% filled")
                    .icon(markerIcon));
        }
        else if(level == 3){
            Drawable dustbin = getResources().getDrawable(R.drawable.ic_dustbin_red);
            BitmapDescriptor markerIcon = getMarkerIconFromDrawable(dustbin);

            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng( longitude,latitude))
                    .title("Dustbin: " + id)
                    .snippet("Area: " + area + ", 100% filled")
                    .icon(markerIcon));
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this,"Map is ready",Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: Map is ready here");
        mMap = googleMap;
        addingFirebaseData(mMap);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        getLocationPermission();
    }

    private void initMap(){
        Log.d(TAG, "initMap: Initializing the map");
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);
    }

    private void getLocationPermission(){
        Log.d(TAG, "getLocationPermission: Getting location permissions");
        String[] permissions = {FINE_LOCATION, COARSE_LOCATION};
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
                initMap();
            }
            else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        }
        else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionGranted = false;
        switch(requestCode){
            case 1234 : {
                if(grantResults.length > 0){
                    for(int i = 0; i<grantResults.length;i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    mLocationPermissionGranted= true;
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    //initialize our map
                    initMap();
                }
            }
        }
    }
    public void addingFirebaseData(final GoogleMap googleMap){
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        final DatabaseReference reference = firebaseDatabase.getReference("dusbins");
        reference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                double latitude, longitude;
                long id, level, area;
                id = (Long)dataSnapshot.child("id").getValue();
                latitude = (Double)dataSnapshot.child("latitude").getValue();
                longitude = (Double)dataSnapshot.child("longitude").getValue();
                level = (Long)dataSnapshot.child("level").getValue();
                area = (Long)dataSnapshot.child("area").getValue();
                Log.d(TAG, "onChildAdded:" + "id: " + id +latitude + longitude + level + area);
                markerAdder(googleMap,id,level,area,latitude,longitude);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                googleMap.clear();
                addingFirebaseData(googleMap);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
