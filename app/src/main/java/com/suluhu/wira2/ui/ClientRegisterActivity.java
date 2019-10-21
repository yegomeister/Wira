package com.suluhu.wira2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.suluhu.wira2.R;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import static android.text.TextUtils.isEmpty;
import static com.suluhu.wira2.utils.Check.doStringsMatch;

public class ClientRegisterActivity extends AppCompatActivity implements View.OnClickListener{

    //widgets
    private EditText clientEmail, clientPass, confirmClientPass;
    private ProgressBar mProgressBar;

    //vars
    private FirebaseFirestore mDb;

    //static values
    private static final String TAG = "ClientRegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_register);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        clientEmail = findViewById(R.id.reg_email);
        clientPass = findViewById(R.id.reg_pass);
        confirmClientPass = findViewById(R.id.confirm_reg_pass);
        mProgressBar = findViewById(R.id.client_reg_progress);

        mProgressBar.setVisibility(View.INVISIBLE);

        //clickables
        findViewById(R.id.client_login_btn).setOnClickListener(this);
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

                            String new_client_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

                            Log.d(TAG, "Current User ID is: " + new_client_id);

                            DocumentReference clientDocument = mDb.getInstance()
                                    .collection(getString(R.string.collection_clients))
                                    .document(new_client_id);

                            Map<String , Object> email = new HashMap<>();
                            email.put("user_email" , clientEmail.getText().toString());

                            clientDocument.set(email)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            if(task.isSuccessful()){

                                                Toast.makeText(ClientRegisterActivity.this, "Welcome to Wira!", Toast.LENGTH_SHORT).show();
                                                redirectLoginScreen();
                                            } else {

                                                if(task.getException().getMessage() != null){

                                                    String errorMessage = task.getException().getMessage();
                                                    Toast.makeText(ClientRegisterActivity.this, "Couldn't Sign You Up:" + errorMessage, Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }
                                    });


                        } else {

                            Toast.makeText(ClientRegisterActivity.this, "Something went wrong.", Toast.LENGTH_SHORT).show();
                            hideDialog();
                        }

                        // ...
                    }
                });
    }

    private void redirectLoginScreen(){
        Log.d(TAG, "redirectLoginScreen: redirecting to login screen.");

        Intent intent = new Intent(ClientRegisterActivity.this, LoginActivity.class);
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

                showDialog();

                Log.d(TAG, "onClick: attempting to register.");

                //check for null valued EditText fields
                if(!isEmpty(clientEmail.getText().toString())
                        && !isEmpty(clientPass.getText().toString())
                        && !isEmpty(confirmClientPass.getText().toString())){

                    //check if passwords match
                    if(doStringsMatch(clientPass.getText().toString(), clientPass.getText().toString())){

                        //Initiate registration task
                        registerNewEmail(clientEmail.getText().toString(), clientPass.getText().toString());
                    }else{

                        hideDialog();
                        Toast.makeText(ClientRegisterActivity.this, "Passwords do not Match", Toast.LENGTH_SHORT).show();
                    }

                }else{

                    hideDialog();
                    Toast.makeText(ClientRegisterActivity.this, "You must fill out all the fields", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.client_login_btn:
                //TODO send to login

                redirectLoginScreen();
                break;
        }
    }
}
