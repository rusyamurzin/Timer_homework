package ru.ok.timer;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import static ru.ok.timer.MainActivity.EXTRA_IS_STARTED;
import static ru.ok.timer.MainActivity.EXTRA_TIME_LEFT;
import static ru.ok.timer.MainActivity.MAX_TIME;
import static ru.ok.timer.TimerApp.TIMER_TAG;

public class TimerService extends Service {
    private long timeLeft;
    CountDownTimer countDownTimer;
    private boolean isStarted;
    private Intent mainIntent;
    private BroadcastReceiver receiver;
    private NotificationManager manager;
    private NotificationCompat.Builder notificationBuilder;
    private Context ctx;
    private static final int NOTIFICATION_ID = 1;
    private final RemoteViews remoteViews = new RemoteViews("ru.ok.timer", R.layout.notification);

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mainIntent = intent;
        ctx = getApplicationContext();
        timeLeft = mainIntent.getLongExtra(EXTRA_TIME_LEFT, MAX_TIME);
        Log.w("START", "we started on timeleft = "+timeLeft+" and isStarted = "+isStarted);

        listener(ctx);

        Intent mainActivityIntent = new Intent(ctx, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctx, 0, mainActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), TIMER_TAG)
                .setSmallIcon(R.drawable.ic_notification_timer)
                .setContent(remoteViews)
                .setContentIntent(pendingIntent);
        startForeground(NOTIFICATION_ID, notificationBuilder.build());
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        startCountDownTimer();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.w("onDestroyService", "we destroyed on timeleft = "+timeLeft+" and isStarted = "+isStarted);
        ctx.unregisterReceiver(receiver);
        countDownTimer.cancel();
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
                mainIntent.putExtra(EXTRA_IS_STARTED, isStarted);
                mainIntent.putExtra(EXTRA_TIME_LEFT, timeLeft);
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
                updateSmallTimer(timeLeft);
                remoteViews.setTextViewText(R.id.small_btn_start_stop, getString(R.string.start));
                manager.notify(NOTIFICATION_ID, notificationBuilder.build());
                stopSelf();
            }
        }.start();
    }

    private void updateSmallTimer(long time) {
        int minutes = (int) (time / 1000) / 60;
        int seconds = (int) (time / 1000) % 60;
        remoteViews.setTextViewText(R.id.small_timer, String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void listener(Context context) {
        Intent intentStartStop = new Intent("Start/Stop");
        Intent intentReset = new Intent("Reset");

        PendingIntent pendingIntentStartStop = PendingIntent.getBroadcast(context,1, intentStartStop,0);
        PendingIntent pendingIntentReset = PendingIntent.getBroadcast(context,1, intentReset,0);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("Start/Stop");
        intentFilter.addAction("Reset");

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals("Start/Stop")) {
                        if (isStarted) {
                            if (countDownTimer != null) {
                                countDownTimer.cancel();
                            }
                            isStarted = false;
                            remoteViews.setTextViewText(R.id.small_btn_start_stop, getString(R.string.start));
                            manager.notify(NOTIFICATION_ID, notificationBuilder.build());
                        } else {
                            startCountDownTimer();
                            remoteViews.setTextViewText(R.id.small_btn_start_stop, getString(R.string.stop));
                            manager.notify(NOTIFICATION_ID, notificationBuilder.build());
                        }
                    } else if (action.equals("Reset")) {
                        if (countDownTimer != null) {
                            countDownTimer.cancel();
                        }
                        isStarted = false;
                        timeLeft = MAX_TIME;
                        updateSmallTimer(timeLeft);
                        remoteViews.setTextViewText(R.id.small_btn_start_stop, getString(R.string.start));
                        manager.notify(NOTIFICATION_ID, notificationBuilder.build());
                    }
                }
            }
        };

        context.registerReceiver(receiver,intentFilter);
        remoteViews.setOnClickPendingIntent(R.id.small_btn_start_stop, pendingIntentStartStop);
        remoteViews.setOnClickPendingIntent(R.id.small_btn_reset, pendingIntentReset);
    }
}