package ru.ok.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import static ru.ok.timer.MainActivity.IS_STARTED;
import static ru.ok.timer.MainActivity.SHARED_PREFERENCE;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE, Context.MODE_PRIVATE);
            boolean isStarted = sharedPreferences.getBoolean(IS_STARTED, false);
            if (isStarted) {
                Intent serviceIntent = new Intent(context, TimerService.class);
                context.startService(serviceIntent);
                Toast.makeText(context,"Timer is running",Toast.LENGTH_LONG).show();
            }
        }
    }
}