package com.example.breathe_tracking;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Copyrigth © 2025
 *
 * Esta actividad carga los datos de firebase para mostrar la evolución de cada contaminante en un periodo de 24h
 * recogiendo los datos de la colección "datos_grafico".
 * 27/11 - Sandra: creacion actividad y xml
 * 06/12 - Rocio: conexión con base de datos
 */


/**
 * @class GasData
 * @brief Modelo de datos unificado para almacenar las 5 concentraciones por una hora.
 */
class GasData {
    public float ozono;
    public float co2;
    public float co;
    public float no2;
    public float so2;
    public long timestamp;

    public GasData(float ozono, float co2, float co, float no2, float so2, long timestamp) {
        this.ozono = ozono;
        this.co2 = co2;
        this.co = co;
        this.no2 = no2;
        this.so2 = so2;
        this.timestamp = timestamp;
    }
    public GasData(long timestamp) {
        this.co = 0f;
        this.co2 = 0f;
        this.no2 = 0f;
        this.ozono = 0f;
        this.so2 = 0f;
        this.timestamp = timestamp;
    }
}

/**
 * @class HourValueFormatter
 * @brief Formateador simple que convierte el índice de la hora (0-23) a formato HH:00.
 */
class HourValueFormatter extends ValueFormatter {
    private final DecimalFormat mFormat = new DecimalFormat("00");

    @Override
    public String getFormattedValue(float value) {
        int hour = Math.round(value);
        // Formateamos como 00:00, 01:00, etc.
        return mFormat.format(hour) + ":00";
    }
}

/**
 * @class DecimalValueFormatter (Clase Adaptadora para Eje Y)
 * @brief Adaptador para formatear valores numéricos con precisión decimal.
 */
class DecimalValueFormatter extends ValueFormatter {
    private final DecimalFormat mFormat;

    public DecimalValueFormatter(String pattern) {
        mFormat = new DecimalFormat(pattern);
    }

    @Override
    public String getFormattedValue(float value) {
        return mFormat.format(value);
    }
}

/**
 * @class InformacionActivity
 * @brief Muestra el gráfico de datos históricos (CO2 y O3) vinculados al sensor
 * que inició la sesión, leídos de Firebase Firestore.
 *
 * Copyrigth © 2025
 *
 */
public class InformacionActivity extends AppCompatActivity {

    // --- VARIABLES DE FIREBASE ---
    private FirebaseFirestore db;
    private String sensorId;
    private List<GasData> historicalData = new ArrayList<>();
    // -----------------------------

    // ALMACENA EL TIMESTAMP DE MEDIANOCHE DE HOY
    private long midnightTimestamp;

