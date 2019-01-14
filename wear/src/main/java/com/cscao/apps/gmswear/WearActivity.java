package com.cscao.apps.gmswear;

import static com.cscao.apps.shared.Constants.CAPABILITY;
import static com.cscao.apps.shared.Constants.PATH_MSG_ONE;
import static com.cscao.apps.shared.Constants.PATH_MSG_TWO;
import static com.cscao.apps.shared.Constants.PERMISSIONS_REQUEST_CODE;
import static com.cscao.apps.shared.Constants.SYNC_KEY;
import static com.cscao.apps.shared.Constants.SYNC_PATH;
import static com.cscao.apps.shared.Utils.getElapsedTimeMsg;
import static com.cscao.apps.shared.Utils.setImageFromInputStream;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.cscao.apps.shared.Utils;
import com.cscao.libs.gmswear.GmsWear;
import com.cscao.libs.gmswear.connectivity.FileTransfer;
import com.cscao.libs.gmswear.consumer.AbstractDataConsumer;
import com.cscao.libs.gmswear.consumer.DataConsumer;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableStatusCodes;
import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.InputStream;

public class WearActivity extends Activity {
    // already specified in res/values/wear.xml
    //    public static final String MSG_CAPABILITY = "msg_capability";

    private TextView tData;
    private SensorManager SM;
    private  Sensor mAccSensor;
    private  Sensor mGyroSensor;
    private SensorEventListener mAccSensorListener;
    private SensorEventListener mGyroSensorListener;

    public Boolean status;
    public float Ax,Ay,Az,Gx,Gy,Gz;

    private TextView mMsgTextView;
    private Button mMsgOneButton;
    private Button mMsgTwoButton;
    private Button mSyncButton;

    private GmsWear mGmsWear;
    private DataConsumer mDataConsumer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wear_activity);

        mGmsWear = GmsWear.getInstance();

        mMsgTextView = (TextView) findViewById(R.id.tv_msg);
        mMsgTextView.setKeepScreenOn(true);

        mMsgOneButton = (Button) findViewById(R.id.btn_msg_one);
        mMsgTwoButton = (Button) findViewById(R.id.btn_msg_two);
        mSyncButton = (Button) findViewById(R.id.btn_sync);

        tData = (TextView) findViewById(R.id.tData);
        status = Boolean.FALSE;


        mMsgOneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(WearActivity.this, "Start", Toast.LENGTH_SHORT).show();
                status = Boolean.TRUE;
            }
        });

        mMsgTwoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(WearActivity.this, "Stop", Toast.LENGTH_SHORT).show();
                status = Boolean.FALSE;
            }
        });

        //Membuat Sensor Manager
        SM = (SensorManager)getSystemService(SENSOR_SERVICE);

        //Accelerometer Sensor
        mAccSensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroSensor = SM.getDefaultSensor(Sensor.TYPE_GYROSCOPE);


        //Untuk Message API
        mDataConsumer = new AbstractDataConsumer() {
            @Override
            public void onMessageReceived(MessageEvent messageEvent) {
                String msg = new String(messageEvent.getData());
                if (messageEvent.getPath().equals(PATH_MSG_ONE)) {
                    setTextAsync(mMsgTextView, msg);
                } else if (messageEvent.getPath().equals(PATH_MSG_TWO)) {
                    setTextAsync(mMsgTextView, msg);
                }

                if(msg.equals("Start")){
                    Toast.makeText(WearActivity.this, "Start", Toast.LENGTH_SHORT).show();
                    status = Boolean.TRUE;
                } else if (msg.equals("Stop")){
                    Toast.makeText(WearActivity.this, "Stop", Toast.LENGTH_SHORT).show();
                    status = Boolean.FALSE;
                } else status = Boolean.FALSE;
            }

            @Override
            public void onDataChanged(DataEvent event) {
                DataItem item = event.getDataItem();
                final DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    String syncString = dataMap.getString(SYNC_KEY);
                    setTextAsync(mMsgTextView, syncString);
                } else if (event.getType() == DataEvent.TYPE_DELETED) {
                    Logger.d("DataItem Deleted" + event.getDataItem().toString());
                }

            }

        };

        checkPermissions();

        //Register Sensor Listener
        mAccSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                //Aktivitas untuk Sensor Accelero ==================================================
                Ax = event.values[0];
                Ay = event.values[1];
                Az = event.values[2];
                if (status == Boolean.TRUE) {
                    Long tsLong = System.currentTimeMillis();
                    String ts = tsLong.toString();
                    tData.setText("Ax; " + Ax + "  ;Ay; " + Ay + "  ;Az; " + Az
                    + " ;Gx; " + Gx + "  ;Gy; " + Gy + "  ;Gz; " + Gz + " ;T; " + ts);
                    mGmsWear.sendMessage(PATH_MSG_ONE, tData.getText().toString().getBytes());
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
        mGyroSensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                //Aktivitas untuk Sensor Gyro ======================================================
                Gx = event.values[0];
                Gy = event.values[1];
                Gz = event.values[2];
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };
    }

    private void setTextAsync(final TextView view, final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.setText(text);
            }
        });
    }

    public void syncString(View view) {
        mGmsWear.syncString(SYNC_PATH, SYNC_KEY,
                getString(R.string.synced_msg) + getElapsedTimeMsg(), false);
    }

    private void checkPermissions() {
        boolean writeExternalStoragePermissionGranted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED;

        if (writeExternalStoragePermissionGranted) {
            enableButtons();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_CODE);
        }

    }

    private void enableButtons() {
        mMsgOneButton.setEnabled(true);
        mMsgTwoButton.setEnabled(true);
        mSyncButton.setEnabled(true);
    }

    private void disableButtons() {
        mMsgOneButton.setEnabled(false);
        mMsgTwoButton.setEnabled(false);
        mSyncButton.setEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGmsWear.addCapabilities(CAPABILITY);
        mGmsWear.addWearConsumer(mDataConsumer);
        SM.registerListener(mAccSensorListener, mAccSensor, SensorManager.SENSOR_DELAY_NORMAL);
        SM.registerListener(mGyroSensorListener, mGyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        mGmsWear.removeCapabilities(CAPABILITY);
        mGmsWear.removeWearConsumer(mDataConsumer);
        super.onPause();
        SM.unregisterListener(mAccSensorListener);
        SM.unregisterListener(mGyroSensorListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableButtons();
            } else {
                // Permission has been denied before. At this point we should show a dialog to
                // user and explain why this permission is needed and direct him to go to the
                // Permissions settings for the app in the System settings. For this sample, we
                // simply exit to get to the important part.
                disableButtons();
                Toast.makeText(this, R.string.exiting_for_permission, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }


}
