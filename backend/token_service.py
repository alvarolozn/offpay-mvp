# ============================================================
# IMPORTACIONES
# ============================================================

# Librería utilizada para generar hashes SHA-256
import hashlib

# Librería utilizada para acceder a variables de entorno
import os

# Librería para generación segura de valores aleatorios
import secrets

# Librería con constantes de caracteres (A-Z, 0-9, etc.)
import string

# Librería para cargar variables desde archivo .env
from dotenv import load_dotenv


# ============================================================
# CARGA DE VARIABLES DE ENTORNO
# ============================================================

# Carga automáticamente las variables definidas en el archivo .env
load_dotenv()

# Obtiene la SECRET_KEY desde variables de entorno.
# Si no existe, usa una clave por defecto.
SECRET_KEY = os.getenv("SECRET_KEY", "offpay_demo_secret")


# ============================================================
# GENERAR HASH DE CONTRASEÑA
# ============================================================

def hash_password(password: str) -> str:
    """
    Genera un hash SHA-256 de la contraseña.

    El proceso:
    1. Concatena contraseña + SECRET_KEY
    2. Codifica el texto en UTF-8
    3. Genera hash SHA-256
    4. Retorna el hash hexadecimal
    """

    # Concatena contraseña y clave secreta
    raw = f"{password}:{SECRET_KEY}"

    # Genera hash SHA-256
    return hashlib.sha256(
        raw.encode("utf-8")
    ).hexdigest()


# ============================================================
# NORMALIZAR PAYMENT CODE
# ============================================================

def normalize_payment_code(payment_code: str) -> str:
    """
    Normaliza un payment code.

    El proceso:
    1. Elimina espacios al inicio y final
    2. Convierte a mayúsculas
    3. Elimina espacios internos
    """

    return payment_code.strip().upper().replace(" ", "")


# ============================================================
# GENERAR PAYMENT CODE
# ============================================================

def generate_payment_code() -> str:
    """
    Genera un código de pago aleatorio.

    Formato generado:
    OP-XXXX-XXXX

    Donde:
    - X puede ser letra mayúscula o número
    """

    # Define caracteres permitidos
    alphabet = string.ascii_uppercase + string.digits

    # Genera primera parte aleatoria
    part1 = "".join(
        secrets.choice(alphabet)
        for _ in range(4)
    )

    # Genera segunda parte aleatoria
    part2 = "".join(
        secrets.choice(alphabet)
        for _ in range(4)
    )

    # Retorna código final
    return f"OP-{part1}-{part2}"


# ============================================================
# GENERAR HASH DE PAYMENT CODE
# ============================================================

def hash_payment_code(payment_code: str) -> str:
    """
    Genera un hash SHA-256 de un payment code.

    El proceso:
    1. Normaliza el código
    2. Concatena código + SECRET_KEY
    3. Genera hash SHA-256
    4. Agrega prefijo '0x'
    """

    # Normaliza payment code
    normalized = normalize_payment_code(payment_code)

    # Concatena con SECRET_KEY
    raw = f"{normalized}:{SECRET_KEY}"

    # Genera hash SHA-256 y agrega prefijo hexadecimal
    return "0x" + hashlib.sha256(
        raw.encode("utf-8")
    ).hexdigest()


# ============================================================
# GENERAR FIRMA DEL TOKEN
# ============================================================

def make_signature(token_hash: str) -> str:
    """
    Genera una firma criptográfica basada
    en el hash del token y la SECRET_KEY.

    El proceso:
    1. Concatena token_hash + SECRET_KEY
    2. Genera hash SHA-256
    3. Retorna firma hexadecimal
    """

    # Concatena hash y clave secreta
    raw = f"{token_hash}:{SECRET_KEY}"

    # Genera firma SHA-256
    return hashlib.sha256(
        raw.encode("utf-8")
    ).hexdigest()