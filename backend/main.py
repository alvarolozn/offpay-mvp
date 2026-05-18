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

load_dotenv()

APP_NAME = os.getenv("APP_NAME", "OffPay MVP API")
APP_VERSION = os.getenv("APP_VERSION", "1.0.0")

app = FastAPI(
    title=APP_NAME,
    version=APP_VERSION
)


class RegisterClientRequest(BaseModel):
    full_name: str
    email: EmailStr
    password: str


class RegisterSellerRequest(BaseModel):
    full_name: str
    email: EmailStr
    password: str
    commerce_name: str


class RechargeRequest(BaseModel):
    user_id: str
    amount_cop: int


class GenerateTokensRequest(BaseModel):
    client_id: str
    amount_cop: int


class ValidatePaymentRequest(BaseModel):
    seller_id: str
    payment_code: str

class RefundTokenRequest(BaseModel):
    client_id: str
    payment_code: str

@app.get("/")
def root():
    return {
        "message": "Backend de OffPay funcionando",
        "docs": "/docs"
    }


@app.get("/health")
def health():
    return {
        "status": "ok",
        "app": APP_NAME,
        "version": APP_VERSION
    }


@app.get("/db-test")
def db_test():
    try:
        with get_conn() as conn:
            with conn.cursor() as cur:
                cur.execute("select now() as server_time;")
                result = cur.fetchone()

        return {
            "status": "ok",
            "message": "Conexión a Supabase exitosa",
            "server_time": result["server_time"]
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error conectando a la base de datos: {str(e)}")


@app.post("/clients/register")
def register_client(data: RegisterClientRequest):
    return _register_user(
        full_name=data.full_name,
        email=data.email,
        password=data.password,
        role="CLIENT",
        commerce_name=None
    )


@app.post("/sellers/register")
def register_seller(data: RegisterSellerRequest):
    return _register_user(
        full_name=data.full_name,
        email=data.email,
        password=data.password,
        role="SELLER",
        commerce_name=data.commerce_name
    )


def _register_user(full_name: str, email: str, password: str, role: str, commerce_name: str | None):
    password_hash = hash_password(password)

    with get_conn() as conn:
        try:
            with conn.cursor() as cur:
                cur.execute(
                    """
                    insert into profiles(email, password_hash, full_name, role, commerce_name)
                    values (%s, %s, %s, %s, %s)
                    returning id, email, full_name, role, commerce_name
                    """,
                    (email, password_hash, full_name, role, commerce_name)
                )

                user = cur.fetchone()

                cur.execute(
                    """
                    insert into wallets(user_id, available_balance_cop, blocked_balance_cop)
                    values (%s, 0, 0)
                    returning id, available_balance_cop, blocked_balance_cop
                    """,
                    (user["id"],)
                )

                wallet = cur.fetchone()
                conn.commit()

                return {
                    "message": f"{role} registrado correctamente",
                    "user": dict(user),
                    "wallet": dict(wallet)
                }

        except Exception as e:
            conn.rollback()
            raise HTTPException(status_code=400, detail=str(e))


@app.get("/wallets/{user_id}")
def get_wallet(user_id: str):
    clean_user_id = user_id.strip()

    with get_conn() as conn:
        with conn.cursor() as cur:
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
                join profiles p on p.id = w.user_id
                where w.user_id = %s
                """,
                (clean_user_id,)
            )

            wallet = cur.fetchone()

            if not wallet:
                raise HTTPException(status_code=404, detail="Wallet no encontrada")

            return dict(wallet)


@app.post("/wallets/recharge")
def recharge_wallet(data: RechargeRequest):
    if data.amount_cop <= 0:
        raise HTTPException(status_code=400, detail="El monto debe ser mayor a cero")

    with get_conn() as conn:
        try:
            with conn.cursor() as cur:
                cur.execute(
                    "select * from wallets where user_id = %s for update",
                    (data.user_id.strip(),)
                )
                wallet = cur.fetchone()

                if not wallet:
                    raise HTTPException(status_code=404, detail="Wallet no encontrada")

                before = wallet["available_balance_cop"]
                after = before + data.amount_cop

                cur.execute(
                    """
                    update wallets
                    set available_balance_cop = %s, updated_at = now()
                    where user_id = %s
                    returning *
                    """,
                    (after, data.user_id.strip())
                )

                updated_wallet = cur.fetchone()

                cur.execute(
                    """
                    insert into wallet_movements(
                        wallet_id, user_id, movement_type, amount_cop,
                        balance_before_cop, balance_after_cop, description
                    )
                    values (%s, %s, 'RECHARGE', %s, %s, %s, %s)
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
            raise HTTPException(status_code=400, detail=str(e))


@app.post("/tokens/generate")
def generate_tokens(data: GenerateTokensRequest):
    client_id = data.client_id.strip()

    if data.amount_cop <= 0:
        raise HTTPException(status_code=400, detail="El monto debe ser mayor a cero")

    if data.amount_cop % 10000 != 0:
        raise HTTPException(status_code=400, detail="El monto debe ser múltiplo de 10000 COP")

    token_count = data.amount_cop // 10000

    with get_conn() as conn:
        try:
            with conn.cursor() as cur:
                cur.execute(
                    "select * from wallets where user_id = %s for update",
                    (client_id,)
                )
                wallet = cur.fetchone()

                if not wallet:
                    raise HTTPException(status_code=404, detail="Wallet no encontrada")

                if wallet["available_balance_cop"] < data.amount_cop:
                    raise HTTPException(status_code=400, detail="Saldo disponible insuficiente")

                available_before = wallet["available_balance_cop"]
                available_after = available_before - data.amount_cop
                blocked_after = wallet["blocked_balance_cop"] + data.amount_cop

                cur.execute(
                    """
                    update wallets
                    set available_balance_cop = %s,
                        blocked_balance_cop = %s,
                        updated_at = now()
                    where user_id = %s
                    returning *
                    """,
                    (available_after, blocked_after, client_id)
                )

                updated_wallet = cur.fetchone()

                cur.execute(
                    """
                    insert into wallet_movements(
                        wallet_id, user_id, movement_type, amount_cop,
                        balance_before_cop, balance_after_cop, description
                    )
                    values (%s, %s, 'BLOCK_BALANCE', %s, %s, %s, %s)
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

                generated_tokens = []

                for _ in range(token_count):
                    cur.execute(
                        "select coalesce(max(counter), 0) + 1 as next_counter from tokens where client_id = %s",
                        (client_id,)
                    )
                    counter = cur.fetchone()["next_counter"]

                    payment_code = generate_payment_code()
                    token_hash = hash_payment_code(payment_code)
                    signature = make_signature(token_hash)

                    qr_payload = {
                        "version": "OP1",
                        "payment_code": payment_code,
                        "token_hash": token_hash,
                        "value_cop": 10000
                    }

                    cur.execute(
                        """
                        insert into tokens(
                            client_id, counter, payment_code, signature, token_hash,
                            value_cop, status, blockchain_status, chain_id, qr_payload
                        )
                        values (%s, %s, %s, %s, %s, 10000, 'AVAILABLE', 'PENDING', 80002, %s)
                        returning id, client_id, counter, payment_code, token_hash,
                                  value_cop, status, blockchain_status, created_at
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
            raise HTTPException(status_code=400, detail=str(e))


@app.get("/tokens/client/{client_id}")
def get_client_tokens(client_id: str):
    clean_client_id = client_id.strip()

    with get_conn() as conn:
        with conn.cursor() as cur:
            cur.execute(
                """
                select id, client_id, counter, payment_code, token_hash, value_cop,
                       status, blockchain_status, created_at, used_at, returned_at
                from tokens
                where client_id = %s
                order by counter asc
                """,
                (clean_client_id,)
            )

            tokens = cur.fetchall()

            return {
                "client_id": clean_client_id,
                "tokens": [dict(token) for token in tokens]
            }


@app.post("/payments/validate")
def validate_payment(data: ValidatePaymentRequest):
    start = time.perf_counter()
    seller_id = data.seller_id.strip()
    payment_code = normalize_payment_code(data.payment_code)
    token_hash = hash_payment_code(payment_code)

    with get_conn() as conn:
        try:
            with conn.cursor() as cur:
                cur.execute(
                    "select id, role from profiles where id = %s",
                    (seller_id,)
                )
                seller = cur.fetchone()

                if not seller:
                    raise HTTPException(status_code=404, detail="Vendedor no encontrado")

                if seller["role"] != "SELLER":
                    raise HTTPException(status_code=400, detail="El usuario no tiene rol SELLER")

                cur.execute(
                    """
                    select *
                    from tokens
                    where token_hash = %s
                    for update
                    """,
                    (token_hash,)
                )
                token = cur.fetchone()

                response_time_ms = int((time.perf_counter() - start) * 1000)

                if not token:
                    cur.execute(
                        """
                        insert into transactions(
                            token_hash, seller_id, amount_cop, status,
                            rejection_reason, qr_payload, response_time_ms
                        )
                        values (%s, %s, 10000, 'REJECTED', %s, %s, %s)
                        returning *
                        """,
                        (
                            token_hash,
                            seller_id,
                            "TOKEN_NOT_FOUND",
                            Json({"payment_code": payment_code}),
                            response_time_ms
                        )
                    )
                    transaction = cur.fetchone()

                    cur.execute(
                        """
                        insert into invalid_attempts(
                            transaction_id, token_hash, seller_id, reason, qr_payload
                        )
                        values (%s, %s, %s, %s, %s)
                        """,
                        (
                            transaction["id"],
                            token_hash,
                            seller_id,
                            "TOKEN_NOT_FOUND",
                            Json({"payment_code": payment_code})
                        )
                    )

                    conn.commit()

                    return {
                        "approved": False,
                        "reason": "TOKEN_NOT_FOUND",
                        "message": "Token inexistente o inválido"
                    }

                if token["status"] != "AVAILABLE":
                    cur.execute(
                        """
                        insert into transactions(
                            token_id, token_hash, client_id, seller_id, amount_cop,
                            status, rejection_reason, qr_payload, response_time_ms
                        )
                        values (%s, %s, %s, %s, 10000, 'REJECTED', %s, %s, %s)
                        returning *
                        """,
                        (
                            token["id"],
                            token["token_hash"],
                            token["client_id"],
                            seller_id,
                            f"TOKEN_ALREADY_{token['status']}",
                            Json({"payment_code": payment_code}),
                            response_time_ms
                        )
                    )
                    transaction = cur.fetchone()

                    cur.execute(
                        """
                        insert into invalid_attempts(
                            transaction_id, token_id, token_hash, seller_id, reason, qr_payload
                        )
                        values (%s, %s, %s, %s, %s, %s)
                        """,
                        (
                            transaction["id"],
                            token["id"],
                            token["token_hash"],
                            seller_id,
                            f"TOKEN_ALREADY_{token['status']}",
                            Json({"payment_code": payment_code})
                        )
                    )

                    conn.commit()

                    return {
                        "approved": False,
                        "reason": f"TOKEN_ALREADY_{token['status']}",
                        "message": "El token ya no está disponible"
                    }

                cur.execute(
                    """
                    update tokens
                    set status = 'USED',
                        used_at = now()
                    where id = %s
                    returning *
                    """,
                    (token["id"],)
                )
                used_token = cur.fetchone()

                cur.execute(
                    """
                    update wallets
                    set blocked_balance_cop = blocked_balance_cop - 10000,
                        updated_at = now()
                    where user_id = %s
                    returning *
                    """,
                    (token["client_id"],)
                )
                client_wallet = cur.fetchone()

                cur.execute(
                    """
                    update wallets
                    set available_balance_cop = available_balance_cop + 10000,
                        updated_at = now()
                    where user_id = %s
                    returning *
                    """,
                    (seller_id,)
                )
                seller_wallet = cur.fetchone()

                response_time_ms = int((time.perf_counter() - start) * 1000)

                cur.execute(
                    """
                    insert into transactions(
                        token_id, token_hash, client_id, seller_id, amount_cop,
                        status, qr_payload, response_time_ms
                    )
                    values (%s, %s, %s, %s, 10000, 'APPROVED', %s, %s)
                    returning *
                    """,
                    (
                        token["id"],
                        token["token_hash"],
                        token["client_id"],
                        seller_id,
                        Json({"payment_code": payment_code}),
                        response_time_ms
                    )
                )
                transaction = cur.fetchone()

                cur.execute(
                    """
                    insert into wallet_movements(
                        wallet_id, user_id, movement_type, amount_cop,
                        balance_before_cop, balance_after_cop,
                        related_token_id, related_transaction_id, description
                    )
                    values (
                        %s, %s, 'PAYMENT_SENT', 10000,
                        %s, %s,
                        %s, %s, %s
                    )
                    """,
                    (
                        client_wallet["id"],
                        token["client_id"],
                        client_wallet["blocked_balance_cop"] + 10000,
                        client_wallet["blocked_balance_cop"],
                        token["id"],
                        transaction["id"],
                        "Pago realizado con token OffPay"
                    )
                )

                cur.execute(
                    """
                    insert into wallet_movements(
                        wallet_id, user_id, movement_type, amount_cop,
                        balance_before_cop, balance_after_cop,
                        related_token_id, related_transaction_id, description
                    )
                    values (
                        %s, %s, 'PAYMENT_RECEIVED', 10000,
                        %s, %s,
                        %s, %s, %s
                    )
                    """,
                    (
                        seller_wallet["id"],
                        seller_id,
                        seller_wallet["available_balance_cop"] - 10000,
                        seller_wallet["available_balance_cop"],
                        token["id"],
                        transaction["id"],
                        "Pago recibido por vendedor"
                    )
                )

                conn.commit()

                return {
                    "approved": True,
                    "message": "Pago aprobado",
                    "token": {
                        "id": used_token["id"],
                        "payment_code": used_token["payment_code"],
                        "status": used_token["status"],
                        "used_at": used_token["used_at"]
                    },
                    "transaction": dict(transaction),
                    "client_wallet": dict(client_wallet),
                    "seller_wallet": dict(seller_wallet)
                }

        except HTTPException:
            conn.rollback()
            raise
        except Exception as e:
            conn.rollback()
            raise HTTPException(status_code=400, detail=str(e))


@app.post("/tokens/refund")
def refund_token(data: RefundTokenRequest):
    client_id = data.client_id.strip()
    payment_code = normalize_payment_code(data.payment_code)
    token_hash = hash_payment_code(payment_code)

    with get_conn() as conn:
        try:
            with conn.cursor() as cur:
                cur.execute(
                    """
                    select *
                    from tokens
                    where client_id = %s and token_hash = %s
                    for update
                    """,
                    (client_id, token_hash)
                )
                token = cur.fetchone()

                if not token:
                    raise HTTPException(status_code=404, detail="Token no encontrado para este cliente")

                if token["status"] != "AVAILABLE":
                    raise HTTPException(status_code=400, detail="Solo se pueden devolver tokens AVAILABLE")

                cur.execute(
                    "select * from wallets where user_id = %s for update",
                    (client_id,)
                )
                wallet = cur.fetchone()

                if not wallet:
                    raise HTTPException(status_code=404, detail="Wallet no encontrada")

                blocked_before = wallet["blocked_balance_cop"]
                available_before = wallet["available_balance_cop"]

                if blocked_before < 10000:
                    raise HTTPException(status_code=400, detail="Saldo bloqueado insuficiente para devolver token")

                cur.execute(
                    """
                    update tokens
                    set status = 'RETURNED',
                        returned_at = now()
                    where id = %s
                    returning *
                    """,
                    (token["id"],)
                )
                returned_token = cur.fetchone()

                cur.execute(
                    """
                    update wallets
                    set blocked_balance_cop = blocked_balance_cop - 10000,
                        available_balance_cop = available_balance_cop + 10000,
                        updated_at = now()
                    where user_id = %s
                    returning *
                    """,
                    (client_id,)
                )
                updated_wallet = cur.fetchone()

                cur.execute(
                    """
                    insert into wallet_movements(
                        wallet_id, user_id, movement_type, amount_cop,
                        balance_before_cop, balance_after_cop,
                        related_token_id, description
                    )
                    values (%s, %s, 'TOKEN_REFUND', %s, %s, %s, %s, %s)
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
            conn.rollback()
            raise
        except Exception as e:
            conn.rollback()
            raise HTTPException(status_code=400, detail=str(e))
        
@app.get("/transactions")
def get_transactions():
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


@app.get("/admin/invalid-attempts")
def get_invalid_attempts():
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