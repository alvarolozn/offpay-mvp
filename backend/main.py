import os
import time

from fastapi import FastAPI, HTTPException
from dotenv import load_dotenv
from pydantic import BaseModel, EmailStr
from psycopg2.extras import Json

from db import get_conn
from token_service import (
    hash_password,
    generate_payment_code,
    hash_payment_code,
    make_signature,
    normalize_payment_code,
)

# ============================================================
# CARGA DE VARIABLES DE ENTORNO
# ============================================================

# Carga automáticamente las variables definidas en el archivo .env
load_dotenv()

# Obtiene el nombre de la aplicación desde las variables de entorno.
# Si no existe, usa "OffPay MVP API" por defecto.
APP_NAME = os.getenv("APP_NAME", "OffPay MVP API")

# Obtiene la versión de la aplicación desde el .env.
# Si no existe, usa "1.0.0" por defecto.
APP_VERSION = os.getenv("APP_VERSION", "1.0.0")


# ============================================================
# CONFIGURACIÓN DE FASTAPI
# ============================================================

# Inicializa la aplicación FastAPI
app = FastAPI(
    title=APP_NAME,
    version=APP_VERSION
)


# ============================================================
# MODELOS DE VALIDACIÓN (PYDANTIC)
# ============================================================

# Modelo utilizado para registrar clientes
class RegisterClientRequest(BaseModel):
    full_name: str
    email: EmailStr
    password: str


# Modelo utilizado para registrar vendedores
class RegisterSellerRequest(BaseModel):
    full_name: str
    email: EmailStr
    password: str
    commerce_name: str


# Modelo utilizado para recargar saldo
class RechargeRequest(BaseModel):
    user_id: str
    amount_cop: int


# Modelo utilizado para generar tokens
class GenerateTokensRequest(BaseModel):
    client_id: str
    amount_cop: int


# Modelo utilizado para validar pagos individuales
class ValidatePaymentRequest(BaseModel):
    seller_id: str
    payment_code: str


# Modelo utilizado para validar pagos por paquete
class ValidatePackagePaymentRequest(BaseModel):
    seller_id: str
    payment_codes: list[str]


# Modelo utilizado para devolver tokens no usados
class RefundTokenRequest(BaseModel):
    client_id: str
    payment_code: str


# ============================================================
# ENDPOINT PRINCIPAL
# ============================================================

@app.get("/")
def root():
    """
    Endpoint principal de la API.

    Retorna un mensaje indicando que
    el backend está funcionando.
    """
    return {
        "message": "Backend de OffPay funcionando",
        "docs": "/docs"
    }


# ============================================================
# HEALTH CHECK
# ============================================================

@app.get("/health")
def health():
    """
    Endpoint para verificar el estado de la API.

    Retorna:
    - Estado general
    - Nombre de la app
    - Versión actual
    """
    return {
        "status": "ok",
        "app": APP_NAME,
        "version": APP_VERSION
    }


# ============================================================
# PRUEBA DE CONEXIÓN A BASE DE DATOS
# ============================================================

@app.get("/db-test")
def db_test():
    """
    Verifica que PostgreSQL/Supabase
    esté funcionando correctamente.
    """
    try:
        # Abre conexión con PostgreSQL
        with get_conn() as conn:

            # Abre cursor SQL
            with conn.cursor() as cur:

                # Ejecuta consulta simple
                cur.execute("select now() as server_time;")

                # Obtiene resultado
                result = cur.fetchone()

        return {
            "status": "ok",
            "message": "Conexión a Supabase exitosa",
            "server_time": result["server_time"]
        }

    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Error conectando a la base de datos: {str(e)}"
        )


# ============================================================
# REGISTRO DE CLIENTES
# ============================================================

@app.post("/clients/register")
def register_client(data: RegisterClientRequest):
    """
    Registra un nuevo cliente.
    """

    return _register_user(
        full_name=data.full_name,
        email=data.email,
        password=data.password,
        role="CLIENT",
        commerce_name=None
    )


# ============================================================
# REGISTRO DE VENDEDORES
# ============================================================

