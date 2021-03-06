package com.aware.plugin.batterydrainer;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;
import android.content.BroadcastReceiver;

import com.aware.ESM;
import com.aware.providers.Battery_Provider;


public class AlarmReceiver extends BroadcastReceiver {

    public static final String BIDJSON = "[{'esm':{" +
            "'esm_type':" + ESM.TYPE_ESM_TEXT + "," +
            "'esm_title': 'Bid now!'," +
            "'esm_instructions': 'How much money should we pay you for 10% units of your battery? Use X.XX format to enter the amount in EUR.'," +
            "'esm_submit': 'Bid!'," +
            "'esm_expiration_threashold': 600," + //the user has 20 minutes to respond. Set to 0 to disable
            "'esm_trigger': 'com.aware.plugin.batterydrainer.getbid'" +
            "}}]";

    public static final String CANNOTBIDJSON = "[{'esm': {" +
            "'esm_type': 5," +
            "'esm_title': 'Not enough battery left'," +
            "'esm_instructions': 'Cannot bid if you don't have at least 10% battery left...'," +
            "'esm_quick_answers': ['OK, got it!']," +
            "'esm_expiration_threashold': 600," +
            "'esm_trigger': 'com.aware.plugin.batterydrainer.nobattery'" +
            "}}]";

    private final String MYTAG = "BATTERYDRAINER";

    public AlarmReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (getBatteryLevel(context) < 10) {
            Intent queue_esm = new Intent(ESM.ACTION_AWARE_QUEUE_ESM);
            String esmJSON = CANNOTBIDJSON;
            queue_esm.putExtra(ESM.EXTRA_ESM, esmJSON);
            context.sendBroadcast(queue_esm);
        } else {
            Log.d(MYTAG, "ALARM RECEIVED! SENDING!");
            Intent queue_esm = new Intent(ESM.ACTION_AWARE_QUEUE_ESM);
            String esmJSON = BIDJSON;
            queue_esm.putExtra(ESM.EXTRA_ESM, esmJSON);
            context.sendBroadcast(queue_esm);
            playNotification(context);
        }

    }

    private int getBatteryLevel(Context c) {
        Cursor batteryData = c.getContentResolver().query(Battery_Provider.Battery_Data.CONTENT_URI, null, null, null, null);
        String batteryLevel = "-1";
        if (batteryData != null && batteryData.moveToLast()) {
            batteryLevel = batteryData.getString(batteryData.getColumnIndex(Battery_Provider.Battery_Data.LEVEL));
            Log.d(MYTAG, "BATTERY LEFT:" + batteryLevel);
        }
        if (batteryData != null) {
            batteryData.close();
        }
        return Integer.parseInt(batteryLevel);
    }

    private void playNotification(Context c) {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(c, notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}