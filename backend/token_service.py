import hashlib
import os
import secrets
import string

from dotenv import load_dotenv

load_dotenv()

SECRET_KEY = os.getenv("SECRET_KEY", "offpay_demo_secret")


def hash_password(password: str) -> str:
    raw = f"{password}:{SECRET_KEY}"
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()


def normalize_payment_code(payment_code: str) -> str:
    return payment_code.strip().upper().replace(" ", "")


def generate_payment_code() -> str:
    alphabet = string.ascii_uppercase + string.digits
    part1 = "".join(secrets.choice(alphabet) for _ in range(4))
    part2 = "".join(secrets.choice(alphabet) for _ in range(4))
    return f"OP-{part1}-{part2}"


def hash_payment_code(payment_code: str) -> str:
    normalized = normalize_payment_code(payment_code)
    raw = f"{normalized}:{SECRET_KEY}"
    return "0x" + hashlib.sha256(raw.encode("utf-8")).hexdigest()


def make_signature(token_hash: str) -> str:
    raw = f"{token_hash}:{SECRET_KEY}"
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()