@app.post("/sellers/register")
def register_seller(data: RegisterSellerRequest):
    """
    Registra un nuevo vendedor.
    """

    return _register_user(
        full_name=data.full_name,
        email=data.email,
        password=data.password,
        role="SELLER",
        commerce_name=data.commerce_name
    )


# ============================================================
# FUNCIÓN INTERNA DE REGISTRO
# ============================================================

def _register_user(
    full_name: str,
    email: str,
    password: str,
    role: str,
    commerce_name: str | None
):
    """
    Función reutilizable para registrar usuarios.

    Esta función:
    1. Hashea la contraseña
    2. Inserta el perfil del usuario
    3. Crea automáticamente su wallet
    """

    # Genera hash seguro de la contraseña
    password_hash = hash_password(password)

    # Abre conexión con la base de datos
    with get_conn() as conn:

        try:
            with conn.cursor() as cur:

                # ====================================================
                # INSERTAR PERFIL
                # ====================================================

                cur.execute(
                    """
                    insert into profiles(
                        email,
                        password_hash,
                        full_name,
                        role,
                        commerce_name
                    )
                    values (%s, %s, %s, %s, %s)

                    returning
                        id,
                        email,
                        full_name,
                        role,
                        commerce_name
                    """,
                    (
                        email,
                        password_hash,
                        full_name,
                        role,
                        commerce_name
                    )
                )

                # Obtiene usuario recién creado
                user = cur.fetchone()

                # ====================================================
                # CREAR WALLET INICIAL
                # ====================================================

                cur.execute(
                    """
                    insert into wallets(
                        user_id,
                        available_balance_cop,
                        blocked_balance_cop
                    )
                    values (%s, 0, 0)

                    returning
                        id,
                        available_balance_cop,
                        blocked_balance_cop
                    """,
                    (user["id"],)
                )

                # Obtiene wallet creada
                wallet = cur.fetchone()

                # Guarda cambios
                conn.commit()

                return {
                    "message": f"{role} registrado correctamente",
                    "user": dict(user),
                    "wallet": dict(wallet)
                }

        except Exception as e:

            # Revierte cambios si ocurre error
            conn.rollback()

            raise HTTPException(
                status_code=400,
                detail=str(e)
            )


# ============================================================
# OBTENER WALLET
# ============================================================

@app.get("/wallets/{user_id}")
def get_wallet(user_id: str):
    """
    Obtiene la información de la wallet
    de un usuario específico.
    """

    # Elimina espacios extra
    clean_user_id = user_id.strip()

    with get_conn() as conn:
        with conn.cursor() as cur:

            # Consulta información de wallet + perfil
            cur.execute(
                """
                select 
                    w.id as wallet_id,
                    w.user_id,
                    p.full_name,
                    p.role,
                    p.commerce_name,
                    w.available_balance_cop,
                    w.blocked_balance_cop

                from wallets w

                join profiles p
                    on p.id = w.user_id

                where w.user_id = %s
                """,
                (clean_user_id,)
            )

            wallet = cur.fetchone()

            # Si no existe wallet
            if not wallet:
                raise HTTPException(
                    status_code=404,
                    detail="Wallet no encontrada"
                )

            return dict(wallet)


# ============================================================
# RECARGAR WALLET
# ============================================================

