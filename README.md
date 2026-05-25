# OFFPAY MVP

OffPay MVP es un sistema de pagos digitales offline basado en tokens. El cliente puede preparar saldo cuando tiene conexión, recibir tokens y posteriormente realizar pagos sin internet mostrando un QR o JSON. El vendedor siempre debe estar conectado a internet para validar el pago contra el backend.

El backend es la fuente principal de verdad: genera tokens, valida pagos, evita doble gasto, actualiza saldos, registra transacciones y permite auditoría administrativa.

---

## Integrantes

- Álvaro Lozano
- José Manjarrez
- Leonel Aguanche
- Juan Torres

---

## Tecnologías usadas

### Backend

- Python
- FastAPI
- Uvicorn
- PostgreSQL / Supabase
- Pydantic
- psycopg2
- python-dotenv
- web3.py

### Frontend / Panel administrativo

- Node.js
- NPM
- Vite
- React

### Blockchain

- Solidity
- Hardhat
- Polygon Amoy
- web3.py

### App móvil

- Android
- Kotlin
- Jetpack Compose

---

# Instalación y despliegue completo

Esta guía está pensada principalmente para Windows usando PowerShell y VS Code.

---

## 1. Clonar repositorio

```bash
git clone URL_DEL_REPOSITORIO
cd offpay-mvp
```

Si el proyecto fue descargado como ZIP, puede quedar dentro de carpetas anidadas como:

```text
offpay-mvp/offpay-mvp-main/offpay-mvp-main
```

En ese caso, se debe entrar hasta la carpeta donde estén las carpetas principales del proyecto:

```text
backend/
admin_panel/
blockchain_bridge/
seller_app/
```

---

## 2. Habilitar ejecución de scripts en Windows

Esto se hace una sola vez o cada vez que PowerShell bloquee scripts.

Abrir PowerShell como administrador y ejecutar:

```powershell
Set-ExecutionPolicy RemoteSigned
```

Cuando pregunte, escribir:

```text
Y
```

Luego cerrar PowerShell y volver a abrir VS Code.

Si solo se quiere habilitar para la terminal actual, usar:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

---

## 3. Instalar Node.js y NPM

Descargar Node.js desde:

```text
https://nodejs.org/en/download
```

Instalar la versión LTS.

Verificar instalación:

```powershell
node -v
npm -v
```

Si ambos comandos muestran una versión, Node y NPM quedaron instalados correctamente.

---

## 4. Instalar ngrok

Descargar ngrok desde:

```text
https://ngrok.com/download
```

Extraer `ngrok.exe` y moverlo a:

```text
C:\ngrok
```

Entrar a la carpeta:

```powershell
cd C:\ngrok
```

---

## 5. Configurar tokens de ngrok

Cada integrante debe usar su propio token de ngrok.

No se deben subir tokens reales a GitHub.

Ejemplo:

```powershell
.\ngrok.exe config add-authtoken TU_TOKEN_DE_NGROK
```

Si se van a usar dos túneles con configuraciones separadas, se pueden usar dos archivos `.yml`: uno para frontend y otro para backend.

---

## 6. Crear configuración de ngrok para frontend

Crear archivo en:

```text
C:\ngrok\config1.yml
```

Contenido:

```yaml
version: "3"

agent:
  authtoken: TU_TOKEN_NGROK_FRONTEND

tunnels:
  frontend:
    addr: 5173
    proto: http
```

---

## 7. Crear configuración de ngrok para backend

Crear archivo en:

```text
C:\ngrok\config2.yml
```

Contenido:

```yaml
version: "3"

agent:
  authtoken: TU_TOKEN_NGROK_BACKEND

tunnels:
  backend:
    addr: 8000
    proto: http
```

---

# Backend

## 8. Entrar a la carpeta del backend

Desde la raíz del proyecto:

```powershell
cd backend
```

Si el proyecto está dentro de carpetas anidadas, usar la ruta completa. Ejemplo:

```powershell
cd "C:\Users\alvar\Documents\offpay-mvp\offpay-mvp-main\offpay-mvp-main\backend"
```

