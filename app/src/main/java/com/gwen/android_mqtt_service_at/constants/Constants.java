package com.gwen.android_mqtt_service_at.constants;

import android.content.Context;

import com.gwen.android_mqtt_service_at.utils.AssetsPropertyReader;

public class Constants {

    private static Context context;

    /** MQTT */

    /** Constants Client Id */
    public static final String MQTT_CLIENT_ID_PREFIX = "ntdc-";
    /** Constants Timeout */
    public static final int MQTT_TIMEOUT = 50000;
    /** Constants QOS */
    public static final int MQTT_QOS = 1;
    /** Constants Keep Aive Message */
    public static final byte[] MQTT_KEEP_ALIVE_MESSAGE = { 0 };


}
