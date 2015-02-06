package com.aware.plugin.batterydrainer;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.content.BroadcastReceiver;
import com.aware.ESM;

public class AlarmReceiver extends BroadcastReceiver {

    private final String BIDJSON = "[{'esm':{" +
            "'esm_type':" + ESM.TYPE_ESM_TEXT + "," +
            "'esm_title': 'Bid now!'," +
            "'esm_instructions': 'How much should we pay you for 10% (units) of your battery life!'," +
            "'esm_submit': 'Bid!'," +
            "'esm_expiration_threashold': 60," + //the user has 20 minutes to respond. Set to 0 to disable
            "'esm_trigger': 'com.aware.plugin.batterydrainer'" +
            "}}]";

    private final String EMAILJSON = "[{'esm':{" +
            "'esm_type':" + ESM.TYPE_ESM_TEXT + "," +
            "'esm_title': 'Register'," +
            "'esm_instructions': 'Enter your uid below'," +
            "'esm_submit': 'Register'," +
            "'esm_expiration_threashold': 60," + //the user has 20 minutes to respond. Set to 0 to disable
            "'esm_trigger': 'com.aware.plugin.batterydrainer'" +
            "}}]";




    private final String MYTAG = "BATTERYDRAINER";

    public AlarmReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int q = intent.getIntExtra("qno", -1);
        Intent queue_esm = new Intent(ESM.ACTION_AWARE_QUEUE_ESM);
        String esmJSON = POPUPJSON;
        queue_esm.putExtra(ESM.EXTRA_ESM, esmJSON);
        context.sendBroadcast(queue_esm);
    }
}