#!/usr/bin/env python3
"""Seals a file for Emma's beta channel (app: Beta.kt, CI: .github/workflows/beta.yml).

    python3 tools/beta/seal.py IN OUT        (the code in the CAPRI_BETA_CODE environment variable)
    python3 tools/beta/seal.py --open IN OUT (the other way, to check)

Format: b"CPB1" | salt (16) | iv (16) | AES-256-CTR ciphertext | HMAC-SHA256 (32) of everything before it.
Keys: PBKDF2-HMAC-SHA256(code, salt, 100 000 rounds, 64 bytes) = AES key (32) | MAC key (32).
"""
import hashlib, hmac, os, re, sys
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

MAGIC = b"CPB1"
ROUNDS = 100_000
SHAPE = re.compile(r"^[A-Z2-7]{5}(-[A-Z2-7]{5}){3}$")


def keys(code: str, salt: bytes):
    k = hashlib.pbkdf2_hmac("sha256", code.encode(), salt, ROUNDS, 64)
    return k[:32], k[32:]


def norm(code: str) -> str:
    code = code.strip().upper().replace(" ", "")
    if not SHAPE.match(code):
        sys.exit("CAPRI_BETA_CODE no tiene la forma XXXXX-XXXXX-XXXXX-XXXXX (A-Z y 2-7)")
    return code


def seal(code, src, dst):
    salt, iv = os.urandom(16), os.urandom(16)
    ek, mk = keys(code, salt)
    enc = Cipher(algorithms.AES(ek), modes.CTR(iv)).encryptor()
    mac = hmac.new(mk, digestmod=hashlib.sha256)
    with open(src, "rb") as i, open(dst, "wb") as o:
        head = MAGIC + salt + iv
        o.write(head)
        mac.update(head)
        while chunk := i.read(1 << 20):
            c = enc.update(chunk)
            o.write(c)
            mac.update(c)
        c = enc.finalize()
        o.write(c)
        mac.update(c)
        o.write(mac.digest())


def unseal(code, src, dst):
    data = open(src, "rb").read()
    if data[:4] != MAGIC:
        sys.exit("no está sellado")
    salt, iv, body, tag = data[4:20], data[20:36], data[36:-32], data[-32:]
    ek, mk = keys(code, salt)
    if not hmac.compare_digest(hmac.new(mk, data[:-32], hashlib.sha256).digest(), tag):
        sys.exit("código equivocado o archivo dañado")
    dec = Cipher(algorithms.AES(ek), modes.CTR(iv)).decryptor()
    open(dst, "wb").write(dec.update(body) + dec.finalize())


if __name__ == "__main__":
    args = sys.argv[1:]
    code = norm(os.environ.get("CAPRI_BETA_CODE", ""))
    if args and args[0] == "--open":
        unseal(code, args[1], args[2])
    else:
        seal(code, args[0], args[1])
