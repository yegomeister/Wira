package com.suluhu.wira2.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.clustering.ClusterManager;
import com.hootsuite.nachos.NachoTextView;
import com.suluhu.wira2.R;
import com.suluhu.wira2.models.ClusterMarker;
import com.suluhu.wira2.models.User;
import com.suluhu.wira2.models.UserLocation;
import com.suluhu.wira2.models.Worker;
import com.suluhu.wira2.models.WorkerLocation;
import com.suluhu.wira2.utils.ClusterManagerRenderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import de.hdodenhof.circleimageview.CircleImageView;

import static com.suluhu.wira2.Constants.ERROR_DIALOG_REQUEST_CODE;
import static com.suluhu.wira2.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.suluhu.wira2.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;

public class ClientMapActivity extends FragmentActivity implements OnMapReadyCallback ,
        View.OnClickListener{

    public static final String TAG = ".ClientMapActivity";

    //Firebase
    private String user_id;
    private FirebaseFirestore mDb;
    private FirebaseDatabase fDb;

    //vars
    private boolean mLocationPermissionResultGranted = false;
    private UserLocation userLocation;
    private Boolean requestBol = false;
    private ArrayList<Worker> availableWorkerList;
    private ArrayList<WorkerLocation> workerLocations;
    private ListenerRegistration availableWorkerListener , workerLocationEventListener;
//    private String workerFoundID;
    private Worker workerFound;


    //worker details
    private String qualifiedWorkerID;
    private LinearLayout workerInfo;
    private CircleImageView workerImage;
    private TextView workerFirstName, workerLastName, workerPhone;
    private Marker workerMarker = null;

//    private MarkerOptions wMarkerOptions = new MarkerOptions();
//    private ArrayList<Marker> workerMarkers = new ArrayList<>();

    //maps
    private GoogleMap clientMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private ClusterManager clusterManager;
    private ClusterManagerRenderer clusterManagerRenderer;
    private ArrayList<ClusterMarker> clusterMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_map);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        user_id = FirebaseAuth.getInstance().getUid();
        mDb = FirebaseFirestore.getInstance();

        workerLocations = new ArrayList<>();
        availableWorkerList = new ArrayList<>();

        workerInfo = findViewById(R.id.worker_info);
        workerImage = findViewById(R.id.worker_image);
        workerFirstName = findViewById(R.id.worker_first_name);
        workerLastName = findViewById(R.id.worker_last_name);
        workerPhone = findViewById(R.id.worker_phone);


        findViewById(R.id.client_logout_ic).setOnClickListener(this);
        findViewById(R.id.request_worker).setOnClickListener(this);
        findViewById(R.id.action_contact_worker).setOnClickListener(this);
        findViewById(R.id.action_cancel_request).setOnClickListener(this);
        findViewById(R.id.btn_reset_map).setOnClickListener(this);
        findViewById(R.id.cl_settings_ic).setOnClickListener(this);

        getLocationPermission();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        clientMap = googleMap;

        // Add a marker at LatLng 0 , 0
        clientMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
        //workerMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(0 , 0)));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
