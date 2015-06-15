package com.danvelazco.projectficarrier.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.danvelazco.projectficarrier.AlarmHelper;

/**
 * @since 6/12/15
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equalsIgnoreCase(intent.getAction())) {
            AlarmHelper.scheduleAlarm(context);
        }
    }

}
