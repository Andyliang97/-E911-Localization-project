package com.example.liangandy.sensorreading;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    /**
     * EditText for username, password and height
     */
    private EditText username;
    private EditText password;
    private EditText height;
    /**
     * button to submit the registration
     */
    private Button submit;
    /**
     * String for username, password and height
     */
    private String usernameString;
    private String passwordString;
    private String heightString;
    /**
     * file output stream to store the user information in the internal storage
     */
    private FileOutputStream outputStream;

    /**
     * hashmap used to check if the username is duplicate
     */
    private HashMap<String, String[]> hashmap;
    /**
     * spinner for gender
     */
    private Spinner gender;
    private List<String> gender_list;
    private ArrayAdapter<String> gender_adapter;
    private String genderChoice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
        /**
         * receive the haspmap from the main activity
         */
        hashmap = (HashMap<String, String[]>) getIntent().getSerializableExtra("hashmap");

        /**
         * connects to the fields that need to fill in by the user
         */
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        height = findViewById(R.id.height);
        gender = findViewById(R.id.gender);

        /**
         * connects to the submit button
         */
        submit = findViewById(R.id.submit);

        /**
         * the spinner setup for gender
         */
        gender_list = new ArrayList<String>();
        gender_list.add("Select Gender");
        gender_list.add("Male");
        gender_list.add("Female");
        gender_adapter= new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, gender_list){
            @Override
            public boolean isEnabled(int position){
                if(position == 0)
                {
                    // Disable the first item from Spinner
                    // First item will be use for hint
                    return false;
                }
                else
                {
                    return true;
                }
            }

            @Override
            public View getDropDownView(int position, View convertView,
                                        ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                if(position == 0){
                    // Set the hint text color gray
                    tv.setTextColor(Color.GRAY);
                }
                else {
                    tv.setTextColor(Color.BLACK);
                }
                return view;
            }
        };
        gender_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gender.setAdapter(gender_adapter);
        gender.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {//选择item的选择点击监听事件
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // 将所选mySpinner 的值带入myTextView 中
                if (position > 0) {
                    genderChoice = gender_list.get(position);
                }
                else{
                    genderChoice = "";
                }
            }
            public void onNothingSelected(AdapterView<?> parentView) {
                genderChoice = "";
            }


        });


        /**
         * listener for buttons
         * if the user click submit, this method will be triggered.
         */
        submit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                usernameString = username.getText().toString();
                passwordString = password.getText().toString();
                heightString = height.getText().toString();
                if (usernameString.trim().length() >0 && passwordString.trim().length() >0
                && heightString.trim().length() >0 && genderChoice.trim().length() >0){
                    if (hashmap.containsKey(usernameString)){
                        Toast.makeText(RegisterActivity.this, "Duplicate Username", Toast.LENGTH_SHORT).show();
                    }
                    else{
                        hashmap.put(usernameString,new String[]{passwordString, heightString, genderChoice});
                        String fileContents = usernameString+"\n"+passwordString+"+"+heightString+"+"+genderChoice+"\n";
                        try {
                            outputStream = openFileOutput("UserRecord.txt", Context.MODE_PRIVATE | Context.MODE_APPEND);
                            outputStream.write(fileContents.getBytes());
                            outputStream.close();
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("newUsername", usernameString);
                            String abbvGender = "";
                            if (genderChoice.equals("Male"))
                            {
                                abbvGender = "M";
                            }
                            else {
                                abbvGender = "F";
                            }
                            double meterHeight = Integer.valueOf(heightString)/100.0;
                            resultIntent.putExtra("newInfo", new String[]{passwordString, String.valueOf(meterHeight), abbvGender});
                            setResult(RESULT_OK,resultIntent);
                            finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                else{
                    Toast.makeText(RegisterActivity.this, "Empty Field!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }




}
