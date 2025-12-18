/**
 * @file EnvioIncidenciasActivity.java
 * @brief Actividad para el reporte de incidencias de sensores IoT.
 * @package com.example.breathe_tracking
 *
 * @copyright Copyright © 2025
 */
package com.example.breathe_tracking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @class EnvioIncidenciasActivity
 * @brief Gestiona la interfaz y lógica para reportar incidencias de desconexión.
 * @extends AppCompatActivity
 *
 * @details
 * Esta actividad se encarga de:
 * 1. Recibir datos del sensor fallido via Intent.
 * 2. Pre-cargar un formulario con un mensaje y título sugerido.
 * 3. Enviar un correo electrónico al administrador usando @ref JavaMailAPI.
 * 4. Registrar la incidencia en la colección 'incidencias' de Firebase Firestore.
 *
 * @author Sandra (Creación y autocomplete)
 * @author Rocio (Subida a Firebase)
 * @date 30/10/2024 (Creación)
 * @date 25/11/2024 (Integración Firebase)
 *
 * @see JavaMailAPI
 */
public class EnvioIncidenciasActivity extends AppCompatActivity {
    /**
     * @brief Método de inicialización de la actividad.
     *
     * Se encarga de:
     * - Vincular las vistas del layout.
     * - Recuperar datos del Intent (SENSOR_NAME, UBICACION, ULTIMA_CONEXION).
     * - Autocompletar los campos de texto con un mensaje preformateado.
     * - Configurar los listeners para los botones de Aceptar y Cancelar.
     *
     * @param savedInstanceState Estado guardado de la aplicación (si existe).
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.envio_incidencias);

        // Encontrar las vistas
        // Campo de texto para el título de la incidencia.
        EditText tituloEditText = findViewById(R.id.editText_tituloIncidencia);
        // Campo de texto para el mensaje detallado de la incidencia.
        EditText mensajeEditText = findViewById(R.id.editText_mensaje);
        // Botón para enviar la incidencia.
        Button acceptButton = findViewById(R.id.button_aceptar);
        // Botón para cancelar y cerrar la actividad.
        Button cancelButton = findViewById(R.id.button_cancelar);

        // --- Lógica para rellenar los datos automáticamente ---
        Intent intent = getIntent();
        // Nombre o código del sensor recibido de la actividad anterior.
        String sensorName = intent.getStringExtra("SENSOR_NAME");
        // Ubicación actual del sensor (coordenadas convertidas a dirección).
        String ubicacion = intent.getStringExtra("UBICACION");
        // Cadena de texto que indica la hora de la última conexión.
        String ultimaConexion = intent.getStringExtra("ULTIMA_CONEXION");

        if (ultimaConexion != null) {
            ultimaConexion = ultimaConexion.replace("Última conex. ", "");
        }

        // --- Lógica para rellenar los campos de texto ---
        // Rellenamos automaticamente el asunto y el mensaje
        String titulo = String.format("AVISO: Sensor %s  Desconectado", sensorName);
        String mensaje = String.format(Locale.getDefault(),
                "El sensor %s de la zona %s ha dejado de funcionar. La última lectura se recibió a las %s. Por favor, compruebe la conexión o si existe algún problema con el sensor.",
                sensorName, ubicacion, ultimaConexion);

        tituloEditText.setText(titulo);
        mensajeEditText.setText(mensaje);

        // --- Lógica de los botones ---
        // Listener para el botón Cancelar. Cierra la actividad.
        cancelButton.setOnClickListener(v -> {
            finish();
        });

        //Listener para el botón Aceptar (Enviar).
        // Recoge los datos, crea un mapa de incidencia y lo sube a Firebase Firestore
        acceptButton.setOnClickListener(v -> {
            String tituloIncidencia = tituloEditText.getText().toString();
            String mensajeIncidencia = mensajeEditText.getText().toString();

            // 0. Validación básica antes de enviar nada
            if (tituloIncidencia.isEmpty() || mensajeIncidencia.isEmpty()) {
                Toast.makeText(this, "Por favor, rellena título y mensaje", Toast.LENGTH_SHORT).show();
                return;
            }

            // ------ Implementación Envío de Correo (JavaMail) -----------------
            // Dirección de correo del administrador que recibirá la alerta. */
            String emailDestino = "sandralovesel@gmail.com";

            // Asunto del correo concatenando el prefijo fijo y el título del usuario. */
            String asuntoCorreo = tituloIncidencia;

            // Instancia de la clase asíncrona encargada de la conexión SMTP con el 'Robot'. */
            JavaMailAPI mailSender = new JavaMailAPI(this, emailDestino, asuntoCorreo, mensajeIncidencia);

            // Ejecuta el envío del correo electrónico en segundo plano. */
            mailSender.execute();
            // ------------------------------------------------------------------


            // ------ Implementación Firebase ---------------------------
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> incidencia = new HashMap<>();

            // Código o nombre del sensor afectado (asegúrate de tener esta variable definida). */
            incidencia.put("sensor_id", sensorName);
            // Título de la incidencia (manual o precargado). */
            incidencia.put("titulo", tituloIncidencia);
            // Contenido detallado de la incidencia (manual o precargado). */
            incidencia.put("mensaje", mensajeIncidencia);
            // Ubicación registrada del sensor en el momento de la incidencia. */
            incidencia.put("ubicacion", ubicacion);
            // Estado inicial de la incidencia. */
            incidencia.put("estado", "PENDIENTE");
            // Indicador de si la incidencia ha sido resuelta. */
            incidencia.put("resuelta", false);
            // Timestamp del servidor para registrar la fecha de envío exacta. */
            incidencia.put("fecha", com.google.firebase.firestore.FieldValue.serverTimestamp());
            // ------------------------------------------

            // Sube el documento a la colección 'incidencias'
            db.collection("incidencias").add(incidencia)
                    .addOnSuccessListener(documentReference -> {
                        //PARA EL CAMBIO DE BOTON DE INCIDENCIAS
                        setResult(RESULT_OK);
                        // Muestra un diálogo de éxito y cierra la actividad al aceptar. */
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("Enviado")
                                .setMessage("Incidencia registrada en el sistema y correo enviado al administrador.")
                                .setPositiveButton("Aceptar", (dialog, which) -> {
                                    finish();
                                })
                                .setCancelable(false)
                                .show();
                    })
                    .addOnFailureListener(e -> {
                        // Manejo de errores en caso de fallo de conexión con Firebase. */
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("Error de Envío")
                                .setMessage("No se pudo guardar la incidencia en la base de datos. Error: " + e.getMessage())
                                .setPositiveButton("Aceptar", null)
                                .show();
                    });
        });
    }
}
