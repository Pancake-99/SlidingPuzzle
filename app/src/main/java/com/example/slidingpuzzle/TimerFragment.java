package com.example.slidingpuzzle;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Locale;

public class TimerFragment extends Fragment {

    private TextView tvTimer;
    private long startTime = 0L;
    //el handler sirve para agendar tareas a futuro
    private Handler handler = new Handler();
    private boolean isRunning = false;
    private String lastFormattedTime = "00:00.000";

    //el runnable es el codigo que se repite para actualizar el texto
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                //systemclock da el tiempo real desde que prendio el cel
                long millis = SystemClock.elapsedRealtime() - startTime;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds = seconds % 60;
                int milliseconds = (int) (millis % 1000);

                lastFormattedTime = String.format(Locale.getDefault(), "%02d:%02d.%03d", minutes, seconds, milliseconds);
                tvTimer.setText(lastFormattedTime);
                //se vuelve a llamar a si mismo en 10 milisegundos
                handler.postDelayed(this, 10);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timer, container, false);
        tvTimer = view.findViewById(R.id.tv_timer);
        return view;
    }

    //arranca el reloj
    public void startTimer() {
        if (!isRunning) {
            startTime = SystemClock.elapsedRealtime();
            isRunning = true;
            handler.post(runnable);
        }
    }

    //frena el reloj
    public void stopTimer() {
        isRunning = false;
        handler.removeCallbacks(runnable);
    }

    //vuelve a cero
    public void resetTimer() {
        stopTimer();
        lastFormattedTime = "00:00.000";
        tvTimer.setText(lastFormattedTime);
    }

    //devuelve el tiempo actual en texto
    public String getFormattedTime() {
        return lastFormattedTime;
    }
}
