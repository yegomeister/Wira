package com.suluhu.wira2.ui;

import android.content.Intent;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.suluhu.wira2.R;

import javax.annotation.Nullable;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import static android.text.TextUtils.isEmpty;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "LoginActivity";

    //Firebase
    private FirebaseAuth.AuthStateListener mAuthListener;

    // widgets
    private EditText mEmail, mPassword;
    private ProgressBar mProgressBar;

    //vars
    private String current_user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mEmail = findViewById(R.id.login_email);
        mPassword = findViewById(R.id.login_pass);
        mProgressBar = findViewById(R.id.login_progress);
        mProgressBar.setVisibility(View.INVISIBLE);

        setupFirebaseAuth();
        findViewById(R.id.login_btn).setOnClickListener(this);
        findViewById(R.id.reg_btn).setOnClickListener(this);
    }

    private void setupFirebaseAuth(){
        Log.d(TAG, "setupFirebaseAuth: started.");

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if(user != null){
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());

                    redirectToMain();
                    //checkUserExistence();
                } else {
                    //User is signed out...

                }
            }
        };
    }

    @Override
    public void onClick(View view) {
        // TODO stuff later
        switch (view.getId()){

            case R.id.login_btn:
                login();
                break;

            case R.id.reg_btn:
                Intent intent = new Intent(LoginActivity.this , RegisterChoiceActivity.class);
                startActivity(intent);
                break;
        }
    }

    private void login(){
        //check if the fields are filled out
        if(!isEmpty(mEmail.getText().toString()) && !isEmpty(mPassword.getText().toString())){

            Log.d(TAG, "onClick: attempting to authenticate.");

            showDialog();

            FirebaseAuth.getInstance().signInWithEmailAndPassword(mEmail.getText().toString(),
                    mPassword.getText().toString())
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){

                                redirectToMain();
                                //checkUserExistence();

                            } else {

                                if(task.getException() != null){

                                    String errorMessage  = task.getException().getMessage();
                                    Toast.makeText(LoginActivity.this, "Error Logging In:" + errorMessage, Toast.LENGTH_SHORT).show();
                                }

                            }
                            hideDialog();

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(LoginActivity.this, "Authentication Failed" + e, Toast.LENGTH_SHORT).show();
                    hideDialog();
                }
            });
        }else{
            Toast.makeText(LoginActivity.this, "You didn't fill in all the fields.", Toast.LENGTH_SHORT).show();
        }
    }

    /**

    private void checkUserExistence(){

        current_user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore db =FirebaseFirestore.getInstance();
        DocumentReference clientRef =db.collection(getString(R.string.collection_clients))
                .document(current_user_id);

        clientRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot,
                                @Nullable FirebaseFirestoreException e) {
                if(documentSnapshot.exists()){

                    redirectToMain();

                } else {

                    checkWorkerExistence();
                }
            }
        });
    }

    private void checkWorkerExistence(){
        current_user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore db =FirebaseFirestore.getInstance();
        DocumentReference clientRef =db.collection(getString(R.string.collection_workers))
                .document(current_user_id);

        clientRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot,
                                @Nullable FirebaseFirestoreException e) {
                if(documentSnapshot.exists()){

                    redirectToMain();
                } else {
                    Toast.makeText(LoginActivity.this, "Hmm, can't seem to find you", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    */

    private void showDialog(){

        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideDialog(){
        if(mProgressBar.getVisibility() == View.VISIBLE){
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }
    private void redirectToMain(){
        Intent mainIntent = new Intent(LoginActivity.this , MainActivity.class);
        startActivity(mainIntent);
        finish();
    }
}
