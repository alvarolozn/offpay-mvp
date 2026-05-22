# OffPay Admin Panel

Panel administrativo web para monitorear el sistema OffPay MVP.

---

## ¿Qué es esto?

Un panel web que te permite consultar el estado del backend,
ver wallets, tokens, transacciones e intentos inválidos.
Solo lectura. No modifica ningún dato.

---

## Requisitos previos

Antes de abrir esto necesitas tener instalado:

- **Node.js** versión 18 o superior
  → Descárgalo en: https://nodejs.org (botón LTS)
- El backend de FastAPI corriendo (en localhost:8000 u otro puerto)

Para verificar que Node.js está instalado, abre una terminal y escribe:
```
node --version
```
Debe decir algo como `v20.x.x`. Si da error, instala Node.js primero.

---

## Cómo abrir el proyecto

### Paso 1: Abrir la carpeta en VS Code

Abre VS Code y haz File → Open Folder → selecciona la carpeta `admin_panel`.

### Paso 2: Abrir la terminal integrada

En VS Code: menú Terminal → New Terminal

### Paso 3: Instalar dependencias

Escribe este comando y presiona Enter:
```
npm install
```
Esto descarga todas las librerías que necesita el proyecto.
Puede tardar 30-60 segundos. Solo se hace una vez.

### Paso 4: Levantar el servidor de desarrollo

```
npm run dev
```

Verás algo como:
```
  VITE v5.x.x  ready in xxx ms
  ➜  Local:   http://localhost:5173/
```

### Paso 5: Abrir en el navegador

Abre tu navegador y ve a: **http://localhost:5173**

---

## Configuración inicial

La primera vez que abras el panel:

1. Ve a la sección **Configuración** (ícono ⚙ en el sidebar)
2. Ingresa la URL de tu backend (por defecto: `http://localhost:8000`)
3. Ingresa el Client ID y Seller ID de demo (UUIDs de tu base de datos)
4. Haz clic en **Guardar configuración**

Luego ve a **Estado del sistema** para verificar que todo conecta.

---

## Estructura del proyecto

```
admin_panel/
├── index.html              ← punto de entrada HTML
├── package.json            ← dependencias y scripts
├── vite.config.js          ← configuración del servidor de desarrollo
└── src/
    ├── main.jsx            ← arranca React
    ├── App.jsx             ← rutas y layout
    ├── index.css           ← estilos globales
    ├── config.js           ← manejo de configuración (localStorage)
    ├── api/
    │   └── offpay.js       ← todas las llamadas al backend
    ├── components/
    │   └── Sidebar.jsx     ← barra de navegación lateral
    └── pages/
        ├── ConfigPage.jsx          ← sección 1: configuración
        ├── StatusPage.jsx          ← sección 2: estado del sistema
        ├── WalletPage.jsx          ← sección 3: consulta wallet
        ├── TokensPage.jsx          ← sección 4: tokens del cliente
        ├── TransactionsPage.jsx    ← sección 5: transacciones
        └── InvalidAttemptsPage.jsx ← sección 6: intentos inválidos
```

---

## Endpoints que consume

| Sección | Endpoint |
|---|---|
| Estado del sistema | `GET /health` |
| Estado del sistema | `GET /db-test` |
| Consulta Wallet | `GET /wallets/{user_id}` |
| Tokens del cliente | `GET /tokens/client/{client_id}` |
| Transacciones | `GET /transactions` |
| Intentos inválidos | `GET /admin/invalid-attempts` |

---

## Si algo falla

**Error: Cannot find module** → Ejecuta `npm install` de nuevo.

**Error de CORS** → El backend FastAPI necesita permitir peticiones desde
`http://localhost:5173`. Agrega esto en tu `main.py`:
```python
from fastapi.middleware.cors import CORSMiddleware

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173"],
    allow_methods=["GET"],
    allow_headers=["*"],
)
```

**Error de conexión** → Verifica que el backend esté corriendo y que la
URL en Configuración sea correcta.

---

## Detener el servidor

En la terminal donde está corriendo, presiona **Ctrl + C**.
