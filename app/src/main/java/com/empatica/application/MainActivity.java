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
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;
import android.util.Log;
import io.reactivex.functions.Consumer;

import java.io.File;
import java.lang.Math;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.io.FileWriter;

import com.opencsv.CSVWriter;
import com.empatica.application.retrofit.IBackend;
import com.empatica.application.retrofit.RetrofitClient;
import com.empatica.application.retrofit.SchedulerProvider;
import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.EmpaticaDevice;
import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaDataDelegate;
import com.empatica.empalink.delegate.EmpaStatusDelegate;

public class MainActivity extends AppCompatActivity implements EmpaDataDelegate, EmpaStatusDelegate {
    private static final String EMPATICA_API_KEY = "ccd024d253354014994e5eece248b84d";
    private static final int PERMISSION_CODE = 1;
    private static final int ACCEL_DEVIATION = 10;
    private boolean isDataLocalized = true;
    private String ipAddress = "";
    private EmpaDeviceManager deviceManager = null;

    private ArrayList<String[]> Physiology_Store = new ArrayList<String[]>();
    private ArrayList<String[]> Accels_Store = new ArrayList<String[]>();

    private Float bvp = null;
    private Float eda = null;
    private Float ibi = null;
    private Float heartRate = null;
    private Float temperature = null;
    private Integer accelX = null;
    private Integer accelY = null;
    private Integer accelZ = null;

    private TextView accel_xLabel;
    private TextView accel_yLabel;
    private TextView accel_zLabel;
    private TextView bvpLabel;
    private TextView edaLabel;
    private TextView ibiLabel;
    private TextView temperatureLabel;
    private TextView heartLabel;
    private TextView statusLabel;
    private TextView batteryLabel;
    private LinearLayout dataArea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusLabel = findViewById(R.id.status);
        dataArea = findViewById(R.id.data_area);
        accel_xLabel = findViewById(R.id.accel_x);
        accel_yLabel = findViewById(R.id.accel_y);
        accel_zLabel = findViewById(R.id.accel_z);
        bvpLabel = findViewById(R.id.bvp);
        edaLabel = findViewById(R.id.eda);
        ibiLabel = findViewById(R.id.ibi);
        temperatureLabel = findViewById(R.id.temperature);
        heartLabel = findViewById(R.id.heart);
        batteryLabel = findViewById(R.id.battery_label);

        final MainActivity temp = this;
        final Button disconnectButton = findViewById(R.id.disconnectButton);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(deviceManager != null) {
                    deviceManager.stopScanning();
                    deviceManager.disconnect();
                    deviceManager.cleanUp();
                    deviceManager = null;
                }

