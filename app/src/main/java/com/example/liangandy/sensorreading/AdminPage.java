package com.example.liangandy.sensorreading;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.HashMap;

public class AdminPage extends AppCompatActivity {
    private HashMap<String, String[]> hashmap;
    private TextView infoDisplay;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_page);
        infoDisplay = findViewById(R.id.userinfo);
        hashmap = (HashMap<String, String[]>) getIntent().getSerializableExtra("hashmap");

        for (String name: hashmap.keySet()){
            String key = "Username:" + name +"\n";
            String[] value = hashmap.get(name);
            String output = key + "height:" + value[1] + "\tgender:" + value[2] + "\tStarting point:" + value[3] +"\n\n";
            infoDisplay.append(output);
        }
    }
}