    private LineChart chart;
    private Spinner spinner;
    private ImageView imgCarita;
    private ImageView imgBackArrow;
    private TextView txtExplicacion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.informacion);

        // 1. Inicialización de Firebase y obtención del ID
        db = FirebaseFirestore.getInstance();

        //views
        chart = findViewById(R.id.chart_gases);
        spinner = findViewById(R.id.spinner_gases);
        imgCarita = findViewById(R.id.img_carita);
        imgBackArrow = findViewById(R.id.img_back_arrow);
        txtExplicacion = findViewById(R.id.txt_explicacion);

        imgBackArrow.setOnClickListener(v -> finish());

        // 2. Configurar Spinner
        String[] gases = {
                "Ozono (O3)",
                "Monóxido de Carbono (CO)",
                "Dióxido de Nitrógeno (NO2)",
                "Dióxido de Azufre (SO2)",
                "Dióxido de Carbono (CO2)"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, gases);
        spinner.setAdapter(adapter);

        // 3. Listener del Spinner
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                cargarDatosGas(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        // 4. Configuración base del gráfico
        configurarEstiloGrafico();

        // 5. Cargar los datos de Firebase y luego cargar el gráfico por defecto (Ozono)
        cargarDatosDeFirebase();
    }

    /**
     * @brief Consulta Firestore para obtener los datos históricos del sensor actual
     */
    private void cargarDatosDeFirebase() {

        Toast.makeText(this, "Cargando datos por hora...", Toast.LENGTH_SHORT).show();

        // IDs de documento que esperamos encontrar en Firestore
        final String CO_ID = "co";
        final String CO2_ID = "co2";
        final String NO2_ID = "no2";
        final String O3_ID = "ozono";
        final String SO2_ID = "so2";

        db.collection("datos_grafico").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    Map<String, List<Number>> hourlyArrays = new HashMap<>();

                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "Colección 'datos_grafico' vacía.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // 1. Procesar los documentos y guardar los arrays
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String contaminantId = document.getId();
                        // Firestore devuelve arrays de números como List<Number>
                        List<Number> values = (List<Number>) document.get("valor");

                        if (values != null && values.size() >= 24) {
                            hourlyArrays.put(contaminantId, values);
                        }
                    }

                    // 2. Inicializar la lista final con 24 entradas (una por hora)
                    historicalData.clear();
                    for (int i = 0; i < 24; i++) {
                        historicalData.add(new GasData(0L)); // Inicializa todos los valores en 0
                    }

                    // 3. Llenar los datos de cada hora
                    for (Map.Entry<String, List<Number>> entry : hourlyArrays.entrySet()) {
                        String contaminantId = entry.getKey();
                        List<Number> values = entry.getValue();

                        for (int h = 0; h < 24 && h < values.size(); h++) {
                            GasData data = historicalData.get(h);

                            // Conversión segura a float (ya que todos los valores son numéricos en la BD)
                            float value = values.get(h).floatValue();

                            switch (contaminantId) {
                                case CO_ID: data.co = value; break;
                                case CO2_ID: data.co2 = value; break;
                                case NO2_ID: data.no2 = value; break;
                                case O3_ID: data.ozono = value; break;
                                case SO2_ID: data.so2 = value; break;
                            }
                        }
                    }

                    // 4. Configurar el Eje X y cargar la gráfica por defecto (Ozono = índice 0)
                    configurarEjeXDemo();
                    cargarDatosGas(spinner.getSelectedItemPosition());
                    Toast.makeText(InformacionActivity.this, "Datos cargados con éxito (24 horas).", Toast.LENGTH_SHORT).show();

                })
                .addOnFailureListener(e -> {
                    Log.e("FirebaseDebug", "Error al leer datos DEMO: " + e.getMessage(), e);
                    Toast.makeText(InformacionActivity.this, "Error lectura datos", Toast.LENGTH_LONG).show();
                });
    }


    private void configurarEstiloGrafico() {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setTextColor(Color.BLACK);


        chart.getAxisRight().setEnabled(false);
    }

    /**
     * @brief Configura el Eje X para mostrar 24 horas completas (0 a 23).
     */
    private void configurarEjeXDemo() {
        XAxis xAxis = chart.getXAxis();

        // 1. Aplicar el formateador simple (0 -> 00:00, 1 -> 01:00, etc.)
        xAxis.setValueFormatter(new HourValueFormatter());

        // 2. FORZAR RANGO: El rango máximo debe ser 24 para que el índice 23 tenga espacio.
        xAxis.setAxisMinimum(0f);      // Mínimo: 0 (Hora 0)
        xAxis.setAxisMaximum(23f);     // MÁXIMO CORREGIDO: 24 (Representa el punto final 24:00)

        // 3. Mejorar legibilidad (Etiquetas cada 2 horas)
        xAxis.setGranularity(1f);      // Paso de 2 horas (2)
        xAxis.setLabelCount(12, true); // 13 etiquetas (00, 02, 04, ..., 24)
        xAxis.setLabelRotationAngle(-45); // Rotar etiquetas
    }

    /**
     * @brief Carga y dibuja los datos del gas seleccionado.
     */
    private void cargarDatosGas(int position) {

        float limiteSeguro = 0f;
        float limitePeligro = 0f;
        String unidad = "";
        String nombreGas = "";

        List<Entry> entries = new ArrayList<>();
        int color = Color.GRAY;
        float yMax = 1f;

        // Formateadores para Eje Y (usando la solución de clase anónima)
        final DecimalFormat formatTwoDecimals = new DecimalFormat("0.00");
        final DecimalFormat formatZeroDecimals = new DecimalFormat("0");


        // Iterar sobre los datos ya cargados y extraer el valor del gas correspondiente
        if (!historicalData.isEmpty()) {
            for (int i = 0; i < historicalData.size(); i++) {
                GasData data = historicalData.get(i);
                float valueY = 0f;

                // El valor X es el índice 'i', que representa la hora (0, 1, 2... 23)
                float valueX = (float) i;

                switch (position) {
                    case 0: // Ozono (O3)
                        valueY = data.ozono;
                        nombreGas = "Ozono (O3)"; unidad = "ppm"; color = Color.parseColor("#8A2BE2");
                        limiteSeguro = 0.6f; limitePeligro = 0.9f; yMax = 1f;
                        break;
                    case 1: // Monóxido de Carbono (CO)
                        valueY = data.co;
                        nombreGas = "Monóxido de Carbono (CO)"; unidad = "mg/m³"; color = Color.parseColor("#FF8C00");
                        limiteSeguro = 10f; limitePeligro = 30f; yMax = 40f;
                        break;
                    case 2: // Dióxido de Nitrógeno (NO2)
                        valueY = data.no2;
                        nombreGas = "Dióxido de Nitrógeno (NO2)"; unidad = "µg/m³"; color = Color.parseColor("#E6A100");
                        limiteSeguro = 200f; limitePeligro = 400f; yMax = 500f;
                        break;
                    case 3: // Dióxido de Azufre (SO2)
                        valueY = data.so2;
                        nombreGas = "Dióxido de Azufre (SO2)"; unidad = "µg/m³"; color = Color.parseColor("#00BFFF");
                        limiteSeguro = 350f; limitePeligro = 500f; yMax = 700f;
                        break;
                    case 4: // Dióxido de Carbono (CO2)
                        valueY = data.co2;
                        nombreGas = "Dióxido de Carbono (CO2)"; unidad = "ppm"; color = Color.parseColor("#32CD32");
                        limiteSeguro = 800f; limitePeligro = 1200f; yMax = 1300f;
                        break;
                }
                entries.add(new Entry(valueX, valueY));
            }
        }

        // Configuración de Eje Y y Limit Lines
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        leftAxis.setAxisMinimum(0f);

        // Ajuste de precisión del Eje Y
        if (position == 0) { // Ozono (ppm) requiere precisión de dos decimales
            leftAxis.setValueFormatter(new ValueFormatter() {
                @Override public String getFormattedValue(float value) { return formatTwoDecimals.format(value); }
            });
            leftAxis.setGranularity(0.1f);
        } else {
            leftAxis.setValueFormatter(new ValueFormatter() {
                @Override public String getFormattedValue(float value) { return formatZeroDecimals.format(value); }
            });
            leftAxis.setGranularity(1f);
        }


        LimitLine llSeguro = new LimitLine(limiteSeguro, "Seguro (" + limiteSeguro + " " + unidad + ")");
        llSeguro.setLineColor(Color.GREEN);
        llSeguro.setLineWidth(2f);
        llSeguro.enableDashedLine(10f, 10f, 0f);
        llSeguro.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_BOTTOM);
        llSeguro.setTextSize(10f);

        LimitLine llPeligro = new LimitLine(limitePeligro, "Peligroso (" + limitePeligro + " " + unidad + ")");
        llPeligro.setLineColor(Color.RED);
        llPeligro.setLineWidth(2f);
        llPeligro.enableDashedLine(10f, 10f, 0f);
        llPeligro.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        llPeligro.setTextSize(10f);

        leftAxis.addLimitLine(llSeguro);
        leftAxis.addLimitLine(llPeligro);
        leftAxis.setAxisMaximum(yMax); // Establece el límite superior basado en el contaminante


        // Creación del Dataset y estilización
        LineDataSet dataSet = new LineDataSet(entries, "Nivel de " + nombreGas);
        dataSet.setColor(color); // Usar el color asignado
        dataSet.setLineWidth(1f);
        dataSet.setCircleRadius(2f);
        dataSet.setDrawCircleHole(false);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);

        dataSet.setFillDrawable(ContextCompat.getDrawable(this, R.drawable.gradiente_semaforo));

        // Aplicar y animar el gráfico
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.animateX(1000);

        chart.invalidate();

        // Evaluar exposición
        evaluarExposicion(entries, limiteSeguro, limitePeligro);
    }

    private void evaluarExposicion(List<Entry> datos, float limiteSeguro, float limitePeligro) {
        int contadorPeligroso = 0;
        int contadorRiesgo = 0;

        for (Entry e : datos) {
            float val = e.getY();
            if (val > limitePeligro) {
                contadorPeligroso++;
            } else if (val > limiteSeguro) {
                contadorRiesgo++;
            }
        }

        if (contadorPeligroso >= 4) {
            // Nivel 5: CRÍTICO (4 o más puntos en el rango ROJO)
            imgCarita.setImageResource(R.drawable.ic_face_sad);
            txtExplicacion.setText("¡PELIGRO CRÍTICO! Exposición prolongada. Evite la zona de inmediato.");

        } else if (contadorPeligroso > 0) {
            // Nivel 4: PELIGRO AISLADO (1 a 3 picos en el rango ROJO)
            // Esto cubre tu regla: "si solo hay un punto peligroso sea regular [peligro aislado]"
            imgCarita.setImageResource(R.drawable.ic_face_neutral);
            txtExplicacion.setText("ADVERTENCIA: Se ha detectado un pico peligroso puntual.");

        } else if (contadorRiesgo >= 10) {
            // Nivel 3: RIESGO MODERADO (Múltiples puntos en el rango de riesgo/sobre el límite bueno)
            imgCarita.setImageResource(R.drawable.ic_face_neutral);
            txtExplicacion.setText("ALERTA MODERADA: La calidad del aire es mala, muchos puntos fuera de lo 'Bueno'.");

        } else if (contadorRiesgo > 0) {
            // Nivel 2: ACEPTABLE CON ADVERTENCIA (1 a 9 puntos ligeramente sobre el límite bueno)
            imgCarita.setImageResource(R.drawable.ic_face_happy);
            txtExplicacion.setText("ACEPTABLE: Calidad del aire dentro de los límites, pero con pequeñas variaciones.");

        } else {
            // Nivel 1: ÓPTIMO (Todos los puntos son "Buenos" o inferiores)
            imgCarita.setImageResource(R.drawable.ic_face_happy);
            txtExplicacion.setText("¡ÓPTIMO! Calidad del aire excelente y segura.");
        }
    }
}