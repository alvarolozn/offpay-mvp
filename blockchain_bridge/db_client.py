import os
from typing import Any, List, Dict, Sequence

from dotenv import load_dotenv
import psycopg2
from psycopg2.extras import RealDictCursor


load_dotenv()


DATABASE_URL = os.getenv("DATABASE_URL")


def validate_db_config() -> None:
    if not DATABASE_URL:
        raise ValueError("Falta DATABASE_URL en el .env")


def get_connection():
    validate_db_config()
    return psycopg2.connect(DATABASE_URL)


def fetch_all(query: str, params: tuple = ()) -> List[Dict[str, Any]]:
    conn = get_connection()
    try:
        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute(query, params)
            rows = cur.fetchall()
            return [dict(row) for row in rows]
    finally:
        conn.close()


def fetch_one(query: str, params: tuple = ()) -> Dict[str, Any] | None:
    conn = get_connection()
    try:
        with conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute(query, params)
            row = cur.fetchone()
            return dict(row) if row else None
    finally:
        conn.close()


def execute(query: str, params: tuple = ()) -> None:
    conn = get_connection()
    try:
        with conn.cursor() as cur:
            cur.execute(query, params)
        conn.commit()
    finally:
        conn.close()


def execute_many(query: str, params_list: Sequence[tuple]) -> None:
    conn = get_connection()
    try:
        with conn.cursor() as cur:
            cur.executemany(query, params_list)
        conn.commit()
    finally:
        conn.close()