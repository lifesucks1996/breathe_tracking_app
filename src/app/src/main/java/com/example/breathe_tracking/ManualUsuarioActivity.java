package com.example.breathe_tracking;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

/**
 * @class ManualUsuarioActivity
 * @brief Actividad que muestra el Manual de Usuario y FAQ con diseño de acordeón multinivel.
 */
public class ManualUsuarioActivity extends AppCompatActivity {

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
     * @brief Configura el listener de clic para expandir/colapsar una sección.
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
