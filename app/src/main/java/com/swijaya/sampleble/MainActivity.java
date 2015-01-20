package com.swijaya.sampleble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.nio.charset.Charset;


public class MainActivity extends Activity {

    private static final String TAG = "SampleBLE";

    private static final int REQUEST_ENABLE_BT = 1;

    private static final int DEFAULT_ADVERTISE_TIMEOUT = 10 * 1000;
    private static final ParcelUuid SAMPLE_UUID =
            ParcelUuid.fromString("0000FE00-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private AdvertiseSettings.Builder mBleAdvertiseSettingsBuilder;
    private AdvertiseData.Builder mBleAdvertiseDataBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
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

        // check for peripheral mode support
        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            Toast.makeText(this, R.string.ble_peripheral_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // all clear!
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        assert (mBluetoothLeAdvertiser != null);

        mBleAdvertiseSettingsBuilder = new AdvertiseSettings.Builder()
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTimeout(DEFAULT_ADVERTISE_TIMEOUT)
                .setConnectable(false);

        mBleAdvertiseDataBuilder = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(true);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // stop ongoing advertising, if any
        stopAdvertising();
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startAdvertising() {
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

}
