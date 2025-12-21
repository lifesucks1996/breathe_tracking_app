/**
 * @file EnvioIncidenciasActivity.java
 * @brief Actividad para permitir al usuario reportar incidencias, tanto automáticas como manuales.
 * @package com.example.breathe_tracking
 */
package com.example.breathe_tracking;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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
 * @brief Activity responsable de la interfaz y lógica para reportar una incidencia a través de Firebase Firestore y correo.
 *        Puede funcionar de dos modos:
 *        1. Modo automático: Precarga los datos de una desconexión de sensor.
 *        2. Modo manual: Presenta los campos vacíos para que el usuario los rellene.
 *
 * Copyright © 2025
 *
 * @extends AppCompatActivity
 */
public class EnvioIncidenciasActivity extends AppCompatActivity {
    /**
     * @brief Método llamado al crear la actividad.
     * @param savedInstanceState Si la actividad se está recreando, este Bundle contiene los datos de estado más recientes.
     */

    private String sensorIdRecibido;
    private String ubicacionRecibida;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.envio_incidencias);

        // Encontrar las vistas
        EditText tituloEditText = findViewById(R.id.editText_tituloIncidencia);
        EditText mensajeEditText = findViewById(R.id.editText_mensaje);
        Button acceptButton = findViewById(R.id.button_aceptar);
        Button cancelButton = findViewById(R.id.button_cancelar);

        // --- Lógica de Modo (Automático vs. Manual) ---
        Intent intent = getIntent();

        if (intent != null) {
            // Recogemos los datos independientemente de si el modo es automático o manual
            sensorIdRecibido = intent.getStringExtra("SENSOR_ID");
            ubicacionRecibida = intent.getStringExtra("UBICACION");
        }

        // Comprobamos si el intent contiene datos de un sensor. Si no, es un reporte manual.
        if (intent != null && intent.hasExtra("SENSOR_NAME")) {
            // --- Modo Automático: Precargar datos ---
            String sensorName = intent.getStringExtra("SENSOR_NAME");
            String ubicacion = intent.getStringExtra("UBICACION");
            String ultimaConexion = intent.getStringExtra("ULTIMA_CONEXION");

            if (ultimaConexion != null) {
                ultimaConexion = ultimaConexion.replace("Última conex. ", "");
            }

            String titulo = String.format("AVISO: Sensor %s Desconectado", sensorName);
            String mensaje = String.format(Locale.getDefault(),
                    "El sensor %s de la zona %s ha dejado de funcionar. La última lectura se recibió a las %s. Por favor, compruebe la conexión o si existe algún problema con el sensor.",
                    sensorName, ubicacion, ultimaConexion);

            tituloEditText.setText(titulo);
            mensajeEditText.setText(mensaje);
        } else {
            // --- Modo Manual: Los campos permanecen vacíos ---
            tituloEditText.setText("");
            mensajeEditText.setText("");
        }

        // --- Lógica de los botones ---
        cancelButton.setOnClickListener(v -> finish());

        acceptButton.setOnClickListener(v -> {
            String tituloIncidencia = tituloEditText.getText().toString().trim();
            String mensajeIncidencia = mensajeEditText.getText().toString().trim();

            // --- Validación de campos ---
            if (TextUtils.isEmpty(tituloIncidencia) || TextUtils.isEmpty(mensajeIncidencia)) {
                new AlertDialog.Builder(this)
                        .setTitle("Campos incompletos")
                        .setMessage("Por favor, rellena todos los campos para poder enviar la incidencia.")
                        .setPositiveButton("Aceptar", null)
                        .show();
                return; // Detiene la ejecución si los campos están vacíos
            }

            // ------ Implementación Envío de Correo (JavaMail) -----------------
            String emailDestino = "sandralovesel@gmail.com";
            JavaMailAPI mailSender = new JavaMailAPI(this, emailDestino, tituloIncidencia, mensajeIncidencia);
            mailSender.execute();
            // ------------------------------------------------------------------

            // ------ Implementación Firebase ---------------------------
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> incidencia = new HashMap<>();
            incidencia.put("titulo", tituloIncidencia);
            incidencia.put("mensaje", mensajeIncidencia);
            incidencia.put("estado", "PENDIENTE");
            incidencia.put("resuelta", false);
            incidencia.put("fecha", FieldValue.serverTimestamp());

            // Esto sube (en caso de recibirlo) a firebase el id_sensor cuando la incidencia es manual
            if (sensorIdRecibido != null) {
                incidencia.put("sensor_id", sensorIdRecibido);
            }
            if (ubicacionRecibida != null) {
                incidencia.put("ubicacion", ubicacionRecibida);
            }
            // ------------------------------------------

            // Sube el documento a la colección 'incidencias'
            db.collection("incidencias").add(incidencia)
                    .addOnSuccessListener(documentReference -> {
                        setResult(RESULT_OK);

                        // --- Actualizar Historial de Incidencias ---
                        TrackingDataHolder dataHolder = TrackingDataHolder.getInstance();
                        List<String> currentHistory = dataHolder.incidenciasEnviadasData.getValue();
                        if (currentHistory == null) {
                            currentHistory = new ArrayList<>();
                        }

                        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                        String historyEntry = currentTime + " - " + tituloIncidencia;

                        currentHistory.add(0, historyEntry);
                        if (currentHistory.size() > 4) { // Mantenemos un máximo de 4
                            currentHistory.remove(currentHistory.size() - 1);
                        }
                        dataHolder.incidenciasEnviadasData.postValue(currentHistory);
                        // ---------------------------------------------

                        new AlertDialog.Builder(this)
                                .setTitle("Enviado")
                                .setMessage("Incidencia registrada en el sistema y correo enviado al administrador.")
                                .setPositiveButton("Aceptar", (dialog, which) -> finish())
                                .setCancelable(false)
                                .show();
                    })
                    .addOnFailureListener(e -> {
                        new AlertDialog.Builder(this)
                                .setTitle("Error de Envío")
                                .setMessage("No se pudo guardar la incidencia en la base de datos. Error: " + e.getMessage())
                                .setPositiveButton("Aceptar", null)
                                .show();
                    });
        });
    }
}