@app.post("/wallets/recharge")
def recharge_wallet(data: RechargeRequest):

    # Valida monto
    if data.amount_cop <= 0:
        raise HTTPException(
            status_code=400,
            detail="El monto debe ser mayor a cero"
        )

    with get_conn() as conn:

        try:
            with conn.cursor() as cur:

                # ====================================================
                # BLOQUEA WALLET
                # ====================================================

                cur.execute(
                    "select * from wallets where user_id = %s for update",
                    (data.user_id.strip(),)
                )

                wallet = cur.fetchone()

                if not wallet:
                    raise HTTPException(
                        status_code=404,
                        detail="Wallet no encontrada"
                    )

                # ====================================================
                # CALCULA NUEVOS SALDOS
                # ====================================================

                before = wallet["available_balance_cop"]
                after = before + data.amount_cop

                # ====================================================
                # ACTUALIZA WALLET
                # ====================================================

                cur.execute(
                    """
                    update wallets

                    set
                        available_balance_cop = %s,
                        updated_at = now()

                    where user_id = %s

                    returning *
                    """,
                    (
                        after,
                        data.user_id.strip()
                    )
                )

                updated_wallet = cur.fetchone()

                # ====================================================
                # REGISTRA MOVIMIENTO
                # ====================================================

                cur.execute(
                    """
                    insert into wallet_movements(
                        wallet_id,
                        user_id,
                        movement_type,
                        amount_cop,
                        balance_before_cop,
                        balance_after_cop,
                        description
                    )

                    values (
                        %s,
                        %s,
                        'RECHARGE',
                        %s,
                        %s,
                        %s,
                        %s
                    )
                    """,
                    (
                        wallet["id"],
                        data.user_id.strip(),
                        data.amount_cop,
                        before,
                        after,
                        "Recarga simulada de saldo"
                    )
                )

                # Guarda cambios
                conn.commit()

                return {
                    "message": "Saldo recargado correctamente",
                    "wallet": dict(updated_wallet)
                }

        except HTTPException:

            conn.rollback()
            raise

        except Exception as e:

            conn.rollback()

            raise HTTPException(
                status_code=400,
                detail=str(e)
            )

# ============================================================
# GENERAR TOKENS
# ============================================================

@app.post("/tokens/generate")
def generate_tokens(data: GenerateTokensRequest):

    # Limpia espacios
    client_id = data.client_id.strip()

    # ========================================================
    # VALIDACIONES
    # ========================================================

    if data.amount_cop <= 0:
        raise HTTPException(
            status_code=400,
            detail="El monto debe ser mayor a cero"
        )

    if data.amount_cop % 10000 != 0:
        raise HTTPException(
            status_code=400,
            detail="El monto debe ser múltiplo de 10000 COP"
        )

    # Cantidad de tokens a generar
    token_count = data.amount_cop // 10000

    with get_conn() as conn:

        try:
            with conn.cursor() as cur:

                # ====================================================
                # BLOQUEA WALLET
                # ====================================================

                cur.execute(
                    "select * from wallets where user_id = %s for update",
                    (client_id,)
                )

                wallet = cur.fetchone()

                if not wallet:
                    raise HTTPException(
                        status_code=404,
                        detail="Wallet no encontrada"
                    )

                # Verifica saldo suficiente
                if wallet["available_balance_cop"] < data.amount_cop:
                    raise HTTPException(
                        status_code=400,
                        detail="Saldo disponible insuficiente"
                    )

                # ====================================================
                # CALCULA NUEVOS SALDOS
                # ====================================================

                available_before = wallet["available_balance_cop"]

                available_after = (
                    available_before - data.amount_cop
                )

                blocked_after = (
                    wallet["blocked_balance_cop"] + data.amount_cop
                )

                # ====================================================
                # ACTUALIZA WALLET
                # ====================================================

                cur.execute(
                    """
                    update wallets

                    set
                        available_balance_cop = %s,
                        blocked_balance_cop = %s,
                        updated_at = now()

                    where user_id = %s

                    returning *
                    """,
                    (
                        available_after,
                        blocked_after,
                        client_id
                    )
                )

                updated_wallet = cur.fetchone()

                # ====================================================
                # REGISTRA MOVIMIENTO
                # ====================================================

                cur.execute(
                    """
                    insert into wallet_movements(
                        wallet_id,
                        user_id,
                        movement_type,
                        amount_cop,
                        balance_before_cop,
                        balance_after_cop,
                        description
                    )

                    values (
                        %s,
                        %s,
                        'BLOCK_BALANCE',
                        %s,
                        %s,
                        %s,
                        %s
                    )
                    """,
                    (
                        wallet["id"],
                        client_id,
                        data.amount_cop,
                        available_before,
                        available_after,
                        "Saldo bloqueado para generación de tokens"
                    )
                )

                # Lista donde se almacenan los tokens generados
                generated_tokens = []

                # ====================================================
                # GENERACIÓN DE TOKENS
                # ====================================================

                for _ in range(token_count):

                    # Obtiene siguiente contador
                    cur.execute(
                        """
                        select coalesce(max(counter), 0) + 1 as next_counter
                        from tokens
                        where client_id = %s
                        """,
                        (client_id,)
                    )

                    counter = cur.fetchone()["next_counter"]

                    # Genera código de pago
                    payment_code = generate_payment_code()

                    # Genera hash del código
                    token_hash = hash_payment_code(payment_code)

                    # Firma criptográfica
                    signature = make_signature(token_hash)

                    # Payload QR
                    qr_payload = {
                        "version": "OP1",
                        "payment_code": payment_code,
                        "token_hash": token_hash,
                        "value_cop": 10000
                    }

                    # Inserta token
                    cur.execute(
                        """
                        insert into tokens(
                            client_id,
                            counter,
                            payment_code,
                            signature,
                            token_hash,
                            value_cop,
                            status,
                            blockchain_status,
                            chain_id,
                            qr_payload
                        )

                        values (
                            %s,
                            %s,
                            %s,
                            %s,
                            %s,
                            10000,
                            'AVAILABLE',
                            'PENDING',
                            80002,
                            %s
                        )

                        returning
                            id,
                            client_id,
                            counter,
                            payment_code,
                            token_hash,
                            value_cop,
                            status,
                            blockchain_status,
                            created_at
                        """,
                        (
                            client_id,
                            counter,
                            payment_code,
                            signature,
                            token_hash,
                            Json(qr_payload)
                        )
                    )

                    token = cur.fetchone()

                    generated_tokens.append(dict(token))

                # Guarda cambios
                conn.commit()

                return {
                    "message": f"{token_count} token(es) generado(s) correctamente",
                    "wallet": dict(updated_wallet),
                    "tokens": generated_tokens
                }

        except HTTPException:

            conn.rollback()
            raise

        except Exception as e:

            conn.rollback()

            raise HTTPException(
                status_code=400,
                detail=str(e)
            )

