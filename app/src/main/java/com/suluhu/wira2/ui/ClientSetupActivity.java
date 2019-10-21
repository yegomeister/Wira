package com.suluhu.wira2.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.hbb20.CountryCodePicker;
import com.hootsuite.nachos.NachoTextView;
import com.suluhu.wira2.R;
import com.suluhu.wira2.models.User;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import de.hdodenhof.circleimageview.CircleImageView;

import static com.suluhu.wira2.Constants.EXTERNAL_STORAGE_REQUEST_CODE;
import static com.suluhu.wira2.utils.Check.isEmpty;

public class ClientSetupActivity extends AppCompatActivity implements View.OnClickListener{

    private FirebaseAuth mAuth;
    private FirebaseFirestore mDb;
    private User user = null;

    //widgets
    private CircleImageView clientImage;
    private Uri clientImageUri;
    private ProgressBar clientProgressBar;;
    private EditText clFirstName , clLastName , clPhone ;
    private CountryCodePicker ccp;

    //vars
    private String user_id;
    private Boolean isChanged = false;
    private StorageTask uploadTask;
    private Boolean externalStoragePermissionGranted;
    private static final String TAG = ".clientSetupActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_setup);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        //Firebase
        mAuth = FirebaseAuth.getInstance();
        user_id = mAuth.getCurrentUser().getUid();

        clientImage = findViewById(R.id.client_setup_image);

        clFirstName = findViewById(R.id.client_first_name);
        clLastName = findViewById(R.id.client_last_name);
        clPhone = findViewById(R.id.client_phone);
        ccp = findViewById(R.id.ccp);

        ccp.registerCarrierNumberEditText(clPhone);

        clientProgressBar = findViewById(R.id.client_progress_bar);
        clientProgressBar.setVisibility(View.INVISIBLE);

        findViewById(R.id.client_setup_btn).setOnClickListener(this);
        findViewById(R.id.client_logout_btn).setOnClickListener(this);
        findViewById(R.id.client_setup_image).setOnClickListener(this);

        //Initialize Image Picker
        clientImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(ContextCompat.checkSelfPermission(ClientSetupActivity.this , Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

                    ActivityCompat.requestPermissions(ClientSetupActivity.this , new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_REQUEST_CODE);

                } else {

                    externalStoragePermissionGranted = true;
                    showImagePicker();
                }
            }

        });

        checkProfileInstance();
    }

    private void checkProfileInstance(){
        mDb.collection(getString(R.string.collection_clients))
                .document(user_id)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(documentSnapshot.exists()){
                            user = documentSnapshot.toObject(User.class);
                            preLoadInfo();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "couldn't load user information: " + e);
                    }
                });
    }

    private void preLoadInfo(){
        if(user != null){
            String image_url = user.getUser_image_url();
            String first_name = user.getFirst_name();
            String last_name = user.getLast_name();
            String phone = user.getPhone_number();

            clientImageUri = Uri.parse(image_url);

            Glide
                    .with(getApplicationContext())
                    .load(clientImageUri)
                    .centerCrop()
                    .placeholder(R.drawable.default_profile)
                    .into(clientImage);
            clFirstName.setText(first_name);
            clLastName.setText(last_name);
            clPhone.setText(phone);
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        externalStoragePermissionGranted = false;

        switch(requestCode){

            case EXTERNAL_STORAGE_REQUEST_CODE:{

                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                    externalStoragePermissionGranted = true;
                    showImagePicker();
                } else {

                    Toast.makeText(this, "Please Allow Permissions to Continue", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

            CropImage.ActivityResult result = CropImage.getActivityResult(data);

            if (resultCode == RESULT_OK) {

                clientImageUri = result.getUri();
                clientImage.setImageURI(clientImageUri);

                isChanged = true;

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

                Exception error = result.getError();
                Toast.makeText(ClientSetupActivity.this , "Error:" + error , Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onClick(View view) {

        switch(view.getId()){

            case R.id.client_setup_btn:

                showDialog();

                //TODO save client profile to Firebase
                if(uploadTask != null && uploadTask.isInProgress()){

                    Toast.makeText(ClientSetupActivity.this, "Setting up your profile", Toast.LENGTH_SHORT).show();

                } else {

                    if(!isEmpty(clFirstName.getText().toString()) && !isEmpty(clLastName.getText().toString())
                            && !isEmpty(clPhone.getText().toString())
                            && !isEmpty(clientImageUri.toString())
                    ){

                        if(isChanged){

                            uploadClientProfile(clFirstName.getText().toString() , clLastName.getText().toString() , ccp.getFullNumberWithPlus() ,
                                    clientImageUri.toString());

                        } else{

                            uploadClientProfile(clFirstName.getText().toString() , clLastName.getText().toString() , ccp.getFullNumberWithPlus() ,
                                    clientImageUri.toString());
                        }
                    } else {
                        hideDialog();
                        Toast.makeText(this, "Please Fill in all required fields", Toast.LENGTH_SHORT).show();
                    }

                }
                break;

            case R.id.client_logout_btn:{
                mAuth.signOut();
                sendToLogin();
                break;
            }
        }
    }

    private void showImagePicker(){

        // start picker to get image for cropping and then use the image in cropping activity
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setRequestedSize(500 , 500 , CropImageView.RequestSizeOptions.RESIZE_INSIDE)
                .setAspectRatio(1, 1)
                .start(ClientSetupActivity.this);
    }

    private void uploadClientProfile(final String first_name , final String last_name , final String phone_number , String image_uri){

        final StorageReference profile_image_path = FirebaseStorage.getInstance().getReference()
                .child(getString(R.string.profile_images))
                .child(getString(R.string.collection_clients))
                .child(user_id + ".jpg");
        uploadTask = profile_image_path.putFile(clientImageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        taskSnapshot.getStorage().getDownloadUrl()
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {

                                        final DocumentReference clientProfileRef = FirebaseFirestore.getInstance()
                                                .collection(getString(R.string.collection_clients))
                                                .document(user_id);

                                        User client = new User();
                                        client.setFirst_name(first_name);
                                        client.setLast_name(last_name);
                                        client.setPhone_number(phone_number);
                                        client.setUser_image_url(uri.toString());
                                        client.setUser_id(FirebaseAuth.getInstance().getUid());

                                        clientProfileRef.set(client).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {

                                                if(task.isSuccessful()){

                                                    Toast.makeText(ClientSetupActivity.this, "Profile Successfully Saved", Toast.LENGTH_SHORT).show();
                                                    hideDialog();
                                                    sendToMain();
                                                }
                                            }
                                        });

                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(ClientSetupActivity.this, "Couldn't upload Image" + e, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    private void sendToMain(){
        Intent mainIntent = new Intent(ClientSetupActivity.this , MainActivity.class);
        startActivity(mainIntent);
    }

    private void sendToLogin(){
        Intent loginIntent = new Intent(ClientSetupActivity.this, LoginActivity.class);
        startActivity(loginIntent);
        finish();
    }

    private void showDialog() {
        clientProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideDialog() {
        clientProgressBar.setVisibility(View.INVISIBLE);
    }
}
