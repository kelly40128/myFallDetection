package com.example.rainy.myfalldetection;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

public class FallDetectionService extends Service implements SensorEventListener {

    Handler handler = new Handler();
    int waitTimer = 5;
    Intent intent;

    SensorManager mSensorManager;
    Sensor mAccSensor, mLinearAccSensor, mGravitySensor;
    String Ax, Ay, Az, Lx, Ly, Lz, Gx, Gy, Gz;

    double alpha = 0.8, max_SMV_A = 11, min_SMV_A = 9, max_SMV_L = 8.5, max_SMV_Llpf = 2.5;
    float UR_Gx, UR_Gy, UR_Gz, lpf_Lx = -10, lpf_Ly = -10, lpf_Lz = -10;
    float changeGx, changeGy, changeGz;
    MediaPlayer ding, alarm;
    String state, body;
    double SMV_A, SMV_L, SMV_Llpf, thetaTilt, thetaChange, thetaT = 35, thetaC = 50;

    Long startTime, spentTime, seconds, motionTime, motionlessTime, m_seconds, ml_seconds;
    boolean motion = false, motionless = false;
    SharedPreferences contactData;

    @Override
    public void onCreate() {
        super.onCreate();
        intent = new Intent();
        ding = MediaPlayer.create(this, R.raw.ding);
        alarm = MediaPlayer.create(this, R.raw.alarm);
        state = "motionless";
        body = "upright";
        contactData = getApplicationContext().getSharedPreferences("contactData", Context.MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startTime = SystemClock.elapsedRealtime();
        initializeSensors();
        handler.postDelayed(updateTimer, 1000);
        handler.postDelayed(updatethetaChange, 1000);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        handler.removeCallbacks(updateTimer);
        handler.removeCallbacks(updatethetaChange);
        ding.release();
        alarm.release();
        super.onDestroy();
    }

    private void initializeSensors() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mLinearAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        mSensorManager.registerListener(this, mAccSensor, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mLinearAccSensor, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mGravitySensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private final Runnable updateTimer = new Runnable() {
        @Override
        public void run() {
            spentTime = SystemClock.elapsedRealtime() - startTime;
            seconds = (spentTime/1000);
            Log.d("seconds = ", String.format("%d", seconds));
            getSensorData(Ax, Ay, Az, Lx, Ly, Lz, Gx, Gy, Gz);
            intent.setAction("data");
            sendBroadcast(intent);
            handler.postDelayed(this, 90);
        }
    };

    private final Runnable updatethetaChange = new Runnable() {
        @Override
        public void run() {
            changeGx = Float.parseFloat(Gx);
            changeGy = Float.parseFloat(Gy);
            changeGz = Float.parseFloat(Gz);
            handler.postDelayed(this, 1000);
        }
    };

    private void getSensorData(String ax, String ay, String az, String lx, String ly, String lz, String gx, String gy, String gz) {
        float Ax = Float.parseFloat(ax);
        float Ay = Float.parseFloat(ay);
        float Az = Float.parseFloat(az);
        float Lx = Float.parseFloat(lx);
        float Ly = Float.parseFloat(ly);
        float Lz = Float.parseFloat(lz);
        float Gx = Float.parseFloat(gx);
        float Gy = Float.parseFloat(gy);
        float Gz = Float.parseFloat(gz);

        Log.d("Ax=", "" + Ax);
        Log.d("Ay=", "" + Ay);
        Log.d("Az=", "" + Az);
        Log.d("Lx=", "" + Lx);
        Log.d("Ly=", "" + Ly);
        Log.d("Lz=", "" + Lz);
        Log.d("Gx=", "" + Gx);
        Log.d("Gy=", "" + Gy);
        Log.d("Gz=", "" + Gz);

        if(seconds <= waitTimer) {
            getUR(Gx, Gy, Gz);
            if(seconds == waitTimer)
                ding.start();
            return;
        }
        if(Rule_motionless(Ax, Ay, Az))
        {
            if(Rule_thetaTilt(Gx, Gy, Gz))
                Rule_fall();
            else
                getUR(Gx, Gy, Gz);
        }
        else if(Rule_motion(Lx, Ly, Lz, Gx, Gy, Gz))
            thetaChange(Gx, Gy, Gz);
    }

    private boolean Rule_motionless(float Ax, float Ay, float Az) {
        SMV_A = Math.sqrt(Math.pow(Ax, 2) + Math.pow(Ay, 2) + Math.pow(Az, 2));

        if(min_SMV_A < SMV_A && SMV_A < max_SMV_A)
        {
            motion = false;
            if(motionless == false)
                motionlessTime = seconds;
            motionless = true;
            ml_seconds = seconds - motionlessTime;
            Log.d("ml_seconds=", "" + ml_seconds);
            if(ml_seconds >= 2)
            {
                state = "motionless";
                return true;
            }
        }
        return false;
    }

    private boolean Rule_motion(float Lx, float Ly, float Lz, float Gx, float Gy, float Gz) {

        if(lpf_Lx == -10 && lpf_Ly == -10 && lpf_Lz == -10)
        {
            lpf_Lx = Lx;
            lpf_Ly = Ly;
            lpf_Lz = Lz;
            SMV_Llpf = Math.sqrt(Math.pow(lpf_Lx, 2) + Math.pow(lpf_Ly, 2) + Math.pow(lpf_Lz, 2));
        }
        else
        {
            lpf_Lx = (float) (alpha * lpf_Lx + (1-alpha) * Lx);
            lpf_Ly = (float) (alpha * lpf_Ly + (1-alpha) * Ly);
            lpf_Lz = (float) (alpha * lpf_Lz + (1-alpha) * Lz);
            SMV_Llpf = Math.sqrt(Math.pow(lpf_Lx, 2) + Math.pow(lpf_Ly, 2) + Math.pow(lpf_Lz, 2));
            SMV_L = Math.sqrt(Math.pow(Lx, 2) + Math.pow(Ly, 2) + Math.pow(Lz, 2));

            if(SMV_L > max_SMV_L || SMV_Llpf > max_SMV_Llpf)
            {
                motionless = false;
                if(motion == false)
                    motionTime = spentTime / 1000;
                motion = true;
                m_seconds = seconds - motionTime;

                if(m_seconds >= 1)
                    state = "motion";

                Log.d("m_seconds=", "" + m_seconds);
                return true;
            }
        }
        return false;
    }

    private void getUR(float Gx, float Gy, float Gz) {
        if(state.equals("motionless") && body.equals("upright"))
            UR_Gx = Gx;
            UR_Gy = Gy;
            UR_Gz = Gz;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor == mAccSensor)
        {
            Ax = String.format("%.2f", sensorEvent.values[0]);
            Ay = String.format("%.2f", sensorEvent.values[1]);
            Az = String.format("%.2f", sensorEvent.values[2]);
            intent.putExtra("Ax", Ax);
            intent.putExtra("Ay", Ay);
            intent.putExtra("Az", Az);
        }
        else if(sensorEvent.sensor == mLinearAccSensor)
        {
            Lx = String.format("%.2f", sensorEvent.values[0]);
            Ly = String.format("%.2f", sensorEvent.values[1]);
            Lz = String.format("%.2f", sensorEvent.values[2]);
            intent.putExtra("Lx", Lx);
            intent.putExtra("Ly", Ly);
            intent.putExtra("Lz", Lz);
        }
        else if(sensorEvent.sensor == mGravitySensor)
        {
            Gx = String.format("%.2f", sensorEvent.values[0]);
            Gy = String.format("%.2f", sensorEvent.values[1]);
            Gz = String.format("%.2f", sensorEvent.values[2]);
            intent.putExtra("Gx", Gx);
            intent.putExtra("Gy", Gy);
            intent.putExtra("Gz", Gz);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void thetaChange(float Gx, float Gy, float Gz) {
        double lengthA = Math.sqrt(Math.pow(Gx, 2) + Math.pow(Gy, 2) + Math.pow(Gz, 2));
        double lengthB = Math.sqrt(Math.pow(changeGx, 2) + Math.pow(changeGy, 2) + Math.pow(changeGz, 2));
        double dotAB = Gx*changeGx + Gy*changeGy + Gz*changeGz;
        thetaChange = Math.acos(dotAB / (lengthA*lengthB)) * 180 / 3.14;
        Log.d("thetaChange = ", "" + thetaChange);

        if(thetaChange > thetaC)
            body = "tilt";
        else if(state.equals("motionless"))
            body = "upright";
    }

    private boolean Rule_thetaTilt(float Gx, float Gy, float Gz) {
        double lengthA = Math.sqrt(Math.pow(Gx, 2) + Math.pow(Gy, 2) + Math.pow(Gz, 2));
        double lengthB = Math.sqrt(Math.pow(UR_Gx, 2) + Math.pow(UR_Gy, 2) + Math.pow(UR_Gz, 2));
        double dotAB = Gx*UR_Gx + Gy*UR_Gy + Gz*UR_Gz;
        thetaTilt = Math.acos(dotAB / (lengthA*lengthB)) * 180 / 3.14;
        Log.d("thetaTilt = ", "" + thetaTilt);

        if(thetaTilt > thetaT)
            return true;
        return false;
    }

    private void Rule_fall() {

        if(state.equals("motionless") && body.equals("tilt")) {
            body = "fall";
            alarm.setLooping(true);
            alarm.start();
            showDialog();
        }
        return;
    }

    private void showDialog() {
        Log.i("service","show dialog function");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Warning");
        builder.setMessage("Are you OK?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
                body = "upright";
                alarm.pause();
            }
        });
        builder.setNegativeButton("Help", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
                alarm.pause();
                Toast.makeText(FallDetectionService.this, contactData.getString("contactMessage", ""), Toast.LENGTH_SHORT).show();
                String phoneNumber = contactData.getString("phoneNumber", "");
                String message = contactData.getString("contactMessage", "");
                SmsManager sms = SmsManager.getDefault();
                sms.sendTextMessage(phoneNumber, null, message, null, null);
            }
        });
        AlertDialog alert = builder.create();
        alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alert.show();
    }
}

