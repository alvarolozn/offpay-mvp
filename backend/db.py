
import os
from contextlib import contextmanager

import psycopg2
from psycopg2.extras import RealDictCursor
from dotenv import load_dotenv

# Carga las variables del archivo .env
load_dotenv()

# Obtiene la URL de conexión de la base de datos
DATABASE_URL = os.getenv("DATABASE_URL")


@contextmanager
def get_conn():
    """
    Crea y administra una conexión a PostgreSQL.

    La función:
    - Verifica que DATABASE_URL exista.
    - Abre la conexión a la base de datos.
    - Retorna la conexión para ser utilizada.
    - Cierra automáticamente la conexión al finalizar.
    """

    # Verifica que exista la variable de entorno
    if not DATABASE_URL:
        raise RuntimeError("DATABASE_URL no está configurada en el archivo .env")

    # Crea la conexión con PostgreSQL
    conn = psycopg2.connect(
        DATABASE_URL,
        cursor_factory=RealDictCursor
    )

    try:
        # Retorna la conexión
        yield conn

    finally:
        # Cierra la conexión automáticamente
        conn.close()