Verificar que estás en la carpeta correcta:

```powershell
dir
```

Debes ver archivos como:

```text
main.py
db.py
token_service.py
requirements.txt
.env
```

---

## 9. Crear entorno virtual del backend

```powershell
python -m venv venv
```

---

## 10. Activar entorno virtual del backend

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\venv\Scripts\Activate.ps1
```

Debe verse algo así:

```powershell
(venv) PS C:\...\backend>
```

---

## 11. Instalar dependencias del backend

```powershell
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
```

Si falta alguna dependencia, instalar manualmente:

```powershell
python -m pip install fastapi "uvicorn[standard]" psycopg2-binary python-dotenv web3 eth-account email-validator
```

---

## 12. Variables de entorno del backend

En la carpeta `backend`, debe existir un archivo `.env`.

Ejemplo:

```env
DATABASE_URL=postgresql://USUARIO:PASSWORD@HOST:PUERTO/DB

SECRET_KEY=CLAVE_SECRETA_DEL_BACKEND

BLOCKCHAIN_WRITE_ENABLED=true
BLOCKCHAIN_RPC_URL=https://rpc-amoy.polygon.technology/
BLOCKCHAIN_CHAIN_ID=80002
BLOCKCHAIN_PRIVATE_KEY=PRIVATE_KEY_DE_WALLET_DE_PRUEBA
BLOCKCHAIN_CONTRACT_ADDRESS=0xDIRECCION_DEL_CONTRATO
```

Importante:

```text
No subir el archivo .env a GitHub.
No usar una wallet personal.
Usar solo wallet de prueba para Polygon Amoy.
```

---

## 13. Activar escritura blockchain para la sesión

Desde la terminal del backend con el entorno virtual activo:

```powershell
$env:BLOCKCHAIN_WRITE_ENABLED="true"
```

---

## 14. Levantar backend

Usar este comando recomendado:

```powershell
python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

También puede funcionar:

```powershell
uvicorn main:app --reload
```

Pero si Windows no reconoce `uvicorn`, usar siempre:

```powershell
python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

---

## 15. Verificar backend

Abrir en el navegador:

```text
http://127.0.0.1:8000/docs
```

También se puede probar:

```text
http://127.0.0.1:8000/health
```

y:

```text
http://127.0.0.1:8000/db-test
```

---

# Panel administrativo / Frontend

## 16. Entrar a la carpeta del panel

Abrir una nueva terminal sin entorno virtual.

Si aparece `(venv)`, ejecutar:

```powershell
deactivate
```

Luego entrar a la carpeta:

```powershell
cd admin_panel
```

Si el proyecto está en carpetas anidadas, usar ruta completa. Ejemplo:

```powershell
cd "C:\Users\alvar\Documents\offpay-mvp\offpay-mvp-main\offpay-mvp-main\admin_panel"
```

Verificar que estás en la carpeta correcta:

```powershell
dir
```

Debes ver:

```text
package.json
package-lock.json
src/
```

---

## 17. Instalar dependencias del frontend

```powershell
npm install
```

Si aparece error de Vite, instalarlo manualmente:

```powershell
npm install vite @vitejs/plugin-react --save-dev
```

Verificar Vite:

```powershell
npx vite --version
```

---

## 18. Configurar Vite para ngrok

Abrir el archivo:

```text
admin_panel/vite.config.js
```

Debe quedar así:

```javascript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    allowedHosts: 'all'
  }
})
```

Si el proyecto no usa plugin de React en ese archivo, dejar al menos:

```javascript
import { defineConfig } from 'vite'

