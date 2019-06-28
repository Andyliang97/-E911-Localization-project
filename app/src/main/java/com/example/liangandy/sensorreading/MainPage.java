package com.example.liangandy.sensorreading;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


import android.os.Environment;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainPage extends AppCompatActivity {

    /**
     * hardcoded file name
     */
    private String outfilename = "sensor_data.csv";

    /**
     * Permission code
     */
    private int STORAGE_PERMISSION_CODE = 1;
    private int FINE_LOCATION_PERMISSION_CODE = 1;
    private int COARSE_LOCATION_PERMISSION_CODE = 1;

    /**
     * Components: File, Sensor Manager, Timer
     */
    private File file;
    private SensorManager mSensorManager;
    private Timer timer = null; //timer for capture the sensor data

    /**
     * Text box for showing the sensor data
     */
    private TextView etGyro;
    private TextView etMagnetic;
    private TextView etLinearAcc;
    private TextView azimuth;
    private TextView pitch;
    private TextView roll;

    /**
     * EditText
     */
    private EditText url;
    private EditText ip; //The text box for input file name
    private EditText xcood;
    private EditText ycood;
    private EditText xygroupjson;

    /**
     * Button
     */
    private Button start;
    private Button stop;
    private Button httpconnect;
    private Button request_plot;
    private Button confirm;
    private Button xygroupconfirm;
    private float flag = 0;

    /**
     * TextView
     */
    private TextView jsondisplay;

    /**
     * ImageView
     */
    private ImageView floorplan;

    /**
     * Elements for choosing start point
     */
    private Spinner startpoint;
    private ArrayList<String> startpoint_list;
    private ArrayAdapter<String> startpoint_adapter;
    private String startpointChoice;

    /**
     * Array List for temporarily store sensor data
     */
    private ArrayList<Float> AccList = new ArrayList<Float>();
    private ArrayList<Float> GyrList = new ArrayList<Float>();
    private ArrayList<Float> MagList = new ArrayList<Float>();
    private ArrayList<Double> LongList = new ArrayList<Double>();
    private ArrayList<Double> LatList = new ArrayList<Double>();
    private ArrayList<Double> AltList = new ArrayList<Double>();
    private ArrayList<Float> AzimuthList = new ArrayList<Float>();
    private ArrayList<Float> PitchList = new ArrayList<Float>();
    private ArrayList<Float> RollList = new ArrayList<Float>();
    private ArrayList<String> timeStampList = new ArrayList<String>();

    /**
     * Element to capture sensor data
     */
    private float AccData[] = new float[3];
    private float GyrData[] = new float[3];
    private float MagData[] = new float[3];
    private float[] orientationAngles = new float[3];
    private double longtitude;
    private double altitude;
    private double latitude;
    private float Azimuth;
    private float Pitch;
    private float Roll;

    /**
     * Elements for checking permission and GPS providers
     */
    private LocationManager lm;
    private List<String> locationProviders;
    private boolean storagePermission;
    private boolean gpslocationPermission;
    private boolean netlocationPermission;
    private boolean hasLocationProvider;
    private String locationProvider;

    /**
     * Calibration data
     */
    private String CurrentPoint_X;
    private String CurrentPoint_Y;
    private Double theta0;

    /**
     * Bitmap for the changing floor plan
     */
    private Bitmap changedfloorplan;

    /**
     * MediaType
     */
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    /**
     * the initial setup is here
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainpage); //connect to the layout
        changedfloorplan = resizePhoto(R.drawable.durham);
        floorplan = findViewById(R.id.floorplan);
        xcood = findViewById(R.id.xcood);
        ycood = findViewById(R.id.ycood);
        confirm = findViewById(R.id.confirm);
        xygroupjson = findViewById(R.id.xygroupjson);
        xygroupconfirm = findViewById(R.id.xygroupconfirm);
        xygroupconfirm.setOnClickListener(myListner);
        startpoint = findViewById(R.id.startpoint);
        request_plot = findViewById(R.id.request_plot);
        request_plot.setOnClickListener(myListner);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (xcood.getText().toString().equals("") || ycood.getText().toString().equals("")) {
                    changedfloorplan = resizePhoto(R.drawable.durham);
                    floorplan.setImageBitmap(changedfloorplan);
                    Toast.makeText(MainPage.this, "Please fill in the x and y", Toast.LENGTH_SHORT).show();
                }
                else
                    floorplan.setImageBitmap(drawDot(Integer.valueOf(xcood.getText().toString()), Integer.valueOf(ycood.getText().toString()), R.drawable.durham));
            }
        });
        floorplan.setImageBitmap(resizePhoto(R.drawable.durham));

        startpoint_list = new ArrayList<String>();
        startpoint_list.add("Select Start Point");
        startpoint_list.add("Lab Wall: x0 = 1456, y0 = 1157");
        startpoint_list.add("Lab Door: x0 = 1456, y0 = 635");
        startpoint_list.add("Corridor Right: x0 = 2600, y0 = 427");
        startpoint_list.add("Corridor Left: x0 = 395, y0 = 430");

        /**
         * spinner/picker setup
         */
        startpoint_adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, startpoint_list) {
            @Override
            public boolean isEnabled(int position) {
                if (position == 0) {
                    // Disable the first item from Spinner
                    // First item will be use for hint
                    return false;
                } else {
                    return true;
                }
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if (position == 0) {
                    // Set the hint text color gray
                    tv.setTextColor(Color.GRAY);
                } else {
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };
        startpoint_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        startpoint.setAdapter(startpoint_adapter);
        startpoint.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() { //listen to which item the user pick for the spinner
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // put the value that the user chooses into variable "startpointChoice"
                if (position > 0) {
                    startpointChoice = startpoint_list.get(position);
                    if (position == 1) theta0 = 0.0;
                    else if (position == 2) theta0 = Math.PI;
                    else if (position == 3) theta0 = - Math.PI / 2;
                    else if (position == 4) theta0 = Math. PI / 2;

                } else {
                    startpointChoice = "";
                }
            }

            public void onNothingSelected(AdapterView<?> parentView) {
                startpointChoice = "";
            }
        });


        /**
         * location manager setup
         */
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE); //set up the location manager
        locationProviders = lm.getProviders(true);  //get the location providers
        locationProvider = this.chooseProvider(locationProviders);
        hasLocationProvider = (locationProvider != ""); //check if we have location provider
        storagePermission = false;
        gpslocationPermission = false;
        netlocationPermission = false;

        /**
         * layout components connection
         */
        start = findViewById(R.id.bn1);
        stop = findViewById(R.id.bn2);
        etGyro = findViewById(R.id.etGyro);
        etLinearAcc = findViewById(R.id.etLinearAcc);
        etMagnetic = findViewById(R.id.etMagnetic);
        azimuth = findViewById(R.id.Azimuth);
        pitch = findViewById(R.id.Pitch);
        roll = findViewById(R.id.Roll);
        ip = findViewById(R.id.ip);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        start.setOnClickListener(myListner);//add listener to start button and stop button
        stop.setOnClickListener(myListner);

        /**
         * http connector setup
         */
        httpconnect = findViewById(R.id.httpConnect);
        httpconnect.setOnClickListener(myListner);
        jsondisplay = findViewById(R.id.JsonDisplay);
        url = findViewById(R.id.url);
    }

    /**
     * collect the sensor information and add them to the lists
     */
    private void collectSensorInfo(){

        if (0 == flag) {
            AccList.add(AccData[0]);
            AccList.add(AccData[1]);
            AccList.add(AccData[2]);
            GyrList.add(GyrData[0]);
            GyrList.add(GyrData[1]);
            GyrList.add(GyrData[2]);
            MagList.add(MagData[0]);
            MagList.add(MagData[1]);
            MagList.add(MagData[2]);
            LongList.add(longtitude);
            LatList.add(latitude);
            AltList.add(altitude);
            AzimuthList.add(Azimuth);
            PitchList.add(Pitch);
            RollList.add(Roll);
            timeStampList.add((new Timestamp(System.currentTimeMillis()).toString()));
            //counter++;
        }
    }

    /**
     * Write all the data in the lists to the file and clear everything in the list
     */
    private void sendCSVandClearData(){
        WriteCSV(); //write the data to a csv file
        /**
         * cleaning all the data from the lists in order to reuse them
         */
        AccList.clear();//clear the list
        GyrList.clear();
        MagList.clear();
        LongList.clear();
        LatList.clear();
        AltList.clear();
        AzimuthList.clear();
        PitchList.clear();
        RollList.clear();
        timeStampList.clear();
        try {
            if (ip.getText().toString().trim().length() == 0){
                //uploadFile2("http://73.40.6.115");
                uploadFile2("http://172.29.22.154");
            }
            else {
                uploadFile2(ip.getText().toString().trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * This is for the getting the GPS location. Only onLocationChanged required
     */
    private LocationListener locationListener = new LocationListener() {
        /**
         * when the status of the provider is changed
         */
        @Override
        public void onStatusChanged(String provider, int status, Bundle arg2) {

        }

        /**
         * when a provider is enabled
         */
        @Override
        public void onProviderEnabled(String provider) {

        }

        /**
         * when a provider is disabled
         */
        @Override
        public void onProviderDisabled(String provider) {

        }

        /**
         * when the position is changed, we will put those value into the
         * private variables "altitude", "longtitude" and "latitude". The timer
         * will capture those variables and put it into the list every certain time
         */
        @Override
        public void onLocationChanged(Location location) {
            location.getAccuracy(); //accuracy
            altitude = location.getAltitude();
            longtitude = location.getLongitude();
            latitude = location.getLatitude();
        }
    };


    /**
     * listener for the sensor. When sensor data changed, those sensor
     * variables/arrays will be updated. They will not be put into the list
     * after that. The timer will retrieve the data from the variables/ arrays
     * every certain time and put them into the list.
     */
    private SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }

        public void onSensorChanged(SensorEvent e) {

            StringBuilder sb = null;
            switch (e.sensor.getType()) {
                case Sensor.TYPE_GYROSCOPE:     //Gyroscope
                    sb = new StringBuilder();
                    sb.append("GYROSCOPE - X:");
                    sb.append(e.values[0]);
                    sb.append("\nGYROSCOPE - Y:");
                    sb.append(e.values[1]);
                    sb.append("\nGYROSCOPE - Z:");
                    sb.append(e.values[2]);
                    etGyro.setText(sb.toString());
                    GyrData[0] = e.values[0];
                    GyrData[1] = e.values[1];
                    GyrData[2] = e.values[2];
                    break;

                case Sensor.TYPE_MAGNETIC_FIELD:    //magnetic field
                    sb = new StringBuilder();
                    sb.append("MAGNETIC FIELD - X:");
                    sb.append(e.values[0]);
                    sb.append("\nMAGNETIC FIELD - Y:");
                    sb.append(e.values[1]);
                    sb.append("\nMAGNETIC FIELD - Z:");
                    sb.append(e.values[2]);
                    etMagnetic.setText(sb.toString());
                    MagData[0] = e.values[0];
                    MagData[1] = e.values[1];
                    MagData[2] = e.values[2];
                    break;

                case Sensor.TYPE_ACCELEROMETER:   //accelerometer
                    sb = new StringBuilder();
                    sb.append("ACCELEROMETER - X:");
                    sb.append(e.values[0]);
                    sb.append("\nACCELEROMETER - Y:");
                    sb.append(e.values[1]);
                    sb.append("\nACCELEROMETER - Z:");
                    sb.append(e.values[2]);
                    etLinearAcc.setText(sb.toString());
                    AccData[0] = e.values[0];
                    AccData[1] = e.values[1];
                    AccData[2] = e.values[2];
                    break;
            }
            //The reason why azimuth/pitch/roll are retrieved here is because
            //the rotationMatrix depends on the AccData and MagData
            // Rotation matrix based on current readings from accelerometer and magnetometer.
            final float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrix(rotationMatrix, null, AccData, MagData);

            // Express the updated rotation matrix as three orientation angles.
            //final float[] orientationAngles = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
            Azimuth = (float)Math.toDegrees(orientationAngles[0]);
            Pitch = (float)Math.toDegrees(orientationAngles[1]);
            Roll = (float)Math.toDegrees(orientationAngles[2]);
            azimuth.setText("Azimuth:" + Azimuth);
            pitch.setText("Pitch:" + Pitch);
            roll.setText("Roll:" + Roll);
        }
    };

    /**
     * listener for the buttons
     */
    private Button.OnClickListener myListner = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.bn1:  //Start button
                    getPermission();
                    //if (ip.getText().toString().equals("")) {
                    //    StopSensorListening(); // stop getting value from the sensor
                    //    break;
                    //}
                    /**
                     * check if we can write to the external storage and access to the GPS
                     */
                    if (storagePermission && gpslocationPermission && netlocationPermission && startpointChoice.trim().length() >0) {
                        try {
                            lm.requestLocationUpdates(locationProvider, 0, 0, locationListener);
                        } catch (SecurityException e) {
                            e.printStackTrace();// lets the user know there is a problem with the gps
                        }

                        CurrentPoint_X = startpointChoice.split(":")[1].split(",")[0].split("=")[1].trim();
                        CurrentPoint_Y = startpointChoice.split(":")[1].split(",")[1].split("=")[1].trim();

                        StartSensorListening();//start getting value from the sensor

                        if (timer == null) {
                            timer = new Timer();

                        }

                        //******CALL FUNCTION WHICH IN THE BACKGROUND COLLECTS DATA AND WRITES/SEND HTTP REQUEST EVERY 30s
                        //startHandlerRepeatingTask();

                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {

                                if (0 == flag) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            //collectSensorInfo();
                                            //collectAndWriteHandler.postDelayed(mHandlerTask, WRITING_TO_FILE_INTERVAL);
                                            //sendCSVandClearData();
                                            for(int i = 0; i < 1000; i++){
                                                try {
                                                    Thread.sleep(10);
                                                } catch (InterruptedException e) {
                                                    e.printStackTrace();
                                                }
                                                collectSensorInfo();

                                            }
                                            //sendRequestWithOkHttp();
                                            sendCSVandClearData();
                                        }
                                    }).start();

                                }
                            }
                        }, 499, 35000);

                        /**
                         * if everything works well, then disable the start button and input filename button,
                         * enable the stop button
                         */

                        ip.setEnabled(false);//once the data start collecting, client cannot input file name any more
                        stop.setEnabled(true); //turn on stop button
                        start.setEnabled(false);//turn off start button

                    } else {
                        start.setEnabled(true);
                        stop.setEnabled(false);
                        ip.setEnabled(true);
                        Toast.makeText(MainPage.this, "Permission Denied or No Start Point Selected", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case R.id.bn2:
                    /**
                     * stop collecting data and putting it into the list
                     * write the data to the file
                     */
                    StopSensorListening();//stop collecting the data from the sensor
                    lm.removeUpdates(locationListener); //stop collecting data from the GPS
                    timer.cancel(); //stop the timer and stop putting things into the list
                    timer = null;
                    ip.setEnabled(true);
                    start.setEnabled(true);
                    stop.setEnabled(false);
                    break;

                case R.id.httpConnect:
                    /**
                     * send request to the website and display the message
                     */
                    // sendRequestWithHttpURLConnection();
                    sendRequestWithOkHttp();
                    break;
                case R.id.request_plot:
                    /**
                     * send the sensor_data.csv to the server.
                     * After receiving the response, send the start algorithm request to the sever.
                     * Receive the response with the json format from the server and plot it on the
                     * floorplan.
                     */
                    if (ip.getText().toString().trim().length() == 0){
                        //uploadFile2("http://73.40.6.115");
                        uploadFile2("http://172.29.22.154");
                    }
                    else {
                        uploadFile2(ip.getText().toString().trim());
                    }
                    //sendPlotRequestWithOkHttp();
                    break;
                case R.id.xygroupconfirm:
                    /**
                     * 1.drawDotContinuously(xygroupjson.getText().toString(), R.drawable.durham);
                     * it read the json format data from the textbox xygroupjson and plot
                     */
                    //drawDotContinuously(xygroupjson.getText().toString(), R.drawable.durham);
                    sendPlotRequestWithOkHttp("http://www.mocky.io/v2/5c99549d3200004f00d90888");
                    break;
            }
        }

    };

    /**
     * Write data in the lists to the file
     */
    private void WriteCSV() {
        //String title = "TimeStamp,ACCx/mg,ACCy/mg,ACCz/mg,GYROx/m°,GYROy/m°,GYROz/m°,Magx/mT,Magy/mT,Magz/mT,Longtitude,Altitude,Latitude,Azimuth,Pitch,Roll\n";
        String title = "Timestamp,accelX,accelY,accelZ,gyroX(rad/s),gyroY(rad/s),gyroZ(rad/s),magX(µT),magY(µT),magZ(µT),Long,Alt(feet),Lat,Yaw(rads),Roll(rads),Pitch(rads)\n";
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream out = new FileOutputStream(file, false);
            out.write(title.getBytes());
            int j = 0;
            for (int i = 0; i < AccList.size(); i = i + 3) {
                out.write((String.valueOf(timeStampList.get(j) + ",").getBytes()));
                if (AccList != null && AccList.get(i) != null) {
                    out.write((String.valueOf(AccList.get(i) + ",").getBytes()));
                    out.write((String.valueOf(AccList.get(i + 1) + ",").getBytes()));
                    out.write((String.valueOf(AccList.get(i + 2) + ",").getBytes()));
                }
                if (GyrList != null && GyrList.get(i) != null) {
                    out.write((String.valueOf(GyrList.get(i) + ",").getBytes()));
                    out.write((String.valueOf(GyrList.get(i + 1) + ",").getBytes()));
                    out.write((String.valueOf(GyrList.get(i + 2) + ",").getBytes()));
                }
                if (MagList != null && MagList.get(i) != null) {
                    out.write((String.valueOf(MagList.get(i) + ",").getBytes()));
                    out.write((String.valueOf(MagList.get(i + 1) + ",").getBytes()));
                    out.write((String.valueOf(MagList.get(i + 2) + ",").getBytes()));
                }
                out.write((String.valueOf(LongList.get(j) + ",").getBytes()));
                out.write((String.valueOf(AltList.get(j) + ",").getBytes()));
                out.write((String.valueOf(LatList.get(j) + ",").getBytes()));
                out.write((String.valueOf(AzimuthList.get(j) + ",").getBytes()));
                out.write((String.valueOf(PitchList.get(j) + ",").getBytes()));
                out.write((String.valueOf(RollList.get(j) + "\n").getBytes()));

                j++;
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    /**
     * start the sensor listener. After calling this function, the sensor will collect data.
     */
    public void StartSensorListening() {
        //super.onResume();
        //register gyroscope listener
        mSensorManager.registerListener(listener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_FASTEST);
        //register magnetic field listener
        mSensorManager.registerListener(listener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
        //register accelerometer listener
        mSensorManager.registerListener(listener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
    }

    /**
     * stop the sensor listener. The sensor will no longer get the data
     */
    public void StopSensorListening() {
        mSensorManager.unregisterListener(listener);
    }

    /**
     * Send request to the given ip address of the server. The calibration data will be sent along
     * with the request. The calibration data contains user, height, x and y coordinates of the start
     * point and theta0 that is necessary for the algorithm.
     * The response from server will contains points with x and y coordinate.
     * The function will create a new thread and plot those point on the floor plan
     * and display the reponse on a textview.
     * @param ipurl given ip address of the server
     */
    private void sendPlotRequestWithOkHttp(final String ipurl) {
        //create a new thread to execute the http request
        new Thread(new Runnable() {
            @Override
            public void run() {
                //Use okhttp
                OkHttpClient client = new OkHttpClient();
                //Bundle extras = getIntent().getExtras();
                String[] infoArray = getIntent().getStringArrayExtra("info");
                JSONObject obj = new JSONObject();
                try {
                    obj.put("user", String.valueOf(infoArray[2].charAt(0)));
                    obj.put("height", Double.toString(((double)Integer.valueOf(infoArray[1]))/100));
                    obj.put("x0", CurrentPoint_X);
                    obj.put("y0", CurrentPoint_Y);
                    obj.put("theta0", theta0);
                }
                catch (JSONException e) {
                    Toast.makeText(MainPage.this, "Something wrong with JSONObject", Toast.LENGTH_SHORT).show();
                }
                String jsonToString = obj.toString();
                RequestBody body = RequestBody.create(JSON, jsonToString);
                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(ipurl)//.url("http://172.29.19.219/start")
                        .post(body)
                        .build();
                try {
                    okhttp3.Response response = client.newCall(request).execute();
                    //Get the response from the server
                    final String data = response.body().string();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //drawDotBasedonRequest(data);
                            drawDotContinuously(data);
                        }
                    });
                    showResponse(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * Method to upload the sensor data file to the server.
     * The ip of the server is given as ipurl.
     * @param ipurl the ip of the server
     * @return true if upload succeeds and false if upload fails
     */
    public Boolean uploadFile2(final String ipurl) {
        try {
            OkHttpClient okHttpClient = new OkHttpClient();
            File file = new File(Environment.getExternalStorageDirectory(), outfilename);
            RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(),
                            RequestBody.create(MediaType.parse("text/csv"), file))
                    .addFormDataPart("some-field", "some-value")
                    .build();

            Request request = new Request.Builder()
                    .url(ipurl+"/recv")
                    .post(requestBody)
                    .build();

            okHttpClient.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(final Call call, final IOException e) {
                    // Handle the error
                }

                @Override
                public void onResponse(final Call call, final Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        // Handle the error
                    }
                    showResponse(response.toString());
                    sendPlotRequestWithOkHttp(ipurl+"/start");
                }
            });
            return true;
        } catch (Exception ex) {
            // Handle the error
        }
        return false;
    }

    /**
     * Send request to the server and get the response back. It works!
     */
    private void sendRequestWithOkHttp() {
        //create a new thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                //using okhttp
                OkHttpClient client = new OkHttpClient();

                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url(url.getText().toString())
                        .build();
                try {
                    okhttp3.Response response = client.newCall(request).execute();
                    //Get the response from the server
                    String data = response.body().string();
                    showResponse(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * show the response that gets back from the server
     * @param response the response message
     */
    private void showResponse(final String response) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //在这进行ui操作，将结果显示在界面上
                jsondisplay.setText(response);
            }
        });
    }


    /**
     * draw a single dot on the given bitmap
     * @param x x coordinate of the point
     * @param y y coordinate of the point
     * @param pictureId the drawable id
     * @return a bitmap after the drawing
     */
    private Bitmap drawDot(int x, int y, int pictureId){
        Bitmap bp = resizePhoto(pictureId);
        int width = bp.getWidth();
        int height = bp.getHeight();
        Canvas canvas = new Canvas(bp);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setColor(Color.parseColor("#CD5C5C"));
        canvas.drawCircle(y, height - x, 10, paint);
        return bp;
    }

    /**
     * draw a single dot on the given bitmap
     * @param x x coordinate of the point
     * @param y y coordinate of the point
     * @param bp the given bitmap needed to be drawn on
     * @return a bitmap after the drawing
     */
    private Bitmap drawDot(int x, int y, Bitmap bp){
        int width = bp.getWidth();
        int height = bp.getHeight();
        Canvas canvas = new Canvas(bp);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setColor(Color.parseColor("#CD5C5C"));
        canvas.drawCircle(y, height - x, 10, paint);
        return bp;
    }

    /**
     * Given a json message that contains many points, this method can draw all those point
     * on the durham floor plan. The drawable id is hardcoded here
     * @param jsonmsg the json message that contains all the x, y coordinates of the points
     */
    private void drawDotBasedonRequest(String jsonmsg){
        try {
            JSONObject reader = new JSONObject(jsonmsg);
            int x = reader.getInt("xLoc");
            int y = reader.getInt("yLoc");
            floorplan.setImageBitmap(drawDot(x, y, R.drawable.durham));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Given a message contains x and y coordinates, this method will draw all the point
     * on the floor plan and update the start point for next transmission.
     * @param msg message that contains points. The format of the message should be
     *            "x1":"y1","x2":"y2","x3":"y3",........
     */
    private void drawDotContinuously(String msg){
        msg = msg.substring(1, msg.length() - 1);
        String[] arr = msg.split(",");
        for(int i = 0; i< arr.length; i++) {
            String[] temp = arr[i].split(":");
            int x = Integer.valueOf(temp[0]);
            int y = Integer.valueOf(temp[1]);
            changedfloorplan = drawDot(x, y, changedfloorplan);
            if (i == arr.length - 1){
                CurrentPoint_X = String.valueOf(x);
                CurrentPoint_Y = String.valueOf(y);
            }

        }
        floorplan.setImageBitmap(changedfloorplan);
    }

    /**
     * Resize the photo since the resolution of given floor plan is too big.
     * the width and the height of the picture will be divided by 2.625
     * @param pictureId the drawable id that need to be resized
     * @return the resize picture in Bitmap format
     */
    private Bitmap resizePhoto(int pictureId)
    {
        Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), pictureId);
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();

        int newWidth = (int)(originalWidth/2.625); //the new width for the picture
        int newHeight = (int)(originalHeight/2.625);  // the new height for the picture

        float scale_width = ((float) newWidth) / originalWidth;
        float scale_height = ((float) newHeight) / originalHeight;

        Matrix matrix = new Matrix();
        matrix.postScale(scale_width, scale_height);
        Bitmap changedBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalWidth, originalHeight, matrix, true);
        return changedBitmap;
    }

    /**
     * Get permission from reading/writing to external storage and accessing to the GPS location
     * from the built-in hardware and network
     */
    public void getPermission() {
        //check if we have the permission to write to external storage
        if (ContextCompat.checkSelfPermission(MainPage.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainPage.this, "You have already granted storage permission!", Toast.LENGTH_SHORT).show();
        } else {
            requestStoragePermission(); //request the permission if we don't have the permission
        }

        //check if we have the permission to access the gps location
        if (ContextCompat.checkSelfPermission(MainPage.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainPage.this, "You have already granted storage permission!", Toast.LENGTH_SHORT).show();

        } else {
            requestFineLocationPermission(); //request the permission if we don't have the permission
        }

        //chekc if we have he mpermission to access the network location
        if (ContextCompat.checkSelfPermission(MainPage.this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainPage.this, "You have already granted storage permission!", Toast.LENGTH_SHORT).show();
        } else {
            requestCoarseLocationPermission(); //request the permission if we don't have the permission
        }


        //check if the external stoarage is writable
        if (isExternalStorageWritable()
                && ContextCompat.checkSelfPermission(MainPage.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            //file = new File(Environment.getExternalStorageDirectory(), ip.getText().toString() + ".csv");
            file = new File(Environment.getExternalStorageDirectory(), outfilename);
            storagePermission = true;
            //Log.d("CC",excelPath);
        } else {
            Toast.makeText(MainPage.this, "Cannot write to external storage", Toast.LENGTH_SHORT).show();
            storagePermission = false;
        }

        if (ContextCompat.checkSelfPermission(MainPage.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            gpslocationPermission = true;

        } else {
            gpslocationPermission = false;

        }

        //check if we have the permission to access the network location
        if (ContextCompat.checkSelfPermission(MainPage.this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            netlocationPermission = true;
        } else {
            netlocationPermission = false;
        }

    }

    /**
     * chekc if the external storage writable
     * @return true if writable, otherwise false
     */
    private boolean isExternalStorageWritable() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Log.i("State", "Yes It is writable");
            return true;
        } else {
            return false;
        }
    }

    /**
     * function for getting storage permission
     */
    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed because we are Team E911")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainPage.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
        }
    }

    /**
     * function for getting GPS location from hardware
     */
    private void requestFineLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed because we are Team E911")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainPage.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_PERMISSION_CODE);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_PERMISSION_CODE);
        }
    }

    /**
     * function for getting GPS location from network
     */
    private void requestCoarseLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed because we are Team E911")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainPage.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, COARSE_LOCATION_PERMISSION_CODE);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, COARSE_LOCATION_PERMISSION_CODE);
        }
    }

    /**
     * check the storage permission result
     * @param requestCode given request code
     * @param permissions  given permission string array
     * @param grantResults given grant result string array
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission GRANTED", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * choose location provider. It can either be network provider or built-in gps provider
     * @param providers a string list that store network provider and gps provider
     * @return the location provider
     */
    private String chooseProvider(List<String> providers) {
        String locationProvider = "";
        if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
            //if it is network provider
            Log.i("network_provider", "Using Network Provider");
            locationProvider = LocationManager.NETWORK_PROVIDER;
        } else if (providers.contains(LocationManager.GPS_PROVIDER)) {
            //if it is gpa provider
            Log.i("gps_provider", "Using GPS Provider");
            locationProvider = LocationManager.GPS_PROVIDER;
        } else {
            Log.e("no_provider", "No Location Provider");
        }
        return locationProvider;
    }
}