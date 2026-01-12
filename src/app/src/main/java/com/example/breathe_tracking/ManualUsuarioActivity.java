/**
 * @file ManualUsuarioActivity.java
 * @brief Actividad que muestra la documentación de ayuda y preguntas frecuentes.
 * @package com.example.breathe_tracking
 * @copyright Copyright © 2025
 */

package com.example.breathe_tracking;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

/**
 * @class ManualUsuarioActivity
 * @brief Actividad de ayuda que presenta el Manual de Usuario y FAQ mediante un diseño de acordeón.
 * @extends AppCompatActivity
 *
 * @details
 * Esta actividad proporciona soporte al usuario explicando el funcionamiento de la aplicación.
 * Para mejorar la usabilidad y no saturar la pantalla con texto, implementa un patrón de UI tipo **Acordeón** (Expandable List).
 *
 *
 *
 * **Funcionamiento:**
 * - La información está dividida en 7 categorías temáticas.
 * - Al pulsar sobre una cabecera, el contenido asociado se expande o contrae.
 * - Incluye animaciones visuales (rotación de flechas) para indicar el estado de cada sección.
 *
 * @author Sandra (Diseño UI y Lógica de acordeón)
 * @date 20/12/2025
 */

public class ManualUsuarioActivity extends AppCompatActivity {

    /**
     * @brief Inicializa la actividad y configura las secciones del manual.
     *
     * Realiza el enlace (binding) de los componentes de la interfaz para las 7 secciones:
     * 1. Inicio de Sesión.
     * 2. Panel y Estado.
     * 3. Contaminantes.
     * 4. Gráficas.
     * 5. Alertas.
     * 6. Incidencias.
     * 7. Preguntas Frecuentes (FAQ).
     *
     * (savedInstanceState:Bundle) -> onCreate() -> ()
     * @param savedInstanceState Estado guardado de la aplicación.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manual_usuario);

        // Funcionalidad del botón de volver
        findViewById(R.id.img_back_faq).setOnClickListener(v -> finish());

        // Inicializar los 7 acordeones
        // 1. Inicio de Sesión
        setupAccordion(R.id.header_login, R.id.content_login, R.id.arrow_login);
        // 2. Panel y Estado
        setupAccordion(R.id.header_panel, R.id.content_panel, R.id.arrow_panel);
        // 3. Contaminantes
        setupAccordion(R.id.header_contaminantes, R.id.content_contaminantes, R.id.arrow_contaminantes);
        // 4. Gráficas
        setupAccordion(R.id.header_graficas, R.id.content_graficas, R.id.arrow_graficas);
        // 5. Alertas
        setupAccordion(R.id.header_alertas, R.id.content_alertas, R.id.arrow_alertas);
        // 6. Incidencias
        setupAccordion(R.id.header_incidencias, R.id.content_incidencias, R.id.arrow_incidencias);
        // 7. FAQ
        setupAccordion(R.id.header_faq, R.id.content_faq, R.id.arrow_faq);
    }

    /**
     * @brief Método auxiliar para configurar la lógica de expansión/colapso de una sección.
     *
     * Asigna un `OnClickListener` al layout de cabecera que:
     * 1. Alterna la visibilidad del layout de contenido (`View.VISIBLE` <-> `View.GONE`).
     * 2. Anima la rotación de la flecha indicadora (0° -> 180°) para dar feedback visual.
     * (headerId:int, contentId:int, arrowId:int) -> setupAccordion() -> ()
     *
     * @param headerId ID del recurso (LinearLayout) que actúa como cabecera/botón.
     * @param contentId ID del recurso (LinearLayout) que contiene la información a mostrar/ocultar.
     * @param arrowId ID del recurso (ImageView) del icono de la flecha.
     */
    private void setupAccordion(int headerId, int contentId, int arrowId) {
        LinearLayout header = findViewById(headerId);
        LinearLayout content = findViewById(contentId);
        ImageView arrow = findViewById(arrowId);

        if (header != null && content != null && arrow != null) {
            header.setOnClickListener(v -> {
                boolean isVisible = content.getVisibility() == View.VISIBLE;
                content.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                arrow.animate().rotation(isVisible ? 0 : 180).setDuration(200).start();
            });
        }
    }
}
