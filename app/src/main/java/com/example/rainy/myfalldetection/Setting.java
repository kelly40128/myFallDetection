package com.example.rainy.myfalldetection;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;

public class Setting extends AppCompatActivity {

    EditText phoneNumber, contactMessage;
    SharedPreferences contactData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        phoneNumber = findViewById(R.id.phoneNumber);
        contactMessage = findViewById(R.id.contactMessage);

        // Get data
        contactData = getSharedPreferences("contactData", MODE_PRIVATE);
        phoneNumber.setText(contactData.getString("phoneNumber", ""));
        contactMessage.setText(contactData.getString("contactMessage", ""));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // Save data
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putString("phoneNumber", phoneNumber.getText().toString());
        editor.putString("contactMessage", contactMessage.getText().toString());
        editor.commit();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Save data
        SharedPreferences.Editor editor = contactData.edit();
        editor.putString("phoneNumber", phoneNumber.getText().toString());
        editor.putString("contactMessage", contactMessage.getText().toString());
        editor.commit();
    }
}
