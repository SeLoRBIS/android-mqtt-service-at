package com.gwen.android_mqtt_service_at;

/**
 * Created by gwendal.charles on 18/12/2016.
 */

import android.app.Service;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.gwen.android_mqtt_service_at.constants.Constants;
import com.gwen.android_mqtt_service_at.utils.AssetsPropertyReader;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MqttService{
    private static final String TAG = MqttService.class.getSimpleName();

    private final Context mContext;
    private final String mAppname;
    private final String mTopic;

    private Handler mHandler;
    private HandlerThread mHandlerThread;

    private static final long PUBLISH_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1);

    private static String MQTT_SERVER_URI = "";
    private MqttAsyncClient mMqttAndroidClient;

    MqttService(Context context, String appname, String topic) throws IOException {
        mContext = context;
        mAppname = appname;
        mTopic = topic;

        mHandlerThread = new HandlerThread("mqttPublisherThread");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());

        MQTT_SERVER_URI = AssetsPropertyReader.getInstance(mContext).getProperty("mqtt.wss_uri");
        // mMqttAndroidClient = new MqttAndroidClient(mContext, MQTT_SERVER_URI, Constants.MQTT_CLIENT_ID_PREFIX + MqttClient.generateClientId());
        try {
            mMqttAndroidClient = new MqttAsyncClient(MQTT_SERVER_URI, Constants.MQTT_CLIENT_ID_PREFIX + MqttClient.generateClientId(), new MemoryPersistence());
        } catch (MqttException e) {
            e.printStackTrace();
        }
        mMqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.d(TAG, "MQTT connection complete");
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d(TAG, "MQTT connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d(TAG, "MQTT message arrived");
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(TAG, "MQTT delivery complete");
            }
        });

        final MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setUserName(AssetsPropertyReader.getInstance(mContext).getProperty("mqtt.username"));
        mqttConnectOptions.setPassword(AssetsPropertyReader.getInstance(mContext).getProperty("mqtt.pwd").toCharArray());
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(true);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // Connect to the broker
                IMqttToken token;
                try {
                    token = mMqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                            disconnectedBufferOptions.setBufferEnabled(true);
                            disconnectedBufferOptions.setBufferSize(100);
                            disconnectedBufferOptions.setPersistBuffer(false);
                            disconnectedBufferOptions.setDeleteOldestMessages(false);
                            mMqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            Log.d(TAG, "MQTT connection failure", exception);
                        }
                    });
                    token.waitForCompletion(Constants.MQTT_TIMEOUT);

                } catch (MqttException e) {
                    Log.d(TAG, "MQTT connection failure", e);
                }

            }
        });
    }

    public void start() {
        mHandler.post(mSuscribeRunnable);
    }

    public void stop() {
        mHandler.removeCallbacks(mSuscribeRunnable);
    }

    public void close() {
        mHandler.removeCallbacks(mSuscribeRunnable);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mMqttAndroidClient.disconnect();
                } catch (MqttException e) {
                    Log.d(TAG, "error disconnecting MQTT client");
                } finally {
                    mMqttAndroidClient = null;
                }
            }
        });
        mHandlerThread.quitSafely();
    }

    private Runnable mSuscribeRunnable = new Runnable() {
        @Override
        public void run() {

            if(isActiveNetwork()){
                try {
                    Log.d(TAG, "Topic Subscription");
                    mMqttAndroidClient.subscribe(mTopic, 0);
                } catch (MqttException e) {
                    Log.e(TAG, "Error publishing message", e);
                } finally {
                    mHandler.postDelayed(mSuscribeRunnable, PUBLISH_INTERVAL_MS);
                }
            }
        }

    };

    private Runnable mPublishRunnable = new Runnable() {
        @Override
        public void run() {
            if(isActiveNetwork()) {
                try {
                    String messagePayload = "";
                    if (!messagePayload.equals("")) {
                        Log.d(TAG, "no message to publish");
                        return;
                    }
                    Log.d(TAG, "publishing message: " + messagePayload);
                    MqttMessage m = new MqttMessage();
                    m.setPayload(messagePayload.getBytes());
                    m.setQos(1);
                    mMqttAndroidClient.publish(mTopic, m);
                } catch (MqttException e) {
                    Log.e(TAG, "Error publishing message", e);
                } finally {
                    mHandler.postDelayed(mPublishRunnable, PUBLISH_INTERVAL_MS);
                }
            }
        }

    };


    private boolean isActiveNetwork() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnectedOrConnecting()) {
            Log.e(TAG, "no active network");
            return false;
        }
        return true;
    }
}