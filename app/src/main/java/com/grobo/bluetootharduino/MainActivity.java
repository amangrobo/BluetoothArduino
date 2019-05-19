package com.grobo.bluetootharduino;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import static com.grobo.bluetootharduino.DeviceList.EXTRA_ADDRESS;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private TextView selectedDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        selectedDevice = findViewById(R.id.selected_device_tv);

        CardView deviceList = findViewById(R.id.card_device_list);
        deviceList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, DeviceList.class));
            }
        });

        CardView joystick = findViewById(R.id.card_joystick);
        joystick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (prefs.getString(EXTRA_ADDRESS, "").equals("")) {
                    Toast.makeText(MainActivity.this, "No device selected", Toast.LENGTH_SHORT).show();
                } else {
                    startActivity(new Intent(MainActivity.this, ControlActivity.class));
                }
            }
        });

        CardView terminal = findViewById(R.id.card_terminal);
        terminal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (prefs.getString(EXTRA_ADDRESS, "").equals("")) {
                    Toast.makeText(MainActivity.this, "No device selected", Toast.LENGTH_SHORT).show();
                } else {
                    startActivity(new Intent(MainActivity.this, TerminalActivity.class));
                }
            }
        });

        CardView info = findViewById(R.id.card_info);
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        String deviceAddress = prefs.getString(EXTRA_ADDRESS, "");

        if (!deviceAddress.equals("")) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            String deviceName = device.getName() != null ? device.getName() : device.getAddress();

            selectedDevice.setText("Selected Device: " + deviceName);
        } else {
            startActivity(new Intent(MainActivity.this, DeviceList.class));
        }
    }
}
