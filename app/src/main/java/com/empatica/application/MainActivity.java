package com.empatica.application;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.EmpaticaDevice;
import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaDataDelegate;
import com.empatica.empalink.delegate.EmpaStatusDelegate;

public class MainActivity extends AppCompatActivity implements EmpaDataDelegate, EmpaStatusDelegate {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSION_ACCESS_COARSE_LOCATION = 1;
    private static final String EMPATICA_API_KEY = "ccd024d253354014994e5eece248b84d";
    private EmpaDeviceManager deviceManager = null;

    private TextView accel_xLabel;
    private TextView accel_yLabel;
    private TextView accel_zLabel;
    private TextView bvpLabel;
    private TextView edaLabel;
    private TextView ibiLabel;
    private TextView temperatureLabel;
    private TextView batteryLabel;
    private TextView statusLabel;
    private TextView deviceNameLabel;
    private LinearLayout dataCnt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusLabel = findViewById(R.id.status);
        dataCnt = findViewById(R.id.dataArea);
        accel_xLabel = findViewById(R.id.accel_x);
        accel_yLabel = findViewById(R.id.accel_y);
        accel_zLabel = findViewById(R.id.accel_z);
        bvpLabel = findViewById(R.id.bvp);
        edaLabel = findViewById(R.id.eda);
        ibiLabel = findViewById(R.id.ibi);
        temperatureLabel = findViewById(R.id.temperature);
        batteryLabel = findViewById(R.id.battery);
        deviceNameLabel = findViewById(R.id.deviceName);

