package com.jeremy.jappbuild;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.jeremy.router.annotations.Destination;

@Destination(url = "router://page-home", description = "应用主页")
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}