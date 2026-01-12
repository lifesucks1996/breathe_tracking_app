/**
 * @file EnvioIncidenciasActivity.java
 * @brief Actividad para la gestión y envío de reportes de incidencias.
 * @package com.example.breathe_tracking
 * @copyright Copyright © 2025
 */
package com.example.breathe_tracking;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @class EnvioIncidenciasActivity
 * @brief Activity responsable de la interfaz y lógica para reportar incidencias.
 * @extends AppCompatActivity
 *
 * @details
 * Esta clase maneja el formulario de envío de problemas. Soporta dos modos de operación:
 * 1. **Modo Automático (Contextual):** Se activa cuando el usuario pulsa "Reportar" desde el overlay de desconexión.
 * El formulario se pre-rellena con los datos del sensor y el motivo del fallo.
 * 2. **Modo Manual:** Se activa desde el menú de incidencias. Los campos aparecen vacíos para reporte libre.
 *
 * **Flujo de Envío:**
 * 1. Validación de campos.
 * 2. Envío asíncrono de correo electrónico (JavaMail).
 * 3. Persistencia en la nube (Firestore).
 * 4. Actualización del historial local en tiempo real (@ref TrackingDataHolder).
 *
 *
 *
 * @author Sandra (Creación, Lógica de UI y Autocomplete)
 * @author Rocio (Integración Firebase y Email)
 */
public class EnvioIncidenciasActivity extends AppCompatActivity {

    /** @brief ID del sensor asociado a la incidencia (si existe). */
    private String sensorIdRecibido;

    /** @brief Ubicación física del sensor o de la incidencia. */
    private String ubicacionRecibida;

    /** @brief Campo de texto para el título de la incidencia. */
    private EditText tituloEditText;

    /** @brief Campo de texto para la descripción detallada de la incidencia. */
    private EditText mensajeEditText;

    /**
     * @brief Inicializa la actividad.
     * Configura las vistas, procesa el Intent entrante y asigna listeners.
     * @param savedInstanceState Estado guardado de la aplicación.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.envio_incidencias);

        // 1. Inicializar Vistas
        tituloEditText = findViewById(R.id.editText_tituloIncidencia);
        mensajeEditText = findViewById(R.id.editText_mensaje);
        Button acceptButton = findViewById(R.id.button_aceptar);
        Button cancelButton = findViewById(R.id.button_cancelar);

        // 2. Manejo de Datos de Entrada
        manejarIntent(getIntent());

        // 3. Listeners
        cancelButton.setOnClickListener(v -> finish());
        acceptButton.setOnClickListener(v -> validarYEnviar());
    }

    /**
     * @brief Procesa el Intent recibido al iniciar la actividad.
     * (intent:Intent) -> manejarIntent() -> ()
     *
     * @details Extrae el ID del sensor y la ubicación. Si detecta que es un reporte automático
     * (por desconexión), llama al método para prellenar el formulario.
     * @param intent El Intent que inició la actividad.
     */
    private void manejarIntent(Intent intent) {
        if (intent == null) return;

        // Capturamos datos persistentes (se enviarán a Firebase aunque sea modo manual)
        sensorIdRecibido = intent.getStringExtra("SENSOR_ID");
        ubicacionRecibida = intent.getStringExtra("UBICACION");
        // Verificamos si es un aviso automático
        if (intent.hasExtra("SENSOR_ID")) {
            prellenarFormularioAutomatico(intent);
        }
    }


    /**
     * @brief Rellena automáticamente los campos de texto en caso de fallo de sensor.
     * (intent:Intent) -> prellenarFormularioAutomatico() -> ()
     *
     * @details Genera un título y un mensaje descriptivo utilizando el nombre del sensor,
     * la ubicación y la hora de la última conexión conocida.
     * @param intent Intent que contiene los datos del error ("SENSOR_NAME", "ULTIMA_CONEXION").
     */
    private void prellenarFormularioAutomatico(Intent intent) {
        String sensorName = intent.getStringExtra("SENSOR_ID");
        String ultimaCon = intent.getStringExtra("ULTIMA_CONEXION");

        if (ultimaCon != null) ultimaCon = ultimaCon.replace("Última conex. ", "");

        tituloEditText.setText(String.format("AVISO: Sensor %s Desconectado", sensorName));
        mensajeEditText.setText(String.format(Locale.getDefault(),
                "El sensor %s de la zona %s ha dejado de funcionar. Última lectura: %s. Revise la conexión.",
                sensorName, ubicacionRecibida, ultimaCon));
    }

