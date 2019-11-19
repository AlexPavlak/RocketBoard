package com.example.myapplication;


import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;

import java.io.File;
import java.io.FileOutputStream;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

//Bluetooth dependencies
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Set;
import java.util.Date;
import java.util.Calendar;
import java.util.UUID;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import static android.graphics.Color.GREEN;
import static android.graphics.Color.RED;

public class InFlightActivity extends AppCompatActivity {

    private RecyclerView.Adapter fAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<String> dataTypes = new ArrayList<>();
    private ArrayList<String> fData = new ArrayList<>();
    // Debugging
    private static final String TAG = "BluetoothChatService";

    //Text Fields
    private TextView m_accelView;
    private TextView m_velocityView;
    private TextView m_altitudeView;
    private TextView m_maxAltitudeView;
    private TextView m_maxVelocityView;
    private TextView m_BtStatusView;
    private TextView m_recordStatusView;

    private Button m_StartBtn;
    private Button m_StopBtn;


    boolean recording;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    float currentVelocity;
    float initialVelocity;
    float maxVelocity;
    float maxAltitude;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    private boolean connected = false;
    FileOutputStream outStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_flight);
        m_accelView = (TextView) findViewById(R.id.tv_Accel);
        m_velocityView = (TextView) findViewById(R.id.tv_velocity);
        m_altitudeView = (TextView) findViewById(R.id.tv_altitude);
        m_maxAltitudeView = (TextView) findViewById(R.id.tv_maxAltitude);
        m_maxVelocityView = (TextView) findViewById(R.id.tv_maxSPeed);
        m_BtStatusView = (TextView) findViewById(R.id.tv_BlueTooth);
        m_recordStatusView= (TextView) findViewById(R.id.tv_record);
        currentVelocity = 0;
        initialVelocity = 0;
        maxVelocity = 0;
        maxAltitude = 0;
        recording = false;

        m_StartBtn = (Button) findViewById(R.id.btn_record);
        m_StartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recording = true;
                m_recordStatusView.setText("Recording");
                m_recordStatusView.setTextColor(GREEN);
            }
        });

        m_StopBtn = (Button) findViewById(R.id.btn_stopRecord);
        m_StopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recording = false;
                m_recordStatusView.setText("Not Recording");
                m_recordStatusView.setTextColor(RED);
            }
        });

        getData();
        Button btButton = (Button)findViewById(R.id.BlueToothbtn);
        btButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    findBT();
                    openBT();
                }
                catch (IOException ex) { }
            }
        });

        Date currentTime = Calendar.getInstance().getTime();
        File file = new File(this.getFilesDir(), "Flight: " + currentTime.toString());

        try{
            outStream = openFileOutput(file.getName(), this.MODE_PRIVATE);

        } catch (Exception e){
            e.printStackTrace();
        }

    }

    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Log.d("BT","No bluetooth adapter available");
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                Log.d("BT", "Device List: " + device.getName());
                if(device.getName().equals("RNBT-8BE8"))
                {
                    mmDevice = device;
                    Log.d("BT","Bluetooth Device Found " + device.getName());
                    break;
                }
            }
        }else{
            Log.d("BT","No bluetooth devices...");
        }

    }

    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        Log.d("BT","Bluetooth Opened");
        m_BtStatusView.setTextColor(GREEN);
        m_BtStatusView.setText("Blue Tooth Connected");
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();

    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            Log.d("BT","Bytes available " + bytesAvailable);
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");


                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            Log.i("BTData", data);
                                            writeToFile(data);

                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }

                }
            }
        });

        workerThread.start();
    }



    private void getData() {

    }

    private void writeToFile(String data){

        if(recording) {
            DataPacket packet = new DataPacket(data);
           updateData(packet);
            try {
                outStream.write(packet.toString().getBytes());
                outStream.write("\n".getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }


    private void updateData(DataPacket packet) {
        m_altitudeView.setText("Altitude: " + packet.getAlt_altitude());

        currentVelocity = packet.getSpeed();
        m_velocityView.setText("Velocity: " + currentVelocity);
        if(currentVelocity > maxVelocity){
            maxVelocity = currentVelocity;
            m_maxVelocityView.setText("Max Velocity: " + maxVelocity);
        }
        initialVelocity = currentVelocity;

        if(packet.getAlt_altitude() > maxAltitude){
            maxAltitude = packet.getAlt_altitude();
            m_maxAltitudeView.setText("Max Altitude: " + maxAltitude);
        }

    }

    //go to map
    public void goToMaps(View view) {
        startActivity(new Intent(this, MapsActivity.class));
    }

}
