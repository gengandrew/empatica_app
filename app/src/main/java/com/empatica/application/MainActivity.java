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
import retrofit2.Call;
import retrofit2.Callback;
import java.lang.Math;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.empatica.application.retrofit.IBackend;
import com.empatica.application.retrofit.CallResponse;
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
    private EmpaDeviceManager deviceManager = null;

    private Integer participantID = null;
    private Integer sessionID = null;
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
    private TextView sessionLabel;
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
        sessionLabel = findViewById(R.id.session_label);

        final Button disconnectButton = findViewById(R.id.disconnectButton);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deviceManager != null) {
                    deviceManager.stopScanning();
                    deviceManager.disconnect();
                    deviceManager.cleanUp();
                    deviceManager = null;
                }
                initDeviceManager();
            }
        });

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("Please Insert Participant Number");
        alertBuilder.setCancelable(false);
        final EditText participantIdInput = new EditText(this);
        participantIdInput.setInputType(InputType.TYPE_CLASS_TEXT);
        alertBuilder.setView(participantIdInput);
        alertBuilder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                participantID = Integer.parseInt(participantIdInput.getText().toString());
                InsertAssociation(participantID);
            }
        });
        alertBuilder.show();

        initDeviceManager();
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
    public void didReceiveTag(double timestamp) {
        Log.d("CustomDebug", "Tag is [" + timestamp + "]");
    }

    @Override
    public void didReceiveBatteryLevel(float battery, double timestamp) {
        Log.d("CustomDebug", "Battery is [" + battery + "]");
    }

    @Override
    public void didReceiveBVP(float bvp, double timestamp) {
        this.bvp = bvp;
        if(checkDataPostConditions()) {
            InsertData(this.sessionID, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
                    timestamp, this.bvp, this.eda, this.ibi, this.heartRate, this.temperature);
            Log.d("CustomDebug", "BVP of [" + bvp + "] has been pushed");
        }
        updateLabel(bvpLabel, Float.toString(bvp));
    }

    @Override
    public void didReceiveGSR(float gsr, double timestamp) {
        this.eda = gsr;
        if(checkDataPostConditions()) {
            InsertData(this.sessionID, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
                    timestamp, this.bvp, this.eda, this.ibi, this.heartRate, this.temperature);
            Log.d("CustomDebug", "GSR of [" + gsr + "] has been pushed");
        }
        updateLabel(edaLabel, Float.toString(gsr));
    }

    @Override
    public void didReceiveIBI(float ibi, double timestamp) {
        this.ibi = ibi;
        this.heartRate = 60/ibi;
        if(checkDataPostConditions()) {
            InsertData(this.sessionID, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
                    timestamp, this.bvp, this.eda, this.ibi, this.heartRate, this.temperature);
            Log.d("CustomDebug", "IBI of [" + ibi + "] has been pushed");
        }
        updateLabel(ibiLabel, Float.toString(ibi));
        updateLabel(heartLabel, Float.toString(heartRate));
    }

    @Override
    public void didReceiveTemperature(float temp, double timestamp) {
        this.temperature = temp;
        if(checkDataPostConditions()) {
            InsertData(this.sessionID, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
                    timestamp, this.bvp, this.eda, this.ibi, this.heartRate, this.temperature);
            Log.d("CustomDebug", "Temperature of [" + temp + "] has been pushed");
        }
        updateLabel(temperatureLabel, Float.toString(temp));
    }

    @Override
    public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
        if(checkAccelPostConditions(x, y, z)) {
            InsertAcceleration(this.sessionID, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()),
                    timestamp, x, y, z);
            Log.d("CustomDebug", "Acceleration of [" + x + " " + y + " " + z + "] has been pushed");
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
        if(sessionID != null && bvp != null && eda != null && ibi != null
                && heartRate != null && temperature != null) {
            return true;
        }
        return false;
    }

    private boolean checkAccelPostConditions(int x, int y, int z) {
        if(this.sessionID == null) {
            return false;
        } else if(this.accelX == null || this.accelY == null || this.accelZ == null) {
            return true;
        } else if(Math.abs(this.accelX - x) > ACCEL_DEVIATION || Math.abs(this.accelY - y) > ACCEL_DEVIATION
                || Math.abs(this.accelZ - z) > ACCEL_DEVIATION) {
            return true;
        } else {
            return false;
        }
    }

    private void InsertAssociation(int value) {
        IBackend backendService = RetrofitClient.getService();
        Call<CallResponse> call = backendService.InsertAssociation(value);
        call.enqueue(new Callback<CallResponse>() {
            @Override
            public void onResponse(Call<CallResponse> call, retrofit2.Response<CallResponse> response) {
                Log.d("CustomDebug", "Going to on Response");
                if(response.isSuccessful()) {
                    sessionID = response.body().getSessionID();
                    updateLabel(sessionLabel, "Current session has ID of [" + sessionID.toString() + "]");
                    Log.d("CustomDebug", "Internal sessionID has been set as " + sessionID);
                } else {
                    Log.d("CustomDebug", "Going to on not successful response");
                    updateLabel(sessionLabel, "onResponse failure from InsertAssociation [please try again]");
                    Toast.makeText(MainActivity.this, "Failure to get response from InsertAssociation", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CallResponse> call, Throwable t) {
                Log.d("CustomDebug", "Going to on Failure");
                Log.d("CustomDebug", t.toString());
                updateLabel(sessionLabel, "onFailure from InsertAssociation [please try again]");
                Toast.makeText(MainActivity.this, "InsertAssociation returns to onFailure", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void InsertData(int sessionID, String utc, double e4Time, float bvp, float eda,
                            float ibi, float heartRate, float temperature) {
        IBackend backendService = RetrofitClient.getService();
        backendService.InsertData(sessionID, utc, e4Time, bvp, eda, ibi, heartRate, temperature)
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

    private void InsertAcceleration(int sessionID, String utc, double e4Time, float accelX,
                                    float accelY, float accelZ) {
        IBackend backendService = RetrofitClient.getService();
        backendService.InsertAcceleration(sessionID, utc, e4Time, accelX, accelY, accelZ)
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
                sessionLabel.setVisibility(View.VISIBLE);
                dataArea.setVisibility(View.VISIBLE);
            }
        });
    }

    void hide() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sessionLabel.setVisibility(View.INVISIBLE);
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
