package com.weisser.bluetoothtester;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

// DONE Add timer and refresh automatically
// TODO Implement all lifecycle methods correctly
// TODO Powersave mode for fio and bluetooth bee
// TODO Simple recording and charting

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private BluetoothThread bluetoothThread;

    private long startTime = 0;

    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            updateTemperature();

            timerHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button1 = (Button) findViewById(R.id.startBluetoothButton);
        button1.setOnClickListener(this);

        Button button2 = (Button) findViewById(R.id.readTemperatureButton);
        button2.setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (bluetoothThread != null) {
            bluetoothThread.interrupt();
        }

        bluetoothThread = new BluetoothThread();
        bluetoothThread.initConnection(GPSDevices.BLUETOOTH_BEE);
        bluetoothThread.start();

        timerHandler.postDelayed(timerRunnable, 0);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (bluetoothThread != null) {
            bluetoothThread.interrupt();
            bluetoothThread = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();

        timerHandler.removeCallbacks(timerRunnable);
    }

    public void onClick(View v) {
        final int id = v.getId();
        switch (id) {
            case R.id.startBluetoothButton:
                startTime = System.currentTimeMillis();
                timerHandler.postDelayed(timerRunnable, 0);
                break;
            case R.id.readTemperatureButton:
                onRequestTemperature(v);
                break;
        }
    }

    public void onRequestTemperature(View v) {
        updateTemperature();
    }

    private void updateTemperature() {
        if (bluetoothThread != null) {
            double temp = bluetoothThread.readTemperature();
            TextView temperatureView = (TextView) findViewById(R.id.temperatureView);
            temperatureView.setText(Double.toString(temp) + " °C");
        }
    }
}
