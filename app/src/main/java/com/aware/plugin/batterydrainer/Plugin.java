package com.aware.plugin.batterydrainer;

import android.app.AlarmManager;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.ESM;
import com.aware.utils.Aware_Plugin;
import com.aware.providers.ESM_Provider.*;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.database.Cursor;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class Plugin extends Aware_Plugin {

    private final String MYTAG = "BATTERYDRAINER";

    private final String BD_PREFS = "DRAINERPLUGINPREFS";
    private final String USER_UID = "DRAINERUSERUID";

    private ESMStatusListener esm_statuses;

    private AlarmManager alarmManager;

    private final int NEXTBIDRC = 123123; //request code for next bid
    private PendingIntent nextPendingIntent = null;
    private Intent nextBidIntent = null;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(MYTAG, "CREATING THE BATTERY DRAINER PLUGIN, ONCREATE()");
        esm_statuses = new ESMStatusListener();

        nextBidIntent = new Intent(this, AlarmReceiver.class);

        //listen to ESM notifications
        IntentFilter esm_filter = new IntentFilter();
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_DISMISSED);
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_EXPIRED);
        esm_filter.addAction(ESM.ACTION_AWARE_ESM_ANSWERED);
        registerReceiver(esm_statuses, esm_filter);

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        setNextGetBidAlarm();

        //aware sync? get battery level, if below 10 do not bid
        Toast.makeText(getBaseContext(), "Starting Battery Drainer Game!", Toast.LENGTH_LONG).show();



    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setNextGetBidAlarm();
        return START_STICKY;
    }

    private void handleGotBid() {
        sendBroadcast(new Intent(Aware.ACTION_AWARE_SYNC_DATA));
        Calendar cal = Calendar.getInstance();
        int hourNow = cal.get(Calendar.HOUR_OF_DAY); //can be from 0 to 23
        int minuteNow = cal.get(Calendar.MINUTE);
        if(hourNow == 21 || hourNow == 22){
            scheduleForNextMorning();
        } else if(minuteNow >= 50) {
            //not yet the next hour, schedule for next hour*!
            scheduleForNextHour();
        } else if(minuteNow <= 15) {
            scheduleForThisHour();
        }
    }



    private void getBidNow() {
        Intent queue_esm = new Intent(ESM.ACTION_AWARE_QUEUE_ESM);
        String esmJSON = AlarmReceiver.BIDJSON;
        queue_esm.putExtra(ESM.EXTRA_ESM, esmJSON);
        sendBroadcast(queue_esm);
    }



    //this will be called many times just to be sure.... so this is why it's important to make it work at all times
    public void setNextGetBidAlarm() {
        Calendar cal = Calendar.getInstance();
        int hourNow = cal.get(Calendar.HOUR_OF_DAY); //can be from 0 to 23
        int minuteNow = cal.get(Calendar.MINUTE);

        if (hourNow <= 9 && minuteNow < 50) {
            scheduleForThisMorning();
            return;
        } else if ((hourNow == 21 && minuteNow > 50) || hourNow > 21 ) {
            //anything after 21:50 -> tomorrow
            scheduleForNextMorning();
            return;
        } else if (minuteNow < 50) {
           scheduleForThisHour();
            return;
        } else if (minuteNow > 50) {
            scheduleForNextHour();
        }
    }

    private void scheduleForThisMorning() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 50);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        nextPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), NEXTBIDRC, nextBidIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), nextPendingIntent); //use WEEKLY_INTENT_RC, so this gets overwritten in case we call this one twice...
        Log.d(MYTAG, "Set get next bid alarm for :" + cal.getTimeInMillis());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
        Date readable = new Date(cal.getTimeInMillis());
        Toast.makeText(getBaseContext(), sdf.format(readable), Toast.LENGTH_LONG).show();
    }

    private void scheduleForThisHour() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MINUTE, 50);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        nextPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), NEXTBIDRC, nextBidIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), nextPendingIntent); //use WEEKLY_INTENT_RC, so this gets overwritten in case we call this one twice...
        Log.d(MYTAG, "Set get next bid alarm for :" + cal.getTimeInMillis());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
        Date readable = new Date(cal.getTimeInMillis());
        Toast.makeText(getBaseContext(), sdf.format(readable), Toast.LENGTH_LONG).show();
    }

    private void scheduleForNextHour() {
        Calendar cal = Calendar.getInstance();

        cal.add(Calendar.HOUR_OF_DAY, 1);

        cal.set(Calendar.MINUTE, 50);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        nextPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), NEXTBIDRC, nextBidIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), nextPendingIntent); //use WEEKLY_INTENT_RC, so this gets overwritten in case we call this one twice...
        Log.d(MYTAG, "Set get next bid alarm for :" + cal.getTimeInMillis());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
        Date readable = new Date(cal.getTimeInMillis());
        Toast.makeText(getBaseContext(), sdf.format(readable), Toast.LENGTH_LONG).show();
    }

    /**
     * Set the alarm for next morning, 09:50. This is called _every time_ when battery is under 10%,
     * as the user cannot possibly bid during that day anymore... So next morning it is!
     */
    private void scheduleForNextMorning() {
        // Good night, schedule the next one for next morning 9:50 here!
        Calendar cal = Calendar.getInstance();

        cal.add(Calendar.DAY_OF_MONTH, 1);

        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 50);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        nextPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), NEXTBIDRC, nextBidIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), nextPendingIntent); //use WEEKLY_INTENT_RC, so this gets overwritten in case we call this one twice...
        Log.d(MYTAG, "Set get next bid alarm for :" + cal.getTimeInMillis());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss");
        Date readable = new Date(cal.getTimeInMillis());
        Toast.makeText(getBaseContext(), sdf.format(readable), Toast.LENGTH_LONG).show();
    }





    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(MYTAG, "Battery Drainer Plugin Deactivated");
        unregisterReceiver(esm_statuses);

        if (nextPendingIntent != null) {
            alarmManager.cancel(nextPendingIntent);
        }
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
                scheduleForNextMorning();
                return;
            }

            if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_EXPIRED)) {
                // expired, cya next time.
                setNextGetBidAlarm();
                return;
            } else if (intent.getAction().equals(ESM.ACTION_AWARE_ESM_DISMISSED)) {
                // canceled, cya next time.
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
                handleGotBid();
            }
        }
    }



}

        /*if (getUserId() == null){
            String awareUID = Aware.getSetting(this, Aware_Preferences.DEVICE_ID);
            storeUserId(awareUID);
        }*/


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