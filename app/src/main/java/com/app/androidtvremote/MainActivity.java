package com.app.androidtvremote;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.companion.AssociationRequest;
import android.companion.BluetoothDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "AndroidTVRemote";
    private static final int REQUEST_CODE_BT_DEVICE_SELECTED = 1;
    static final int MESSAGE_FROM_SCAN_THREAD = 4;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    static Vibrator vibrator;
    private BluetoothLeScanner bluetoothLeScanner;
    private SwitchMaterial swtConnect;
    private SwitchMaterial swtConnectMouse;
    private TextView txtOut;
    private EditText txtInput;
    private Spinner cmbBondedDevices;
    protected static Handler handlerUi;
    private List<Button> buttons;
    private ActivityResultLauncher<Intent> launcherEnableBluetooth;
    private SensorManager sensorManager;
    private Sensor sensorGyroscope;
    private Button btnCurLeft;
    private Button btnCurClick;
    private Button btnCurRight;
    private SeekBar seekBar;
    private TextView seekBarLabel;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        MenuItem item = menu.findItem(R.id.item_connect);
        item.setActionView(R.layout.switch_item);

        swtConnect = item.getActionView().findViewById(R.id.swtConnect);
        swtConnect.setOnClickListener(this::connectSwitchAction);
        return true;
    }


    @Override
    @SuppressLint({"MissingPermission", "ClickableViewAccessibility"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        vibrator = getSystemService(Vibrator.class);
        launcherEnableBluetooth = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), activityResult -> {
            if (activityResult.getResultCode() == -1) {
                populateBondedDevices();
            } else {
                Toast.makeText(MainActivity.this, "Bluetooth not enabled, exiting now.", Toast.LENGTH_LONG).show();
                finish();
            }
        });

        btnCurLeft = findViewById(R.id.cur_left);
        btnCurClick = findViewById(R.id.cur_middle);
        btnCurRight = findViewById(R.id.cur_right);
        swtConnectMouse = findViewById(R.id.swtConnectMouse);
        swtConnectMouse.setOnClickListener(this::connectMouseAction);
        txtOut = findViewById(R.id.txtOut);
        txtInput = findViewById(R.id.txtInput);
        txtInput.setEnabled(false);

        seekBar=findViewById(R.id.seekBar);
        seekBar.setMin(1);
        seekBar.setMax(60);
        seekBar.setProgress(30);
        seekBarLabel=findViewById(R.id.seekBarLabel);

        assignButtonActions();

    }

    private void createUIHandler() {
        if (handlerUi == null) {
            handlerUi = new Handler(getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    if (msg.what == BluetoothHidService.WHAT.BLUETOOTH_DISCONNECTED) {
                        swtConnect.setChecked(false);
                        swtConnect.setEnabled(true);
                        setButtonsEnabled(false);
                        txtInput.setEnabled(false);
                    } else if (msg.what == BluetoothHidService.WHAT.BLUETOOTH_CONNECTING) {
                        swtConnect.setChecked(true);
                        swtConnect.setEnabled(false);
                    } else if (msg.what == BluetoothHidService.WHAT.BLUETOOTH_CONNECTED) {
                        swtConnect.setChecked(true);
                        swtConnect.setEnabled(true);
                        setButtonsEnabled(true);
                        txtInput.setEnabled(true);
                    }
                }
            };
        }
    }

    private void connectMouseAction(View v) {
        if (swtConnectMouse.isChecked()) {
            startGyroscope();
        } else {
            stopGyroscope();
        }
    }

    private void startGyroscope() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (sensorGyroscope != null) {
            sensorManager.registerListener(this, sensorGyroscope, 10000);
            btnCurLeft.setVisibility(View.VISIBLE);
            btnCurRight.setVisibility(View.VISIBLE);
            seekBar.setVisibility(View.VISIBLE);
            seekBarLabel.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, "No Gyroscope sensor!", Toast.LENGTH_LONG).show();
            swtConnectMouse.setChecked(false);
        }
    }

    private void stopGyroscope() {
        if (sensorManager != null)
            sensorManager.unregisterListener(this);
        btnCurLeft.setVisibility(View.INVISIBLE);
        btnCurRight.setVisibility(View.INVISIBLE);
        seekBar.setVisibility(View.INVISIBLE);
        seekBarLabel.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (BluetoothHidService.isHidDeviceConnected && sensorGyroscope != null) {
            MouseHelper.sendData(false, false, false, Math.round(sensorEvent.values[2] * seekBar.getProgress()) * -1,
                    Math.round(sensorEvent.values[0] * seekBar.getProgress()) * -1, 0);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private void setButtonsEnabled(boolean enabled) {
        for (Button button : buttons) {
            button.setEnabled(enabled);
        }
    }

    private void startService() {
        Intent serviceIntent = new Intent(this, BluetoothHidService.class);
        createUIHandler();
        BluetoothHidService.bluetoothDevice = getSelectedBluetoothDevice();
        startForegroundService(serviceIntent);
    }

    private void stopService() {
        Intent serviceIntent = new Intent(this, BluetoothHidService.class);
        stopService(serviceIntent);
    }

    private void pairBtnAction(View v) {
        startBluetoothLEAdvertise();
    }

    private void connectSwitchAction(View v) {
        if (swtConnect.isChecked()) {
            startService();
        } else {
            stopService();
            swtConnectMouse.setChecked(false);
            stopGyroscope();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (swtConnect != null)
            swtConnect.setChecked(BluetoothHidService.isRunning);

        if (!bluetoothAdapter.isEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
            }
            launcherEnableBluetooth.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        } else {
            populateBondedDevices();
        }

        if (swtConnectMouse.isChecked()) {
            startGyroscope();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cmbBondedDevices != null)
            getSharedPreferences().edit().putInt("selectedBluetoothDevice", cmbBondedDevices.getSelectedItemPosition()).apply();
        stopGyroscope();
    }

    private void debug(String msg) {
        Log.e(TAG, "------------------------- " + msg);
        txtOut.setText(msg + "\n" + txtOut.getText());
    }

    private void populateBondedDevices() {
        cmbBondedDevices = findViewById(R.id.cmbBondedDevices);
        List<BondedDevice> spinnerArray = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if (bondedDevices.size() > 0) {
            for (BluetoothDevice device : bondedDevices) {
                spinnerArray.add(new BondedDevice(device));
            }
        }
        ArrayAdapter<BondedDevice> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerArray);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cmbBondedDevices.setAdapter(adapter);

        if (cmbBondedDevices.getCount() > getSharedPreferences().getInt("selectedBluetoothDevice", 0)) {
            cmbBondedDevices.setSelection(getSharedPreferences().getInt("selectedBluetoothDevice", 0));
        }
    }

    private BluetoothDevice getSelectedBluetoothDevice() {
        return ((BondedDevice) cmbBondedDevices.getSelectedItem()).bluetoothDevice;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cmbBondedDevices != null)
            getSharedPreferences().edit().putInt("selectedBluetoothDevice", cmbBondedDevices.getSelectedItemPosition()).apply();
    }

    private SharedPreferences getSharedPreferences() {
        return this.getPreferences(Context.MODE_PRIVATE);
    }

    private void startBluetoothLEAdvertise() {
        AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                debug("AdvertiseCallback onStartSuccess");
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                debug("AdvertiseCallback onStartFailure");
            }
        };
        BluetoothLeAdvertiser bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .setTimeout(0)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .build();

        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(Constants.HOGP_UUID)
                .build();

        AdvertiseData scanResult = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_ADVERTISE}, 1);
        }
        bluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, scanResult, advertiseCallback);
    }

    @SuppressLint("MissingPermission")
    private void startBluetoothLEScan() {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        ScanCallback leScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                debug("onScanResult " + result.getDevice().getAddress() + " " + result.getDevice().getName());
                ScanCallback scanCallbackStopped = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        super.onScanResult(callbackType, result);
                        debug("scanCallbackStopped onScanResult");
                    }
                };
                bluetoothLeScanner.stopScan(scanCallbackStopped);
                result.getDevice().createBond();
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                debug("onScanFailed " + errorCode);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                debug("onBatchScanResults " + results);
            }
        };

        List<ScanFilter> scanFilters = new ArrayList<>();
        ScanFilter scanFilter = new ScanFilter.Builder()
                .setDeviceAddress("FD:E9:85:78:D6:D7")
                .build();
        scanFilters.add(scanFilter);

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
        bluetoothLeScanner.startScan(null, scanSettings, leScanCallback);

    }

    @SuppressLint("MissingPermission")
    private void startBluetoothDiscovery() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(getBroadcastReceiver(), intentFilter);
        boolean discoveryStarted = bluetoothAdapter.startDiscovery();
        debug("discoveryStarted=" + discoveryStarted);
    }

    @SuppressLint("MissingPermission")
    private BroadcastReceiver getBroadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            @SuppressLint("MissingPermission")
            public void onReceive(Context context, Intent intent) {
                debug("onReceive " + intent.getAction());
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress();
                    debug(deviceName + ", " + deviceHardwareAddress);

                    if (deviceName.equals("CHAINWAY R6")) {
                        device.createBond();
                    }
                }
                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                    if (bondState == BluetoothDevice.BOND_BONDED) {
                        String deviceName = device.getName();
                        String deviceHardwareAddress = device.getAddress();
                        debug("Bonding completed with " + deviceName + ", " + deviceHardwareAddress);
                        populateBondedDevices();
                    }
                }
            }
        };
    }

    @SuppressLint("MissingPermission")
    private void connectGATT() {

        BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    debug("successfully connected to the GATT Server");
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    debug("disconnected from the GATT Server");
                }
            }
        };
        getSelectedBluetoothDevice().connectGatt(this, false, bluetoothGattCallback);

    }

    @SuppressLint("MissingPermission")
    private void openGattServer() {
        BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
            @Override
            public void onServiceAdded(int status, BluetoothGattService service) {
                super.onServiceAdded(status, service);
                debug("onServiceAdded");
            }

            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                super.onConnectionStateChange(device, status, newState);
                debug("onConnectionStateChange " + device.getName());
            }
        };

        bluetoothManager.openGattServer(this, gattServerCallback);
    }

    @SuppressLint("MissingPermission")
    private void companionPair() {
        CompanionDeviceManager companionDeviceManager = getSystemService(CompanionDeviceManager.class);

        BluetoothDeviceFilter bluetoothDeviceFilter = new BluetoothDeviceFilter.Builder()
                .build();

        AssociationRequest pairingRequest = new AssociationRequest.Builder()
                .addDeviceFilter(bluetoothDeviceFilter)
                .build();

        CompanionDeviceManager.Callback callback = new CompanionDeviceManager.Callback() {
            @Override
            public void onDeviceFound(IntentSender chooserLauncher) {
                debug("onDeviceFound");
                try {
                    startIntentSenderForResult(chooserLauncher, REQUEST_CODE_BT_DEVICE_SELECTED, null, 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    debug("onDeviceFound SendIntentException");
                }
            }

            @Override
            public void onFailure(CharSequence error) {
                debug("onFailure");
            }
        };
        companionDeviceManager.associate(pairingRequest, callback, null);


    }


    @SuppressLint({"WrongConstant", "ClickableViewAccessibility"})
    private void assignButtonActions() {
        Button btnPower = findViewById(R.id.btnPower);
        Button btnMenu = findViewById(R.id.btnMenu);
        Button btnPair = findViewById(R.id.btnPair);
        btnPair.setOnClickListener(this::pairBtnAction);
        Button btnLeft = findViewById(R.id.btnLeft);
        Button btnRight = findViewById(R.id.btnRight);
        Button btnUp = findViewById(R.id.btnUp);
        Button btnDown = findViewById(R.id.btnDown);
        Button btnMiddle = findViewById(R.id.btnMiddle);
        Button btnBack = findViewById(R.id.btnBack);
        Button btnHome = findViewById(R.id.btnHome);
        Button btnVolInc = findViewById(R.id.btnVolInc);
        Button btnVolDec = findViewById(R.id.btnVolDec);
        Button btnMute = findViewById(R.id.btnMute);
        Button btnPlayPause = findViewById(R.id.btnPlayPause);
        Button btnRewind = findViewById(R.id.btnRewind);
        Button btnForward = findViewById(R.id.btnForward);

        buttons = new ArrayList<>();
        buttons.add(btnLeft);
        buttons.add(btnRight);
        buttons.add(btnUp);
        buttons.add(btnDown);
        buttons.add(btnMiddle);
        buttons.add(btnHome);
        buttons.add(btnBack);
        buttons.add(btnVolDec);
        buttons.add(btnVolInc);
        buttons.add(btnPlayPause);
        buttons.add(btnPower);
        buttons.add(btnMenu);
        buttons.add(btnMute);
        buttons.add(btnRewind);
        buttons.add(btnForward);
        buttons.add(swtConnectMouse);

        buttons.add(btnCurLeft);
        buttons.add(btnCurClick);
        buttons.add(btnCurRight);
        btnCurLeft.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                boolean sent = MouseHelper.sendData(true, false, false, 0, 0, 0);
                if (sent)
                    vibrate();
            }
            return false;
        });
        btnCurRight.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                boolean sent = MouseHelper.sendData(false, true, false, 0, 0, 0);
                if (sent)
                    vibrate();
            }
            return false;
        });


        setButtonsEnabled(BluetoothHidService.isRunning);
        addRemoteKeyListeners(btnPower, RemoteControlHelper.Key.POWER);
        addRemoteKeyListeners(btnMenu, RemoteControlHelper.Key.MENU);
        addRemoteKeyListeners(btnLeft, RemoteControlHelper.Key.MENU_LEFT);
        addRemoteKeyListeners(btnRight, RemoteControlHelper.Key.MENU_RIGHT);
        addRemoteKeyListeners(btnUp, RemoteControlHelper.Key.MENU_UP);
        addRemoteKeyListeners(btnDown, RemoteControlHelper.Key.MENU_DOWN);
        addRemoteKeyListeners(btnMiddle, RemoteControlHelper.Key.MENU_PICK);
        addRemoteKeyListeners(btnBack, RemoteControlHelper.Key.BACK);
        addRemoteKeyListeners(btnHome, RemoteControlHelper.Key.HOME);
        addRemoteKeyListeners(btnVolInc, RemoteControlHelper.Key.VOLUME_INC);
        addRemoteKeyListeners(btnVolDec, RemoteControlHelper.Key.VOLUME_DEC);
        addRemoteKeyListeners(btnMute, RemoteControlHelper.Key.MUTE);
        addRemoteKeyListeners(btnPlayPause, RemoteControlHelper.Key.PLAY_PAUSE);
        addRemoteKeyListeners(btnRewind, RemoteControlHelper.Key.MEDIA_REWIND);
        addRemoteKeyListeners(btnForward, RemoteControlHelper.Key.MEDIA_FAST_FORWARD);


        txtInput.setOnKeyListener(this::handleInputText);
