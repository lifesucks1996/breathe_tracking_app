package com.example.breathe_tracking;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class SesionSensorActivity extends AppCompatActivity {

    private TextView ubicacionTextView;
    private TextView ultimaConexionTextView;
    private BroadcastReceiver locationReceiver;
    private BroadcastReceiver timeReceiver;

    // Launcher para pedir permisos de ubicación
    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                if (Boolean.TRUE.equals(permissions.get(Manifest.permission.ACCESS_FINE_LOCATION))) {
                    startTrackingService();
                } else {
                    Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sesion_sensor);

        ubicacionTextView = findViewById(R.id.textView_ubicacion);
        ultimaConexionTextView = findViewById(R.id.textView_ultimaConexion);
        ImageView cerrarSesionButton = findViewById(R.id.imageView_cerrarSesion);

        // Botón para cerrar sesión y detener el servicio
        cerrarSesionButton.setOnClickListener(v -> {
            stopTrackingService();
            Intent intent = new Intent(SesionSensorActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // Receiver para la ubicación
        locationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String address = intent.getStringExtra(SensorTrackingService.EXTRA_LOCATION_ADDRESS);
                if (address != null) {
                    ubicacionTextView.setText(address);
                }
            }
        };

        // Receiver para la hora
        timeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String time = intent.getStringExtra(SensorTrackingService.EXTRA_TIME);
                if (time != null) {
                    ultimaConexionTextView.setText(time);
                }
            }
        };

        // Comprobar permisos e iniciar el servicio
        checkPermissionsAndStartService();
    }

    private void checkPermissionsAndStartService() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        } else {
            startTrackingService();
        }
    }

    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, SensorTrackingService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void stopTrackingService() {
        Intent serviceIntent = new Intent(this, SensorTrackingService.class);
        stopService(serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Registrar los receivers para empezar a escuchar los datos del servicio
        LocalBroadcastManager.getInstance(this).registerReceiver(locationReceiver, new IntentFilter(SensorTrackingService.ACTION_LOCATION_UPDATE));
        LocalBroadcastManager.getInstance(this).registerReceiver(timeReceiver, new IntentFilter(SensorTrackingService.ACTION_TIME_UPDATE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Dejar de escuchar cuando la actividad no está en primer plano para no hacer trabajo innecesario
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(timeReceiver);
    }
}
