/**
 * @file MainActivity.java
 * @brief Actividad principal responsable del inicio de sesión (Login), escaneo de código QR y acceso biométrico.
 * @package com.example.breathe_tracking
 */

package com.example.breathe_tracking;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
// Necesarios para la consulta
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnCompleteListener;


/**
 *
 * CLASE MAIN ACTIVITY - LOG IN
 *
 * Copyrigth © 2025
 *
 * Fecha, autor, aportacion: Sandra Moll Cots, Permisos bluetooh, ubicacion y  comportamiento login
 * - Logica fake log in (30/10/25 - Sandra)
 * - Lector QR (16/11/25 - Rocio)
 * - Acceso biometrico (17/11/25 - Rocio)
 * - Logica bbdd log in (30/10/25 - Rocio)
 */


/**
 * @class MainActivity
 * @brief Clase que gestiona la pantalla de inicio de sesión de la aplicación.
 *
 * Responsabilidades:
 * 1. **Verificación de Sensor:** Comprueba la validez de un código de sensor en **Firebase Firestore**.
 * 2. **Lector QR:** Permite escanear códigos QR para obtener el ID del sensor.
 * 3. **Autenticación Biométrica:** Ofrece acceso rápido si existe un ID de sensor previamente guardado.
 * 4. **Persistencia:** Guarda el último ID de sensor utilizado mediante SharedPreferences.
 *
 * @extends AppCompatActivity
 */
public class MainActivity extends AppCompatActivity {

    /** @brief Campo de texto para ingresar manualmente el código del sensor. */
    private EditText sensorCodeEditText;

    /** @brief Instancia de Firebase Firestore para la verificación del código. */
    private FirebaseFirestore db;



