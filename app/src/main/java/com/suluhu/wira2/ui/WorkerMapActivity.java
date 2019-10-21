package com.suluhu.wira2.ui;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.os.Bundle;
//import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.suluhu.wira2.R;
import com.suluhu.wira2.WorkerClient;
import com.suluhu.wira2.models.PolyLineData;
import com.suluhu.wira2.models.User;
import com.suluhu.wira2.models.UserLocation;
import com.suluhu.wira2.models.Worker;
import com.suluhu.wira2.models.WorkerLocation;
import com.suluhu.wira2.services.LocationService;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import de.hdodenhof.circleimageview.CircleImageView;

import static com.suluhu.wira2.Constants.ERROR_DIALOG_REQUEST_CODE;
import static com.suluhu.wira2.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.suluhu.wira2.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;

public class WorkerMapActivity extends FragmentActivity implements OnMapReadyCallback,
        View.OnClickListener,
        GoogleMap.OnPolylineClickListener{

    public static final String TAG = ".WorkerMapActivity";

    //Firebase
    private String user_id;
    private FirebaseFirestore mDb;
    private FirebaseDatabase fDb;
    private DatabaseReference workerDbRef;
    private String requestingClientID;
    private DocumentReference workerDoc;

    //vars
    private boolean mLocationPermissionResultGranted = false;
//    private Location currentWorkerLocation;
    private WorkerLocation workerLocation;
    private ArrayList<PolyLineData> mPolylinesData = new ArrayList<>();

    //Client Details
    private User requestingUser;
    private UserLocation requestPoint;
    private LinearLayout clientInfo;
    private CircleImageView clientImage;
    private TextView clientFirstName, clientLastName, clientPhone;


    //maps
    private GoogleMap workerMap;
    private LatLngBounds workerMapBounds;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GeoApiContext geoApiContext = null;
    private ArrayList<Marker> tripMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_map);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fDb = FirebaseDatabase.getInstance();

        user_id = FirebaseAuth.getInstance().getUid();
        mDb = FirebaseFirestore.getInstance();
        workerDoc = mDb.collection(getString(R.string.collection_workers)).document(user_id);

        clientInfo = findViewById(R.id.request_client_info);
        clientImage = findViewById(R.id.request_client_image);
        clientFirstName = findViewById(R.id.request_first_name);
        clientLastName = findViewById(R.id.request_last_name);
        clientPhone = findViewById(R.id.request_phone);

        findViewById(R.id.worker_logout_ic).setOnClickListener(this);
        findViewById(R.id.w_gps_marker).setOnClickListener(this);
        findViewById(R.id.action_contact_client).setOnClickListener(this);
        findViewById(R.id.worker_settings_ic).setOnClickListener(this);

        getLocationPermission();
    }
    private void startLocationService(){
        if(!isLocationServiceRunning()){
            Intent serviceIntent = new Intent(this , LocationService.class);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                WorkerMapActivity.this.startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }

    private void removeTripMarkers(){

        if(tripMarkers.size() > 0){
            for(Marker marker : tripMarkers){
                marker.remove();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private boolean isLocationServiceRunning(){
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){

            String serviceName = service.service.getClassName();
            Log.d(TAG , "isLocationServiceRunning : service name :" + serviceName);
            if("com.suluhu.wira2.services.LocationService".equals(service.service.getClassName())){

                Log.d(TAG ,"isLocationServiceRunning : Location services is running");
                return true;
            }
        }
        Log.d(TAG , "isLocationServiceRunning : service is not running");
        return false;

    }
    private void getWorkerDetails() {
        if (workerLocation == null) {
            workerLocation = new WorkerLocation();

            DocumentReference workerRef = FirebaseFirestore.getInstance()
                    .collection(getString(R.string.collection_workers))
                    .document(user_id);

            workerRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    Log.d(TAG, "onComplete: successfully retrieved worker details");

                    Worker worker = task.getResult().toObject(Worker.class);
                    workerLocation.setWorker(worker);
                    ((WorkerClient)getApplicationContext()).setWorker(worker);
                    getLastKnownLocation();
                }
            });
        } else {
            getLastKnownLocation();
        }
    }

    private void saveWorkerLocation() {
        if (workerLocation != null) {
            DocumentReference workerLocationRef = mDb
                    .collection(getString(R.string.collection_worker_location))
                    .document(user_id);

            workerLocationRef.set(workerLocation).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "saveUserLocation: "
                                + "\n inserted user location into DB"
                                + "\n latitude: " + workerLocation.getGeo_point().getLatitude()
                                + "\n longitude: " + workerLocation.getGeo_point().getLongitude());

                        checkClientInstance();
                    } else {
                        if (task.getException() != null) {
                            String error = task.getException().getMessage();
                            Toast.makeText(WorkerMapActivity.this, "Couldn't save your location: " + error, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }
    }

    private void checkClientInstance(){
        final DocumentReference clientRef = mDb.collection(getString(R.string.requesting_client_id))
                .document(user_id);

        clientRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable DocumentSnapshot documentSnapshot, @javax.annotation.Nullable FirebaseFirestoreException e) {
                if(e != null) {
                    Log.w(TAG, "Listen failed: ", e);
                }
                if(documentSnapshot!= null && documentSnapshot.exists()){

                    workerDoc.update("status", getString(R.string.engaged))
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d(TAG, "changed status to engaged");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG, "Failed to change status: " + e);
                                }
                            });

                    requestingClientID = documentSnapshot.getString("requestID");

                    getClientDetails(requestingClientID);
                } else {
                    workerDoc.update("status", getString(R.string.available))
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d(TAG, "changed status to available");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG, "Failed to change status to available: " + e);
                                }
                            });
                    clearClientInfo();
                }
            }
        });
    }

    private void loadUserDetails(){
        if(requestingUser != null){
            clientInfo.setVisibility(View.VISIBLE);

            Glide
                    .with(getApplicationContext())
                    .load(requestingUser.getUser_image_url())
                    .centerCrop()
                    .placeholder(R.drawable.default_profile)
                    .into(clientImage);
            clientFirstName.setText(requestingUser.getFirst_name());
            clientLastName.setText(requestingUser.getLast_name());
            clientPhone.setText(requestingUser.getPhone_number());

            Log.d(TAG, "successfully loaded user details");

            getClientLocation();
        } else{

            Toast.makeText(this, "Could not load user information", Toast.LENGTH_SHORT).show();
        }
    }

