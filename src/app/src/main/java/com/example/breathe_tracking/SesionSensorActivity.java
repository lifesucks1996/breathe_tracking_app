package com.example.breathe_tracking;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;


/**
 * Clase que representa la actividad de la sesión del sensor.
 * - Muestra datos, incidencias, alertas, ubicacion, bateria baja
 * - Conexión con base de datos (19/11/25 - Rocio)
 */

public class SesionSensorActivity extends AppCompatActivity {

    // Variables de los datos a mostrar que recibimos del sensor o que calculamos
    private TextView ubicacionTextView, ultimaConexionTextView, bateriaTextView, ozonoTextView, temperaturaTextView, co2TextView, alertaTextView, incidenciaTextView, estadoTextView, reportarIncidenciaTextView, nombreSensorTextView;
    //Variables para dibujar con colores las medidas
    private ProgressBar co2ProgressBar, ozonoProgressBar, temperaturaProgressBar;

    // Instancia de la clase para acceder a los datos del sensor y del usuario en tiempo real
    private TrackingDataHolder dataHolder;


    // --- Permisos ------------------------------------------------------------------------------------------------------------
    // Solicitar los permisos necesarios (ubicación, bluetooth)
    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                if (Boolean.TRUE.equals(permissions.get(Manifest.permission.ACCESS_FINE_LOCATION))) {
                    startTrackingService();
                } else {
                    Toast.makeText(this, "Permisos necesarios denegados", Toast.LENGTH_LONG).show();
                }
            });
    // --- Fin Permisos --------------------------------------------------------------------------------------------------------

    // --- Inicio onCreate --------------------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sesion_sensor);

        // Inicialización de Vistas y Objetos que vamos a usar en la actividad
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



        // Listener para el botón de cerrar sesión
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

        // Inicializamos los Observers
        setupObservers();
        // Pedimos los permisos necesarios
        checkPermissionsAndStartService();

    }

    // --- Fin onCreate --------------------------------------------------------------------------------------------------------


    // --- setupObservers --------------------------------------------------------------------------------------------------------

    // Funcion para inicializar los Observers que van a estar observando los cambios en los datos del sensor y del usuario
    //Cambios en ubicacion, ultimaConexion, bateria, ozono, temperatura, co2, alerta, incidencia, estado
    private void setupObservers() {

        // Ubicacion del dispositivo -----------------------------------
        dataHolder.locationData.observe(this, address -> {
            if (address != null) ubicacionTextView.setText(address);
        });

        // Hora de recepcion de datos del sensor -----------------------
        dataHolder.timeData.observe(this, time -> {
            if (time != null) ultimaConexionTextView.setText(time);
        });

        // Bateria del dispositivo -------------------------------------
        // Si es baja cambiamos el color a rojo para que destaque, si no el texto se ve en negro
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

        // Dato de ozono -----------------------------------------------
        // Mostramos el dato de ozono y ademas en la progress bar se muestra por rangos de colores:
        // Verde: 0-0.6 ppm, Amarillo: 0.6-0.9 ppm y Rojo: >0.9 ppm
        dataHolder.ozonoData.observe(this, ozono -> {
            if (ozono != null) {
                ozonoTextView.setText(String.format(Locale.getDefault(), "%.3f ppm", ozono));
                ozonoProgressBar.setProgress((int) (ozono * 1000));
                Drawable d = (ozono < 0.6) ? ContextCompat.getDrawable(this, R.drawable.progress_bar_green) : (ozono < 0.9) ? ContextCompat.getDrawable(this, R.drawable.progress_bar_orange) : ContextCompat.getDrawable(this, R.drawable.progress_bar_red);
                ozonoProgressBar.setProgressDrawable(d);
            }

        });

        // Dato de temperatura ----------------------------------------
        // Mostramos el dato de temperatura y ademas en la progress bar se muestra por rangos de colores:
        // Verde: 0-20 ºC, Amarillo: 20-28 ºC y Rojo: >28 ºC
        dataHolder.temperaturaData.observe(this, temperatura -> {
            if (temperatura != null) {
                temperaturaTextView.setText(String.format(Locale.getDefault(), "%.1f ºC", temperatura));
                temperaturaProgressBar.setProgress(temperatura.intValue());
                Drawable d = (temperatura <= 20) ? ContextCompat.getDrawable(this, R.drawable.progress_bar_blue) : (temperatura <= 28) ? ContextCompat.getDrawable(this, R.drawable.progress_bar_orange) : ContextCompat.getDrawable(this, R.drawable.progress_bar_red);
                temperaturaProgressBar.setProgressDrawable(d);
            }
        });

        // Dato de Co2 ------------------------------------------------
        // Mostramos el dato de Co2 y ademas en la progress bar se muestra por rangos de colores:
        // Verde: 0-800 ppm, Amarillo: 800-1200 ppm y Rojo: >1200 ppm
        dataHolder.co2Data.observe(this, co2 -> {
            if (co2 != null) {
                co2TextView.setText(String.format(Locale.getDefault(), "%d ppm", co2));
                co2ProgressBar.setProgress(co2);
                Drawable d = (co2 < 800) ? ContextCompat.getDrawable(this, R.drawable.progress_bar_green) : (co2 < 1200) ? ContextCompat.getDrawable(this, R.drawable.progress_bar_orange) : ContextCompat.getDrawable(this, R.drawable.progress_bar_red);
                co2ProgressBar.setProgressDrawable(d);
            }
        });

        // Alertas ----------------------------------------------------
        dataHolder.alertData.observe(this, alert -> {
            if(alert != null) alertaTextView.setText(alert); // si no es null muestra el texto de alerta

        });

        // Incidencias ------------------------------------------------
        dataHolder.incidenciaData.observe(this, incidencia -> {
            if(incidencia != null) incidenciaTextView.setText(incidencia); // si no es null muestra el texto de incidencia
        });

        // Estado conectado - desconectado ----------------------------
        // Si esta conectado cambiamos el color a verde, si no el texto se ve en rojo
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

    // --- Fin setupObservers --------------------------------------------------------------------------------------------------

    // metodo pedir los permisos necesarios de bluetooth y ubicacion para empezar el servicio de TRACKING
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

        //Si hay permisos empezamos el servicio de tracking
        if (allPermissionsGranted) {
            startTrackingService();
        } else {
            requestPermissionLauncher.launch(permissionsToRequest);
        }
    }

    // Metodo para empezar el servicio de tracking --------------------------
    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, SensorTrackingService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    // Metodo para parar el servicio de tracking --------------------------
    private void stopTrackingService() {
        Intent serviceIntent = new Intent(this, SensorTrackingService.class);
        stopService(serviceIntent);
    }



}
