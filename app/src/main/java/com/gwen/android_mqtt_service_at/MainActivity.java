package com.gwen.android_mqtt_service_at;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private MqttService mMqttService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            mMqttService = new MqttService(this, getResources().getString(R.string.app_name), BuildConfig.MQTT_TOPIC);
        } catch (IOException e) {
            Log.e(TAG, "error creating mqttpublisher", e);
        }
        mMqttService.start();
    }
}
