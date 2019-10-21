package com.suluhu.wira2.ui;

import android.content.Intent;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.suluhu.wira2.R;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore mDb;

    //vars
    private static final String TAG = ".MainActivity";
    private String current_user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mAuth = FirebaseAuth.getInstance();
        mDb = FirebaseFirestore.getInstance();

    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG , "OnStart Triggered");
        checkIfLoggedIn();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG , "onResume Triggered");
        checkIfLoggedIn();
    }

    private void  checkIfLoggedIn(){

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if(currentUser == null){

            sendToLogin();

        } else {

            current_user_id = mAuth.getCurrentUser().getUid();

            checkClientExistence();
        }
    }
    private void checkClientExistence(){

        mDb.collection(getString(R.string.collection_clients))
                .document(current_user_id)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if(task.isSuccessful()){

                    if(task.getResult().exists()){

                        if(task.getResult().contains("first_name")){

                            String first_name = task.getResult().get("first_name").toString();
                            Toast.makeText(MainActivity.this, "Hello, " +first_name, Toast.LENGTH_SHORT).show();

                            Intent clientMapIntent = new Intent(MainActivity.this , ClientMapActivity.class);
                            startActivity(clientMapIntent);
                            finish();
                        } else{
                            Toast.makeText(MainActivity.this, "Please Set Your Profile to Continue", Toast.LENGTH_SHORT).show();

                                Intent setupIntent = new Intent(MainActivity.this , ClientSetupActivity.class);
                                startActivity(setupIntent);
                                finish();
                        }

                    } else {

                        checkWorkerExistence();
                    }
                } else {

                    if(task.getException().getMessage() != null){

                        String error = task.getException().getMessage();
                        Toast.makeText(MainActivity.this, "Oops: " + error, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void checkWorkerExistence(){
        mDb.collection(getString(R.string.collection_workers))
                .document(current_user_id)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()){

                            if(task.getResult().exists()){

                                Log.d(TAG , "checkWorkerExistence onComplete: " + task.isSuccessful());
                                //checkWorkerProfileExistence();

                                if(task.getResult().contains("status")){

                                    Intent workerMap = new Intent(MainActivity.this , WorkerMapActivity.class);
                                    startActivity(workerMap);
                                    finish();

                                } else{

                                    sendtoWorkerSetup();

                                }
                            } else {
                                Toast.makeText(MainActivity.this, "Can't seem to find you", Toast.LENGTH_SHORT).show();
                                sendToLogin();
                                //TODO send to worker setup activity
                            }

                        }
                    }
                });
    }


    private void checkClientProfileExistence(){

//        mDb.collection(getString(R.string.collection_client_profiles))
//                .document(current_user_id)
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                        if(task.isSuccessful()){
//
//                            if(task.getResult().exists()){
//
//                                //TODO send to client map activity
//                                Toast.makeText(MainActivity.this, "All set!", Toast.LENGTH_SHORT).show();
//
//                                Intent clientMapIntent = new Intent(MainActivity.this , ClientMapActivity.class);
//                                startActivity(clientMapIntent);
//                                finish();
//                            } else {
//
//                                Toast.makeText(MainActivity.this, "Please Set Your Profile to Continue", Toast.LENGTH_SHORT).show();
//
//                                Intent setupIntent = new Intent(MainActivity.this , ClientSetupActivity.class);
//                                startActivity(setupIntent);
//                                finish();
//                            }
//                        } else {
//                            if(task.getException().getMessage() != null){
//                                String error = task.getException().getMessage();
//                                Toast.makeText(MainActivity.this, "Error Retrieving Client Profile" + error, Toast.LENGTH_SHORT).show();
//                            }
//                        }
//                    }
//                });

    }

    private void checkWorkerProfileExistence(){


//        mDb.collection(getString(R.string.collection_worker_profiles))
//                .document(current_user_id)
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                        if(task.isSuccessful()){
//
//                            Log.d(TAG , "Beginning User Profile Check");
//
//                            if(task.getResult().exists()){
//
//                                Intent workerMap = new Intent(MainActivity.this , WorkerMapActivity.class);
//                                startActivity(workerMap);
//                                finish();
//                                //TODO send to worker map activity
//
//                            } else {
//                                Log.d(TAG, "Redirecting to Profile Setup");
//                                Toast.makeText(MainActivity.this, "Please setup your profile to continue", Toast.LENGTH_SHORT).show();
//
//                                sendtoWorkerSetup();
//                            }
//                        } else {
//                            if(task.getException().getMessage() != null){
//                                String error = task.getException().getMessage();
//                                Toast.makeText(MainActivity.this, "Error Retrieving User Profile" + error, Toast.LENGTH_SHORT).show();
//                            }
//                        }
//                    }
//                });
    }

    private void sendtoWorkerSetup(){

        Toast.makeText(this, "Please setup your profile to continue", Toast.LENGTH_SHORT).show();
        Intent workerSetupIntent = new Intent(MainActivity.this , WorkerSetupActivity.class);
        startActivity(workerSetupIntent);
        finish();
    }

    private void sendToLogin(){

        Intent loginIntent = new Intent(MainActivity.this , LoginActivity.class);
        startActivity(loginIntent);
        finish();
    }
}
