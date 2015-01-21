package com.swijaya.sampleble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {

    private static final String TAG = "SampleBLE";

    private static final int REQUEST_ENABLE_BT = 1;

    private static final int DEFAULT_ADVERTISE_TIMEOUT = 10 * 1000;
    private static final ParcelUuid SAMPLE_UUID =
            ParcelUuid.fromString("0000FE00-0000-1000-8000-00805F9B34FB");

    private static final long DEFAULT_SCAN_PERIOD = 25 * 1000;

    private BluetoothAdapter mBluetoothAdapter;

    // helper objects for BLE advertising, derived from mBluetoothAdapter above
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private AdvertiseSettings.Builder mBleAdvertiseSettingsBuilder;
    private AdvertiseData.Builder mBleAdvertiseDataBuilder;

    // helper objects for BLE scanning, derived from mBluetoothAdapter above
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanSettings.Builder mBleScanSettingsBuilder;

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mHandler = new Handler();

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // check for peripheral mode support
        if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
            assert (mBluetoothLeAdvertiser != null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
            finish();
            return;
        }

        // instantiate BLE advertising helper objects
        if (mBluetoothLeAdvertiser != null) {
            mBleAdvertiseSettingsBuilder = new AdvertiseSettings.Builder()
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setTimeout(DEFAULT_ADVERTISE_TIMEOUT)
                    .setConnectable(false);
            mBleAdvertiseDataBuilder = new AdvertiseData.Builder()
                    .setIncludeDeviceName(true)
                    .setIncludeTxPowerLevel(true);
        }

        // instantiate BLE scanner helper objects
        if (mBluetoothLeScanner != null) {
            mBleScanSettingsBuilder = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // stop ongoing advertising/scanning, if any
        stopAdvertising();
        stopScanning();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // TODO
                break;
            default:
                // TODO
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                return true;
            case R.id.action_advertise:
                startAdvertising();
                return true;
            case R.id.action_scan:
                startScanning();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startAdvertising() {
        if (mBluetoothLeAdvertiser == null) {
            Toast.makeText(this, R.string.ble_peripheral_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] serviceData = "HELLO".getBytes(Charset.forName("US-ASCII"));

        AdvertiseSettings advertiseSettings = mBleAdvertiseSettingsBuilder.build();
        AdvertiseData advertiseData = mBleAdvertiseDataBuilder
                .addServiceUuid(SAMPLE_UUID)
                .addServiceData(SAMPLE_UUID, serviceData)
                .build();

        Log.d(TAG, "Starting advertising with settings:" + advertiseSettings + " and data:" + advertiseData);

        // the default settings already put a time limit of 10 seconds, so there's no need to schedule
        // a task to stop it
        mBluetoothLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, mBleAdvertiseCallback);
    }

    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser != null) {
            Log.d(TAG, "Stop advertising.");
            mBluetoothLeAdvertiser.stopAdvertising(mBleAdvertiseCallback);
        }
    }

    private void startScanning() {
        assert (mBluetoothLeScanner != null);
        assert (mBleScanSettingsBuilder != null);

        // add a filter to only scan for advertisers with the given service UUID
        List<ScanFilter> bleScanFilters = new ArrayList<>();
        bleScanFilters.add(
                new ScanFilter.Builder().setServiceUuid(SAMPLE_UUID).build()
        );

        ScanSettings bleScanSettings = mBleScanSettingsBuilder.build();

        Log.d(TAG, "Starting scanning with settings:" + bleScanSettings + " and filters:" + bleScanFilters);

        // tell the BLE controller to initiate scan
        mBluetoothLeScanner.startScan(bleScanFilters, bleScanSettings, mBleScanCallback);

        // post a future task to stop scanning after (default:25s)
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScanning();
            }
        }, DEFAULT_SCAN_PERIOD);
    }

    private void stopScanning() {
        if (mBluetoothLeScanner != null) {
            Log.d(TAG, "Stop scanning.");
            mBluetoothLeScanner.stopScan(mBleScanCallback);
        }
    }

    private void processScanResult(ScanResult result) {
        Log.d(TAG, "processScanResult: " + result);

        BluetoothDevice device = result.getDevice();
        Log.d(TAG, "Device name: " + device.getName());
        Log.d(TAG, "Device address: " + device.getAddress());
        Log.d(TAG, "Device service UUIDs: " + device.getUuids());

        ScanRecord record = result.getScanRecord();
        Log.d(TAG, "Record advertise flags: 0x" + Integer.toHexString(record.getAdvertiseFlags()));
        Log.d(TAG, "Record Tx power level: " + record.getTxPowerLevel());
        Log.d(TAG, "Record device name: " + record.getDeviceName());
        Log.d(TAG, "Record service UUIDs: " + record.getServiceUuids());
        Log.d(TAG, "Record service data: " + record.getServiceData());
    }

    private final AdvertiseCallback mBleAdvertiseCallback = new AdvertiseCallback() {

        private static final String TAG = "SampleBLE.AdvertiseCallback";

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(TAG, "onStartSuccess: " + settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode) {
            String description;
            switch (errorCode) {
                case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                    description = "ADVERTISE_FAILED_ALREADY_STARTED";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                    description = "ADVERTISE_FAILED_DATA_TOO_LARGE";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    description = "ADVERTISE_FAILED_FEATURE_UNSUPPORTED";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                    description = "ADVERTISE_FAILED_INTERNAL_ERROR";
                    break;
                case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    description = "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS";
                    break;
                default:
                    description = "Unknown error code " + errorCode;
                    break;
            }
            Log.e(TAG, "onStartFailure: " + description);
        }
    };

    private final ScanCallback mBleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            processScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                processScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            String description;
            switch (errorCode) {
                case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                    description = "SCAN_FAILED_ALREADY_STARTED";
                    break;
                case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    description = "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED";
                    break;
                case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                    description = "SCAN_FAILED_FEATURE_UNSUPPORTED";
                    break;
                case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                    description = "SCAN_FAILED_INTERNAL_ERROR";
                    break;
                default:
                    description = "Unknown error code " + errorCode;
                    break;
            }
            Log.e(TAG, "onScanFailed: " + description);
        }
    };

}
