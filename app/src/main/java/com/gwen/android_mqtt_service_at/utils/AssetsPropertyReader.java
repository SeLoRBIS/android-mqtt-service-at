package com.gwen.android_mqtt_service_at.utils;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AssetsPropertyReader {

    private static String TAG = AssetsPropertyReader.class.getName();
    private Context context;
    private Properties properties;
    private static AssetsPropertyReader instance = null;

    /** Private constructor */
    private AssetsPropertyReader(Context context){
        this.context=context;
        this.properties = new Properties();
        AssetManager assetManager = context.getAssets();
        try {
            InputStream inputStream = assetManager.open("config.properties");
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Creates the instance is synchronized to avoid multithreads problems */
    private synchronized static void createInstance(Context ctx) {
        if (instance == null) {
            instance = new AssetsPropertyReader(ctx);
        }
    }

    /** Get the properties instance. Uses singleton pattern */
    public static AssetsPropertyReader getInstance(Context ctx){
        // Uses singleton pattern to guarantee the creation of only one instance
        if(instance == null) {
            createInstance(ctx);
        }
        return instance;
    }

    /** Get a property of the property file */
    public String getProperty(String key){
        String result = null;
        if(key !=null && !key.trim().isEmpty()){
            result = this.properties.getProperty(key);
        }
        return result;
    }
}