//    private void loadPolyLines(){
//        //TODO retrieve requesting client's geoPoint from "Client Location" and use it to calculate polylines, trip duration, etc.
//    }

    private void clearClientInfo(){
        if(requestingClientID != null && requestingUser != null){
            requestingUser = null;
            requestingClientID = null;
            requestPoint = null;

            removeTripMarkers();
            removePolylines();
            clearClientLayout();
        }
    }

    private void clearClientLayout(){
        if(clientInfo != null){
            if(clientImage!= null){
                clientImage.setImageResource(R.drawable.default_profile);
            }
            if(clientFirstName != null){
                clientFirstName.setText(getString(R.string.firstname));
            }
            if(clientLastName!= null){
                clientLastName.setText(getString(R.string.lastname));
            }
            if(clientPhone!= null){
                clientPhone.setText(getString(R.string.phone));
            }

            clientInfo.setVisibility(View.GONE);
            getLastKnownLocation();
        }
    }

    private void getClientDetails(String requestID){

        if(requestID != null){
            DocumentReference requestRef = mDb.collection(getString(R.string.collection_clients))
                    .document(requestID);

            requestRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful() && task.getResult().exists()){
                        Vibrator v = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                        if(Build.VERSION.SDK_INT >=  Build.VERSION_CODES.O){
                            v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                        } else{
                            v.vibrate(100);
                        }
                        requestingUser = task.getResult().toObject(User.class);
                        loadUserDetails();
                    }
                }
            });
        }
    }

    private void getClientLocation(){

        Log.d(TAG, "calculateDirections: calculating directions.");

        if(requestingUser!= null && requestingClientID != null){
             mDb.
                    collection(getString(R.string.collection_client_location))
                    .document(requestingClientID)
                     .get()
                     .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                         @Override
                         public void onSuccess(DocumentSnapshot documentSnapshot) {

                             if(documentSnapshot != null && documentSnapshot.exists()){
                                 requestPoint = documentSnapshot.toObject(UserLocation.class);

                                 Log.d(TAG, "the requesting client is at: " + requestPoint.getGeo_point().toString());

                                 calculateDirections(requestPoint);

                             } else{

                                 removePolylines();
                                 removeTripMarkers();
                             }

                         }
                     })
                     .addOnFailureListener(new OnFailureListener() {
                         @Override
                         public void onFailure(@NonNull Exception e) {

                             removePolylines();
                             removeTripMarkers();
                         }
                     });
        } else {
            Log.d(TAG, "Requesting User or Requesting User ID seem to be null");
        }

    }

    private void calculateDirections(UserLocation requestLocation){

        Log.d(TAG, "calculateDirections called");

        if(requestLocation != null){
            com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                    requestLocation.getGeo_point().getLatitude(),
                    requestLocation.getGeo_point().getLongitude()

            );
            Log.d(TAG, "request destination: " + destination);

            DirectionsApiRequest directions = new DirectionsApiRequest(geoApiContext);

            directions.alternatives(true);
            directions.origin(
                    new com.google.maps.model.LatLng(
                            workerLocation.getGeo_point().getLatitude(),
                            workerLocation.getGeo_point().getLongitude()
                    )
            );
            Log.d(TAG, "calculateDirections: destination: " + destination.toString());

            directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
                @Override
                public void onResult(DirectionsResult result) {
                    Log.d(TAG, "calculateDirections: routes: " + result.routes[0].toString());
                    Log.d(TAG, "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[0].toString());

                    addPolylinesToMap(result);
                }

                @Override
                public void onFailure(Throwable e) {
                    Log.e(TAG, "calculateDirections: Failed to get directions: " + e.getMessage() );

                }
            });
        } else {

            Log.d(TAG, "Failed to calculate directions to requesting client");
        }



    }

    public void zoomRoute(List<LatLng> lstLatLngRoute) {

        if (workerMap == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) return;

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng latLngPoint : lstLatLngRoute)
            boundsBuilder.include(latLngPoint);

        int routePadding = 120;
        LatLngBounds latLngBounds = boundsBuilder.build();

        workerMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding),
                600,
                null
        );
    }

    private void removePolylines(){

        if(mPolylinesData.size() > 0){
            for(PolyLineData polyLineData : mPolylinesData){
                polyLineData.getPolyline().remove();
            }
            mPolylinesData.clear();
            mPolylinesData = new ArrayList<>();
        }
    }

    private void addPolylinesToMap(final DirectionsResult result){

        Log.d(TAG, "addPolyLinesToMap called");

        removeTripMarkers();

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: result routes: " + result.routes.length);

                removePolylines();

                double duration = 999999;

                for(DirectionsRoute route: result.routes){
                    Log.d(TAG, "run: leg: " + route.legs[0].toString());
                    List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

                    List<LatLng> newDecodedPath = new ArrayList<>();

                    // This loops through all the LatLng coordinates of ONE polyline.
                    for(com.google.maps.model.LatLng latLng: decodedPath){

//                        Log.d(TAG, "run: latlng: " + latLng.toString());

                        newDecodedPath.add(new LatLng(
                                latLng.lat,
                                latLng.lng
                        ));
                    }
                    Polyline polyline = workerMap.addPolyline(new PolylineOptions().addAll(newDecodedPath));
                    polyline.setColor(ContextCompat.getColor(getApplicationContext(), R.color.Black));
                    polyline.setClickable(true);
                    mPolylinesData.add(new PolyLineData(polyline, route.legs[0]));

                    double tempDuration = route.legs[0].duration.inSeconds;
                    if(tempDuration < duration){
                        duration = tempDuration;
                        onPolylineClick(polyline);
                        zoomRoute(polyline.getPoints());
                    }

                }
            }
        });
    }

    private void setCameraView() {

        LatLng workerLatLng = new LatLng(workerLocation.getGeo_point().getLatitude(), workerLocation.getGeo_point().getLongitude());

        workerMap.moveCamera(CameraUpdateFactory.newLatLngZoom(workerLatLng, 15));
    }

    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation called");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    Location currentWorkerLocation = task.getResult();
                    Log.d(TAG, currentWorkerLocation.toString());
                    GeoPoint geoPoint = new GeoPoint(currentWorkerLocation.getLatitude(), currentWorkerLocation.getLongitude());

                    Log.d(TAG, "onComplete Latitude: " + geoPoint.getLatitude());
                    Log.d(TAG, "onComplete Longitude" + geoPoint.getLongitude());

                    workerLocation.setGeo_point(geoPoint);
                    workerLocation.setTimestamp(null);

                    setCameraView();
                    saveWorkerLocation();
                    startLocationService();
                } else{
                    Toast.makeText(WorkerMapActivity.this, "we can't retrieve your location", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {


        //workerMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(0 , 0)));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        workerMap = googleMap;
        workerMap.setOnPolylineClickListener(this);
        workerMap.setMyLocationEnabled(true);
        // Add a marker at LatLng 0 , 0

    }

    private void initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        if(geoApiContext == null){
            geoApiContext = new GeoApiContext.Builder()
                    .apiKey(getString(R.string.server_key))
                    .build();
        }
    }

    private boolean checkMapsServices() {
        if (isServicesOk()) {
            if (isMapsEnabled()) {
                return true;
            }
        }
        return false;
    }


    private void buildNoGpsAlertMessage() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This application requires GPS to work properly, do you wish to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialogInterface, @SuppressWarnings("unused") final int i) {
                        Intent enableGpsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public boolean isMapsEnabled() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            Log.d(TAG, "isMapsEnabled: false");
            buildNoGpsAlertMessage();
            return false;
        }
        Log.d(TAG, "isMapsEnabled: true");
        return true;
    }

    private void getLocationPermission() {

        /* Request the location permission to access device location
         *the onRequestPermissionsResult callback handles the results of the
         * permission request result

         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            mLocationPermissionResultGranted = true;
            //workerMap.setMyLocationEnabled(true);
            initMap();
            Log.d(TAG, "getLocationPermissionGranted");
            getWorkerDetails();
            //TODO locate worker and move camera to their location
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    public boolean isServicesOk() {
        Log.d(TAG, "isServicesOk : checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(WorkerMapActivity.this);

        if (available == ConnectionResult.SUCCESS) {
            Log.d(TAG, "isServicesOk : Google Play Services is working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //user resolvable error occurred
            Log.d(TAG, "isServicesOk: fixable error occured");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(WorkerMapActivity.this, available, ERROR_DIALOG_REQUEST_CODE);
            dialog.show();
        } else {
            Toast.makeText(this, "It seems you can't make Map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionResultGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    mLocationPermissionResultGranted = true;
                    initMap();
                    getWorkerDetails();

                    //TODO locate worker and move camera to their location
                    Toast.makeText(this, "All Set!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult called");

        switch (requestCode) {
            case PERMISSIONS_REQUEST_ENABLE_GPS: {
                if (mLocationPermissionResultGranted) {
                    getWorkerDetails();
                    //TODO locate worker and move camera to their location

                } else {
                    getLocationPermission();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkMapsServices()) {
            if (mLocationPermissionResultGranted) {

                initMap();
                getWorkerDetails();
                //TODO locate worker and move camera to their location
            } else {

                getLocationPermission();
            }
        }
    }

    private void startSetupIntent(){
        AlertDialog.Builder setupDialog = new AlertDialog.Builder(WorkerMapActivity.this);
        setupDialog.setTitle("Profile Settings");
        setupDialog.setMessage("This takes you to profile setup. Continue?");

        setupDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent profileSetup = new Intent(WorkerMapActivity.this, WorkerSetupActivity.class);
                startActivity(profileSetup);
                finish();
                dialogInterface.dismiss();
            }
        });
        setupDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.worker_logout_ic: {
                if(isLocationServiceRunning()){
                    Intent locationIntent = new Intent(this, LocationService.class);
                    stopService(locationIntent);
                }
                FirebaseAuth.getInstance().signOut();
                Intent login = new Intent(WorkerMapActivity.this, LoginActivity.class);
                startActivity(login);
                finish();
                break;
            }

            case R.id.w_gps_marker: {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                fusedLocationProviderClient.getLastLocation()
                        .addOnCompleteListener(new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                if(task.isSuccessful()){
                                    Location currentWorkerLocation = (Location) task.getResult();

                                    workerMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentWorkerLocation.getLatitude() ,
                                            currentWorkerLocation.getLongitude()) , 15));
                                }

                            }
                        });
                break;
            }
            case R.id.action_contact_worker: {
                if(requestingUser!= null){
                    String uri = "tel:" + requestingUser.getPhone_number();
                    Intent contact = new Intent(Intent.ACTION_DIAL);
                    contact.setData(Uri.parse(uri));
                    startActivity(contact);
                    break;
                }
            }
            case R.id.worker_settings_ic: {
                startSetupIntent();
            }
        }
    }

    @Override
    public void onPolylineClick(Polyline polyline) {
        for(PolyLineData polylineData: mPolylinesData){
            Log.d(TAG, "onPolylineClick: toString: " + polylineData.toString());
            if(polyline.getId().equals(polylineData.getPolyline().getId())){
                polylineData.getPolyline().setColor(ContextCompat.getColor(getApplicationContext(), R.color.DarkTurquoise));
                polylineData.getPolyline().setZIndex(1);

                LatLng requestLocation = new LatLng(
                        polylineData.getLeg().endLocation.lat,
                        polylineData.getLeg().endLocation.lng
                );

                Marker marker = workerMap.addMarker(new MarkerOptions()
                        .position((requestLocation))
                        .title("Client Destination")
                        .snippet("Duration:" + polylineData.getLeg().duration)
                );


                marker.showInfoWindow();

                tripMarkers.add(marker);
            }
            else{
                polylineData.getPolyline().setColor(ContextCompat.getColor(getApplicationContext(), R.color.DarkSlateGray));
                polylineData.getPolyline().setZIndex(0);
            }
        }

    }
}