    // --- Definición del Lanzador de Permisos ----------------------------------------------------------------------------
    // Permisos para abrir la camara (si acepta llama al metodo para abrir la camara, si no se muestra un toast)
    /**
     * @brief Launcher para solicitar el permiso de cámara necesario para el escáner QR.
     * Si el permiso es concedido, llama a \ref openCamera().
     */
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                }
            });
    // --- Fin Lanzador de Permisos ---------------------------------------------------------------------------------------


    //----- Lanzador de la camara -----------------------------------------------------------------------------------------
    /** @brief Launcher para iniciar la actividad del escáner QR y manejar su resultado. */
    private ActivityResultLauncher<Intent> qrScannerLauncher;
    //----- Fin Lanzador de la camara -------------------------------------------------------------------------------------


    // --- Metodo Principal (onCreate) ----------------------------------------------------------------
    /**
     * @brief Método llamado al crear la actividad.
     * (savedInstanceState:Bundle) -> onCreate() -> ()
     * @param savedInstanceState Si la actividad se está recreando, este Bundle contiene los datos de estado.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Vinculamos con la vista de login
        setContentView(R.layout.login);

        // Inicialización de Firebase
        db = FirebaseFirestore.getInstance();

        // Buscamos los elementos de la vista  y los inicializamos en variables
        sensorCodeEditText = findViewById(R.id.editText_codigo);
        Button loginButton = findViewById(R.id.button_entrar);
        TextView qrCodeTextView = findViewById(R.id.textView_codigoQR);

        // Llamada a acceso biométrico
        mostrarAutenticacionBiometrica();

        /**
         * @brief Inicializamos el manejador de resultados del QR.
         * Procesa el resultado del escaneo, pegando el contenido en el campo del código del sensor.
         */
        //Inicializamos el manejador de resultados del QR
        qrScannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Manejador del resultado de la actividad QR
                    IntentResult scanResult = IntentIntegrator.parseActivityResult(result.getResultCode(), result.getData());

                    if (scanResult != null && scanResult.getContents() != null) {
                        String codigoQR = scanResult.getContents();

                        //PEGAR EL CÓDIGO EN EL CAMPO
                        sensorCodeEditText.setText(codigoQR);

                        Toast.makeText(this, "Código de sesión pegado", Toast.LENGTH_SHORT).show();

                    } else if (result.getResultCode() == RESULT_CANCELED) {
                        Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show();
                    }
                }
        );


        /**
         * @brief Listener para el botón de login.
         * Recoge el código, verifica que no esté vacío y llama a \ref checkSensorCodeInDatabase().
         */
        loginButton.setOnClickListener(v -> {
            String sensorCode = sensorCodeEditText.getText().toString().trim();

            // Comprobamos que el campo no esté vacío
            if (sensorCode.isEmpty()) {
                // Si el campo está vacío, mostramos un mensaje de error
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error, campo vacío")
                        .setMessage("Añade el código del sensor para vincular.")
                        .setPositiveButton("Aceptar", null)
                        .show();
            } else {
                // Si no está vacío, iniciamos la verificación asíncrona con Firebase
                checkSensorCodeInDatabase(sensorCode);
            }
        });

        /**
         * @brief Listener para el texto del QR.
         * Comprueba el permiso de cámara y, si es necesario, lo solicita antes de abrir la cámara.
         */
        qrCodeTextView.setOnClickListener(v -> {
            //Comprobamos si hay permiso para abrir la camara. Si no lo tenemos, lo pedimos!!
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });
    }
    // --- Fin Metodo Principal (onCreate) ------------------------------------------------------------------------------------------


    // --- Intent para abrir la camara ------------------------------------------------------------------------------------
    /**
     * @brief Inicializa y lanza la actividad del escáner QR.
     * () -> openCamera() -> ()
     */
    private void openCamera() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Escanea el Código de Sesión");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(true);

        Intent scanIntent = integrator.createScanIntent();

        //Iniciar el escaneo usando el lanzador que configuramos para manejar el resultado
        qrScannerLauncher.launch(scanIntent);
    }
    // --- Fin Intent para abrir la camara ------------------------------------------------------------------------------


    // --- Metodos para acceso biométrico --------------------------------------------------------------------------
    /**
     * @brief Muestra el diálogo de autenticación biométrica (huella dactilar).
     * Si la autenticación es exitosa, intenta recuperar el último ID de sensor guardado para iniciar la sesión rápida.
     * () -> mostrarAutenticacionBiometrica() -> ()
     */
    private void mostrarAutenticacionBiometrica() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Acceso Biométrico")
                .setSubtitle("Usa tu huella dactilar para iniciar sesión")
                .setNegativeButtonText("Usar código de sensor") // Opción para volver al login normal
                .setConfirmationRequired(true)
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(
                MainActivity.this,
                ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        // 1. Recuperar el último ID guardado
                        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        String lastSensorId = prefs.getString("LAST_SENSOR_ID", null);

                        if (lastSensorId != null) {
                            // 2. Si se encuentra un ID real, iniciar la sesión con ese ID.
                            Log.d("BIOMETRIA", "Acceso biométrico exitoso. Usando ID guardado: " + lastSensorId);
                            iniciarSesionExitosa(lastSensorId); // ¡Usamos el ID real aquí!
                        } else {
                            // 3. Caso de primer uso: No hay ID guardado.
                            Toast.makeText(MainActivity.this, "Inicie sesión con un código primero para habilitar el acceso rápido.", Toast.LENGTH_LONG).show();
                            // Nota: Mantenemos el usuario en la pantalla de login.
                        }
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        // Error
                        Toast.makeText(MainActivity.this, "Error de autenticación: " + errString, Toast.LENGTH_SHORT).show();
                    }
                });

        biometricPrompt.authenticate(promptInfo);
    }

    /**
     * @brief Guarda el código del sensor en SharedPreferences e inicia la \ref SesionSensorActivity.
     * Esta función se llama tras un login exitoso (manual o biométrico).
     * (code:String) -> iniciarSesionExitosa() -> ()
     * @param code El código de sensor verificado.
     */
    private void iniciarSesionExitosa(String code) {
        // Esta acción se realiza siempre, sobreescribiendo el ID con el mismo ID, lo cual es seguro.
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().putString("LAST_SENSOR_ID", code).apply();
        Log.d("BIOMETRIA", "ID de sensor guardado/actualizado: " + code);

        // Inicio de actividad
        Intent intent = new Intent(MainActivity.this, SesionSensorActivity.class);
        intent.putExtra("SENSOR_CODE", code);
        startActivity(intent);
        finish();
    }

    // --- Fin Metodos para acceso biométrico ----------------------------------------------------------------------

    // --- Inicio Metodo para comprobación firebase ----------------------------------------------------------------------

    /**
     * @brief Verifica asíncronamente si el código del sensor existe como documento en la colección 'sensores' de Firebase Firestore.
     * (sensorCode:String) -> checkSensorCodeInDatabase() -> ()
     * @param sensorCode El código de sensor ingresado por el usuario.
     */
    private void checkSensorCodeInDatabase(String sensorCode) {
        Toast.makeText(this, "Verificando código en Firestore...", Toast.LENGTH_SHORT).show();

        // Referencia específica al documento del sensor: coleccion(sensores)/documento(código)
        db.collection("sensores").document(sensorCode)
                .get() // Consulta de un solo evento
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            // Diseño Lógico: Validación de Existencia
                            if (document.exists()) {
                                // Éxito: El documento existe en la colección 'sensores'.
                                Log.d("Firestore_LOGIN", "Sensor encontrado: " + sensorCode);
                                Toast.makeText(MainActivity.this, "Conexión exitosa con el sensor.", Toast.LENGTH_SHORT).show();
                                iniciarSesionExitosa(sensorCode);
                            } else {
                                // Fallo 1: El código NO existe
                                Log.e("Firestore_LOGIN", "Código NO encontrado: " + sensorCode);
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Error de Vinculación")
                                        .setMessage("El código de sensor '" + sensorCode + "' no es válido o no está registrado.")
                                        .setPositiveButton("Aceptar", null)
                                        .show();
                            }
                        } else {
                            // Fallo 2: Error de conexión o tarea
                            Log.e("Firestore_LOGIN", "Fallo al consultar Firestore: ", task.getException());
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Error de Conexión")
                                    .setMessage("Fallo al consultar la BD: " + task.getException().getMessage())
                                    .setPositiveButton("Aceptar", null)
                                    .show();
                        }
                    }
                });
    }
    // --- Fin Metodo para comprobación firebase ----------------------------------------------------------------------



}
