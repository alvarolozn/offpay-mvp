import os

from fastapi import FastAPI, HTTPException
from dotenv import load_dotenv

from db import get_conn

load_dotenv()

APP_NAME = os.getenv("APP_NAME", "OffPay MVP API")
APP_VERSION = os.getenv("APP_VERSION", "1.0.0")

app = FastAPI(
    title=APP_NAME,
    version=APP_VERSION
)


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