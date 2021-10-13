package com.example.ba3ba3i;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    public final String ACTION_USB_PERMISSION = "com.example.ensi.USB_PERMISSION";

    private float Vright, Vleft; //  m/s


    /**
     * update Vright and send it to the Arduino
     * @param Vright Velocity of the right model
     */
    public void setVright(float Vright) {
        Vright = Vright;
        send("R"+String.valueOf(Vright));

    }

    /**
     * update Vleft and send it to the Arduino
     * @param Vleft Velocity of the right model
     */
    public void setVleft(float Vleft) {
        Vleft = Vleft;
        send("L"+String.valueOf(Vleft));
    }


    private SensorManager mSensorManager;
    Sensor gyroscope;
    private UsbManager usbManager;
    private Logger logger = new Logger("main", this);

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);
        start();

        if(serialPort != null ){
            send("l");
            try {
                TimeUnit.MILLISECONDS.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            send("l");
        }

        gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }


    private static final float NS2S = 1.0f / 1000000000.0f;
    private float timestamp = 0;
    private float angularVelocity = 0f;
    private PID angularController = new PID(0.5,0,0);
    private float currentAngle = 0;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float alpha;
        float t = 0.5f;
        float speed = 0;
        if (gyroscope.equals(sensorEvent.sensor)) {

            if (timestamp != 0) {

                final float dT = (sensorEvent.timestamp - timestamp) * NS2S;

                // Integrate Angular Velocity to get Angle
                angularVelocity = (angularVelocity + (sensorEvent.values[0] - angularVelocity));
                currentAngle = currentAngle + (angularVelocity * dT);   // around X axis


                speed = (float) angularController.getOutput( (double) currentAngle ,
                        (double) 0);

                setVleft(-speed);
                setVright(speed);

            }
            timestamp = sensorEvent.timestamp;

        } else {
            throw new IllegalStateException("Unexpected value: " + sensorEvent.sensor);
        }



    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            data = new String(arg0, StandardCharsets.UTF_8);
            data.concat("/n");
            logger.log("Received : " + data);
        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            Log.i("Arduino", "Serial Connection Opened!\n");

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                start();
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                stop();

            }
        }
    };




    /**
     * Search for Connection and establish it if exist
     */
    public void start() {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2341)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                    Log.i("DEVICE", "no device connected");
                    //tvAppend(textView, "no device connected");
                }

                if (!keep)
                    break;
            }
        }else{
            Log.i("DEBUG", "No Device");
        }


    }

    /**
     * Stop Conenction with Arduino
     */
    public void stop() {
        serialPort.close();
    }


    /**
     * Send Data as a String to the Arduino
     * @param data Data to Send as String
     */
    public void send(String data) {
        if(serialPort != null)
        {
            serialPort.write(data.getBytes());
            Log.i("ARDUINO", data);
            logger.log("Sent : " + data);
        }else{
            Log.i("ARDUINO","Arduino Not found");
            logger.log("Arduino not found");
        }

    }


}