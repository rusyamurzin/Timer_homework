package ru.ok.timer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private long endTime;
    private long timeLeft;
    public static final long MAX_TIME = 3600000;
    private TextView timerTextView;
    private Button startStopButton;
    private Button resetButton;
    private boolean isStarted = false;
    private CountDownTimer countDownTimer;
    private Intent timerServiceIntent;
    private static final String SHARED_PREFERENCE = "TIMER_PREFERENCE";
    private static final String TIME_LEFT = "TIME_LEFT";
    private static final String IS_STARTED = "IS_STARTED";
    private static final String END_TIME = "END_TIME";
    public static final String EXTRA_TIME_LEFT = "ru.ok.timer.EXTRA_TIME_LEFT";
    public static final String EXTRA_END_TIME = "ru.ok.timer.EXTRA_END_TIME";
    public static final String EXTRA_IS_STARTED = "ru.ok.timer.EXTRA_IS_STARTED";

    @Override
    protected void onStop() {
        super.onStop();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCE, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(TIME_LEFT, timeLeft);
        editor.putLong(END_TIME, endTime);
        editor.putBoolean(IS_STARTED, isStarted);
        editor.apply();
        if (isStarted) {
            timerServiceIntent.putExtra(EXTRA_TIME_LEFT, timeLeft);
            startService(timerServiceIntent);
            //ContextCompat.startForegroundService(this, timerServiceIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (stopService(timerServiceIntent)) {
            timeLeft = timerServiceIntent.getLongExtra(EXTRA_TIME_LEFT, MAX_TIME);
            isStarted = timerServiceIntent.getBooleanExtra(EXTRA_IS_STARTED, false);
            endTime = System.currentTimeMillis() + timeLeft;
            Log.w("onStartMainActivity", "we resume on timeleft = "+timeLeft+" and isStarted = "+isStarted);
        } else {
            SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCE, MODE_PRIVATE);
            isStarted = sharedPreferences.getBoolean(IS_STARTED, false);
            endTime = sharedPreferences.getLong(END_TIME, 0);
            timeLeft = sharedPreferences.getLong(TIME_LEFT, MAX_TIME);
        }

        startStopButton.setText(getString(R.string.start));
        if(isStarted) {
            timeLeft = endTime - System.currentTimeMillis();
            if(timeLeft < 0) {
                timeLeft = 0;
                isStarted = false;
                updateTimerView(timeLeft);
                startStopButton.setText(getString(R.string.start));
            }
            else {
                startTimer();
            }
        } else {
            updateTimerView(timeLeft);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timerTextView = (TextView) findViewById(R.id.timer);
        startStopButton = (Button) findViewById(R.id.btn_start_stop);
        resetButton = (Button) findViewById(R.id.btn_reset);
        timerServiceIntent = new Intent(this, TimerService.class);
        setupButtons();
    }

    private void setupButtons() {
        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Button b = (Button) v;
                if (isStarted) {
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }
                    isStarted = false;
                    b.setText(getString(R.string.start));
                } else {
                    endTime = System.currentTimeMillis() + timeLeft;
                    startTimer();
                }
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (countDownTimer != null) {
                    countDownTimer.cancel();
                }
                isStarted = false;
                timeLeft = MAX_TIME;
                updateTimerView(timeLeft);
                startStopButton.setText(getString(R.string.start));
            }
        });
    }

    private void startTimer() {
        isStarted = true;
        startStopButton.setText(getString(R.string.stop));
        countDownTimer = new CountDownTimer(timeLeft, 17) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isStarted) {
                    timeLeft = millisUntilFinished;
                    updateTimerView(timeLeft);
                } else {
                    countDownTimer.cancel();
                }
            }

            @Override
            public void onFinish() {
                isStarted = false;
                timeLeft = 0;
                updateTimerView(timeLeft);
                startStopButton.setText(getString(R.string.start));
            }
        }.start();
    }

    private void updateTimerView(long time) {
        int minutes = (int) (time / 1000) / 60;
        int seconds = (int) (time / 1000) % 60;
        int millis = (int) time % 1000;
        timerTextView.setText(String.format(Locale.getDefault(), "%02d:%02d:%03d", minutes, seconds, millis));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
