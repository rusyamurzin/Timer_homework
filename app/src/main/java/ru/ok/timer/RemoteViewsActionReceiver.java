package ru.ok.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import static ru.ok.timer.MainActivity.END_TIME;
import static ru.ok.timer.MainActivity.IS_STARTED;
import static ru.ok.timer.MainActivity.MAX_TIME;
import static ru.ok.timer.MainActivity.SHARED_PREFERENCE;
import static ru.ok.timer.MainActivity.TIME_LEFT;
import static ru.ok.timer.TimerService.RESET_ACTION;
import static ru.ok.timer.TimerService.START_STOP_ACTION;

public class RemoteViewsActionReceiver extends BroadcastReceiver {
    public RemoteViewsActionReceiver() {
        super();
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && (action.equals(START_STOP_ACTION) || action.equals(RESET_ACTION))) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCE, Context.MODE_PRIVATE);
            long endTime = sharedPreferences.getLong(END_TIME, 0);
            long timeLeft = sharedPreferences.getLong(TIME_LEFT, MAX_TIME);
            boolean isStarted = sharedPreferences.getBoolean(IS_STARTED, false);
            Intent timerServiceIntent = new Intent(context, TimerService.class);
            if (action.equals(START_STOP_ACTION)) {
                if (isStarted) {
                    isStarted = false;
                    saveSharePref(sharedPreferences, timeLeft, endTime, isStarted);
                    context.stopService(timerServiceIntent);
                } else {
                    isStarted = true;
                    saveSharePref(sharedPreferences, timeLeft, endTime, isStarted);
                    context.startService(timerServiceIntent);
                }
            } else if (action.equals(RESET_ACTION)) {
                isStarted = false;
                timeLeft = MAX_TIME;
                saveSharePref(sharedPreferences, timeLeft, endTime, isStarted);
                context.stopService(timerServiceIntent);
            }
        }
    }

    private void saveSharePref(SharedPreferences sharedPref, long timeLeft, long endTime, boolean isStarted) {
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(TIME_LEFT, timeLeft);
        editor.putLong(END_TIME, endTime);
        editor.putBoolean(IS_STARTED, isStarted);
        editor.apply();
    }
}