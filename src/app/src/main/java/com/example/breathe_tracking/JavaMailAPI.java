/**
 * @file JavaMailAPI.java
 * @brief Implementación de tarea asíncrona para el envío de correos electrónicos SMTP.
 * @package com.example.breathe_tracking
 * @copyright Copyright © 2025
 */

package com.example.breathe_tracking;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * @class JavaMailAPI
 * @brief Gestiona el envío de correos electrónicos en segundo plano.
 * @extends AsyncTask<Void, Void, Void>
 *
 * @details
 * Esta clase evita el bloqueo del hilo principal (Main Thread) al realizar operaciones de red.
 * Utiliza la biblioteca **JavaMail** para conectar con el servidor SMTP de Gmail.
 *
 * **Características principales:**
 * - Autenticación segura mediante "Contraseña de Aplicación" de Google.
 * - Construcción de mensajes MIME multipartes (soporte HTML).
 * - Inyección de estilos CSS para correos corporativos/profesionales.
 * - Notificación visual (Toast) al finalizar el envío.
 */
public class JavaMailAPI extends AsyncTask<Void, Void, Void> {

    // --- CONFIGURACIÓN DEL ROBOT ---
    /** @brief Dirección de correo electrónico del remitente (Cuenta Robot). */
    private String EMAIL_ROBOT = "rousio2211@gmail.com";

    /** @brief Contraseña de aplicación (App Password) generada por Google para autenticación segura. */
    private String PASSWORD_ROBOT = "qksu kdas eluz wofs";

    // -------------------------------

    /** @brief Contexto de la aplicación, necesario para mostrar mensajes Toast en la UI. */
    private Context context;

    /** @brief Sesión de correo que mantiene la configuración y autenticación. */
    private Session session;

    /** @brief Dirección de correo electrónico del destinatario (Administrador). */
    private String emailDestino;

    /** @brief Asunto del correo electrónico. */
    private String asunto;

    /** @brief Cuerpo del mensaje (texto base que será formateado a HTML). */
    private String mensaje;

    /**
     * @brief Constructor de la clase JavaMailAPI.
     *
     * Inicializa los datos necesarios para componer y enviar el correo.
     *
     *
     * @param context Contexto de la actividad o aplicación.
     * @param emailDestino Dirección de email a la que se enviará la alerta.
     * @param asunto Título del correo.
     * @param mensaje Contenido del mensaje.
     */
    public JavaMailAPI(Context context, String emailDestino, String asunto, String mensaje) {
        this.context = context;
        this.emailDestino = emailDestino;
        this.asunto = asunto;
        this.mensaje = mensaje;
    }

    /**
     * @brief Lógica de conexión y envío en hilo secundario.
     *
     * 1. Configura las propiedades (`Properties`) para TLS/SSL en el puerto 465 de Gmail.
     * 2. Inicia sesión (`Session`) con las credenciales del robot.
     * 3. Crea el objeto `MimeMessage`.
     * 4. Asigna un alias ("⚠️ Alertas Breathe Tracking") al remitente.
     * 5. Genera el contenido HTML llamando a @ref construirHTML.
     * 6. Envía el mensaje mediante `Transport.send()`.
     *
     * @param params Void (no se usan parámetros variables).
     * @return null
     */
    @Override
    protected Void doInBackground(Void... params) {
        Properties props = new Properties();
        // Configuración para conectarse a Gmail
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        // Iniciar sesión
        session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_ROBOT, PASSWORD_ROBOT);
            }
        });

        try {
            MimeMessage mm = new MimeMessage(session);

            /** @brief Configuración del remitente con alias visual personalizado para apariencia profesional. */
            mm.setFrom(new InternetAddress(EMAIL_ROBOT, "⚠️ Alertas Breathe Tracking"));
            // ---------------------------

            mm.addRecipient(Message.RecipientType.TO, new InternetAddress(emailDestino));
            mm.setSubject(asunto);

            String contenidoHTML = construirHTML(asunto, mensaje);
            /** @brief Establece el contenido como HTML con codificación UTF-8 para soporte de tildes. */
            mm.setContent(contenidoHTML, "text/html; charset=utf-8");

            Transport.send(mm);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @brief Se ejecuta en el hilo principal (UI Thread) al finalizar el envío.
     * Muestra una notificación Toast al usuario indicando que la incidencia se ha enviado.
     * (result:Void) -> onPostExecute() -> ()
     *
     * @param result Resultado de doInBackground (siempre null en este caso).
     */
    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        // Mostrar aviso al usuario cuando termine
        Toast.makeText(context, "Incidencia enviada correctamente", Toast.LENGTH_LONG).show();
    }

    // --- NUEVO MÉTODO PARA CREAR EL DISEÑO HTML ---
    /**
     * @brief Genera una estructura HTML con estilos CSS integrados para el cuerpo del correo.
     *
     * Crea un diseño visual profesional con cabecera corporativa, contenedor de datos
     * y pie de página, insertando dinámicamente el título y el mensaje.
     * (titulo:String, mensaje:String) -> construirHTML() -> String (HTML)
     *
     * @param titulo El título de la incidencia.
     * @param mensaje El mensaje descriptivo de la incidencia.
     * @return String que contiene el código HTML completo del correo.
     */
    private String construirHTML(String titulo, String mensaje) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body {font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f4f4f4;}" +
                ".container {max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 5px rgba(0,0,0,0.1);}" +
                ".header {background-color: #0E344C; color: #ffffff; padding: 20px; text-align: center;}" +
                ".header h1 {margin: 0; font-size: 24px; letter-spacing: 2px;}" +
                ".content {padding: 30px; color: #333333; line-height: 1.6;}" +
                ".label {font-weight: bold; color: #0E344C; display: block; margin-top: 15px;}" +
                ".value {background-color: #f9f9f9; padding: 10px; border-left: 4px solid #0E344C; margin-top: 5px;}" +
                ".footer {background-color: #eeeeee; text-align: center; padding: 15px; font-size: 12px; color: #777777;}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "  <div class='container'>" +
                "    <div class='header'>" +
                "      <h1>BREATHE TRACKING</h1>" +
                "      <p>Sistema de Gestión de Incidencias</p>" +
                "    </div>" +
                "    <div class='content'>" +
                "      <p>Hola Administrador,</p>" +
                "      <p>Se ha recibido un nuevo reporte automático desde la aplicación móvil.</p>" +
                "      <hr style='border: 0; border-top: 1px solid #eee; margin: 20px 0;'>" +

                "      <span class='label'>TÍTULO:</span>" +
                "      <div class='value'>" + titulo + "</div>" +

                "      <span class='label'>DETALLE DEL MENSAJE:</span>" +
                "      <div class='value'>" + mensaje + "</div>" +

                "      <p style='margin-top: 30px; font-size: 0.9em;'><em>Por favor, revise el panel de control para más detalles técnicos del sensor afectado.</em></p>" +
                "    </div>" +
                "    <div class='footer'>" +
                "      <p>Este es un mensaje automático. No responder a este correo.</p>" +
                "      <p>&copy; 2025 Breathe Tracking System</p>" +
                "    </div>" +
                "  </div>" +
                "</body>" +
                "</html>";
    }
}