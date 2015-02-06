package com.aware.plugin.batterydrainer;

import android.app.AlarmManager;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.utils.Aware_Plugin;
import com.aware.providers.ESM_Provider.*;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import android.util.Log;
import android.widget.Toast;
import android.database.Cursor;

import java.util.Date;
import java.text.SimpleDateFormat;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.utils.Aware_Plugin;


import java.util.Calendar;



public class Plugin extends Aware_Plugin {

    private final String MYTAG = "BATTERYDRAINER";

    private final String BD_PREFS = "DRAINERPLUGINPREFS";
    private final String USER_UID = "DRAINERUSERUID";
    private ESMStatusListener esm_statuses;

    private AlarmManager alarmManager;


    @Override
    public void onCreate() {
        super.onCreate();
        esm_statuses = new ESMStatusListener();
        Log.d(MYTAG, "CREATING THE BACK PAIN PLUGIN, ONCREATE()");
        Toast.makeText(getBaseContext(), "Selkäkipututkimus aloitettu.", Toast.LENGTH_LONG).show();
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.DEBUG_FLAG, true);

        //listen to ESM notifications
        IntentFilter esm_filter = new IntentFilter();
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_QUEUE_COMPLETE);
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_DISMISSED);
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_EXPIRED);
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_ANSWERED);
        registerReceiver(esm_statuses, esm_filter);

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        if (getUserId() == null) {
        } else {
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if( DEBUG ) Log.d(MYTAG, "Template plugin terminated");
    }

    /**
     * Gets the local user id stored in the prefs, and acquired with the question number 0.
     * This is a mandatory thing to have, so if null is returned,something is wrong and
     * we need to trigger q0
     */
    private String getUserId() {
        String uid = null;
        SharedPreferences settings = getSharedPreferences(BD_PREFS, MODE_PRIVATE);
        uid = settings.getString(USER_UID, null);
        return uid;
    }

    private void setUserId(String newUID) {
        SharedPreferences settings = getSharedPreferences(BD_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putString(USER_UID, newUID);
        prefEditor.apply();
    }


    public class ESMStatusListener extends BroadcastReceiver {
        private final String MYTAG = "BACKPAINV2";

        public void onReceive(Context context, Intent intent) {

            String trigger = null;
            String ans = null;

            Cursor esm_data = context.getContentResolver().query(ESM_Data.CONTENT_URI, null, null, null, null);

            if (esm_data != null && esm_data.moveToLast()) {
                ans = esm_data.getString(esm_data.getColumnIndex(ESM_Data.ANSWER));
                trigger = esm_data.getString(esm_data.getColumnIndex(ESM_Data.TRIGGER));
                Log.d(MYTAG, "ESM ANSWERED WITH:" + ans + " and triggered by: " + trigger);
            }
            if (esm_data != null) {
                esm_data.close();
            }
            if (trigger.equals("com.aware.plugin.batterydrainer") == false){
                Log.d(MYTAG, "Somebody else initiated the ESM, no need to react, returning.");
                return;
            }

            if (ans == null) {
                Log.d(MYTAG, "ANS was null, returning.");
                return;
            }




            if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_EXPIRED)) {
            } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_DISMISSED)) {
            } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_ANSWERED)) {

            }
        }
    }

}