export default defineConfig({
  server: {
    allowedHosts: 'all'
  }
})
```

---

## 19. Configurar URL del backend en el frontend

Abrir:

```text
admin_panel/src/config.js
```

Configurar la URL del backend.

Para backend local:

```javascript
export const API_URL = "http://127.0.0.1:8000"
```

Para backend expuesto con ngrok:

```javascript
export const API_URL = "https://TU_BACKEND_NGROK"
```

Ejemplo:

```javascript
export const API_URL = "https://abc123.ngrok-free.app"
```

---

## 20. Levantar frontend

Desde la carpeta `admin_panel`:

```powershell
npm run dev
```

Debe aparecer algo como:

```text
Local: http://localhost:5173/
```

Abrir esa URL en el navegador.

---

# Ngrok

## 21. Iniciar ngrok para frontend

Abrir una nueva terminal:

```powershell
cd C:\ngrok
```

Ejecutar:

```powershell
.\ngrok.exe start --all --config config1.yml
```

Ngrok mostrará una URL HTTPS.

Ejemplo:

```text
https://frontend-demo.ngrok-free.app
```

Esa será la URL pública del frontend.

---

## 22. Iniciar ngrok para backend

Abrir otra terminal:

```powershell
cd C:\ngrok
```

Ejecutar:

```powershell
.\ngrok.exe start --all --config config2.yml
```

Ngrok mostrará una URL HTTPS.

Ejemplo:

```text
https://backend-demo.ngrok-free.app
```

Esa será la URL pública del backend.

Swagger quedaría disponible en:

```text
https://TU_BACKEND_NGROK/docs
```

---

# Blockchain

## 23. Entrar a la carpeta blockchain

Desde la raíz del proyecto:

```powershell
cd blockchain_bridge
```

O usando ruta completa:

```powershell
cd "C:\Users\alvar\Documents\offpay-mvp\offpay-mvp-main\offpay-mvp-main\blockchain_bridge"
```

---

## 24. Instalar dependencias blockchain

```powershell
npm install
```

Si hace falta instalar Hardhat:

```powershell
npm install --save-dev hardhat @nomicfoundation/hardhat-toolbox dotenv
```

---

## 25. Compilar contrato

```powershell
npx hardhat compile
```

---

## 26. Desplegar contrato en Polygon Amoy

```powershell
npx hardhat run scripts/deploy.js --network amoy
```

Si el script es TypeScript:

```powershell
npx hardhat run scripts/deploy.ts --network amoy
```

Después de desplegar, copiar la dirección del contrato y colocarla en el `.env` del backend:

```env
BLOCKCHAIN_CONTRACT_ADDRESS=0xDIRECCION_DEL_CONTRATO
```

---

## 27. Red blockchain usada

```text
Red: Polygon Amoy
Chain ID: 80002
Token de gas: POL de prueba
```

Se necesita una wallet de prueba con POL en Amoy para poder escribir eventos on-chain.

---

# Flujo funcional del sistema

## 1. Recarga de saldo

El cliente recarga saldo en su billetera.

Ejemplo:

```text
Saldo disponible inicial: 0 COP
Recarga: 50.000 COP
Saldo disponible final: 50.000 COP
```

El backend registra un movimiento:

```text
RECHARGE
```

---

## 2. Apartar saldo y generar tokens

El cliente aparta un monto múltiplo de 10.000 COP.

Ejemplo:

```text
Monto apartado: 30.000 COP
Valor por token: 10.000 COP
Tokens generados: 3
```

El backend:

```text
Resta saldo disponible
Aumenta saldo bloqueado
Genera tokens
Marca tokens como AVAILABLE
Registra movimiento BLOCK_BALANCE
```

---

## 3. Pago offline del cliente

El cliente puede quedar sin conexión después de recibir los tokens.

La app cliente selecciona localmente cuántos tokens usar según el monto del pago.

Ejemplo:

```text
Pago de 20.000 COP
Tokens usados: 2
```

El QR o JSON contiene un paquete de códigos de pago.

---

## 4. Validación online del vendedor

El vendedor siempre debe estar conectado.

La app del vendedor envía el QR o JSON al backend.

El backend valida:

```text
Que los tokens existan
Que pertenezcan al cliente correcto
Que estén disponibles
Que no hayan sido usados
Que no hayan sido devueltos
Que el paquete sea consistente
```

---

## 5. Pago aprobado

Cuando el pago se aprueba, el backend:

```text
Marca tokens como USED
Reduce saldo bloqueado del cliente
Aumenta saldo disponible del vendedor
Registra transacciones APPROVED
Registra movimientos PAYMENT_SENT y PAYMENT_RECEIVED
```

---

## 6. Pago rechazado

El backend rechaza el pago si ocurre alguno de estos casos:

```text
Token inexistente
Token ya usado
Token devuelto
Paquete incompleto
Tokens mezclados de diferentes clientes
Monto inconsistente
```

El intento queda registrado en:

```text
transactions
invalid_attempts
```

---

## 7. Devolución de tokens

Si el cliente recupera conexión, puede devolver tokens no usados.

El backend:

```text
Verifica que estén AVAILABLE
Los marca como RETURNED
Reduce saldo bloqueado
Aumenta saldo disponible
Registra movimiento TOKEN_REFUND
```

---

# Endpoints principales

## Estado del backend

```text
GET /
GET /health
GET /db-test
```

## Autenticación

```text
POST /auth/demo-login
```

## Clientes y vendedores

```text
POST /clients/register
POST /sellers/register
```

## Billetera

```text
GET  /wallets/{user_id}
POST /wallets/recharge
```

## Tokens

```text
POST /tokens/generate
GET  /tokens/client/{client_id}
POST /tokens/refund
```

## Pagos

```text
POST /payments/validate-package
```

## Administración

```text
GET  /admin/transactions
GET  /admin/transactions/export
GET  /admin/invalid-attempts
GET  /admin/fraud-alerts
POST /admin/invalid-attempts/{attempt_id}/review
```

## Blockchain

```text
GET  /blockchain/health
GET  /blockchain/token/{token_hash}
POST /blockchain/resync-pending
```

---

# Tablas principales

## profiles

Guarda usuarios del sistema.

```text
id
email
password_hash
full_name
role
commerce_name
```

Roles:

```text
CLIENT
SELLER
ADMIN
```

---

## wallets

Guarda saldos de cada usuario.

```text
user_id
available_balance_cop
blocked_balance_cop
```

---

## tokens

Guarda los tokens generados.

```text
id
client_id
counter
payment_code
signature
token_hash
value_cop
status
blockchain_status
created_at
used_at
returned_at
```

Estados:

```text
AVAILABLE
USED
RETURNED
```

---

## transactions

Guarda transacciones aprobadas y rechazadas.

```text
token_id
token_hash
client_id
seller_id
amount_cop
status
rejection_reason
qr_payload
response_time_ms
created_at
```

Estados:

```text
APPROVED
REJECTED
```

---

## wallet_movements

Guarda movimientos financieros.

```text
RECHARGE
BLOCK_BALANCE
PAYMENT_SENT
PAYMENT_RECEIVED
TOKEN_REFUND
```

---

## invalid_attempts

Guarda intentos inválidos o sospechosos.

```text
token_id
token_hash
seller_id
reason
qr_payload
created_at
is_reviewed
review_note
```

---

## blockchain_events

Guarda eventos relacionados con blockchain.

```text
token_id
token_hash
event_type
tx_hash
block_number
chain_id
contract_address
payload
created_at
```

Eventos:

```text
TOKEN_REGISTERED
TOKEN_USED
TOKEN_RETURNED
ERROR
```

---

# Atomicidad y prevención de doble gasto

El backend evita que un token sea usado más de una vez.

La validación del pago se ejecuta dentro de una transacción de base de datos. Durante la validación, los tokens se bloquean usando una operación equivalente a:

```sql
SELECT *
FROM tokens
WHERE token_hash IN (...)
FOR UPDATE;
```

Esto significa que si dos solicitudes intentan usar el mismo token al mismo tiempo:

```text
La primera solicitud bloquea el token
La segunda solicitud queda esperando
La primera marca el token como USED
La segunda continúa y ve que el token ya está USED
La segunda es rechazada
```

Resultado:

```text
1 pago aprobado
Los demás intentos rechazados
0 doble gasto
```

---

# Comandos rápidos

## Backend

```powershell
cd backend
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\venv\Scripts\Activate.ps1
$env:BLOCKCHAIN_WRITE_ENABLED="true"
python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

