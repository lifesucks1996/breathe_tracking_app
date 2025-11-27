/**
 * @file EnvioIncidenciasActivity.java
 * @brief Actividad para permitir al usuario reportar incidencias relativas a la desconexión de un sensor.
 * @package com.example.breathe_tracking
 */
package com.example.breathe_tracking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @class EnvioIncidenciasActivity
 * @brief Activity responsable de la interfaz y lógica para reportar una incidencia a través de Firebase Firestore.
 *
 * Esta actividad precarga el formulario con información contextual sobre el sensor desconectado
 * (nombre, ubicación, última conexión) y gestiona la subida de la incidencia a la colección 'incidencias' de Firebase.
 *
 * @extends AppCompatActivity
 */
public class EnvioIncidenciasActivity extends AppCompatActivity {
    /**
     * @brief Método llamado al crear la actividad.
     * @param savedInstanceState Si la actividad se está recreando, este Bundle contiene los datos de estado más recientes.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.envio_incidencias);

        // Encontrar las vistas
        /** @brief Campo de texto para el título de la incidencia. */
        EditText tituloEditText = findViewById(R.id.editText_tituloIncidencia);
        /** @brief Campo de texto para el mensaje detallado de la incidencia. */
        EditText mensajeEditText = findViewById(R.id.editText_mensaje);
        /** @brief Botón para enviar la incidencia. */
        Button acceptButton = findViewById(R.id.button_aceptar);
        /** @brief Botón para cancelar y cerrar la actividad. */
        Button cancelButton = findViewById(R.id.button_cancelar);

        // --- Lógica para rellenar los datos automáticamente ---
        Intent intent = getIntent();
        /** @brief Nombre o código del sensor recibido de la actividad anterior. */
        String sensorName = intent.getStringExtra("SENSOR_NAME");
        /** @brief Ubicación actual del sensor (coordenadas convertidas a dirección). */
        String ubicacion = intent.getStringExtra("UBICACION");
        /** @brief Cadena de texto que indica la hora de la última conexión. */
        String ultimaConexion = intent.getStringExtra("ULTIMA_CONEXION");

        if (ultimaConexion != null) {
            ultimaConexion = ultimaConexion.replace("Última conex. ", "");
        }

        // --- Lógica para rellenar los campos de texto ---
        // rellenamos automaticamente el asunto y el mensaje
        String titulo = String.format("AVISO:'%s' Desconectado", sensorName);
        String mensaje = String.format(Locale.getDefault(),
                "El '%s' de la zona '%s' ha dejado de funcionar. La última lectura se recibió a las %s. Por favor, compruebe la conexión o si existe algún problema con el sensor.",
                sensorName, ubicacion, ultimaConexion);

        tituloEditText.setText(titulo);
        mensajeEditText.setText(mensaje);

        // --- Lógica de los botones ---
        /**
         * @brief Listener para el botón Cancelar. Cierra la actividad.
         */
        cancelButton.setOnClickListener(v -> {
            finish();
        });

        /**
         * @brief Listener para el botón Aceptar (Enviar).
         * Recoge los datos, crea un mapa de incidencia y lo sube a Firebase Firestore.
         */
        acceptButton.setOnClickListener(v -> {
            String tituloIncidencia = tituloEditText.getText().toString();
            String mensajeIncidencia = mensajeEditText.getText().toString();

            // ------ Implemetnacion Firebase ---------------------------
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> incidencia = new HashMap<>();
            /** @brief Código o nombre del sensor afectado. */
            incidencia.put("sensor_id", sensorName);
            /** @brief Título de la incidencia (manual o precargado). */
            incidencia.put("titulo", tituloIncidencia);
            /** @brief Contenido detallado de la incidencia (manual o precargado). */
            incidencia.put("mensaje", mensajeIncidencia);
            /** @brief Ubicación registrada del sensor en el momento de la incidencia. */
            incidencia.put("ubicacion", ubicacion);
            /** @brief Estado inicial de la incidencia. */
            incidencia.put("estado", "PENDIENTE");
            /** @brief Indicador de si la incidencia ha sido resuelta. */
            incidencia.put("resuelta", false);
            /** @brief Timestamp del servidor para registrar la fecha de envío. */
            incidencia.put("fecha", FieldValue.serverTimestamp());
            // ------------------------------------------

            // Sube el documento a la colección 'incidencias'
            db.collection("incidencias").add(incidencia)
                    .addOnSuccessListener(documentReference -> {
                        // ... (Manejo del éxito)
                        new AlertDialog.Builder(this)
                                .setTitle("Enviado")
                                .setMessage("Mensaje enviado correctamente al administrador")
                                .setPositiveButton("Aceptar", (dialog, which) -> {
                                    finish();
                                })
                                .show();
                    })
                    .addOnFailureListener(e -> {
                        // ... (Manejo del fallo)
                        new AlertDialog.Builder(this)
                                .setTitle("Error de Envío")
                                .setMessage("No se pudo enviar la incidencia. Compruebe su conexión a Internet. Error: " + e.getMessage())
                                .setPositiveButton("Aceptar", null)
                                .show();
                    });
        });
    }
}
