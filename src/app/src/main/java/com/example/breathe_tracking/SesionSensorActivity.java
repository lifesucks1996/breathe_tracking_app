/**
 * @file SesionSensorActivity.java
 * @brief Actividad principal de monitorización en tiempo real.
 * @package com.example.breathe_tracking
 * @copyright Copyright © 2025
 */
package com.example.breathe_tracking;

import android.Manifest;
import android.app.Activity;
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

/**
 * @class SesionSensorActivity
 * @brief Actividad principal que muestra la interfaz de usuario con los datos del sensor en tiempo real.
 * @extends AppCompatActivity
 *
 * @details
 * Esta clase actúa como el "Dashboard" principal de la aplicación. Su arquitectura se basa en el patrón Observer
 * a través de la clase Singleton @ref TrackingDataHolder.
 *
 *
 *
 * **Funcionalidades principales:**
 * 1. **Visualización de Datos:** Muestra CO2, Ozono, Temperatura, Batería y Ubicación actualizándose automáticamente.
 * 2. **Gestión de Servicios:** Inicia y detiene el @ref SensorTrackingService (Foreground Service).
 * 3. **Feedback Visual:** Cambia el color de las barras de progreso según umbrales de seguridad definidos.
 * 4. **Gestión de Incidencias:** Permite reportar problemas de conexión o hardware.
 *
 * @author Sandra (UI, Lógica de visualización y alertas - 11/11/2025)
 * @author Rocio (Conexión con base de datos y lógica de servicio - 19/11/2025)
 * @see SensorTrackingService
 * @see TrackingDataHolder
 */

public class SesionSensorActivity extends AppCompatActivity {

    /** @brief Muestra la ubicación actual del dispositivo. */
    private TextView ubicacionTextView;
    /** @brief Muestra la hora de la última conexión/recepción de datos del sensor. */
    private TextView ultimaConexionTextView;
    /** @brief Muestra el porcentaje de batería del sensor. */
    private TextView bateriaTextView;
    /** @brief Muestra el nivel de ozono. */
    private TextView ozonoTextView;
    /** @brief Muestra la temperatura. */
    private TextView temperaturaTextView;
    /** @brief Muestra la concentración de CO2. */
    private TextView co2TextView;
    /** @brief Muestra las alertas de mediciones fuera de rango. */
    private TextView alertaTextView;
    /** @brief Muestra el estado de incidencias o de desconexión. */
    private TextView incidenciaTextView;
    /** @brief Muestra el estado de conexión del sensor ("Conectado"/"Desconectado"). */
    private TextView estadoTextView;
    /** @brief Botón o enlace para iniciar la actividad de reportar incidencia. */
    private TextView reportarIncidenciaTextView;
    /** @brief Muestra el código o nombre del sensor que se está rastreando. */
    private TextView nombreSensorTextView;
    /** @brief Botón para ir a la pantalla de gráficas de información. */
    private TextView verGraficasTextView;
    /** @brief Icono para mostrar la intensidad de la señal (RSSI). */
    private ImageView imgSignal;

    //Variables para dibujar con colores las medidas
    /** @brief Barra de progreso visual para el nivel de CO2. */
    private ProgressBar co2ProgressBar;
    /** @brief Barra de progreso visual para el nivel de Ozono. */
    private ProgressBar ozonoProgressBar;
    /** @brief Barra de progreso visual para el nivel de Temperatura. */
    private ProgressBar temperaturaProgressBar;

    /** @brief Instancia Singleton para acceder a los datos observados (LiveData). */
    private TrackingDataHolder dataHolder;
    /** @brief Código único del sensor que se está monitorizando. */
    private String sensorId;


    /**
     * @brief Lanzador de actividad para gestionar el resultado del reporte de incidencias.
     *
     * Registra un callback para la actividad de envío de incidencias (\ref EnvioIncidenciasActivity).
     * Escucha el código de resultado (resultCode) al finalizar dicha actividad.
     *
     * Si el resultado es Activity.RESULT_OK:
     * - Actualiza el texto del botón a "Incidencia reportada".
     * - Cambia el color del texto a gris para indicar visualmente que está deshabilitado.
     * - Desactiva la interacción (setClickable(false) y setEnabled(false)) para prevenir envíos duplicados.
     * - Muestra un mensaje Toast de confirmación al usuario.
     */
    private final ActivityResultLauncher<Intent> reportarIncidenciaLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Aquí cambiamos el aspecto del botón cuando volvemos
                    reportarIncidenciaTextView.setText("Incidencia reportada");
                    reportarIncidenciaTextView.setTextColor(Color.LTGRAY);
                    reportarIncidenciaTextView.setClickable(false);
                    reportarIncidenciaTextView.setEnabled(false);