        final Button disconnectButton = findViewById(R.id.disconnectButton);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deviceManager != null) {
                    deviceManager.disconnect();
                }
            }
        });

        initEmpaticaDeviceManager();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_PERMISSION_ACCESS_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initEmpaticaDeviceManager();
            } else {
                final boolean needRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION);
                new AlertDialog.Builder(this).setTitle("Permission required")
                        .setMessage("Without this permission bluetooth low energy devices cannot be found, allow it in order to connect to the device.")
                        .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (needRationale) {
                                    initEmpaticaDeviceManager();
                                } else {
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                                    intent.setData(uri);
                                    startActivity(intent);
                                }
                            }
                        }).setNegativeButton("Exit application", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        }).show();
            }
        }
    }

    private void initEmpaticaDeviceManager() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, REQUEST_PERMISSION_ACCESS_COARSE_LOCATION);
        } else {
            if (TextUtils.isEmpty(EMPATICA_API_KEY)) {
                new AlertDialog.Builder(this)
                        .setTitle("Warning")
                        .setMessage("Please insert your API KEY")
                        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // without permission exit is the only way
                                finish();
                            }
                        })
                        .show();
                return;
            }

            deviceManager = new EmpaDeviceManager(getApplicationContext(), this, this);
            deviceManager.authenticateWithAPIKey(EMPATICA_API_KEY);
            Log.d("CustomDebug", "Authentication with key is complete");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("CustomDebug", "Entering onPause");
        if (deviceManager != null) {
            deviceManager.stopScanning();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("CustomDebug", "Entering onDestroy");
        if (deviceManager != null) {
            deviceManager.cleanUp();
        }
    }

    @Override
    public void didDiscoverDevice(EmpaticaDevice bluetoothDevice, String deviceName, int rssi, boolean allowed) {
        if(allowed) {
            Log.d("CustomDebug", "Device is Allowed");
        } else {
            Log.d("CustomDebug", "Device is Not Allowed");
        }
        if (allowed) {
            deviceManager.stopScanning();
            try {
                deviceManager.connectDevice(bluetoothDevice);
                updateLabel(deviceNameLabel, "To: " + deviceName);
                BluetoothDevice bd = deviceManager.getActiveDevice();
                Log.d("CustomDebug", "Connected to device is named [" + bd.getName() + "] with address [" + bd.getAddress() + "]");
            } catch (ConnectionNotAllowedException e) {
                Log.d("CustomDebug", "Fails to connect to bluetooth");
                Toast.makeText(MainActivity.this, "Sorry, you can't connect to this device", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void didRequestEnableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            // TODO: Deal with bluetooth permission is still denied
            Log.d("CustomDebug", "Bluetooth is not enabled");
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void didUpdateSensorStatus(@EmpaSensorStatus int status, EmpaSensorType type) {
        Log.d("CustomDebug", "Sensor status has been updated");
        didUpdateOnWristStatus(status);
    }

    @Override
    public void didUpdateStatus(EmpaStatus status) {
        updateLabel(statusLabel, status.name());
        Log.d("CustomDebug", "Entering didUpdateStatus function");

        if (status == EmpaStatus.READY) {
            updateLabel(statusLabel, status.name() + " - Turn on your device");
            Log.d("CustomDebug", "Device is ready");
            deviceManager.startScanning();
            hide();
        } else if (status == EmpaStatus.CONNECTED) {
            Log.d("CustomDebug", "Device is Connected");
            show();
        } else if (status == EmpaStatus.DISCONNECTED) {
            Log.d("CustomDebug", "Device is Disconnect with count");
            updateLabel(deviceNameLabel, " The Device has been disconnected");
            hide();
        }
    }

    @Override
    public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
        Log.d("CustomDebug", "Acceleration is [" + x+y+z + "]");
        updateLabel(accel_xLabel, "" + x);
        updateLabel(accel_yLabel, "" + y);
        updateLabel(accel_zLabel, "" + z);
    }

    @Override
    public void didReceiveBVP(float bvp, double timestamp) {
        Log.d("CustomDebug", "BVP is [" + bvp + "]");
        updateLabel(bvpLabel, "" + bvp);
    }

    @Override
    public void didReceiveBatteryLevel(float battery, double timestamp) {
        Log.d("CustomDebug", "Battery is [" + battery + "]");
        updateLabel(batteryLabel, Float.toString(battery*100));
    }

    @Override
    public void didReceiveGSR(float gsr, double timestamp) {
        Log.d("CustomDebug", "GSR is [" + gsr + "]");
        updateLabel(edaLabel, "" + gsr);
    }

    @Override
    public void didReceiveIBI(float ibi, double timestamp) {
        Log.d("CustomDebug", "IBI is [" + ibi + "]");
        updateLabel(ibiLabel, "" + ibi);
    }

    @Override
    public void didReceiveTemperature(float temp, double timestamp) {
        Log.d("CustomDebug", "Temperature is [" + temp + "]");
        updateLabel(temperatureLabel, "" + temp);
    }

    private void updateLabel(final TextView label, final String text) {
        Log.d("CustomDebug", "Label update is [" + text + "]");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                label.setText(text);
            }
        });
    }

    @Override
    public void didReceiveTag(double timestamp) {
        Log.d("CustomDebug", "Tag is [" + timestamp + "]");
    }

    @Override
    public void didEstablishConnection() {
        Log.d("CustomDebug", "Connection has been established");
        show();
    }

    @Override
    public void didUpdateOnWristStatus(@EmpaSensorStatus final int status) {
        if(status == EmpaSensorStatus.ON_WRIST) {
            Log.d("CustomDebug", "Wrist Status is [On Wrist]");
        } else {
            Log.d("CustomDebug", "Wrist Status is [Not On Wrist]");
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (status == EmpaSensorStatus.ON_WRIST) {
                    ((TextView)findViewById(R.id.wrist_status_label)).setText(R.string.on_wrist);
                }
                else {
                    ((TextView)findViewById(R.id.wrist_status_label)).setText(R.string.not_on_wrist);
                }
            }
        });
    }

    void show() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dataCnt.setVisibility(View.VISIBLE);
            }
        });
    }

    void hide() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dataCnt.setVisibility(View.INVISIBLE);
            }
        });
    }
}
