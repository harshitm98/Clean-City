package com.example.android.cleancity;

import android.*;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import static com.google.android.gms.maps.GoogleMap.*;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback,OnMarkerClickListener {

    private static final String TAG = "MapActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;

    private Boolean mLocationPermissionGranted = false;
    private static int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private GoogleMap mMap;

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private CardView cardView;

    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation: getting the current device location");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try{
            if(mLocationPermissionGranted){
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "onComplete: got the location");
                            Location currentLocation = (Location)task.getResult();
                            LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                            float zoom = 15f;
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoom));
                        }
                        else{
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapActivity.this, "Couldn't find the current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }catch(SecurityException e){
            Log.d(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    private void markerAdder(GoogleMap googleMap, long id, long level, long area,
                             double longitude, double latitude){
        if(level == 1){
            Drawable dustbin = getResources().getDrawable(R.drawable.ic_dustbin_green);
            BitmapDescriptor markerIcon = getMarkerIconFromDrawable(dustbin);

            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng( longitude,latitude))
                    .title("Dustbin: " + id)
                    .icon(markerIcon));
        }
        else if(level == 2){
            Drawable dustbin = getResources().getDrawable(R.drawable.ic_dustbin_yellow);
            BitmapDescriptor markerIcon = getMarkerIconFromDrawable(dustbin);

            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng( longitude,latitude))
                    .title("Dustbin: " + id)
                    .icon(markerIcon));
        }
        else if(level == 3){
            Drawable dustbin = getResources().getDrawable(R.drawable.ic_dustbin_red);
            BitmapDescriptor markerIcon = getMarkerIconFromDrawable(dustbin);

            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng( longitude,latitude))
                    .title("Dustbin: " + id)
                    .icon(markerIcon));
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this,"Map is ready",Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: Map is ready here");
        mMap = googleMap;
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.google_maps_style));

            if (!success) {
                Log.e("MapsActivityRaw", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("MapsActivityRaw", "Can't find style.", e);
        }
        mMap.setOnMarkerClickListener((OnMarkerClickListener)this);
        mMap.setOnMapClickListener(new OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Log.d(TAG, "onMapClick: Map clicked");
                cardView.setVisibility(View.GONE);
            }
        });
        addingFirebaseData(mMap);
        getDeviceLocation();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);//Remove title bar
        getSupportActionBar().hide();
        setContentView(R.layout.activity_map);
        getLocationPermission();
    }

    private void initMap(){
        Log.d(TAG, "initMap: Initializing the map");
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);
        cardView = findViewById(R.id.card_view);
        Button newButton = findViewById(R.id.make_toast);
        newButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MapActivity.this, "Your complaint has been reported!", Toast.LENGTH_SHORT).show();
            }
        });
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

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d(TAG, "onMarkerClick: " + marker.getTitle());
        cardView.setVisibility(View.VISIBLE);

        final TextView percentage, area, id, rateCard;
        final double[] rate = new double[1];
        final RatingBar ratingBar = findViewById(R.id.rating_card);
        rateCard = findViewById(R.id.rate_card);
        percentage = findViewById(R.id.percentage_card);
        area = findViewById(R.id.area_card);
        id = findViewById(R.id.id_card);

        final String id_s = marker.getTitle().substring(9);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        //String path = "d" + marker.getTitle().substring(1,7) + marker.getTitle().substring(9);
        DatabaseReference reference = database.getReference("dusbins");
        reference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                if(id_s.equals(dataSnapshot.child("id").getValue().toString())){
                    long p = (Long)dataSnapshot.child("level").getValue();
                    if(p == 1){
                        percentage.setTextSize(72);
                        percentage.setText("50%");
                    }
                    else if(p == 2){
                        percentage.setTextSize(72);
                        percentage.setText("75%");
                    }
                    else if (p == 3) {
                        percentage.setTextSize(65);
                        percentage.setText("100%");
                    }
                    area.setText(dataSnapshot.child("area").getValue().toString());
                    id.setText(dataSnapshot.child("id").getValue().toString());
                    ratingBar.setRating(Float.valueOf(dataSnapshot.child("rate").getValue().toString()));
                    rateCard.setText(dataSnapshot.child("rate").getValue().toString());
                    Log.d(TAG, "onChildAdded: " + dataSnapshot.getValue().toString());
                }

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

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
        return false;
    }
}
