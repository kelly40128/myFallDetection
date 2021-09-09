package com.example.rainy.myfalldetection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;


public class FallDetection extends AppCompatActivity {

    TextView Ax, Ay, Az, Lx, Ly, Lz, Gx, Gy, Gz;
    Intent serviceIntent;
    DetectionReceiver detectionReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Ax = findViewById(R.id.Ax);
        Ay = findViewById(R.id.Ay);
        Az = findViewById(R.id.Az);
        Lx = findViewById(R.id.Lx);
        Ly = findViewById(R.id.Ly);
        Lz = findViewById(R.id.Lz);
        Gx = findViewById(R.id.Gx);
        Gy = findViewById(R.id.Gy);
        Gz = findViewById(R.id.Gz);


        serviceIntent = new Intent(FallDetection.this, FallDetectionService.class);
        detectionReceiver = new DetectionReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(detectionReceiver);
        stopService(serviceIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Intent intent = new Intent();
            intent.setClass(FallDetection.this, Setting.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    public class DetectionReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            Ax.setText("Ax: " + intent.getStringExtra("Ax"));
            Ay.setText("Ay: " + intent.getStringExtra("Ay"));
            Az.setText("Az: " + intent.getStringExtra("Az"));
            Lx.setText("Lx: " + intent.getStringExtra("Lx"));
            Ly.setText("Ly: " + intent.getStringExtra("Ly"));
            Lz.setText("Lz: " + intent.getStringExtra("Lz"));
            Gx.setText("Gx: " + intent.getStringExtra("Gx"));
            Gy.setText("Gy: " + intent.getStringExtra("Gy"));
            Gz.setText("Gz: " + intent.getStringExtra("Gz"));

            Log.d("Ax = ", Ax.getText().toString());

        }
    }

    public void btn_start(View v) {
        IntentFilter filter = new IntentFilter("data");
        registerReceiver(detectionReceiver, filter);
        startService(serviceIntent);
    }
}