# ============================================================
# DEVOLVER TOKEN NO UTILIZADO
# ============================================================

@app.post("/tokens/refund")
def refund_token(data: RefundTokenRequest):
    """
    Devuelve un token que no haya sido utilizado.

    El proceso:
    1. Verifica que el token exista
    2. Verifica que pertenezca al cliente
    3. Verifica que el token siga AVAILABLE
    4. Libera el saldo bloqueado
    5. Marca el token como RETURNED
    6. Registra el movimiento en wallet_movements
    """

    # Limpia espacios del client_id
    client_id = data.client_id.strip()

    # Normaliza el payment code recibido
    payment_code = normalize_payment_code(data.payment_code)

    # Genera el hash del token
    token_hash = hash_payment_code(payment_code)

    # Abre conexión a la base de datos
    with get_conn() as conn:

        try:
            with conn.cursor() as cur:

                # ====================================================
                # BUSCA Y BLOQUEA EL TOKEN
                # ====================================================

                cur.execute(
                    """
                    select *
                    from tokens
                    where client_id = %s
                    and token_hash = %s
                    for update
                    """,
                    (client_id, token_hash)
                )

                token = cur.fetchone()

                # Verifica existencia del token
                if not token:
                    raise HTTPException(
                        status_code=404,
                        detail="Token no encontrado para este cliente"
                    )

                # Verifica estado del token
                if token["status"] != "AVAILABLE":
                    raise HTTPException(
                        status_code=400,
                        detail="Solo se pueden devolver tokens AVAILABLE"
                    )

                # ====================================================
                # BLOQUEA WALLET DEL CLIENTE
                # ====================================================

                cur.execute(
                    "select * from wallets where user_id = %s for update",
                    (client_id,)
                )

                wallet = cur.fetchone()

                # Verifica existencia de wallet
                if not wallet:
                    raise HTTPException(
                        status_code=404,
                        detail="Wallet no encontrada"
                    )

                # Guarda saldos anteriores
                blocked_before = wallet["blocked_balance_cop"]
                available_before = wallet["available_balance_cop"]

                # Verifica saldo bloqueado suficiente
                if blocked_before < 10000:
                    raise HTTPException(
                        status_code=400,
                        detail="Saldo bloqueado insuficiente para devolver token"
                    )

                # ====================================================
                # MARCA TOKEN COMO DEVUELTO
                # ====================================================

                cur.execute(
                    """
                    update tokens

                    set
                        status = 'RETURNED',
                        returned_at = now()

                    where id = %s

                    returning *
                    """,
                    (token["id"],)
                )

                returned_token = cur.fetchone()

                # ====================================================
                # ACTUALIZA WALLET
                # ====================================================

                cur.execute(
                    """
                    update wallets

                    set
                        blocked_balance_cop = blocked_balance_cop - 10000,
                        available_balance_cop = available_balance_cop + 10000,
                        updated_at = now()

                    where user_id = %s

                    returning *
                    """,
                    (client_id,)
                )

                updated_wallet = cur.fetchone()

                # ====================================================
                # REGISTRA MOVIMIENTO
                # ====================================================

                cur.execute(
                    """
                    insert into wallet_movements(
                        wallet_id,
                        user_id,
                        movement_type,
                        amount_cop,
                        balance_before_cop,
                        balance_after_cop,
                        related_token_id,
                        description
                    )

                    values (
                        %s,
                        %s,
                        'TOKEN_REFUND',
                        %s,
                        %s,
                        %s,
                        %s,
                        %s
                    )
                    """,
                    (
                        wallet["id"],
                        client_id,
                        10000,
                        blocked_before,
                        updated_wallet["blocked_balance_cop"],
                        token["id"],
                        "Devolución de token no usado"
                    )
                )

                # Guarda cambios
                conn.commit()

                return {
                    "message": "Token devuelto correctamente",

                    "token": {
                        "id": returned_token["id"],
                        "payment_code": returned_token["payment_code"],
                        "status": returned_token["status"],
                        "returned_at": returned_token["returned_at"]
                    },

                    "wallet": dict(updated_wallet)
                }

        except HTTPException:

            # Revierte cambios si ocurre error HTTP
            conn.rollback()
            raise

        except Exception as e:

            # Revierte cambios ante errores generales
            conn.rollback()

            raise HTTPException(
                status_code=400,
                detail=str(e)
            )


