package com.suluhu.wira2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.suluhu.wira2.R;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import static android.text.TextUtils.isEmpty;
import static com.suluhu.wira2.utils.Check.doStringsMatch;

public class WorkerRegisterActivity extends AppCompatActivity implements View.OnClickListener {

    //widgets
    private EditText workerEmail, workerPass, confirmWorkerPass;
    private ProgressBar mProgressBar;


    //vars
    private FirebaseFirestore mDb;

    //static values
    private static final String TAG = "WorkerRegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_register);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        workerEmail = findViewById(R.id.reg_email);
        workerPass = findViewById(R.id.reg_pass);
        confirmWorkerPass = findViewById(R.id.confirm_reg_pass);
        mProgressBar = findViewById(R.id.worker_reg_progress);

        mProgressBar.setVisibility(View.INVISIBLE);

        //clickables
        findViewById(R.id.worker_login_btn).setOnClickListener(this);
        findViewById(R.id.reg_btn).setOnClickListener(this);
    }

    public void registerNewEmail(final String email, String password){

        showDialog();

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());

                        if (task.isSuccessful()){

                            String new_worker_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

                            Log.d(TAG, "Current User ID is: " + new_worker_id);

                            Toast.makeText(WorkerRegisterActivity.this, "Welcome to Wira!", Toast.LENGTH_SHORT).show();


                            DocumentReference workerDocument = mDb.getInstance()
                                    .collection(getString(R.string.collection_workers))
                                    .document(new_worker_id);

                            Map<String , Object> email = new HashMap<>();
                            email.put("user_email" , workerEmail.getText().toString());

                            workerDocument.set(email)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            if(task.isSuccessful()){

                                                redirectLoginScreen();
                                            } else {
                                                if(task.getException().getMessage() != null){

                                                    String errorMessage = task.getException().getMessage();
                                                    Toast.makeText(WorkerRegisterActivity.this, "Couldn't Sign You Up:" + errorMessage, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }
                                    });


                        } else {

                            Toast.makeText(WorkerRegisterActivity.this, "Something went wrong.", Toast.LENGTH_SHORT).show();
                            hideDialog();
                        }

                        // ...
                    }
                });
    }

    private void redirectLoginScreen(){
        Log.d(TAG, "redirectLoginScreen: redirecting to login screen.");

        Intent intent = new Intent(WorkerRegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void showDialog(){
        mProgressBar.setVisibility(View.VISIBLE);

    }

    private void hideDialog(){
        if(mProgressBar.getVisibility() == View.VISIBLE){
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){

            case R.id.reg_btn:
                //TODO register new worker

                showDialog();

                Log.d(TAG, "onClick: attempting to register.");

                //check for null valued EditText fields
                if(!isEmpty(workerEmail.getText().toString())
                        && !isEmpty(workerPass.getText().toString())
                        && !isEmpty(confirmWorkerPass.getText().toString())){

                    //check if passwords match
                    if(doStringsMatch(workerPass.getText().toString(), confirmWorkerPass.getText().toString())){

                        //Initiate registration task
                        registerNewEmail(workerEmail.getText().toString(), workerPass.getText().toString());
                    }else{

                        hideDialog();
                        Toast.makeText(WorkerRegisterActivity.this, "Passwords do not Match", Toast.LENGTH_SHORT).show();
                    }

                }else{

                    hideDialog();
                    Toast.makeText(WorkerRegisterActivity.this, "You must fill out all the fields", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.worker_login_btn:
                //TODO send to login

                redirectLoginScreen();
                break;
        }
    }
}
