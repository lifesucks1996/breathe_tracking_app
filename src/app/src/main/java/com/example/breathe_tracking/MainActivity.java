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
 * Nombre: Sandra Moll Cots
 *
 * CLASE MAIN ACTIVITY - LOG IN
 * - Logica fake log in
 * - Lector QR
 *
 * Copyrigth © 2025
 * Fecha, autor, aportacion: Sandra Moll Cots, Permisos bluetooh, ubicacion y  comportamiento login
 * - Logica fake log in (30/10/25 - Sandra)
 * - Lector QR (16/11/25 - Rocio)
 * - Acceso biometrico (17/11/25 - Rocio)
 */


public class MainActivity extends AppCompatActivity {

    // EditText para el código del sensor
    private EditText sensorCodeEditText;

    // Referencia a la base de datos de Firebase
    private FirebaseFirestore db;



    // --- Definición del Lanzador de Permisos ----------------------------------------------------------------------------
    // Permisos para abrir la camara (si acepta llama al metodo para abrir la camara, si no se muestra un toast)
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
    private ActivityResultLauncher<Intent> qrScannerLauncher;
    //----- Fin Lanzador de la camara -------------------------------------------------------------------------------------


    // --- Metodo Principal (onCreate) ----------------------------------------------------------------

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


        /* Definimos el comportamiento del boton de login
        loginButton.setOnClickListener(v -> {
            String sensorCode = sensorCodeEditText.getText().toString();

            // Comprobamos que el campo no esté vacío y que el codigo sea correcto (12345)
            if (sensorCode.isEmpty()) {
                // Si el campo está vacío, mostramos un mensaje de error
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error, campo vacío")
                        .setMessage("Añade el código del sensor para vincular.")
                        .setPositiveButton("Aceptar", null)
                        .show();
            } else if ("12345".equals(sensorCode)) {
                // Si el código es correcto, pasamos a la siguiente actividad
                Intent intent = new Intent(MainActivity.this, SesionSensorActivity.class);
                intent.putExtra("SENSOR_CODE", sensorCode);
                startActivity(intent);
                finish(); // Cerramos la actividad de login si es correcto
            } else {
                // Si el código es incorrecto, mostramos un mensaje de error
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error")
                        .setMessage("El código de vinculación es incorrecto.")
                        .setPositiveButton("Aceptar", null)
                        .show();
            }
        });*/

        // Definimos el comportamiento del boton de login
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

        //Texto que abre la camara QR
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


    // --- Metodo para acceso biométrico --------------------------------------------------------------------------
    private void mostrarAutenticacionBiometrica() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Acceso Biométrico")
                .setSubtitle("Usa tu huella dactilar o rostro para iniciar sesión")
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

    // Dentro de MainActivity.java

    private void iniciarSesionExitosa(String code) {

        // 1. Lógica de Guardado (SOLO si no es la propia biometría la que lo está llamando)
        if (!"BIOMETRIC_CODE".equals(code)) {
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            prefs.edit().putString("LAST_SENSOR_ID", code).apply();
            Log.d("BIOMETRIA", "ID de sensor guardado para uso futuro: " + code);
        }

        // 2. Inicio de actividad (Se mantiene igual)
        Intent intent = new Intent(MainActivity.this, SesionSensorActivity.class);
        intent.putExtra("SENSOR_CODE", code);
        startActivity(intent);
        finish();
    }

    // --- Fin Metodo para acceso biométrico ----------------------------------------------------------------------

    // --- Inicio Metodo para comprobación firebase ----------------------------------------------------------------------

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
