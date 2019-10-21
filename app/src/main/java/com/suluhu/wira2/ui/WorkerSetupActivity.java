package com.suluhu.wira2.ui;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
//import android.support.annotation.NonNull;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.content.ContextCompat;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
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
import com.suluhu.wira2.models.Worker;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import de.hdodenhof.circleimageview.CircleImageView;

import static com.suluhu.wira2.Constants.EXTERNAL_STORAGE_REQUEST_CODE;
import static com.suluhu.wira2.utils.Check.isEmpty;

public class WorkerSetupActivity extends AppCompatActivity implements View.OnClickListener{

    private FirebaseAuth mAuth;
    private FirebaseFirestore mDb;
    private Worker worker = null;

    //widgets
    private CircleImageView workerImage;
    private Uri workerImageUri;
    private ProgressBar workerProgressBar;
    private Calendar myCalendar;
    private EditText wFirstName , wLastName , wPhone , wDOB ;
    private NachoTextView wSkills;
    private CountryCodePicker ccp;

    //vars
    private String user_id;
    private Boolean isChanged = false;
    private StorageTask uploadTask;
    private Boolean externalStoragePermissionGranted;
    private static final String TAG = ".WorkerSetupActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_setup);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        //Firebase
        mAuth = FirebaseAuth.getInstance();
        user_id = mAuth.getCurrentUser().getUid();

        workerImage = findViewById(R.id.worker_setup_image);

        myCalendar = Calendar.getInstance();

        wFirstName = findViewById(R.id.worker_firstName);
        wLastName = findViewById(R.id.worker_lastName);
        wPhone = findViewById(R.id.worker_phone);
        wDOB = findViewById(R.id.worker_DOB);
        wSkills = findViewById(R.id.nacho_skills);
        ccp = findViewById(R.id.ccp);

        ccp.registerCarrierNumberEditText(wPhone);

        workerProgressBar = findViewById(R.id.worker_progress_bar);
        workerProgressBar.setVisibility(View.INVISIBLE);

        findViewById(R.id.worker_setupBtn).setOnClickListener(this);
        findViewById(R.id.worker_logoutBtn).setOnClickListener(this);
        findViewById(R.id.worker_DOB).setOnClickListener(this);
        findViewById(R.id.worker_setup_image).setOnClickListener(this);