//        clientMap.setMyLocationEnabled(true);
    }

    private void getAvailableWorkers(){

        Log.d(TAG, "getAvailableWorkers called");

        CollectionReference workerRef = mDb
                .collection(getString(R.string.collection_workers));

        availableWorkerListener = workerRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                if(e != null){
                    Log.e(TAG , "onEvent: Listen failed " + e);
                    return;
                }

                if(queryDocumentSnapshots != null){

                    Log.d(TAG , "onEvent: Worker has been found");

                    availableWorkerList.clear();
                    availableWorkerList = new ArrayList<>();

                    for(QueryDocumentSnapshot doc : queryDocumentSnapshots){

                        if(doc.get("status").equals(getString(R.string.available))){

                            Log.d(TAG , "QueryDocSnapshot found for Available worker");
                            Worker worker = doc.toObject(Worker.class);

                            Log.d(TAG , "queryDocumentSnapshots : user ID = " + worker.getUser_id());

                            availableWorkerList.add(worker);

                            getWorkerLocation(worker);


                        }
                        //TODO check whether the worker status is "Available" and place them on the list
                    }
                }
            }
        });

    }


    private BitmapDescriptor bitmapDescriptorFromVector(Context context , @DrawableRes int vectorDrawableResId){
        Drawable drawable = ContextCompat.getDrawable(context, vectorDrawableResId);
        drawable.setBounds(24 , 24 , drawable.getIntrinsicWidth() +12 , drawable.getIntrinsicHeight() +12);

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth() , drawable.getIntrinsicHeight() , Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void addWorkerMarkers(){
        if(clientMap != null){
            if(clusterManager == null){
                clusterManager = new ClusterManager<ClusterMarker>(getApplicationContext() , clientMap);
            }
            if(clusterManagerRenderer == null){
                clusterManagerRenderer = new ClusterManagerRenderer(
                        getApplicationContext() , clientMap , clusterManager
                );
                clusterManager.setRenderer(clusterManagerRenderer);
            }
            for(WorkerLocation workerLocation : workerLocations){
                Log.d(TAG, "worker location to be rendered: " + workerLocation.toString());

                try {
                    String snippet = "";
                    String title = "";
                    int avatar = R.drawable.ic_worker_marker;

                    ClusterMarker newClusterMarker = new ClusterMarker(new LatLng(workerLocation.getGeo_point().getLatitude() ,
                            workerLocation.getGeo_point().getLongitude()),
                            title,
                            snippet,
                            avatar,
                            workerLocation.getWorker());

                    clusterManager.addItem(newClusterMarker);
                    clusterMarkers.add(newClusterMarker);

                    Log.d(TAG, "clustermarker: " + newClusterMarker.getPosition().toString());
                } catch (NullPointerException e){

                    Log.e("TAG" , "addWorkerMarkers : NullPointerException: " + e);
                }

            }
            clusterManager.cluster();
            setCameraView();
        }
    }

    private void setCameraView() {
        if(userLocation != null){

            LatLng clientLatLng = new LatLng(userLocation.getGeo_point().getLatitude(), userLocation.getGeo_point().getLongitude());

            clientMap.moveCamera(CameraUpdateFactory.newLatLngZoom(clientLatLng, 15));
        }
    }

    private void getWorkerLocation(final Worker worker){
        Log.d(TAG, "getWorkerLocation called");

        if(availableWorkerList!= null){
            Log.d(TAG, "Available Workers: " +availableWorkerList.toString());
        } else {
            Log.d(TAG, "Available Workers List is empty");
        }

        String worker_found_id = worker.getUser_id();

        DocumentReference workerLocationRef = mDb
                .collection(getString(R.string.collection_worker_location))
                .document(worker_found_id);

        workerLocationRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    if(task.getResult().toObject(WorkerLocation.class) != null){

                        Log.d(TAG , "task to be retrieved: " + task.getResult().toObject(WorkerLocation.class));

                        workerLocations.add(task.getResult().toObject(WorkerLocation.class));

                        for(WorkerLocation workerLocation : workerLocations){
                            Log.d(TAG , "getWorkerLocation geopoint" + workerLocation.getGeo_point().getLatitude() + " , "
                            + workerLocation.getGeo_point().getLongitude());

                            addWorkerMarkers();
                        }
                    } else {
                        Log.d(TAG , "onComplete : no worker locations found");
                    }
                }
            }
        });

    }

    private void getUserDetails(){
        if(userLocation == null){
            userLocation = new UserLocation();

            DocumentReference clientRef = FirebaseFirestore.getInstance()
                    .collection(getString(R.string.collection_clients))
                    .document(user_id);

            clientRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    Log.d(TAG , "onComplete: successfully retrieved user details");

                    User user = task.getResult().toObject(User.class);
                    userLocation.setUser(user);
                    getLastKnownLocation();
                }
            });
        } else {
            getLastKnownLocation();
        }
    }

    private void saveUserLocation(){

        Log.d(TAG, "saveUserLocationCalled");

        if(userLocation != null){
            Log.d(TAG, "User Location-->" + userLocation.getGeo_point().toString());

            DocumentReference clientLocationRef = mDb
                    .collection(getString(R.string.collection_client_location))
                    .document(user_id);

            clientLocationRef.set(userLocation).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        Log.d(TAG, "saveUserLocation: "
                                +"\n inserted user location into DB"
                                +"\n latitude: " + userLocation.getGeo_point().getLatitude()
                                +"\n longitude: " + userLocation.getGeo_point().getLongitude());

                        getAvailableWorkers();
                    }
                    else {
                        if(task.getException() != null){
                            String error = task.getException().getMessage();
                            Toast.makeText(ClientMapActivity.this, "Couldn't save your location: " + error, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }
    }

    private void updateRequestDb(){
        Log.d(TAG, "updateRequestDb called");

        if(qualifiedWorkerID!= null){
            Log.d(TAG, "qualified worker's ID: " + qualifiedWorkerID);

            DocumentReference requestingClientRef = mDb.collection(getString(R.string.requesting_client_id))
                    .document(qualifiedWorkerID);

            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("requestID", user_id);

            requestingClientRef
                    .set(requestMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "request client table successfully updated");

                            loadWorkerInfo();

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "Error while updating Requesting Client DB: " + e);
                            Toast.makeText(ClientMapActivity.this, "Failed to connect with the worker",
                                    Toast.LENGTH_SHORT).show();

                        }
                    });

