package me.atanasov.rgblights;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.larswerkman.holocolorpicker.ColorPicker;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class MainActivity extends AppCompatActivity {
    private static final String DEVICE_FIELD = "device";
    BluetoothSPP bt;
    TextView status;
    SharedPreferences sharedPref;
    ColorPicker picker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        status = (TextView) findViewById(R.id.status);
        picker = (ColorPicker) findViewById(R.id.picker);

        picker.setOnColorSelectedListener(new ColorPicker.OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color) {
                picker.setOldCenterColor(picker.getColor());

                writeBT(String.format("%d,%d,%d",
                        Color.red(color),
                        Color.green(color),
                        Color.blue(color)));
            }
        });

        bt = new BluetoothSPP(this);

        if (!bt.isServiceAvailable()) {
            bt.setupService();
            bt.startService(BluetoothState.DEVICE_OTHER);
        }

        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            @Override
            public void onDataReceived(byte[] data, String message) {

                String[] colors = new String(data).split(",");

                if (colors.length > 2)
                    status.setText(String.format("R: %s, G: %s, B: %s", colors[0], colors[1], colors[2]));

//                ActionBar actionBar = getSupportActionBar();
//                if (actionBar != null) {
//                    actionBar.setBackgroundDrawable(new ColorDrawable(Color.rgb(Integer.valueOf(colors[0]),
//                            Integer.valueOf(colors[1]),
//                            Integer.valueOf(colors[2])
//                    )));
//                }
            }
        });

        findViewById(R.id.set_color).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeBT("0,0,0");
            }
        });

        findViewById(R.id.auto).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeBT("a");
            }
        });

        findViewById(R.id.read).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                writeBT("r");
            }
        });

        findViewById(R.id.reconnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connect();
            }
        });

        sharedPref = getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);


    }

    private void writeBT(String dataToSend) {
        Log.d("bt_send", dataToSend);
        bt.send(dataToSend.getBytes(), false);
    }

    private void connect() {
        String device = sharedPref.getString(DEVICE_FIELD, "");
        bt.connect(device);
    }

    @Override
    protected void onStart() {
        super.onStart();

        String device = sharedPref.getString(DEVICE_FIELD, "");
        if (device.isEmpty()) {
            if (!bt.isBluetoothEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
            } else {
                Intent intent = new Intent(getApplicationContext(), DeviceList.class);
                startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
            }
        } else if (bt.isServiceAvailable()) {
            connect();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        bt.stopService();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE && data != null) {
            if (resultCode == Activity.RESULT_OK) {
                bt.connect(data);
                sharedPref.edit().putString(DEVICE_FIELD, data.getExtras().getString(BluetoothState.EXTRA_DEVICE_ADDRESS)).apply();
            }

        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
//                setup();
            } else {
// Do something if user doesn't choose any device (Pressed back)
            }
        }

    }

}
