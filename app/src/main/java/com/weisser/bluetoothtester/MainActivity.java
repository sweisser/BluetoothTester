package com.weisser.bluetoothtester;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import static com.weisser.bluetoothtester.BluetoothThread.SUCCESS;

// DONE Add timer and refresh automatically
// TODO Implement all lifecycle methods correctly
// TODO Powersave mode for fio and bluetooth bee
// TODO Simple recording and charting

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String LOGTAG = "MainActivity";

    private BluetoothThread bluetoothThread;

    private static final int REQUEST_ENABLE_BT = 1;

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

        // Bluetooth active?
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (bluetoothThread != null) {
                bluetoothThread.interrupt();
            }

            bluetoothThread = new BluetoothThread();
            int rc = bluetoothThread.initConnection(GPSDevices.BLUETOOTH_BEE);
            if (rc == SUCCESS) {
                bluetoothThread.start();
            }

            timerHandler.postDelayed(timerRunnable, 0);
        }
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

    @Override
    public void onActivityResult (int requestCode,
                           int resultCode,
                           Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            Log.d(LOGTAG, "Bluetooth enabled.");
        }
    }
}
