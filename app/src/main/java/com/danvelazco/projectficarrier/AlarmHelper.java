package com.danvelazco.projectficarrier;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.danvelazco.projectficarrier.service.NetworkIntentService;

/**
 * @since 6/12/15
 */
public class AlarmHelper {

    public static void scheduleAlarm(Context context) {
        // Get the intent and schedule the alarm, this is as a backup in case the network changed receiver is not called
        Intent serviceIntent = new Intent(context, NetworkIntentService.class);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, serviceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        schedulePendingIntent(context, pendingIntent, AlarmManager.INTERVAL_HALF_HOUR);

        // Attempt to query the data right now
        Intent intentService = new Intent(context, NetworkIntentService.class);
        context.startService(intentService);
    }

    private static void schedulePendingIntent(Context context, PendingIntent pendingIntent, long frequency) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, frequency, frequency, pendingIntent);
    }

}
