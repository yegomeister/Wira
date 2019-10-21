package com.suluhu.wira2.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
//import android.support.annotation.NonNull;
//import android.support.annotation.Nullable;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.suluhu.wira2.R;
import com.suluhu.wira2.WorkerClient;
import com.suluhu.wira2.models.Worker;
import com.suluhu.wira2.models.WorkerLocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

public class LocationService extends Service {

//    private FirebaseDatabase fDb;

    private static final String TAG = "LocationService";

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private final static long UPDATE_INTERVAL = 4 * 1000;
    private final static long FASTEST_INTERVAL = 2000;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

//        fDb = FirebaseDatabase.getInstance();

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if(Build.VERSION.SDK_INT >= 26){
            String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID ,
                    "My Channel" , NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder( this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("")
                    .build();

            startForeground(1 , notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG , "onStartCommand : called");
        getLocation();
        return START_NOT_STICKY;
    }

    private void getLocation(){
        LocationRequest mLocationRequestHighAccuracy = new LocationRequest();
        mLocationRequestHighAccuracy.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequestHighAccuracy.setInterval(UPDATE_INTERVAL);
        mLocationRequestHighAccuracy.setFastestInterval(FASTEST_INTERVAL);

        if(ActivityCompat.checkSelfPermission( this , Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            Log.d(TAG , "getLocation : stopping location service");
            stopSelf();
            return;
        }
        Log.d(TAG , "getLocation : getting location info");
        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequestHighAccuracy , new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.d(TAG , "onLocationResult : found a location result");

                Location location = locationResult.getLastLocation();

                if(location != null){
                    Worker worker = ((WorkerClient)getApplicationContext()).getWorker();
                    GeoPoint geoPoint = new GeoPoint(location.getLatitude() , location.getLongitude());
                    WorkerLocation workerLocation = new WorkerLocation(worker, geoPoint, null);
                    saveWorkerLocation(workerLocation);
                }
            }
        } , Looper.myLooper());
    }

    public void saveWorkerLocation(final WorkerLocation workerLocation){

        try{
            DocumentReference workerLocationRef = FirebaseFirestore.getInstance()
                    .collection(getString(R.string.collection_worker_location))
                    .document(FirebaseAuth.getInstance().getUid());
            Worker worker = ((WorkerClient)getApplicationContext()).getWorker();

            workerLocationRef.set(workerLocation).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        Log.d(TAG , "onComplete: \n inserted worker location to dB" +
                                "\n latitude : " + workerLocation.getGeo_point().getLatitude() +
                                "\n longitude : " +workerLocation.getGeo_point().getLongitude());
                    }
                }
            });

        } catch (NullPointerException e){
            Log.e(TAG , "saveUserLocation : NullPointerException: " + e.getMessage());
            stopSelf();
        }
    }
}