                    Toast.makeText(this, "Incidencia registrada correctamente", Toast.LENGTH_SHORT).show();
                }
            }
    );


    // --- Permisos ------------------------------------------------------------------------------------------------------------
    /**
     * @brief Launcher para solicitar múltiples permisos de la aplicación.
     * Si los permisos se conceden (específicamente ACCESS_FINE_LOCATION), inicia el servicio de rastreo.
     */
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
    /**
     * @brief Inicialización de la actividad.
     *
     * 1. Vincula todas las vistas del layout.
     * 2. Recupera el SENSOR_CODE del Intent.
     * 3. Configura los listeners de los botones (Cerrar sesión, Reportar, Ver Gráficas).
     * 4. Inicializa los observadores de datos.
     * 5. Verifica permisos e inicia el servicio.
     *
     * @param savedInstanceState Estado guardado de la aplicación.
     */
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
        verGraficasTextView = findViewById(R.id.textView_graficas);
        imgSignal = findViewById(R.id.img_signal);
        ImageView cerrarSesionButton = findViewById(R.id.imageView_cerrarSesion);

        dataHolder = TrackingDataHolder.getInstance();

        // Logica para codigo de sensor
        Intent currentintent = getIntent();
        if (currentintent != null && currentintent.hasExtra("SENSOR_CODE")) {
            sensorId = currentintent.getStringExtra("SENSOR_CODE");
            // Mostrar el nombre
            String displayMessage = "Sensor " + sensorId;

            if (nombreSensorTextView != null) {
                nombreSensorTextView.setText(displayMessage);
            }
        }



        // Listener para el botón de cerrar sesión
        /**
         * @brief Listener para el botón de cerrar sesión.
         * Detiene el servicio de rastreo y navega de vuelta a la actividad principal.
         */
        cerrarSesionButton.setOnClickListener(v -> {
            stopTrackingService();
            Intent intent = new Intent(SesionSensorActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        /**
         * @brief Listener para el botón de reportar incidencia.
         * Inicia \ref EnvioIncidenciasActivity, pasando los datos de contexto (sensor, ubicación, última conexión).
         */
        reportarIncidenciaTextView.setOnClickListener(v -> {
            Intent intent = new Intent(SesionSensorActivity.this, EnvioIncidenciasActivity.class);
            
            // Recopilamos los datos necesarios de los TextViews
            String sensorName = sensorId;
            String ubicacion = ubicacionTextView.getText().toString();
            String ultimaConexion = ultimaConexionTextView.getText().toString();

            // Los añadimos al Intent para que la otra actividad los pueda leer
            intent.putExtra("SENSOR_NAME", sensorName);
            intent.putExtra("UBICACION", ubicacion);
            intent.putExtra("ULTIMA_CONEXION", ultimaConexion);

            reportarIncidenciaLauncher.launch(intent);
        });

        /**
         * @brief Listener para el botón "Saber más..." (gráficas).
         * Abre la actividad InformacionActivity.
         */
        verGraficasTextView.setOnClickListener(v -> {
            Intent intent = new Intent(SesionSensorActivity.this, InformacionActivity.class);
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
    /**
     * @brief Configura los observadores LiveData para actualizar la UI en tiempo real.
     *
     * () -> setupObservers() -> ()
     *
     * Define la lógica de colores y umbrales para los indicadores visuales:
     * - **Batería:** Rojo si <= 15%.
     * - **Ozono (ppm):**
     * - Verde: < 0.6
     * - Naranja: 0.6 - 0.9
     * - Rojo: >= 0.9
     * - **Temperatura (ºC):**
     * - Azul: <= 20
     * - Naranja: 20 - 28
     * - Rojo: > 28
     * - **CO2 (ppm):**
     * - Verde: < 800
     * - Naranja: 800 - 1200
     * - Rojo: >= 1200
     * - **RSSI:** Actualiza el icono de barras según la potencia (dBm).
     */
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

        // RSSI (Señal) ------------------------------------------------
        // Actualizamos el icono de la señal según el valor RSSI (dBm)
        dataHolder.rssiData.observe(this, rssi -> {
            if (rssi != null) {
                if (rssi >= -60) {
                    imgSignal.setImageResource(R.drawable.ic_signal_bars_4); // Excelente
                } else if (rssi >= -70) {
                    imgSignal.setImageResource(R.drawable.ic_signal_bars_3); // Buena
                } else if (rssi >= -80) {
                    imgSignal.setImageResource(R.drawable.ic_signal_bars_2); // Regular
                } else if (rssi >= -90) {
                    imgSignal.setImageResource(R.drawable.ic_signal_bars_1); // Mala
                } else {
                    imgSignal.setImageResource(R.drawable.ic_signal_bars_0); // Muy mala/Sin señal
                }
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
                    imgSignal.setImageResource(R.drawable.ic_signal_bars_0); // Si desconectado, señal 0
                }
            }
        });



    }

    // --- Fin setupObservers --------------------------------------------------------------------------------------------------


    /**
     * @brief Verifica si todos los permisos necesarios (Ubicación, Bluetooth) están concedidos.
     * Si no, solicita al usuario.
     * () -> checkPermissionsAndStartService() -> ()
     *
     * Si faltan permisos, utiliza @ref requestPermissionLauncher para solicitarlos.
     * Si están concedidos, llama inmediatamente a @ref startTrackingService.
     *
     */
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
    /**
     * @brief Inicia el servicio de rastreo (\ref SensorTrackingService) como un servicio en primer plano.
     * Pasa el \p sensorId al servicio para que pueda establecer su DocumentReference en Firestore.
     * () -> startTrackingService() -> ()
     */
    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, SensorTrackingService.class);
        if (sensorId != null) {
            serviceIntent.putExtra("SENSOR_ID_KEY", sensorId);
        }
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    // Metodo para parar el servicio de tracking --------------------------

    /**
     * @brief Detiene el servicio de rastreo @ref SensorTrackingService
     * () -> stopTrackingService() -> ()
     */
    private void stopTrackingService() {
        Intent serviceIntent = new Intent(this, SensorTrackingService.class);
        stopService(serviceIntent);
    }



}