        //Initialize Image Picker
        workerImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(ContextCompat.checkSelfPermission(WorkerSetupActivity.this , Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

                    ActivityCompat.requestPermissions(WorkerSetupActivity.this , new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_REQUEST_CODE);

                } else {

                    externalStoragePermissionGranted = true;
                    showImagePicker();
                }
            }

        });

        //User Date of Birth Picker

        final DatePickerDialog.OnDateSetListener dateOFBirth = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {

                myCalendar.set(Calendar.YEAR , year);
                myCalendar.set(Calendar.MONTH , month);
                myCalendar.set(Calendar.DAY_OF_MONTH , day);
                updateLabel();
            }

        };

        //@View.OnClickListener for Date of Birth @EditText

        wDOB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new DatePickerDialog(WorkerSetupActivity.this , dateOFBirth , myCalendar.get(Calendar.YEAR) , myCalendar.get(Calendar.MONTH) ,
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        //@NachoTextView Implementation

        String[] skills = getResources().getStringArray(R.array.services);
        ArrayAdapter<String> skillsAdapter = new ArrayAdapter<>(this, R.layout.skills_list_item, skills);
        wSkills.setAdapter(skillsAdapter);
        wSkills.setThreshold(1);

        checkProfileInstance();
    }

    private void checkProfileInstance(){
        mDb.collection(getString(R.string.collection_workers))
                .document(user_id)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if(documentSnapshot.exists()){

                            worker = documentSnapshot.toObject(Worker.class);

                            preLoadInfo();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "couldn't load worker information: " + e);
                    }
                });
    }

    private void preLoadInfo(){
        if(worker != null){
            String image_url = worker.getUser_image_url();
            String first_name = worker.getFirst_name();
            String last_name = worker.getLast_name();
            String phone = worker.getPhone_number();

            workerImageUri = Uri.parse(image_url);

            Glide
                    .with(getApplicationContext())
                    .load(workerImageUri)
                    .centerCrop()
                    .placeholder(R.drawable.default_profile)
                    .into(workerImage);
            wFirstName.setText(first_name);
            wLastName.setText(last_name);
            wPhone.setText(phone);
        }


    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){

            case R.id.worker_setupBtn:

                showDialog();

                //TODO save worker profile to Firebase
                if(uploadTask != null && uploadTask.isInProgress()){

                    Toast.makeText(WorkerSetupActivity.this, "Setting up your profile", Toast.LENGTH_SHORT).show();

                } else {

                    if(!isEmpty(wFirstName.getText().toString()) && !isEmpty(wLastName.getText().toString())
                            && !isEmpty(wDOB.getText().toString()) && !isEmpty(wPhone.getText().toString())
                            && !isEmpty(workerImageUri.toString()) && !wSkills.getChipValues().equals("")
                    ){

                        if(isChanged){

                            uploadWorkerProfile(wFirstName.getText().toString() , wLastName.getText().toString() , wDOB.getText().toString() , ccp.getFullNumberWithPlus() ,
                                    workerImageUri.toString() ,  wSkills.getChipValues());

                        } else{

                            uploadWorkerProfile(wFirstName.getText().toString() , wLastName.getText().toString() , wDOB.getText().toString() , ccp.getFullNumberWithPlus() ,
                                    workerImageUri.toString() ,  wSkills.getChipValues());
                        }
                    } else {
                        hideDialog();
                        Toast.makeText(this, "Please Fill in all required fields", Toast.LENGTH_SHORT).show();
                    }

                }
                break;

            case R.id.worker_logoutBtn:
                mAuth.signOut();
                sendToLogin();
                break;

        }
    }

    private void updateLabel() {

        String myDateFormat = "dd/MM/yyyy";
        SimpleDateFormat dateFormat = new SimpleDateFormat(myDateFormat , Locale.ENGLISH);

        wDOB.setText(dateFormat.format(myCalendar.getTime()));
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

                workerImageUri = result.getUri();
                workerImage.setImageURI(workerImageUri);

                isChanged = true;

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

                Exception error = result.getError();
                Toast.makeText(WorkerSetupActivity.this , "Error:" + error , Toast.LENGTH_LONG).show();
            }
        }
    }


    private void showImagePicker(){

        // start picker to get image for cropping and then use the image in cropping activity
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setRequestedSize(500 , 500 , CropImageView.RequestSizeOptions.RESIZE_INSIDE)
                .setAspectRatio(1, 1)
                .start(WorkerSetupActivity.this);
    }

    private void sendToLogin(){
        Intent loginIntent = new Intent(WorkerSetupActivity.this, LoginActivity.class);
        startActivity(loginIntent);
        finish();
    }

    private void uploadWorkerProfile(final String first_name , final String last_name , final String date_of_birth , final String phone_number , String image_uri, final List<String> skills){

        final StorageReference profile_image_path = FirebaseStorage.getInstance().getReference()
                .child(getString(R.string.profile_images))
                .child(getString(R.string.collection_workers))
                .child(user_id + ".jpg");
        uploadTask = profile_image_path.putFile(workerImageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        taskSnapshot.getStorage().getDownloadUrl()
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {

                                        DocumentReference workerRef = FirebaseFirestore.getInstance()
                                                .collection(getString(R.string.collection_workers))
                                                .document(user_id);

                                        Worker worker = new Worker();
                                        worker.setFirst_name(first_name);
                                        worker.setLast_name(last_name);
                                        worker.setPhone_number(phone_number);
                                        worker.setStatus(getString(R.string.available));
                                        worker.setDate_of_birth(date_of_birth);
                                        worker.setUser_image_url(uri.toString());
                                        worker.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());

                                        workerRef.set(worker).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {

                                                if(task.isSuccessful()){
                                                    DocumentReference skillsRef = FirebaseFirestore.getInstance()
                                                            .collection(getString(R.string.collection_worker_skills))
                                                            .document(user_id);

                                                    Map worker_skills = new HashMap();
                                                    worker_skills.put(getString(R.string.skill) , skills);
                                                    worker_skills.put("workerID", FirebaseAuth.getInstance().getCurrentUser().getUid());

                                                    skillsRef.set(worker_skills , SetOptions.merge())
                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if(task.isSuccessful()){
                                                                        Toast.makeText(WorkerSetupActivity.this, "Profile Successfully Saved", Toast.LENGTH_SHORT).show();
                                                                        hideDialog();
                                                                        sendToMain();
                                                                    }
                                                                }
                                                            });
                                                }

                                            }
                                        });

                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(WorkerSetupActivity.this, "Couldn't upload Image" + e, Toast.LENGTH_SHORT).show();
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
        Intent mainIntent = new Intent(WorkerSetupActivity.this , MainActivity.class);
        startActivity(mainIntent);
    }

    private void showDialog() {
        workerProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideDialog() {
        workerProgressBar.setVisibility(View.INVISIBLE);
    }
}
