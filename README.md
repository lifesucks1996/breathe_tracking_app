# 📱 README: App de Nodoportador (Worker App) - Breathe Tracking

Este documento describe el funcionamiento de la **Aplicación Móvil Ligera (App)** diseñada para el **Nodoportador** (el trabajador o empleado).

El objetivo principal de esta aplicación es servir como un panel de control simple para que el trabajador pueda **vincularse a un sensor físico** y **verificar su estado operativo** durante sus rutas de trabajo.

## 👥 Rol del Nodoportador

El **Nodoportador** es el empleado (ej. repartidor, técnico de mantenimiento, cartero) que lleva el sensor (nodo) en sus rutas. La recolección de datos es pasiva.

La aplicación móvil **no es para la recolección de datos** (eso lo hace el hardware del sensor de forma automática), sino para la **vinculación inicial** y la **supervisión de incidencias**.

***

## ⚙️ Flujo de la Aplicación y Funcionalidades

La aplicación se compone de dos pantallas principales, como se muestra en el diseño:

### 1. Página de Vinculación (Login)

Al iniciar la aplicación, el trabajador debe vincular la sesión de su *smartphone* con el sensor físico que se le ha asignado para su jornada.

* **Entrada de Código:** El usuario debe introducir el **"Código único de vinculación"** del sensor (ej. `12345`).
* **Acceso Alternativo:** La app también permite un acceso rápido mediante **escaneo de QR** (`Acceder con QR`).
* **Validación:** La app comprueba (contra la API) que el código del sensor existe y está activo. Si es correcto, da acceso al Panel de Control.

### 2. Panel de Control del Sensor

Una vez vinculado, el trabajador accede a una pantalla que le permite **comprobar que todo está bien** y monitorizar el estado del hardware.

Esta pantalla tiene tres propósitos clave:

#### A. Supervisión del Estado del Sensor
Permite al trabajador verificar el estado operativo del sensor de un vistazo.
* **Ubicación Actual:** Confirma que el GPS del sensor está reportando.
* **Estado:** Muestra información contextual (ej. "a 20km del punto de origen").
* **Batería:** Muestra la batería restante del hardware del sensor.

#### B. Visualización de Mediciones
Confirma que el sensor está recolectando datos ambientales en tiempo real. El trabajador puede ver los valores actuales de:
* Ozono
* Temperatura
* CO2

#### C. Alertas e Incidencias
Este es el canal de comunicación clave. Si el sensor detecta una anomalía (ej. "Temperaturas altas en C/Acacias"), la alerta se muestra en la app del trabajador.

* **Aviso al Trabajador:** El nodoportador está al tanto de las condiciones de su entorno.
* **Aviso al Administrador:** Estas alertas también se envían automáticamente al *Dashboard* del **Administrador (B2B)**, permitiéndole gestionar la incidencia (ej. gestionar el mantenimiento del sensor).