//            requestingClientRef.update("requestID", user_id).addOnCompleteListener(new OnCompleteListener<Void>() {
//                @Override
//                public void onComplete(@NonNull Task<Void> task) {
//                    if(task.isSuccessful()){
//                        Log.d(TAG, "Successfully saved requesting client ID" + user_id);
//
//                        loadWorkerInfo();
//                    } else {
//                        Log.d(TAG, "Failed to save requesting client ID");
//                    }
//                }
//            });
        }

    }

    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation called");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if(task.isSuccessful()){
                    Location clientLocation = task.getResult();
                    GeoPoint geoPoint = new GeoPoint(clientLocation.getLatitude() , clientLocation.getLongitude());

                    Log.d(TAG , "onComplete Latitude: " +geoPoint.getLatitude());
                    Log.d(TAG , "onComplete Longitude" + geoPoint.getLongitude());

                    userLocation.setGeo_point(geoPoint);
                    userLocation.setTimestamp(null);

                    saveUserLocation();
                }
            }
        });
    }

    private void initMap(){
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void resetMap(){
        if(clientMap != null) {
            clientMap.clear();

            if(clusterManager != null){
                clusterManager.clearItems();
            }

            if (clusterMarkers.size() > 0) {
                clusterMarkers.clear();
                clusterMarkers = new ArrayList<>();
            }

            getUserDetails();
        }
    }

    private boolean checkMapsServices(){
        if(isServicesOk()){
            if(isMapsEnabled()){
                return true;
            }
        }
        return false;
    }

    private void buildNoGpsAlertMessage(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This application requires GPS to work properly, do you wish to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialogInterface, @SuppressWarnings("unused") final int i) {
                        Intent enableGpsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent , PERMISSIONS_REQUEST_ENABLE_GPS);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public boolean isMapsEnabled(){
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if( !manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){

            Log.d(TAG , "isMapsEnabled: false");
            buildNoGpsAlertMessage();
            return false;
        }
        Log.d(TAG , "isMapsEnabled: true");
        return true;
    }

    private void getLocationPermission(){

        /* Request the location permission to access device location
         *the onRequestPermissionsResult callback handles the results of the
         * permission request result

         */
        if(ContextCompat.checkSelfPermission(this.getApplicationContext() ,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){

            mLocationPermissionResultGranted = true;
            Log.d(TAG , "getLocationPermissionGranted");

            initMap();
            getUserDetails();
            //TODO locate worker and move camera to their location
        } else {
            ActivityCompat.requestPermissions(this , new String[]{Manifest.permission.ACCESS_FINE_LOCATION} ,
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    public boolean isServicesOk(){
        Log.d(TAG , "isServicesOk : checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ClientMapActivity.this);

        if(available == ConnectionResult.SUCCESS){
            Log.d(TAG , "isServicesOk : Google Play Services is working");
            return true;
        }
        else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //user resolvable error occurred
            Log.d(TAG , "isServicesOk: fixable error occured");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(ClientMapActivity.this , available , ERROR_DIALOG_REQUEST_CODE);
            dialog.show();
        } else {
            Toast.makeText(this, "It seems you can't make Map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionResultGranted = false;
        switch(requestCode){
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:{
                if(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    mLocationPermissionResultGranted = true;
                    initMap();
                    getUserDetails();

                    //TODO locate worker and move camera to their location
                    Toast.makeText(this, "All Set!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG , "onActivityResult called");

        switch(requestCode){
            case PERMISSIONS_REQUEST_ENABLE_GPS: {
                if(mLocationPermissionResultGranted){
                    getUserDetails();
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
        if(checkMapsServices()){
            if(mLocationPermissionResultGranted){

                initMap();
                getUserDetails();
                refreshWorkerMarker();
                //TODO locate worker and move camera to their location
            } else {

                getLocationPermission();
            }
        }
    }

    private void cancelRequest(){
        if(requestBol){

            AlertDialog.Builder cancelReqBuilder = new AlertDialog.Builder(ClientMapActivity.this);
            cancelReqBuilder.setTitle(getString(R.string.cancel_request_title))
                    .setMessage(getString(R.string.cancel_message));

            cancelReqBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialogInterface, int i) {
                    requestBol = false;
                    if(qualifiedWorkerID != null){
                        mDb.collection(getString(R.string.requesting_client_id))
                                .document(qualifiedWorkerID)
                                .delete()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Button requestBtn = findViewById(R.id.request_worker);
                                        requestBtn.setText(getString(R.string.request_worker));

                                        qualifiedWorkerID = null;
                                        workerFound = null;
                                        resetWorkerInfo();
                                        dialogInterface.dismiss();
                                    }
                                });

                    }
                }
            });
            cancelReqBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });

            AlertDialog cancelAlert = cancelReqBuilder.create();
            cancelAlert.show();
        }

    }

    private void resetWorkerInfo(){

        if(workerImage!= null){
            workerImage.setImageResource(R.drawable.default_profile);
        }
        if (workerFirstName != null) {
            workerFirstName.setText(R.string.firstname);
        }
        if (workerLastName != null) {
            workerLastName.setText(R.string.lastname);
        }
        if (workerPhone != null) {
            workerPhone.setText(R.string.phone);
        }
        workerInfo.setVisibility(View.GONE);
        resetMap();
    }

    private void requestForWorker(){
        if(requestBol){
            cancelRequest();
        }
        else {
            
            Button requestBtn = findViewById(R.id.request_worker);
            requestBtn.setText(getString(R.string.locating_worker));

            AlertDialog.Builder builder = new AlertDialog.Builder(ClientMapActivity.this);

            final AutoCompleteTextView required_skill_view = new AutoCompleteTextView(ClientMapActivity.this);
            required_skill_view.setHint(R.string.skill_hint);
            final String[] SKILLS = getResources().getStringArray(R.array.services);
            ArrayAdapter<String> skillsAdapter = new ArrayAdapter<>(ClientMapActivity.this, R.layout.skills_list_item, SKILLS);
            required_skill_view.setAdapter(skillsAdapter);
            required_skill_view.setThreshold(1);

            builder.setView(required_skill_view);

            builder.setTitle("Worker Request")
                    .setMessage("Please select the service you require");
            builder.setPositiveButton("search", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if(required_skill_view.getText().toString().equals("")){
                        Toast.makeText(ClientMapActivity.this, "Please select a skill to continue", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        String requiredSkill = required_skill_view.getText().toString();
                        Log.d(TAG, "required skill: " + requiredSkill);
                        requestBol = true;
                        locateWorker(requiredSkill);
                    }
                }
            });
            builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                    getLastKnownLocation();
                }
            });

            AlertDialog requestDialog = builder.create();
            requestDialog.show();
        }

    }

    private void loadWorkerInfo(){
        //load the information of the worker found into the worker found layout
        if(workerFound!= null){

            Log.d(TAG, "workerFound ID: " + workerFound.getUser_id());

            workerInfo.setVisibility(View.VISIBLE);

            Glide
                    .with(getApplicationContext())
                    .load(workerFound.getUser_image_url())
                    .centerCrop()
                    .placeholder(R.drawable.default_profile)
                    .into(workerImage);

            workerFirstName.setText(workerFound.getFirst_name());
            workerLastName.setText(workerFound.getLast_name());
            workerPhone.setText(workerFound.getPhone_number());

            refreshWorkerMarker();
        } else {

            Toast.makeText(this, "Hmm, We Couldn't load the worker information", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshWorkerMarker(){
        if(workerFound != null){
            DocumentReference workerFoundRef = mDb.collection(getString(R.string.collection_worker_location))
                    .document(workerFound.getUser_id());

            workerFoundRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                    if(documentSnapshot.exists()){
                        WorkerLocation workerLocation= documentSnapshot.toObject(WorkerLocation.class);

                        LatLng workerLatLng = new LatLng(
                                workerLocation.getGeo_point().getLatitude(),
                                workerLocation.getGeo_point().getLongitude()
                        );
                        if(workerMarker!= null){
                            workerMarker.remove();
                            workerMarker = clientMap.addMarker(new MarkerOptions()
                                    .position((workerLatLng))
                                    .title(workerFound.getFirst_name())

                            );
                            workerMarker.showInfoWindow();


                        }
                    }
                }
            });
        }
    }

    private void startSetupIntent(){
        AlertDialog.Builder setupDialog = new AlertDialog.Builder(ClientMapActivity.this);
        setupDialog.setTitle("Profile Settings");
        setupDialog.setMessage("This takes you to profile setup. Continue?");

        setupDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent profileSetup = new Intent(ClientMapActivity.this, ClientSetupActivity.class);
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

    private void locateWorker(String required_skills){
        Log.d(TAG, "locateWorker called");

        Query skillsQuery = mDb.collection(getString(R.string.collection_worker_skills))
                    .whereArrayContains(getString(R.string.skill), required_skills);


        skillsQuery.get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {

                            if(task.isSuccessful() && task.getResult()!= null){

                                Log.d(TAG, "successfully retrieved worker documents");

                                for(QueryDocumentSnapshot doc : task.getResult()){

                                    Log.d(TAG, "documentID is:  " + doc.getId());
                                    Log.d(TAG, "workerID is: " + doc.getString("workerID"));

                                    qualifiedWorkerID = doc.getString("workerID");

                                    for (Worker worker : availableWorkerList){
                                        Log.d(TAG, worker.getUser_id() + "-->" +worker.getStatus());

                                        if ((worker.getUser_id().equals(qualifiedWorkerID))){

                                            Vibrator v = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                                            if(Build.VERSION.SDK_INT >=  Build.VERSION_CODES.O){
                                                v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                                            } else{
                                                v.vibrate(100);
                                            }

                                            workerFound = worker;
                                            Log.d(TAG, "workerFound First Name: " +workerFound.getFirst_name());
                                            updateRequestDb();

                                        } else{

                                            Toast.makeText(ClientMapActivity.this, "We can't seem to get a worker", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                }
                            } else{
                                Toast.makeText(ClientMapActivity.this, "We can't seem to get a worker", Toast.LENGTH_SHORT).show();
                            }

                        }
                    });

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.client_logout_ic: {
                FirebaseAuth.getInstance().signOut();
                Intent login = new Intent(ClientMapActivity.this , LoginActivity.class);
                startActivity(login);
                finish();
                break;
            }
            case R.id.request_worker: {
                requestForWorker();
                break;
            }

            case R.id.action_cancel_request: {
                cancelRequest();
                break;
            }

            case R.id.action_contact_worker: {
                if(workerFound!= null){
                    String uri = "tel:" + workerFound.getPhone_number();
                    Intent contact = new Intent(Intent.ACTION_DIAL);
                    contact.setData(Uri.parse(uri));
                    startActivity(contact);
                    break;
                }

            }
            case R.id.btn_reset_map: {
                if(workerFound != null){

                    Toast.makeText(this, "Cannot refresh map while engaged", Toast.LENGTH_SHORT).show();
                    break;
                }
                else {
                    resetMap();
                    break;
                }
            }
            case R.id.cl_settings_ic: {
                startSetupIntent();
            }
        }
    }
}
