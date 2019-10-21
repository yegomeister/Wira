package com.suluhu.wira2.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.suluhu.wira2.R;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterChoiceActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_choice);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        findViewById(R.id.reg_as_client).setOnClickListener(this);
        findViewById(R.id.reg_as_worker).setOnClickListener(this);
        findViewById(R.id.back_to_login).setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){

            case R.id.reg_as_client:
                Intent clientRegIntent = new Intent(RegisterChoiceActivity.this , ClientRegisterActivity.class);
                startActivity(clientRegIntent);
                break;

            case R.id.reg_as_worker:
                Intent workerRegIntent = new Intent(RegisterChoiceActivity.this , WorkerRegisterActivity.class);
                startActivity(workerRegIntent);
                break;

            case R.id.back_to_login:
                Intent loginIntent = new Intent(RegisterChoiceActivity.this , LoginActivity.class);
                startActivity(loginIntent);
                break;
        }
    }
}
