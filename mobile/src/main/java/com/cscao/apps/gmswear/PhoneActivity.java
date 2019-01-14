package com.cscao.apps.gmswear;

import static com.cscao.apps.shared.Constants.CAPABILITY;
import static com.cscao.apps.shared.Constants.PATH_MSG_ONE;
import static com.cscao.apps.shared.Constants.PATH_MSG_TWO;
import static com.cscao.apps.shared.Constants.PERMISSIONS_REQUEST_CODE;
import static com.cscao.apps.shared.Constants.SELECT_PICTURE;
import static com.cscao.apps.shared.Constants.SYNC_KEY;
import static com.cscao.apps.shared.Constants.SYNC_PATH;
import static com.cscao.apps.shared.Utils.getElapsedTimeMsg;
import static com.cscao.apps.shared.Utils.sendSelectedImage;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.cscao.apps.shared.Utils;
import com.cscao.libs.gmswear.GmsWear;
import com.cscao.libs.gmswear.connectivity.FileTransfer;
import com.cscao.libs.gmswear.consumer.AbstractDataConsumer;
import com.cscao.libs.gmswear.consumer.DataConsumer;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.orhanobut.logger.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PhoneActivity extends Activity {

    // already specified in res/values/wear.xml
//    public static final String MSG_CAPABILITY = "msg_capability";

    private TextView mMsgTextView;

    private Button mMsgOneButton;
    private Button mMsgTwoButton;
    private Button mSyncButton;
    private Button mSelectButton;
    private Button mSaveButton;
    private Button mFitScr;
    private EditText mFileName;
    private GmsWear mGmsWear;
    private DataConsumer mDataConsumer;
    public String[] dataAcc;
    public List<String> dataSave = new ArrayList<>();

    private LineChart mChart1;
    private LineChart mChart2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phone_activity);

        mGmsWear = GmsWear.getInstance();

        //Khusus Chart
        mChart1 = (LineChart) findViewById(R.id.linechart1);
        mChart2 = (LineChart) findViewById(R.id.linechart2);

        SettingChart1();
        SettingChart2();
        //================================

        mMsgTextView = (TextView) findViewById(R.id.tv_msg);

        mMsgOneButton = (Button) findViewById(R.id.btn_send_msg1);
        mMsgTwoButton = (Button) findViewById(R.id.btn_send_msg2);
        mSyncButton = (Button) findViewById(R.id.btn_sync);
        mSelectButton = (Button) findViewById(R.id.btn_select_image);
        mFitScr = (Button) findViewById(R.id.btn_fitScr);
        mSaveButton = (Button) findViewById(R.id.btn_save);
        mFileName = (EditText) findViewById(R.id.fname);

        mMsgOneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGmsWear.sendMessage(PATH_MSG_TWO, "Start".getBytes());
                Toast.makeText(PhoneActivity.this, "Start", Toast.LENGTH_SHORT).show();
            }
        });

        mMsgTwoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGmsWear.sendMessage(PATH_MSG_TWO, "Stop".getBytes());
                Toast.makeText(PhoneActivity.this, "Stop", Toast.LENGTH_SHORT).show();
            }
        });

        mSelectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChart1.clear();
                mChart2.clear();
                SettingChart1();
                SettingChart2();
                dataSave.clear();
            }
        });

        mFitScr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChart1.fitScreen();
                mChart2.fitScreen();
            }
        });

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
                String fileName = mFileName.getText().toString();
                String filePath = baseDir + File.separator + fileName + ".csv";

                try {
                    File file = new File(filePath);
                    // if file doesn't exists, then create it
                    if (!file.exists()) {
                        file.createNewFile();
                    }

                    FileWriter fw = new FileWriter(file.getAbsoluteFile());
                    BufferedWriter bw = new BufferedWriter(fw);


                    for(String d : dataSave){
                        bw.write(d);
                        bw.newLine();
                    }
                    bw.flush();
                    bw.close();

                    Toast.makeText(PhoneActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(PhoneActivity.this, "Save Failed", Toast.LENGTH_SHORT).show();
                }

            }
        });

        mDataConsumer = new AbstractDataConsumer() {
            @Override
            public void onMessageReceived(MessageEvent messageEvent) {
                String msg = new String(messageEvent.getData());
                if (messageEvent.getPath().equals(PATH_MSG_ONE)) {
                    setTextAsync(mMsgTextView, msg);
                } else if (messageEvent.getPath().equals(PATH_MSG_TWO)) {
                    setTextAsync(mMsgTextView, msg);
                }

                dataAcc = msg.split(";");
                Float AX = Float.valueOf(dataAcc[1]);
                Float AY = Float.valueOf(dataAcc[3]);
                Float AZ = Float.valueOf(dataAcc[5]);
                Float GX = Float.valueOf(dataAcc[7]);
                Float GY = Float.valueOf(dataAcc[9]);
                Float GZ = Float.valueOf(dataAcc[11]);

                //addEntry(AX, AY, AZ, GX, GY, GZ);
                addEntry1(AX, AY, AZ);
                addEntry2(GX, GY, GZ);
                dataSave.add(msg);
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
        mSelectButton.setEnabled(true);
        mFitScr.setEnabled(true);
    }

    private void disableButtons() {
        mMsgOneButton.setEnabled(false);
        mMsgTwoButton.setEnabled(false);
        mSyncButton.setEnabled(false);
        mSelectButton.setEnabled(false);
        mFitScr.setEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.d("onResume");
        mGmsWear.addCapabilities(CAPABILITY);
        mGmsWear.addWearConsumer(mDataConsumer);
    }

    @Override
    protected void onPause() {
        Logger.d("onPause");
        mGmsWear.removeCapabilities(CAPABILITY);
        mGmsWear.removeWearConsumer(mDataConsumer);
        super.onPause();
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

    private void addEntry1(Float X, Float Y, Float Z){
        LineData data = mChart1.getLineData();
        LineData data2 = mChart1.getLineData();
        LineData data3 = mChart1.getLineData();


        if(data!=null){
            LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);
            LineDataSet set2 = (LineDataSet) data2.getDataSetByIndex(0);
            LineDataSet set3 = (LineDataSet) data3.getDataSetByIndex(0);


            if(set==null) {
                set = createSet(Color.RED, "ACCX");
                data.addDataSet(set);
            }

            if(set2==null){
                set2 = createSet(Color.BLUE, "ACCY");
                data2.addDataSet(set2);
            }

            if(set3==null){
                set3 = createSet(Color.GREEN, "ACCZ");
                data3.addDataSet(set3);
            }



            data.addXValue("");
            data.addEntry(new Entry(X,set.getEntryCount()),0);
            data2.addEntry(new Entry(Y,set2.getEntryCount()),1);
            data3.addEntry(new Entry(Z,set3.getEntryCount()),2);

            mChart1.notifyDataSetChanged();
            mChart1.setVisibleXRangeMaximum(50);
            mChart1.moveViewToX(data.getXValCount()-51);

        }
    }
    private void addEntry2(Float Gx, Float Gy, Float Gz){
        LineData datag = mChart2.getLineData();
        LineData datag2 = mChart2.getLineData();
        LineData datag3 = mChart2.getLineData();

        if(datag!=null){

            LineDataSet set = (LineDataSet) datag.getDataSetByIndex(0);
            LineDataSet set2 = (LineDataSet) datag2.getDataSetByIndex(0);
            LineDataSet set3 = (LineDataSet) datag3.getDataSetByIndex(0);

            if(set==null){
                set = createSet(Color.MAGENTA, "GYROX");
                datag.addDataSet(set);
            }

            if(set2==null){
                set2 = createSet(Color.CYAN, "GYROY");
                datag2.addDataSet(set2);
            }

            if(set3==null){
                set3 = createSet(Color.YELLOW, "GYROZ");
                datag3.addDataSet(set3);
            }

            datag.addXValue("");

            datag.addEntry(new Entry(Gx,set.getEntryCount()),0);
            datag2.addEntry(new Entry(Gy,set2.getEntryCount()),1);
            datag3.addEntry(new Entry(Gz,set3.getEntryCount()),2);

            mChart2.notifyDataSetChanged();
            mChart2.setVisibleXRangeMaximum(5);
            mChart2.moveViewToX(datag.getXValCount()-6);

        }
    }

    private LineDataSet createSet(int c, String nama ){
        LineDataSet set = new LineDataSet(null,nama);
        set.setDrawCubic(true);
        set.setCubicIntensity(0.2f);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(c);
        set.setCircleColor(c);
        set.setLineWidth(2f);
        set.setValueTextColor(Color.WHITE);

        return set;
    }

    private void SettingChart1(){
        mChart1.isScaleXEnabled();
        mChart1.setBackgroundColor(Color.WHITE);
        LineData data = new LineData();
        LineData data2 = new LineData();
        LineData data3 = new LineData();

        mChart1.setData(data);
        mChart1.setData(data2);
        mChart1.setData(data3);


        XAxis xl = mChart1.getXAxis();
        xl.setTextColor(Color.LTGRAY);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);

        YAxis y1 = mChart1.getAxisLeft();
        y1.setTextColor(Color.LTGRAY);
        y1.setDrawGridLines(true);
        y1.setAxisMaxValue(15f);
        y1.setAxisMinValue(-15f);

        YAxis y2 = mChart1.getAxisRight();
        y2.setEnabled(false);
    }
    private void SettingChart2(){
        mChart2.isScaleXEnabled();
        mChart2.setBackgroundColor(Color.GREEN);

        LineData datag = new LineData();
        LineData datag2 = new LineData();
        LineData datag3 = new LineData();

        mChart2.setData(datag);
        mChart2.setData(datag2);
        mChart2.setData(datag3);

        XAxis xl = mChart2.getXAxis();
        xl.setTextColor(Color.LTGRAY);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);

        YAxis y1 = mChart2.getAxisLeft();
        y1.setTextColor(Color.LTGRAY);
        y1.setDrawGridLines(true);
        y1.setAxisMaxValue(15f);
        y1.setAxisMinValue(-15f);

        YAxis y2 = mChart2.getAxisRight();
        y2.setEnabled(false);
    }

}