//
    }

    private boolean handleInputText(View view, int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_UP) {
            txtInput.getText().chars().forEach(c -> {
                if (KeyboardHelper.keyMap.containsKey((char) c)) {
                    KeyboardHelper.sendKeyDown(KeyboardHelper.Modifier.NONE, KeyboardHelper.getKey((char) c));
                    KeyboardHelper.sendKeyUp();
                } else if (KeyboardHelper.shiftKeyMap.containsKey((char) c)) {
                    KeyboardHelper.sendKeyDown(KeyboardHelper.Modifier.KEY_MOD_LSHIFT, KeyboardHelper.getShiftKey((char) c));
                    KeyboardHelper.sendKeyUp();
                }
            });
            vibrate();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DEL && keyEvent.getAction() == KeyEvent.ACTION_UP) {
            boolean sent = KeyboardHelper.sendKeyDown(KeyboardHelper.Modifier.NONE, KeyboardHelper.Key.BACKSPACE);
            KeyboardHelper.sendKeyUp();
            if (sent)
                vibrate();
            return true;
        }
        return false;
    }

    private TextWatcher getKeyTextWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean sent = false;
                if (before > count) {
                    sent = KeyboardHelper.sendKeyDown(KeyboardHelper.Modifier.NONE, KeyboardHelper.Key.BACKSPACE);
                    KeyboardHelper.sendKeyUp();
                } else if (start + count > 0) {
                    char c = s.charAt(s.length() - 1);
                    if (KeyboardHelper.keyMap.containsKey(c)) {
                        sent = KeyboardHelper.sendKeyDown(KeyboardHelper.Modifier.NONE, KeyboardHelper.getKey(c));
                        KeyboardHelper.sendKeyUp();
                    } else if (KeyboardHelper.shiftKeyMap.containsKey(c)) {// Upper case letter
                        sent = KeyboardHelper.sendKeyDown(KeyboardHelper.Modifier.KEY_MOD_LSHIFT, KeyboardHelper.getShiftKey(c));
                        KeyboardHelper.sendKeyUp();
                    }
                }
                if (sent)
                    vibrate();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
    }

    private boolean handleRealtimeInputText(View view, int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.getAction() == KeyEvent.ACTION_UP) {
            boolean sent = KeyboardHelper.sendKeyDown(KeyboardHelper.Modifier.NONE, KeyboardHelper.Key.ENTER);
            KeyboardHelper.sendKeyUp();
            if (sent)
                vibrate();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DEL && keyEvent.getAction() == KeyEvent.ACTION_UP) {
            boolean sent = KeyboardHelper.sendKeyDown(KeyboardHelper.Modifier.NONE, KeyboardHelper.Key.BACKSPACE);
            KeyboardHelper.sendKeyUp();
            if (sent)
                vibrate();
            return true;
        }
        return false;
    }


    @SuppressLint("ClickableViewAccessibility")
    private void addKeyBoardListeners(Button button, int... keys) {

        int modifier;
        int key;
        if (keys.length > 1) {
            modifier = keys[0];
            key = keys[1];
        } else {
            modifier = 0;
            key = keys[0];
        }

        button.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                boolean sent = KeyboardHelper.sendKeyDown(modifier, key);
                if (sent)
                    vibrate();
            }
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                boolean sent = KeyboardHelper.sendKeyUp();
            }
            return false;
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addRemoteKeyListeners(Button button, byte... keys) {
        button.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                boolean sent = RemoteControlHelper.sendKeyDown(keys[0], keys[1]);
                if (sent)
                    vibrate();
            }
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                boolean sent = RemoteControlHelper.sendKeyUp();
            }
            return false;
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addRemoteKeysListeners(Button button, byte[] key1, byte[] key2) {
        button.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                boolean sent = RemoteControlHelper.sendKeyDown(key1[0], key1[1]);
                sent = RemoteControlHelper.sendKeyUp();

                sent = RemoteControlHelper.sendKeyDown(key2[0], key2[1]);
                sent = RemoteControlHelper.sendKeyUp();
                if (sent)
                    vibrate();
            }
            return false;
        });
    }

    static void vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private void testNotification() {

        Intent intent = new Intent(this, NotificationBroadcastReceiver.class);
        intent.setAction("Play/Pause");

        PendingIntent pi = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_buttons);
        remoteViews.setOnClickPendingIntent(R.id.btnPower, pi);

        String CHANNEL_ID = "Bluetooth Remote Service";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Bluetooth Remote Service", NotificationManager.IMPORTANCE_MIN);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification notification =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setContentTitle("Bluetooth Remote")
                        .setContentText("Test")
                        .setSmallIcon(R.drawable.remote_control)
                        .setContentIntent(pendingIntent)
                        .setOngoing(true)
                        .setCustomBigContentView(remoteViews)
                        .build();

        getSystemService(NotificationManager.class).notify(1, notification);
    }

}