                if(isDataLocalized) {
                    try {
                        if(ContextCompat.checkSelfPermission(temp, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(temp, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, PERMISSION_CODE);
                        }

                        String csvPath = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
                        csvPath = csvPath + File.separator + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
                        Log.d("CustomDebug", csvPath);

                        File physiology_file = new File(csvPath + "_P.csv");
                        physiology_file.createNewFile();
                        CSVWriter physiology_writer = new CSVWriter(new FileWriter(physiology_file));
                        physiology_writer.writeAll(Physiology_Store);
                        physiology_writer.close();

                        File accel_file = new File(csvPath + "_A.csv");
                        accel_file.createNewFile();
                        CSVWriter accel_writer = new CSVWriter(new FileWriter(accel_file));
                        accel_writer.writeAll(Accels_Store);
                        accel_writer.close();
                    } catch (Exception e) {
                        Log.d("CustomDebug", "Exception occurred during csv write");
                        Log.d("CustomDebug", e.toString());
                    }
                }
                initDeviceManager();
            }
        });

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("Select where the data is stored");
        alertBuilder.setPositiveButton("Store Data Locally", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isDataLocalized = true;
                initDeviceManager();
            }
        });
        alertBuilder.setNegativeButton("Store Data Remotely", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isDataLocalized = false;

                AlertDialog.Builder ipAlertBuilder = new AlertDialog.Builder(temp);
                ipAlertBuilder.setTitle("Please Insert localhost ip");
                ipAlertBuilder.setCancelable(false);
                final EditText participantIdInput = new EditText(temp);
                participantIdInput.setInputType(InputType.TYPE_CLASS_TEXT);
                participantIdInput.setText("192.168.8.107", TextView.BufferType.EDITABLE);
                ipAlertBuilder.setView(participantIdInput);
                ipAlertBuilder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ipAddress = "http://" + participantIdInput.getText().toString() + ":8006/api/";
                        initDeviceManager();
                    }
                });
                ipAlertBuilder.show();
            }
        });
        alertBuilder.show();
    }

    private void initDeviceManager() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_COARSE_LOCATION }, PERMISSION_CODE);
        } else {
            if (TextUtils.isEmpty(EMPATICA_API_KEY)) {
                new AlertDialog.Builder(this)
                        .setTitle("Warning")
                        .setMessage("Please insert your API KEY")
                        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .show();
                return;
            }

            clearLabels();
            deviceManager = new EmpaDeviceManager(getApplicationContext(), this, this);
            deviceManager.authenticateWithAPIKey(EMPATICA_API_KEY);
            Log.d("CustomDebug", "Authentication with key is complete");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initDeviceManager();
            } else {
                final boolean needRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION);
                new AlertDialog.Builder(this).setTitle("Permission required")
                        .setMessage("Without this permission bluetooth low energy devices cannot be found, allow it in order to connect to the device.")
                        .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (needRationale) {
                                    initDeviceManager();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PERMISSION_CODE && resultCode == Activity.RESULT_CANCELED) {
            Log.d("CustomDebug", "Bluetooth is not enabled");
            Toast.makeText(MainActivity.this, "Bluetooth is not enabled, exiting application", Toast.LENGTH_SHORT).show();
            System.exit(1);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void didDiscoverDevice(EmpaticaDevice bluetoothDevice, String deviceName, int rssi, boolean allowed) {
        if(allowed) {
            Log.d("CustomDebug", "Device is Allowed");
        } else {
            Log.d("CustomDebug", "Device is Not Allowed");
        }
        if(allowed) {
            deviceManager.stopScanning();
            try {
                deviceManager.connectDevice(bluetoothDevice);
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
        startActivityForResult(enableBtIntent, PERMISSION_CODE);
    }

    @Override
    public void didUpdateSensorStatus(@EmpaSensorStatus int status, EmpaSensorType type) {
        Log.d("CustomDebug", "Sensor status has been updated");
        didUpdateOnWristStatus(status);
    }

    @Override
    public void didUpdateStatus(EmpaStatus status) {
        updateLabel(statusLabel, status.name());
        if (status == EmpaStatus.READY) {
            updateLabel(statusLabel, status.name() + " - Turn on your device");
            Log.d("CustomDebug", "Device is ready");
            deviceManager.startScanning();
            hide();
        } else if (status == EmpaStatus.CONNECTED) {
            Log.d("CustomDebug", "Device is Connected");
            show();
        } else if (status == EmpaStatus.DISCONNECTED) {
            Log.d("CustomDebug", "Device is Disconnected");
            hide();
        }
    }

    @Override
    public void didEstablishConnection() {
        Log.d("CustomDebug", "Connection has been established");
        show();
    }

    @Override
    public void didFailedScanning(int errorCode) {
        Log.d("CustomDebug", "didFailedScanning has been evoked");
        show();
    }

    @Override
    public void bluetoothStateChanged() {
        Log.d("CustomDebug", "bluetoothStateChanged has been evoked");
        show();
    }

    @Override
    public void didReceiveTag(double timestamp) {
        Log.d("CustomDebug", "Tag is [" + timestamp + "]");
    }

    @Override
    public void didReceiveBatteryLevel(float battery, double timestamp) {
        Log.d("CustomDebug", "Battery is [" + battery + "]");
        updateLabel(batteryLabel, "Battery: " + Float.toString(battery*100) + "%");
    }

    @Override
    public void didReceiveBVP(float bvp, double timestamp) {
        this.bvp = bvp;
        if(checkDataPostConditions()) {
            if(isDataLocalized) {
                String[] data = new String[6];
                data[0] = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
                data[1] = String.valueOf(timestamp);
                data[2] = String.valueOf(this.bvp);
                data[3] = String.valueOf(this.eda);
                data[4] = String.valueOf(this.heartRate);
                data[5] = String.valueOf(this.temperature);
                Physiology_Store.add(data);
            } else {
                InsertData(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
                        timestamp, this.bvp, this.eda, this.ibi, this.heartRate, this.temperature);
                Log.d("CustomDebug", "BVP of [" + bvp + "] has been pushed");
            }
        }
        updateLabel(bvpLabel, Float.toString(bvp));
    }

    @Override
    public void didReceiveGSR(float gsr, double timestamp) {
        this.eda = gsr;
        if(checkDataPostConditions()) {
            if(isDataLocalized) {
                String[] data = new String[6];
                data[0] = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
                data[1] = String.valueOf(timestamp);
                data[2] = String.valueOf(this.bvp);
                data[3] = String.valueOf(this.eda);
                data[4] = String.valueOf(this.heartRate);
                data[5] = String.valueOf(this.temperature);
                Physiology_Store.add(data);
            } else {
                InsertData(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
                        timestamp, this.bvp, this.eda, this.ibi, this.heartRate, this.temperature);
                Log.d("CustomDebug", "GSR of [" + gsr + "] has been pushed");
            }
        }
        updateLabel(edaLabel, Float.toString(gsr));
    }

    @Override
    public void didReceiveIBI(float ibi, double timestamp) {
        this.ibi = ibi;
        this.heartRate = 60/ibi;
        if(checkDataPostConditions()) {
            if(isDataLocalized) {
                String[] data = new String[6];
                data[0] = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
                data[1] = String.valueOf(timestamp);
                data[2] = String.valueOf(this.bvp);
                data[3] = String.valueOf(this.eda);
                data[4] = String.valueOf(this.heartRate);
                data[5] = String.valueOf(this.temperature);
                Physiology_Store.add(data);
            } else {
                InsertData(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
                        timestamp, this.bvp, this.eda, this.ibi, this.heartRate, this.temperature);
                Log.d("CustomDebug", "IBI of [" + ibi + "] has been pushed");
            }
        }
        updateLabel(ibiLabel, Float.toString(ibi));
        updateLabel(heartLabel, Float.toString(heartRate));
    }

    @Override
    public void didReceiveTemperature(float temp, double timestamp) {
        this.temperature = temp;
        if(checkDataPostConditions()) {
            if(isDataLocalized) {
                String[] data = new String[6];
                data[0] = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
                data[1] = String.valueOf(timestamp);
                data[2] = String.valueOf(this.bvp);
                data[3] = String.valueOf(this.eda);
                data[4] = String.valueOf(this.heartRate);
                data[5] = String.valueOf(this.temperature);
                Physiology_Store.add(data);
            } else {
                InsertData(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
                        timestamp, this.bvp, this.eda, this.ibi, this.heartRate, this.temperature);
                Log.d("CustomDebug", "Temperature of [" + temp + "] has been pushed");
            }
        }
        updateLabel(temperatureLabel, Float.toString(temp));
    }

    @Override
    public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
        if(checkAccelPostConditions(x, y, z)) {
            if(isDataLocalized) {
                String[] data = new String[5];
                data[0] = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
                data[1] = String.valueOf(timestamp);
                data[2] = String.valueOf(x);
                data[3] = String.valueOf(y);
                data[4] = String.valueOf(z);
                Accels_Store.add(data);
            } else {
                InsertAcceleration(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
                        timestamp, x, y, z);
                Log.d("CustomDebug", "Acceleration of [" + x + " " + y + " " + z + "] has been pushed");
            }
        }
        this.accelX = x;
        this.accelY = y;
        this.accelZ = z;
        updateLabel(accel_xLabel, Integer.toString(accelX));
        updateLabel(accel_yLabel, Integer.toString(accelY));
        updateLabel(accel_zLabel, Integer.toString(accelZ));
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
                } else {
                    ((TextView)findViewById(R.id.wrist_status_label)).setText(R.string.not_on_wrist);
                }
            }
        });
    }

    private void updateLabel(final TextView label, final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                label.setText(text);
            }
        });
    }

    private void clearLabels() {
        updateLabel(accel_xLabel, "-");
        updateLabel(accel_yLabel, "-");
        updateLabel(accel_zLabel, "-");
        updateLabel(bvpLabel, "calibrating");
        updateLabel(edaLabel, "calibrating");
        updateLabel(ibiLabel, "calibrating");
        updateLabel(temperatureLabel, "calibrating");
        updateLabel(heartLabel, "calibrating");
    }

    private boolean checkDataPostConditions() {
        if(bvp != null && eda != null && ibi != null
                && heartRate != null && temperature != null) {
            return true;
        }
        return false;
    }

    private boolean checkAccelPostConditions(int x, int y, int z) {
        if(this.accelX == null || this.accelY == null || this.accelZ == null) {
            return true;
        } else if(Math.abs(this.accelX - x) > ACCEL_DEVIATION || Math.abs(this.accelY - y) > ACCEL_DEVIATION
                || Math.abs(this.accelZ - z) > ACCEL_DEVIATION) {
            return true;
        } else {
            return false;
        }
    }

    private void InsertData(String utc, double e4Time, float bvp, float eda,
                            float ibi, float heartRate, float temperature) {
        IBackend backendService = RetrofitClient.getService(ipAddress);
        backendService.InsertData(utc, e4Time, bvp, eda, ibi, heartRate, temperature)
                .subscribeOn(SchedulerProvider.IOThread())
                .observeOn(SchedulerProvider.UIThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String input) throws Exception {
                        Log.d("CustomDebug", "InsertData request has been made!");
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

    private void InsertAcceleration(String utc, double e4Time, float accelX,
                                    float accelY, float accelZ) {
        IBackend backendService = RetrofitClient.getService(ipAddress);
        backendService.InsertAcceleration(utc, e4Time, accelX, accelY, accelZ)
                .subscribeOn(SchedulerProvider.IOThread())
                .observeOn(SchedulerProvider.UIThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String input) throws Exception {
                        Log.d("CustomDebug", "InsertData request has been made!");
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        throwable.printStackTrace();
                    }
                });
    }

    void show() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                batteryLabel.setVisibility(View.VISIBLE);
                dataArea.setVisibility(View.VISIBLE);
            }
        });
    }

    void hide() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                batteryLabel.setVisibility(View.INVISIBLE);
                dataArea.setVisibility(View.INVISIBLE);
            }
        });
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
}