# ============================================================
# VALIDAR PAGO POR PAQUETE DE TOKENS
# ============================================================

@app.post("/payments/validate-package")
def validate_package_payment(data: ValidatePackagePaymentRequest):
    """
    Valida un pago utilizando múltiples tokens.

    El proceso:
    1. Verifica vendedor
    2. Verifica tokens
    3. Verifica estados
    4. Verifica cliente único
    5. Descuenta saldo bloqueado
    6. Aumenta saldo del vendedor
    7. Marca tokens como USED
    8. Registra transacciones y movimientos
    """

    # Inicia medición de tiempo de respuesta
    start = time.perf_counter()

    # Limpia seller_id
    seller_id = data.seller_id.strip()

    # Normaliza todos los payment_codes
    payment_codes = [
        normalize_payment_code(code)
        for code in data.payment_codes
        if code.strip()
    ]

    # ========================================================
    # VALIDACIONES INICIALES
    # ========================================================

    # Verifica que existan códigos
    if not payment_codes:
        raise HTTPException(
            status_code=400,
            detail="Debes enviar al menos un payment_code"
        )

    # Verifica duplicados
    if len(set(payment_codes)) != len(payment_codes):
        raise HTTPException(
            status_code=400,
            detail="El paquete contiene payment_codes duplicados"
        )

    # Genera hashes de tokens
    token_hashes = [
        hash_payment_code(code)
        for code in payment_codes
    ]

    # Calcula monto total
    total_amount = len(payment_codes) * 10000

    # ========================================================
    # CONEXIÓN A BASE DE DATOS
    # ========================================================

    with get_conn() as conn:

        try:
            with conn.cursor() as cur:

                # ====================================================
                # VERIFICA VENDEDOR
                # ====================================================

                cur.execute(
                    "select id, role from profiles where id = %s",
                    (seller_id,)
                )

                seller = cur.fetchone()

                # Verifica existencia
                if not seller:
                    raise HTTPException(
                        status_code=404,
                        detail="Vendedor no encontrado"
                    )

                # Verifica rol
                if seller["role"] != "SELLER":
                    raise HTTPException(
                        status_code=400,
                        detail="El usuario no tiene rol SELLER"
                    )

                # ====================================================
                # BUSCA TOKENS
                # ====================================================

                cur.execute(
                    """
                    select *
                    from tokens
                    where token_hash = any(%s)
                    for update
                    """,
                    (token_hashes,)
                )

                tokens = cur.fetchall()

                # Calcula tiempo de respuesta
                response_time_ms = int(
                    (time.perf_counter() - start) * 1000
                )

                # ====================================================
                # VERIFICA TOKENS FALTANTES
                # ====================================================

                if len(tokens) != len(token_hashes):

                    found_hashes = {
                        token["token_hash"]
                        for token in tokens
                    }

                    missing_hashes = [
                        token_hash
                        for token_hash in token_hashes
                        if token_hash not in found_hashes
                    ]

                    return {
                        "approved": False,
                        "reason": "TOKEN_PACKAGE_INCOMPLETE",
                        "message": "Faltan uno o más tokens del paquete"
                    }

                # ====================================================
                # VERIFICA CLIENTE ÚNICO
                # ====================================================

                client_ids = {
                    token["client_id"]
                    for token in tokens
                }

                if len(client_ids) != 1:

                    return {
                        "approved": False,
                        "reason": "TOKEN_PACKAGE_MIXED_CLIENTS",
                        "message": "El paquete contiene tokens de distintos clientes"
                    }

                # ====================================================
                # VERIFICA TOKENS DISPONIBLES
                # ====================================================

                unavailable_tokens = [
                    token
                    for token in tokens
                    if token["status"] != "AVAILABLE"
                ]

                if unavailable_tokens:

                    bad_token = unavailable_tokens[0]

                    return {
                        "approved": False,
                        "reason": f"TOKEN_ALREADY_{bad_token['status']}",
                        "message": "Uno o más tokens del paquete ya no están disponibles"
                    }

                # Obtiene client_id
                client_id = tokens[0]["client_id"]

                # ====================================================
                # BLOQUEA WALLETS
                # ====================================================

                cur.execute(
                    "select * from wallets where user_id = %s for update",
                    (client_id,)
                )

                client_wallet_before = cur.fetchone()

                cur.execute(
                    "select * from wallets where user_id = %s for update",
                    (seller_id,)
                )

                seller_wallet_before = cur.fetchone()

                # Verifica wallet cliente
                if not client_wallet_before:
                    raise HTTPException(
                        status_code=404,
                        detail="Wallet del cliente no encontrada"
                    )

                # Verifica wallet vendedor
                if not seller_wallet_before:
                    raise HTTPException(
                        status_code=404,
                        detail="Wallet del vendedor no encontrada"
                    )

                # Verifica saldo bloqueado
                if client_wallet_before["blocked_balance_cop"] < total_amount:
                    raise HTTPException(
                        status_code=400,
                        detail="Saldo bloqueado insuficiente"
                    )

                # ====================================================
                # MARCA TOKENS COMO USED
                # ====================================================

                token_ids = [token["id"] for token in tokens]

                cur.execute(
                    """
                    update tokens

                    set
                        status = 'USED',
                        used_at = now()

                    where id = any(%s::uuid[])

                    returning
                        id,
                        payment_code,
                        status,
                        used_at
                    """,
                    (token_ids,)
                )

                used_tokens = cur.fetchall()

                # ====================================================
                # ACTUALIZA WALLET CLIENTE
                # ====================================================

                cur.execute(
                    """
                    update wallets

                    set
                        blocked_balance_cop = blocked_balance_cop - %s,
                        updated_at = now()

                    where user_id = %s

                    returning *
                    """,
                    (
                        total_amount,
                        client_id
                    )
                )

                client_wallet_after = cur.fetchone()

                # ====================================================
                # ACTUALIZA WALLET VENDEDOR
                # ====================================================

                cur.execute(
                    """
                    update wallets

                    set
                        available_balance_cop = available_balance_cop + %s,
                        updated_at = now()

                    where user_id = %s

                    returning *
                    """,
                    (
                        total_amount,
                        seller_id
                    )
                )

                seller_wallet_after = cur.fetchone()

                # ====================================================
                # REGISTRA TRANSACCIONES
                # ====================================================

                transaction_ids = []

                for token in tokens:

                    cur.execute(
                        """
                        insert into transactions(
                            token_id,
                            token_hash,
                            client_id,
                            seller_id,
                            amount_cop,
                            status,
                            qr_payload,
                            response_time_ms
                        )

                        values (
                            %s,
                            %s,
                            %s,
                            %s,
                            10000,
                            'APPROVED',
                            %s,
                            %s
                        )

                        returning id
                        """,
                        (
                            token["id"],
                            token["token_hash"],
                            token["client_id"],
                            seller_id,
                            Json({
                                "payment_codes": payment_codes,
                                "package_size": len(payment_codes),
                                "total_amount_cop": total_amount
                            }),
                            response_time_ms
                        )
                    )

                    transaction_ids.append(
                        cur.fetchone()["id"]
                    )

                # ====================================================
                # MOVIMIENTO CLIENTE
                # ====================================================

                cur.execute(
                    """
                    insert into wallet_movements(
                        wallet_id,
                        user_id,
                        movement_type,
                        amount_cop,
                        balance_before_cop,
                        balance_after_cop,
                        description
                    )

                    values (
                        %s,
                        %s,
                        'PAYMENT_SENT',
                        %s,
                        %s,
                        %s,
                        %s
                    )
                    """,
                    (
                        client_wallet_before["id"],
                        client_id,
                        total_amount,
                        client_wallet_before["blocked_balance_cop"],
                        client_wallet_after["blocked_balance_cop"],
                        f"Pago offline aprobado por paquete de {len(payment_codes)} token(es)"
                    )
                )

                # ====================================================
                # MOVIMIENTO VENDEDOR
                # ====================================================

                cur.execute(
                    """
                    insert into wallet_movements(
                        wallet_id,
                        user_id,
                        movement_type,
                        amount_cop,
                        balance_before_cop,
                        balance_after_cop,
                        description
                    )

                    values (
                        %s,
                        %s,
                        'PAYMENT_RECEIVED',
                        %s,
                        %s,
                        %s,
                        %s
                    )
                    """,
                    (
                        seller_wallet_before["id"],
                        seller_id,
                        total_amount,
                        seller_wallet_before["available_balance_cop"],
                        seller_wallet_after["available_balance_cop"],
                        f"Cobro recibido por paquete de {len(payment_codes)} token(es)"
                    )
                )

                # Guarda cambios
                conn.commit()

                return {
                    "approved": True,
                    "message": "Pago aprobado",
                    "total_amount_cop": total_amount,
                    "tokens_used": len(used_tokens),
                    "used_tokens": [dict(token) for token in used_tokens],
                    "transaction_ids": transaction_ids,
                    "client_wallet": dict(client_wallet_after),
                    "seller_wallet": dict(seller_wallet_after)
                }

        except HTTPException:

            # Revierte cambios si ocurre error HTTP
            conn.rollback()
            raise

        except Exception as e:

            # Revierte cambios ante errores generales
            conn.rollback()

            raise HTTPException(
                status_code=400,
                detail=str(e)
            )


# ============================================================
# OBTENER TRANSACCIONES
# ============================================================

@app.get("/transactions")
def get_transactions():
    """
    Retorna todas las transacciones registradas
    ordenadas desde la más reciente.
    """

    with get_conn() as conn:
        with conn.cursor() as cur:

            cur.execute(
                """
                select *
                from transactions
                order by created_at desc
                """
            )

            rows = cur.fetchall()

            return {
                "transactions": [dict(row) for row in rows]
            }


# ============================================================
# OBTENER INTENTOS INVÁLIDOS
# ============================================================

@app.get("/admin/invalid-attempts")
def get_invalid_attempts():
    """
    Retorna todos los intentos inválidos
    registrados en el sistema.
    """

    with get_conn() as conn:
        with conn.cursor() as cur:

            cur.execute(
                """
                select *
                from invalid_attempts
                order by created_at desc
                """
            )

            rows = cur.fetchall()

            return {
                "invalid_attempts": [dict(row) for row in rows]
            }