## Frontend

```powershell
cd admin_panel
npm run dev
```

## Ngrok frontend

```powershell
cd C:\ngrok
.\ngrok.exe start --all --config config1.yml
```

## Ngrok backend

```powershell
cd C:\ngrok
.\ngrok.exe start --all --config config2.yml
```

---

# URLs importantes

Backend local:

```text
http://127.0.0.1:8000
```

Swagger local:

```text
http://127.0.0.1:8000/docs
```

Frontend local:

```text
http://localhost:5173
```

Backend para emulador Android:

```text
http://10.0.2.2:8000
```

Backend para celular físico:

```text
http://IP_DEL_PC:8000
```

Swagger por ngrok:

```text
https://TU_BACKEND_NGROK/docs
```

Frontend por ngrok:

```text
https://TU_FRONTEND_NGROK
```

---

# Archivos que no deben subirse

No subir:

```text
.env
venv/
node_modules/
__pycache__/
*.pyc
```

Tampoco subir tokens reales de ngrok ni private keys de blockchain.

---

# Problemas comunes

## PowerShell no deja activar el entorno

Ejecutar:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

Luego:

```powershell
.\venv\Scripts\Activate.ps1
```

---

## uvicorn no se reconoce

Usar:

```powershell
python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

Si sigue fallando:

```powershell
python -m pip install "uvicorn[standard]" fastapi
```

---

## pip apunta a una ruta vieja

Borrar y recrear el entorno virtual:

```powershell
deactivate
Remove-Item -Recurse -Force .\venv
python -m venv venv
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
```

---

## El backend no encuentra main.py

Verificar carpeta actual:

```powershell
dir
```

Debes ver:

```text
main.py
db.py
token_service.py
requirements.txt
```

Si no aparecen, estás en la carpeta equivocada.

---

## npm run dev dice que no encuentra Vite

Entrar a `admin_panel` y ejecutar:

```powershell
npm install
npm install vite @vitejs/plugin-react --save-dev
npm run dev
```

---

## node_modules está dañado

Desde `admin_panel`:

```powershell
Remove-Item -Recurse -Force node_modules
npm install
npm run dev
```

---

## El celular no conecta con el backend

Levantar backend con:

```powershell
python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