    /**
     * @brief Valida los campos del formulario y coordina el proceso de envío.
     * () -> validarYEnviar() -> ()
     *
     * @details Verifica que el título y el mensaje no estén vacíos. Si son válidos,
     * inicia el envío del correo electrónico y la subida de datos a Firebase.
     */
    private void validarYEnviar() {
        String titulo = tituloEditText.getText().toString().trim();
        String mensaje = mensajeEditText.getText().toString().trim();

        if (TextUtils.isEmpty(titulo) || TextUtils.isEmpty(mensaje)) {
            mostrarAlerta("Campos incompletos", "Por favor, rellena todos los campos.");
            return;
        }

        // Ejecutar envío de correo
        new JavaMailAPI(this, "sandralovesel@gmail.com", titulo, mensaje).execute();
        //new JavaMailAPI(this, "ropibava@gmail.com", titulo, mensaje).execute();


        // Ejecutar guardado en Firebase
        enviarIncidenciaAFirebase(titulo, mensaje);
    }


    /**
     * @brief Guarda el documento de incidencia en Firestore.
     *
     * (titulo:String, mensaje:String) -> enviarIncidenciaAFirebase() -> ()
     *
     * Estructura del documento:
     * - titulo, mensaje
     * - estado: "PENDIENTE"
     * - resuelta: false
     * - fecha: ServerTimestamp
     * - sensor_id, ubicacion (si disponibles)
     *
     * Al tener éxito, actualiza el historial local y cierra la actividad.
     *
     * @param titulo Título de la incidencia.
     * @param mensaje Cuerpo de la incidencia.
     */
    private void enviarIncidenciaAFirebase(String titulo, String mensaje) {
        Map<String, Object> incidencia = new HashMap<>();
        incidencia.put("titulo", titulo);
        incidencia.put("mensaje", mensaje);
        incidencia.put("estado", "PENDIENTE");
        incidencia.put("resuelta", false);
        incidencia.put("fecha", FieldValue.serverTimestamp());

        if (sensorIdRecibido != null) incidencia.put("sensor_id", sensorIdRecibido);
        if (ubicacionRecibida != null) incidencia.put("ubicacion", ubicacionRecibida);

        FirebaseFirestore.getInstance().collection("incidencias")
                .add(incidencia)
                .addOnSuccessListener(doc -> {
                    actualizarHistorialLocal(titulo);
                    mostrarDialogoExito();
                })
                .addOnFailureListener(e -> mostrarAlerta("Error", "No se pudo guardar: " + e.getMessage()));
    }


    /**
     * @brief Inserta la incidencia recién creada en el Singleton de datos.
     *
     * (titulo:String) -> actualizarHistorialLocal() -> ()
     *
     * Esto permite que la actividad `IncidenciasActivity` muestre el nuevo reporte
     * inmediatamente sin tener que volver a consultar toda la base de datos.
     * Mantiene un límite de las últimas 4 incidencias en memoria.
     *
     * @param titulo Título de la incidencia enviada.
     */
    private void actualizarHistorialLocal(String titulo) {
        TrackingDataHolder dataHolder = TrackingDataHolder.getInstance();
        List<String> history = dataHolder.incidenciasEnviadasData.getValue();
        if (history == null) history = new ArrayList<>();

        String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        history.add(0, time + " - " + titulo);

        if (history.size() > 4) history.remove(history.size() - 1);
        dataHolder.incidenciasEnviadasData.postValue(history);
    }

    /**
     * @brief Muestra un diálogo de alerta genérico con un botón de aceptar.
     * (titulo:String, msj:String) -> mostrarAlerta() -> ()
     *
     * @param titulo Título de la alerta.
     * @param msj Mensaje del cuerpo de la alerta.
     */
    private void mostrarAlerta(String titulo, String msj) {
        new AlertDialog.Builder(this).setTitle(titulo).setMessage(msj).setPositiveButton("Aceptar", null).show();
    }

    /**
     * @brief Muestra el diálogo de confirmación de éxito y cierra la actividad.
     * () -> mostrarDialogoExito() -> ()
     *
     * @details Establece el resultado de la actividad como RESULT_OK.
     */
    private void mostrarDialogoExito() {
        setResult(RESULT_OK);
        new AlertDialog.Builder(this)
                .setTitle("Enviado")
                .setMessage("Incidencia registrada y correo enviado.")
                .setPositiveButton("Aceptar", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }
}