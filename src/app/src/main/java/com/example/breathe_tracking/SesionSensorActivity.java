package com.example.breathe_tracking;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class SesionSensorActivity extends AppCompatActivity {

    // Vistas de la UI
    private TextView ubicacionTextView, ultimaConexionTextView, bateriaTextView, ozonoTextView, temperaturaTextView, co2TextView, alertaTextView, incidenciaTextView, estadoTextView, reportarIncidenciaTextView, nombreSensorTextView;
    private ProgressBar co2ProgressBar, ozonoProgressBar, temperaturaProgressBar;

    private TrackingDataHolder dataHolder;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                if (Boolean.TRUE.equals(permissions.get(Manifest.permission.ACCESS_FINE_LOCATION))) {
                    startTrackingService();
                } else {
                    Toast.makeText(this, "Permisos necesarios denegados", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sesion_sensor);

        // Inicialización de Vistas
        ubicacionTextView = findViewById(R.id.textView_ubicacion);
        ultimaConexionTextView = findViewById(R.id.textView_ultimaConexion);
        bateriaTextView = findViewById(R.id.textView_bateria);
        ozonoTextView = findViewById(R.id.textView_ozono);
        temperaturaTextView = findViewById(R.id.textView_temperatura);
        co2TextView = findViewById(R.id.textView_co2);
        co2ProgressBar = findViewById(R.id.progressBar_co2);
        ozonoProgressBar = findViewById(R.id.progressBar_ozono);
        temperaturaProgressBar = findViewById(R.id.progressBar_temperatura);
        alertaTextView = findViewById(R.id.textView_alerta);
        incidenciaTextView = findViewById(R.id.textView_incidencia);
        estadoTextView = findViewById(R.id.textView_estado);
        reportarIncidenciaTextView = findViewById(R.id.textView_reportar_incidencia);
        nombreSensorTextView = findViewById(R.id.textView_nombreSensor);
        ImageView cerrarSesionButton = findViewById(R.id.imageView_cerrarSesion);

        dataHolder = TrackingDataHolder.getInstance();

        cerrarSesionButton.setOnClickListener(v -> {
            stopTrackingService();
            Intent intent = new Intent(SesionSensorActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        // Listener para el botón de reportar incidencia
        reportarIncidenciaTextView.setOnClickListener(v -> {
            Intent intent = new Intent(SesionSensorActivity.this, EnvioIncidenciasActivity.class);
            
            // Recopilamos los datos necesarios de los TextViews
            String sensorName = nombreSensorTextView.getText().toString();
            String ubicacion = ubicacionTextView.getText().toString();
            String ultimaConexion = ultimaConexionTextView.getText().toString();

            // Los añadimos al Intent para que la otra actividad los pueda leer
            intent.putExtra("SENSOR_NAME", sensorName);
            intent.putExtra("UBICACION", ubicacion);
            intent.putExtra("ULTIMA_CONEXION", ultimaConexion);
            
            startActivity(intent);
        });

        setupObservers();
        checkPermissionsAndStartService();
    }

    private void setupObservers() {
        dataHolder.locationData.observe(this, address -> {
            if (address != null) ubicacionTextView.setText(address);
        });

        dataHolder.timeData.observe(this, time -> {
            if (time != null) ultimaConexionTextView.setText(time);
        });

        dataHolder.bateriaData.observe(this, bateria -> {
            if (bateria != null) {
                bateriaTextView.setText(String.format(Locale.getDefault(), "%d%%", bateria));
                if (bateria <= 15) {
                    bateriaTextView.setTextColor(ContextCompat.getColor(this, R.color.progress_red));
                } else {
                    bateriaTextView.setTextColor(Color.BLACK);
                }
            }
        });

        dataHolder.ozonoData.observe(this, ozono -> {
            if (ozono != null) {
                ozonoTextView.setText(String.format(Locale.getDefault(), "%.3f ppm", ozono));
                ozonoProgressBar.setProgress((int) (ozono * 1000));
                Drawable d = (ozono < 0.6) ? ContextCompat.getDrawable(this, R.drawable.progress_bar_green) : (ozono < 0.9) ? ContextCompat.getDrawable(this, R.drawable.progress_bar_orange) : ContextCompat.getDrawable(this, R.drawable.progress_bar_red);
                ozonoProgressBar.setProgressDrawable(d);
            }
        });

        dataHolder.temperaturaData.observe(this, temperatura -> {
            if (temperatura != null) {
                temperaturaTextView.setText(String.format(Locale.getDefault(), "%.1f ºC", temperatura));
                temperaturaProgressBar.setProgress(temperatura.intValue());
                Drawable d = (temperatura <= 20) ? ContextCompat.getDrawable(this, R.drawable.progress_bar_blue) : (temperatura <= 28) ? ContextCompat.getDrawable(this, R.drawable.progress_bar_orange) : ContextCompat.getDrawable(this, R.drawable.progress_bar_red);
                temperaturaProgressBar.setProgressDrawable(d);
            }
        });

        dataHolder.co2Data.observe(this, co2 -> {
            if (co2 != null) {
                co2TextView.setText(String.format(Locale.getDefault(), "%d ppm", co2));
                co2ProgressBar.setProgress(co2);
                Drawable d = (co2 < 800) ? ContextCompat.getDrawable(this, R.drawable.progress_bar_green) : (co2 < 1200) ? ContextCompat.getDrawable(this, R.drawable.progress_bar_orange) : ContextCompat.getDrawable(this, R.drawable.progress_bar_red);
                co2ProgressBar.setProgressDrawable(d);
            }
        });
        
        dataHolder.alertData.observe(this, alert -> {
            if(alert != null) alertaTextView.setText(alert);
        });

        dataHolder.incidenciaData.observe(this, incidencia -> {
            if(incidencia != null) incidenciaTextView.setText(incidencia);
        });

        dataHolder.estadoData.observe(this, estado -> {
            if (estado != null) {
                estadoTextView.setText(estado);
                if ("Conectado".equals(estado)) {
                    estadoTextView.setTextColor(ContextCompat.getColor(this, R.color.progress_green));
                    reportarIncidenciaTextView.setVisibility(View.GONE);
                } else {
                    estadoTextView.setTextColor(ContextCompat.getColor(this, R.color.progress_red));
                    reportarIncidenciaTextView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void checkPermissionsAndStartService() {
        String[] permissionsToRequest = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
        };

        boolean allPermissionsGranted = true;
        for (String permission : permissionsToRequest) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (allPermissionsGranted) {
            startTrackingService();
        } else {
            requestPermissionLauncher.launch(permissionsToRequest);
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
}