Usar la IP local del PC en la app del celular:

```text
http://IP_DEL_PC:8000
```

El celular y el PC deben estar en la misma red WiFi.

---

# Pruebas recomendadas para la demo

## 1. Backend vivo

Abrir:

```text
http://127.0.0.1:8000/docs
```

Probar:

```text
GET /health
GET /db-test
```

---

## 2. Recarga

Usar:

```text
POST /wallets/recharge
```

Confirmar que el saldo disponible aumenta.

---

## 3. Generación de tokens

Usar:

```text
POST /tokens/generate
```

Confirmar:

```text
El saldo disponible baja
El saldo bloqueado sube
Los tokens quedan AVAILABLE
```

---

## 4. Pago aprobado

Usar la app cliente para generar QR o JSON.

Usar la app vendedor para validar.

Confirmar:

```text
Pago aprobado
Tokens pasan a USED
Se registra transactions APPROVED
Se registra PAYMENT_SENT
Se registra PAYMENT_RECEIVED
```

---

## 5. Doble gasto

Intentar usar el mismo QR o JSON dos veces.

Resultado esperado:

```text
Primer intento: APPROVED
Segundo intento: REJECTED
Motivo: TOKEN_ALREADY_USED
```

Confirmar registro en:

```text
transactions
invalid_attempts
```

---

## 6. Devolución

Devolver un token disponible.

Confirmar:

```text
Token pasa a RETURNED
Saldo bloqueado baja
Saldo disponible sube
Se registra TOKEN_REFUND
```

---

# Resumen técnico

OffPay MVP permite pagos offline del lado del cliente mediante tokens generados previamente. El backend controla el ciclo de vida de los tokens, valida pagos en línea desde la app del vendedor, evita doble gasto con transacciones atómicas, actualiza saldos, registra trazabilidad completa y permite auditoría mediante endpoints administrativos.

El cliente puede operar sin internet al momento de pagar, pero la validez final del pago siempre la determina el backend.