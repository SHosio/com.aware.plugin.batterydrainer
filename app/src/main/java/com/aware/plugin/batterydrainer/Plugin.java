package com.aware.plugin.batterydrainer;

import android.app.AlarmManager;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.utils.Aware_Plugin;
import com.aware.providers.ESM_Provider.*;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.database.Cursor;


import java.util.Calendar;


public class Plugin extends Aware_Plugin {

    private final String MYTAG = "BATTERYDRAINER";

    private final String BD_PREFS = "DRAINERPLUGINPREFS";
    private final String USER_UID = "DRAINERUSERUID";

    private ESMStatusListener esm_statuses;

    private AlarmManager alarmManager;

    private final int NEXTBIDRC = 123123; //request code for next bid
    private PendingIntent nextBidIntent = null;


    @Override
    public void onCreate() {
        super.onCreate();
        esm_statuses = new ESMStatusListener();
        Log.d(MYTAG, "CREATING THE BATTERY DRAINER PLUGIN, ONCREATE()");

        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.DEBUG_FLAG, true);

        //listen to ESM notifications
        IntentFilter esm_filter = new IntentFilter();
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_DISMISSED);
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_EXPIRED);
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_ANSWERED);
        registerReceiver(esm_statuses, esm_filter);

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        /*if (getUserId() == null){
            String awareUID = Aware.getSetting(this, Aware_Preferences.DEVICE_ID);
            storeUserId(awareUID);
        }*/


        getDebugBidNow();
        setNextGetBidAlarm();

        //aware sync? get battery level, if below 10 do not bid
        Toast.makeText(getBaseContext(), "Starting Battery Drainer Game!", Toast.LENGTH_LONG).show();


    }

    private void flushData() {
        sendBroadcast(new Intent(Aware.ACTION_AWARE_SYNC_DATA));
    }

    private void getDebugBidNow() {
        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        alarmIntent.putExtra("extra", "getbid");

        Calendar cal = Calendar.getInstance();

        int rc = (int) System.currentTimeMillis();
        nextBidIntent = PendingIntent.getBroadcast(getApplicationContext(), rc, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis() + 5000, nextBidIntent); //use WEEKLY_INTENT_RC, so this gets overwritten in case we call this one twice...
        Log.d(MYTAG, "Set get next bid alarm for :" + cal.getTimeInMillis());
    }

    /*private void storeUserId(String id) {
        SharedPreferences settings = getSharedPreferences("DRAINERPLUGINPREFS", MODE_WORLD_READABLE);
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putString(USER_UID, id);
        prefEditor.commit();
        Log.d(MYTAG, "Stored UID: " + id);

    }

    private String getUserId() {
        String uid = null;
        SharedPreferences settings = getSharedPreferences("DRAINERPLUGINPREFS", MODE_PRIVATE);
        uid = settings.getString(USER_UID, null);
        Log.d(MYTAG, "Returning UID from sharedrefs:" + uid);
        return uid;
    }*/

    public void setNextGetBidAlarm() {


        Intent alarmIntent = new Intent(this, AlarmReceiver.class);

        Calendar cal = Calendar.getInstance();
        int hourToTrigger = cal.get(Calendar.HOUR_OF_DAY); //can be from 0 to 23
        int minuteNow = cal.get(Calendar.MINUTE);


        if (hourToTrigger <= 9 && minuteNow < 45) {
            // Someone signing up for the study before 9:45am, e.g. 0930, should not happen in any other condition. Schedule for 0950 today
            cal.set(Calendar.HOUR_OF_DAY, 9);
            cal.set(Calendar.MINUTE, 50);
            cal.set(Calendar.SECOND, 00);
            nextBidIntent = PendingIntent.getBroadcast(getApplicationContext(), NEXTBIDRC, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), nextBidIntent); //use WEEKLY_INTENT_RC, so this gets overwritten in case we call this one twice...
            Log.d(MYTAG, "Set get next bid alarm for :" + cal.getTimeInMillis());

        } else if (hourToTrigger > 21) {
            setNextMorningBidAlarm();
        } else {
            // Let's get a bid one hour from now... e.g. if it's 14:50, we'll pop up at 15:50!
            // Note, also 21:50 it'll schedule it 22:50..but will do nothing as the step above will catch it in one hour from now
            hourToTrigger++;
            cal.set(Calendar.HOUR_OF_DAY, hourToTrigger);
            cal.set(Calendar.MINUTE, 50);
            cal.set(Calendar.SECOND, 00);
            nextBidIntent = PendingIntent.getBroadcast(getApplicationContext(), NEXTBIDRC, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), nextBidIntent); //use WEEKLY_INTENT_RC, so this gets overwritten in case we call this one twice...
            Log.d(MYTAG, "Set get next bid alarm for :" + cal.getTimeInMillis());
        }
    }

    public void getBidNow (){
        Intent queue_esm = new Intent(ESM.ACTION_AWARE_QUEUE_ESM);
        String esmJSON = AlarmReceiver.BIDJSON;
        queue_esm.putExtra(ESM.EXTRA_ESM, esmJSON);
        sendBroadcast(queue_esm);
    }

    /**
     * Set the alarm for next morning, 09:50. This is called _every time_ when battery is under 10%,
     * as the user cannot possibly bid during that day anymore... So next morning it is!
     */
    private void setNextMorningBidAlarm() {
        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        // Good night, schedule the next one for next morning 9:50 here!
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 50);
        cal.set(Calendar.SECOND, 00);
        nextBidIntent = PendingIntent.getBroadcast(getApplicationContext(), NEXTBIDRC, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), nextBidIntent); //use WEEKLY_INTENT_RC, so this gets overwritten in case we call this one twice...
        Log.d(MYTAG, "Set get next bid alarm for :" + cal.getTimeInMillis());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(MYTAG, "Battery Drainer Plugin Deactivated");
        unregisterReceiver(esm_statuses);

        if (nextBidIntent != null) {
            alarmManager.cancel(nextBidIntent);
        }

        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, false);
        sendBroadcast(new Intent(Aware.ACTION_AWARE_REFRESH));
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
                Log.d(MYTAG, "ESM ANSWERED WITH BID:" + ans + " and triggered by: " + trigger);
            }
            if (esm_data != null) {
                esm_data.close();
            }
            if (trigger != null && !trigger.contains("com.aware.plugin.batterydrainer")) {
                Log.d(MYTAG, "Somebody else initiated the ESM, no need to react, returning.");
                return;
            }

            if (trigger != null && trigger.contains("nobattery")) {
                //no battery, just schedule next and return
                Log.d(MYTAG, "NOT ENOUGH BATTERY, just scheduling next morning next time!");
                setNextMorningBidAlarm();
                return;
            }

            if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_EXPIRED)) {
                // Next time.
                setNextGetBidAlarm();
                return;
            } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_DISMISSED)) {
                // gone, do nothing until next time!
                setNextGetBidAlarm();
                return;
            } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_ANSWERED)) {
                Toast.makeText(getBaseContext(), "Got a bid: " + ans, Toast.LENGTH_LONG).show();


                try {
                    Double.parseDouble(ans);
                } catch (NumberFormatException e) {
                    getBidNow();
                    return;
                }

                Log.d(MYTAG, "Got a bid: " + ans);
                flushData(); //send it to a server! Boom!
                setNextGetBidAlarm(); // cya next time.

            }
        }
    }

}
