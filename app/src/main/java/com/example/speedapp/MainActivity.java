package com.example.speedapp;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {
    SQLiteDatabase speedDB;
    Button StartButton,setButton,viewButton;
TextView currentspeed,viewloc,limitText;
EditText editLimit;
Switch viewswitch;
LocationManager loc;
    TextToSpeech limits;
    float userLimit = 0.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        speedDB = openOrCreateDatabase("speed.db",MODE_PRIVATE,null);
        setContentView(R.layout.activity_main);
        StartButton=findViewById(R.id.StartButton);
        viewswitch=findViewById(R.id.viewswitch);
        currentspeed=findViewById(R.id.currentspeed);
        limitText=findViewById(R.id.limitText);
        loc=(LocationManager) getSystemService(LOCATION_SERVICE);
        viewloc=findViewById(R.id.viewloc);
        setButton=findViewById(R.id.setButton);
        viewButton=findViewById(R.id.viewButton);
        editLimit = findViewById(R.id.editLimit);
        currentspeed.setText("Speed: 0.00 km/h");
        speedDB = openOrCreateDatabase("speed.db",MODE_PRIVATE,null);
        speedDB.execSQL("CREATE TABLE IF NOT EXISTS speed (" +
                "latitude Text, " +
                "longitude Text, " +
                "speed Text, " +
                "timestamp Text)");

        limits = new TextToSpeech(getApplicationContext(), status -> {
            if (status != TextToSpeech.ERROR) {
                limits.setLanguage(Locale.US);
            }
        });
    }

     public void changeColor(int color) {
        View view = this.getWindow().getDecorView();
        view.setBackgroundColor(color);
    }

    public void speed(View view){
        if (currentspeed.getVisibility() == View.VISIBLE) {
            currentspeed.setVisibility(View.INVISIBLE);
        } else {
            currentspeed.setVisibility(View.VISIBLE);
        }
    }

public void start(View view){
    if (ActivityCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
    {
        ActivityCompat.requestPermissions(
                this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},123);
        return;
    }
    loc.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
}

    public void set(View view){
        String userInput = editLimit.getText().toString();
        if (!userInput.isEmpty()) {
            float newUserLimit = Float.parseFloat(userInput);
            if (newUserLimit >= 0 && newUserLimit <= 300) {
                userLimit = newUserLimit;
                float kilometres = userLimit;
                limitText.setTextSize(24);
                limitText.setText(String.format("Limit speed is: %.2f km/h", kilometres));
            }
        }
    }

    public void checkSpeedLimit(float kilometres, Location location) {
        String userInput = editLimit.getText().toString();
        if (!userInput.isEmpty()) {
            float userSpeedLimit = Float.parseFloat(userInput);
            if (kilometres > userSpeedLimit) {
                String date = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
                String insertQuery = String.format(Locale.getDefault(),
                        "INSERT INTO speed (latitude, longitude, speed, timestamp) VALUES (%f, %f, %f, '%s')",
                        location.getLatitude(), location.getLongitude(), kilometres, date);
                speedDB.execSQL(insertQuery);;
                changeColor(Color.BLUE);
                String speechText = "Speeding.";
                limits.setSpeechRate(1.0f);
                limits.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                changeColor(Color.WHITE);
            }
        }
    }

    public void show(View view) {
        Cursor cursor = speedDB.rawQuery("SELECT * FROM speed", null);
        StringBuilder data = new StringBuilder();
        while (cursor.moveToNext()) {
            data.append("Latitude: ").append(cursor.getDouble(0)).append("\n");
            data.append("Longitude: ").append(cursor.getDouble(1)).append("\n");
            data.append("Speed: ").append(cursor.getDouble(2)).append("\n");
            data.append("Date and time: ").append(cursor.getString(3)).append("\n\n");
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(data.toString())
                .setTitle("Limits")
                .setPositiveButton("OK", (dialog, which) -> {
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        float speed = location.getSpeed();
        float kilometres = speed * 3.6f;
        currentspeed.setTextSize(24);
        currentspeed.setText(String.format("Speed: %.2f km/h", kilometres));
        viewloc.setVisibility(View.VISIBLE);
        long timestamp = location.getTime();
        Date date = new Date(timestamp);
        String formattedDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(date);
        viewloc.setText(String.format("\nSpeed: %f,\nLocation: %f, \n%f, \nTimestamp: \n%s", kilometres, location.getLatitude(), location.getLongitude(), formattedDate));
        checkSpeedLimit(kilometres,location);
    }

    private void showMessage(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle("Location Information")
                .setPositiveButton("OK", (dialog, which) -> {
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
