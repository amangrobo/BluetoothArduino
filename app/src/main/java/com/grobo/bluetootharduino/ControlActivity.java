package com.grobo.bluetootharduino;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import io.github.controlwear.virtual.joystick.android.JoystickView;

import static com.grobo.bluetootharduino.DeviceList.EXTRA_ADDRESS;

public class ControlActivity extends AppCompatActivity implements SerialListener, JoystickView.OnMoveListener {

    private enum Connected {False, Pending, True}

    private enum Mode {Manual, LF, Encoder}

    private String deviceAddress;
    private String newline = "#";

    private SerialSocket socket;
    private boolean initialStart = true;
    private Connected connected = Connected.False;
    private ProgressDialog progressDialog;
    private JoystickView joystick;

    private Mode mode = Mode.Manual;

    BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_control);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        deviceAddress = prefs.getString(EXTRA_ADDRESS, "");

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setCanceledOnTouchOutside(false);

        Spinner spinner = findViewById(R.id.mode_spinner);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.mode_spinner_list, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        joystick.setEnabled(true);
                        mode = Mode.Manual;
                        send("MN");
                        break;
                    case 1:
                        joystick.setEnabled(false);
                        mode = Mode.LF;
                        send("LF");
                        break;
                    case 2:
                        joystick.setEnabled(false);
                        mode = Mode.Encoder;
                        send("EC");
                        break;
                }

                Toast.makeText(ControlActivity.this, "mode" + mode.name(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        joystick = findViewById(R.id.joystick_control);
        joystick.setOnMoveListener(this, 200);   //200 ms
        joystick.setBorderColor(Color.parseColor("#000000"));

        Button retryButton = findViewById(R.id.retry_button);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
                connect();
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();

        if (bluetoothAdapter == null || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent bluetoothOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(bluetoothOn, 1);
        }

        if (initialStart || connected == Connected.False) {
            initialStart = false;
            connect();
        }
    }

    private void connect() {

        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            String deviceName = device.getName() != null ? device.getName() : device.getAddress();

            progressDialog.setMessage("Connecting: " + deviceName);
            progressDialog.show();

            connected = Connected.Pending;
            socket = new SerialSocket();
            socket.connect(this, this, device);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void send(String str) {
        if (connected != Connected.True) {
            Toast.makeText(ControlActivity.this, "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            byte[] data = (str + newline).getBytes();
            socket.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    @Override
    public void onSerialConnect() {
        connected = Connected.True;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                changeStatus("Connected");

                switch (mode) {
                    case Manual:
                        send("MN");
                        break;
                    case LF:
                        send("LF");
                        break;
                    case Encoder:
                        send("EC");
                        break;
                }
            }
        });

    }

    private void changeStatus(String message) {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        Toast.makeText(ControlActivity.this, message, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onSerialConnectError(final Exception e) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                changeStatus("Connection Failed");
                Snackbar.make(findViewById(R.id.parent_activity_control), "Connection failed" + e.toString(), Snackbar.LENGTH_LONG)
                        .setAction("retry", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                connect();
                            }
                        }).show();
            }
        });

        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receive(data);
    }

    private void receive(final byte[] data) {

    }

    @Override
    public void onSerialIoError(Exception e) {
        changeStatus("Serial error");

        disconnect();
        connect();
    }

    @Override
    protected void onDestroy() {
        disconnect();
        super.onDestroy();
    }

    private void disconnect() {
        if (connected != Connected.False) {
            connected = Connected.False;
            socket.disconnect();
            socket = null;
        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit ?");
        builder.setMessage("This will end your connection");
        builder.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ControlActivity.super.onBackPressed();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public void onMove(int angle, int strength) {
        determineMovement(angle, strength);

        Log.e("str", angle + ", " + strength);
    }

    private void determineMovement(int angle, int strength) {
        String m = "";
        if ((angle >= 0 && angle <= 22) || (angle >= 338 && angle <= 360)) {
            m = "R";
        } else if (angle > 22 && angle <= 67) {
            m = "S";    //FR
        } else if (angle > 67 && angle < 113) {
            m = "F";
        } else if (angle >= 113 && angle < 158) {
            m = "T";    //FL
        } else if (angle >= 158 && angle <= 202) {
            m = "L";
        } else if (angle > 202 && angle <= 247) {
            m = "V";    //BL
        } else if (angle > 247 && angle < 293) {
            m = "B";
        } else if (angle >= 293 && angle < 338) {
            m = "U";    //BR
        }
        send(m + "_" + strength);
    }
}
