package com.example.liangandy.sensorreading;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    public static final int REGISTER_CODE = 1;
    private EditText username;
    private EditText password;
    private Button login;
    private Button register;
    private HashMap<String, String[]> hashmap = new HashMap<String, String[]>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        login = findViewById(R.id.login);
        register = findViewById(R.id.register);
        loadData();

        /**
         * listener for button.
         * If the admin account is chosen, display the admin page.
         * If the user account is chose, display the main page
         */
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (username.getText().toString().equals("Admin") && password.getText().toString().equals("e911")){
                    openAdmin();
                }
                else if (hashmap.containsKey(username.getText().toString())) {
                    if (hashmap.get(username.getText().toString())[0].equals(password.getText().toString())) {
                        openMainPage();
                    }
                    else{
                        Toast.makeText(MainActivity.this, "Wrong Username or Password", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    Toast.makeText(MainActivity.this, "No user found!", Toast.LENGTH_SHORT).show();
                }

            }
        });

        register.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                openRegister();
            }
        });
    }

    /**
     * load data from the internal storage and put it into the hashmap so that we can check if the account info is
     * correct.
     */
    private void loadData(){
        try {
            FileInputStream fileInputStream = openFileInput("UserRecord.txt");
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuffer buffer = new StringBuffer();
            String name;
            String info;
            while ((name = bufferedReader.readLine()) !=null) {
                info = bufferedReader.readLine();
                String[] infoArray = info.split("\\+", -1);
                hashmap.put(name, infoArray);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The main page used to display all the information including sensor data, floor plan
     */
    private void openMainPage(){
        Intent intent = new Intent(this , MainPage.class);
        //intent.putExtra("info", "{\"user\":\"M\", \"start\":\"Corridor Right\", \"height\":\"1.6\"}" );
        String[] PersonalInfo = hashmap.get(username.getText().toString());
        intent.putExtra("info", PersonalInfo);
        startActivity(intent);
    }

    /**
     * if the Register button is clicked. Open register activity
     */
    private void openRegister(){
        Intent intent = new Intent(this ,RegisterActivity.class);
        intent.putExtra("hashmap",(Serializable) hashmap);
        startActivityForResult(intent, REGISTER_CODE);
    }

    /**
     * if the Admin button is clicked. Open Admin activity
     */
    private void openAdmin(){
        Intent intent = new Intent(this ,AdminPage.class);
        intent.putExtra("hashmap",(Serializable) hashmap);
        startActivity(intent);
    }

    /**
     * Get the result back from Register Activity
     * When people register, new data will be written to the internal storage.
     * That's why we also need to update our hashmap
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REGISTER_CODE){
            if (resultCode == RESULT_OK){
                String newUser = data.getStringExtra("newUsername");
                String[] newInfo = data.getStringArrayExtra("newInfo");
                hashmap.put(newUser,newInfo);
            }
        }
    }
}
