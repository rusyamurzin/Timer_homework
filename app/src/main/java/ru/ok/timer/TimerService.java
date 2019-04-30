package ru.ok.timer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static ru.ok.timer.MainActivity.END_TIME;
import static ru.ok.timer.MainActivity.IS_STARTED;
import static ru.ok.timer.MainActivity.MAX_TIME;
import static ru.ok.timer.MainActivity.SHARED_PREFERENCE;
import static ru.ok.timer.MainActivity.TIME_LEFT;
import static ru.ok.timer.TimerApp.TIMER_TAG;

public class TimerService extends Service {
    private long timeLeft;
    CountDownTimer countDownTimer;
    private long endTime;
    private boolean isStarted = false;
    private NotificationManager manager;
    private NotificationCompat.Builder notificationBuilder;
    private Context ctx;
    public static final int NOTIFICATION_ID = 1;
    SharedPreferences sharedPreferences;
    public static final String START_STOP_ACTION = "ru.ok.timer.Start/Stop";
    public static final String RESET_ACTION = "ru.ok.timer.Reset";
    public static final RemoteViews remoteViews = new RemoteViews("ru.ok.timer", R.layout.notification);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sharedPreferences = getSharedPreferences(SHARED_PREFERENCE, MODE_PRIVATE);
        ctx = getApplicationContext();
        endTime = sharedPreferences.getLong(END_TIME, 0);
        timeLeft = sharedPreferences.getLong(TIME_LEFT, MAX_TIME);
        isStarted = sharedPreferences.getBoolean(IS_STARTED, false);

        setupListeners(ctx);

        Intent mainActivityIntent = new Intent(ctx, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, mainActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), TIMER_TAG)
                .setSmallIcon(R.drawable.ic_notification_timer)
                .setContent(remoteViews)
                .setContentIntent(pendingIntent);
        startForeground(NOTIFICATION_ID, notificationBuilder.build());
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if(isStarted) {
            long timeToEnd = endTime - System.currentTimeMillis();
            if(timeToEnd < 0) {
                timeLeft = 0;
                isStarted = false;
                updateSmallTimer(timeLeft);
                manager.notify(NOTIFICATION_ID, notificationBuilder.build());
                saveSharePref();
            }
            else {
                startCountDownTimer();
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.w("onDestroyService", "we destroyed on timeleft = "+timeLeft+" and isStarted = "+isStarted + " and endTime = " + endTime);
        isStarted = sharedPreferences.getBoolean(IS_STARTED, false);
        if(!isStarted) {
            timeLeft = sharedPreferences.getLong(TIME_LEFT, MAX_TIME);
            updateSmallTimer(timeLeft);
            remoteViews.setTextViewText(R.id.small_btn_start_stop, getString(R.string.start));
            manager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
        if(countDownTimer != null) {
            countDownTimer.cancel();
        }
        saveSharePref();
        super.onDestroy();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startCountDownTimer() {
        isStarted = true;
        remoteViews.setTextViewText(R.id.small_btn_start_stop, getString(R.string.stop));
        manager.notify(NOTIFICATION_ID, notificationBuilder.build());
        countDownTimer = new CountDownTimer(timeLeft, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeft = millisUntilFinished;
                endTime = System.currentTimeMillis() + timeLeft;
                saveSharePref();
                if (isStarted) {
                    updateSmallTimer(timeLeft);
                    manager.notify(NOTIFICATION_ID, notificationBuilder.build());
                } else {
                    countDownTimer.cancel();
                }
            }

            @Override
            public void onFinish() {
                isStarted = false;
                timeLeft = 0;
                endTime = System.currentTimeMillis();
                updateSmallTimer(timeLeft);
                remoteViews.setTextViewText(R.id.small_btn_start_stop, getString(R.string.start));
                manager.notify(NOTIFICATION_ID, notificationBuilder.build());
                saveSharePref();
                stopSelf();
            }
        }.start();
    }

    private void updateSmallTimer(long time) {
        int minutes = (int) (time / 1000) / 60;
        int seconds = (int) (time / 1000) % 60;
        remoteViews.setTextViewText(R.id.small_timer, String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void saveSharePref() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(TIME_LEFT, timeLeft);
        editor.putLong(END_TIME, endTime);
        editor.putBoolean(IS_STARTED, isStarted);
        editor.apply();
    }

    private void setupListeners(Context context) {
        Intent intentStartStop = new Intent(START_STOP_ACTION);
        Intent intentReset = new Intent(RESET_ACTION);

        PendingIntent pendingIntentStartStop = PendingIntent.getBroadcast(context,1, intentStartStop,0);
        PendingIntent pendingIntentReset = PendingIntent.getBroadcast(context,1, intentReset,0);

        remoteViews.setOnClickPendingIntent(R.id.small_btn_start_stop, pendingIntentStartStop);
        remoteViews.setOnClickPendingIntent(R.id.small_btn_reset, pendingIntentReset);
    }
}