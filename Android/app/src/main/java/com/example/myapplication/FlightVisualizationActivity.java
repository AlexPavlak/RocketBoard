package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class FlightVisualizationActivity extends AppCompatActivity {
    LineChart flightPath;
    String line;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_visualization);


        Intent intent = getIntent();
        String fn = intent.getStringExtra("FileName");


        File fileName = new File(getFilesDir() + "/"+fn);


        flightPath = findViewById(R.id.flightPath);
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<DataPacket> packets = new ArrayList<>();

        try{
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            line = reader.readLine();
            Log.d("FileLine", line);

            while(line != null) {
                DataPacket dataPacket = new DataPacket(line);
                Log.d("FileLine", dataPacket.toString());
                packets.add(dataPacket);
                line = reader.readLine();
            }
        } catch(java.io.IOException e) {
            e.printStackTrace();
        }

        //Make packet of each line, stash them somehow like in an array, use the getters
        float x = 0, y;
        for(int i = 0; i < packets.size(); i++) {
            y = packets.get(i).getSpeed();
            entries.add(new Entry(x, y));
            Log.d("Entries", entries.get(i).toString());
            x += 10;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Altitude vs. Time");
        dataSet.setColor((ContextCompat.getColor(this, R.color.colorPrimary)));
        dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));

        XAxis xAxis = flightPath.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        YAxis yAxisRight = flightPath.getAxisRight();
        yAxisRight.setEnabled(false);
        YAxis yAxisLeft = flightPath.getAxisLeft();
        yAxisLeft.setGranularity(1f);

        LineData data = new LineData(dataSet);
        flightPath.setData(data);
    }
}
