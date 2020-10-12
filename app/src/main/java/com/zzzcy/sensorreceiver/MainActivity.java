package com.zzzcy.sensorreceiver;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity{

    private LineChart chart;
    private ArrayList<Entry> values = new ArrayList<>();
    private Button start,stop;
    private TextView temp,rate;
    private int heartrate;
    private float temperature;

    private int i=0;
    private boolean isRun = true;

    private static final int CHANGE_TEMP = 1;
    private static final int CHANGE_HEARTRATE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("人体健康综合管理系统");

//        final Timer timer = new Timer();
//        //模拟数据传入
//        final TimerTask task = new TimerTask(){
//            public void run(){
//                float val = (float)Math.random() * 200;
//                Log.i("MSG:", String.valueOf(val));
//                setData(i,val);
//                chart.invalidate();
//                i++;
//            }
//        };

        start = findViewById(R.id.start);
        stop = findViewById(R.id.stop);
        temp = findViewById(R.id.temperature);
        rate = findViewById(R.id.heartrate);

        start.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
//                timer.schedule(task, 0,100);
                try {
                    isRun = true;
                    wifitest();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isRun = false;
            }
        });

        chart = findViewById(R.id.chart1);
        chart.setViewPortOffsets(0, 0, 0, 0);
        chart.setBackgroundColor(Color.rgb(244, 115, 120));
        chart.getDescription().setEnabled(false);

        // enable touch gestures
        chart.setTouchEnabled(false);

        // enable scaling and dragging
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);

        // if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);
        chart.setMaxHighlightDistance(300);

        XAxis x = chart.getXAxis();
        x.setEnabled(false);

        YAxis y = chart.getAxisLeft();
        y.setLabelCount(6, false);
        y.setTextColor(Color.WHITE);
        y.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        y.setDrawGridLines(false);
        y.setAxisLineColor(Color.WHITE);

        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setEnabled(false);


    }

    private void setData(int i,float val) {
        values.add(new Entry(i, val));

        LineDataSet set1;

        if (chart.getData() != null && chart.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet) chart.getData().getDataSetByIndex(0);
            if(values.size()>50){
                values.remove(0);
            }
            set1.setValues(values);

            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
        } else {
            // create a dataset and give it a type
            set1 = new LineDataSet(values.subList(0,values.size()-1), "DataSet 1");
            set1.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            set1.setCubicIntensity(0.2f);
            set1.setDrawFilled(true);
            set1.setDrawCircles(false);
            set1.setLineWidth(1.8f);
            set1.setCircleRadius(4f);
            set1.setCircleColor(Color.WHITE);
            set1.setHighLightColor(Color.rgb(244, 117, 117));
            set1.setColor(Color.WHITE);
            set1.setFillColor(Color.WHITE);
            set1.setFillAlpha(100);
            set1.setDrawHorizontalHighlightIndicator(false);
            set1.setFillFormatter(new IFillFormatter() {
                @Override
                public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
                    return chart.getAxisLeft().getAxisMinimum();
                }
            });

            // create a data object with the data sets
            LineData data = new LineData(set1);
            data.setValueTextSize(9f);
            data.setDrawValues(false);

            // set data
            chart.setData(data);
        }
    }

    private void wifitest() throws IOException {
        String str_send = "Hello UDPserver";
        byte[] buf = new byte[1024];
        String ip = "192.168.4.1";
        final DatagramSocket ds = new DatagramSocket(9000);
        //定义用来发送数据的DatagramPacket实例
        final DatagramPacket dp_send= new DatagramPacket(str_send.getBytes(),str_send.length(),InetAddress.getByName(ip),8964);
        //定义用来接收数据的DatagramPacket实例
        final DatagramPacket dp_receive = new DatagramPacket(buf, 1024);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ds.send(dp_send);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while(isRun){
                    try {
                        ds.receive(dp_receive);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    String receivedData = new String(dp_receive.getData(),0,dp_receive.getLength());
                    Log.i("TAG:", receivedData);
                    String[] strArr = receivedData.split(";");
                    int ECG = Integer.parseInt(strArr[1]);
                    heartrate = Integer.parseInt(strArr[2]);
                    temperature = Float.parseFloat(strArr[3]);

                    setData(i,ECG);
                    chart.invalidate();
                    i++;

                    Message message = new Message();
                    message.what = CHANGE_TEMP;
                    handler.sendMessage(message);
                }
                ds.close();
            }
        }).start();

    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == CHANGE_TEMP) {
                temp.setText(Float.toString(temperature));
                rate.setText(Integer.toString(heartrate));
            }
        }
    };
}
