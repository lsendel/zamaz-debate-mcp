#!/usr/bin/env python3
"""
GitHub authentication module for Kiro integration.
This module handles secure storage and retrieval of GitHub App credentials.
"""

import base64
import contextlib
import json
import os
import time
from pathlib import Path

from cryptography.fernet import Fernet
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC


class GitHubCredentialManager:
    """Manages secure storage and retrieval of GitHub App credentials."""

    def __init__(self, storage_path=None, encryption_key=None):
        """Initialize the credential manager."""
        self.storage_path = storage_path or os.environ.get(
            "GITHUB_CREDENTIAL_STORAGE", str(Path.home() / ".kiro" / "credentials" / "github.enc")
        )

        # Ensure directory exists
        os.makedirs(os.path.dirname(self.storage_path), exist_ok=True)

        # Set up encryption
        self.encryption_key = encryption_key or os.environ.get("GITHUB_CREDENTIAL_KEY")
        if not self.encryption_key:
            # Generate a key if none is provided (not recommended for production)
            salt = b"kiro-github-integration"
            kdf = PBKDF2HMAC(
                algorithm=hashes.SHA256(),
                length=32,
                salt=salt,
                iterations=100000,
            )
            self.encryption_key = base64.urlsafe_b64encode(kdf.derive(b"kiro-default-key"))

        self.cipher = Fernet(self.encryption_key)

    def store_credentials(self, app_id, client_id, client_secret, private_key, webhook_secret):
        """Store GitHub App credentials securely."""
        credentials = {
            "app_id": app_id,
            "client_id": client_id,
            "client_secret": client_secret,
            "private_key": private_key,
            "webhook_secret": webhook_secret,
            "stored_at": time.time(),
        }

        # Encrypt the credentials
        encrypted_data = self.cipher.encrypt(json.dumps(credentials).encode("utf-8"))

        # Write to file
        with open(self.storage_path, "wb") as f:
            f.write(encrypted_data)

        # Set secure permissions
        os.chmod(self.storage_path, 0o600)

        return True

    def get_credentials(self):
        """Retrieve GitHub App credentials."""
        if not os.path.exists(self.storage_path):
            return None

        try:
            # Read encrypted data
            with open(self.storage_path, "rb") as f:
                encrypted_data = f.read()

            # Decrypt the data
            decrypted_data = self.cipher.decrypt(encrypted_data)

            # Parse JSON
            credentials = json.loads(decrypted_data.decode("utf-8"))

            return credentials
        except Exception:
            return None

    def store_installation_token(self, installation_id, token, expires_at):
        """Store an installation token for a specific installation."""
        credentials = self.get_credentials()
        if not credentials:
            credentials = {"installations": {}}

        if "installations" not in credentials:
            credentials["installations"] = {}

        credentials["installations"][str(installation_id)] = {"token": token, "expires_at": expires_at}

        # Encrypt the credentials
        encrypted_data = self.cipher.encrypt(json.dumps(credentials).encode("utf-8"))

        # Write to file
        with open(self.storage_path, "wb") as f:
            f.write(encrypted_data)

        return True

    def get_installation_token(self, installation_id):
        """Get an installation token for a specific installation."""
        credentials = self.get_credentials()
        if not credentials or "installations" not in credentials:
            return None

        installation = credentials["installations"].get(str(installation_id))
        if not installation:
            return None

        # Check if token is expired
        if time.time() > installation["expires_at"]:
            return None

        return installation["token"]

    def clear_credentials(self):
        """Clear all stored credentials."""
        if os.path.exists(self.storage_path):
            os.remove(self.storage_path)
        return True


def setup_credentials_from_env():
    """Set up credentials from environment variables."""
    app_id = os.environ.get("GITHUB_APP_ID")
    client_id = os.environ.get("GITHUB_CLIENT_ID")
    client_secret = os.environ.get("GITHUB_CLIENT_SECRET")
    webhook_secret = os.environ.get("GITHUB_WEBHOOK_SECRET")

    # Private key can be provided as a file path or as a base64-encoded string
    private_key = None
    private_key_path = os.environ.get("GITHUB_PRIVATE_KEY_PATH")
    private_key_base64 = os.environ.get("GITHUB_PRIVATE_KEY_BASE64")

    if private_key_path and os.path.exists(private_key_path):
        with open(private_key_path) as f:
            private_key = f.read()
    elif private_key_base64:
        with contextlib.suppress(Exception):
            private_key = base64.b64decode(private_key_base64).decode("utf-8")

    if all([app_id, client_id, client_secret, private_key, webhook_secret]):
        manager = GitHubCredentialManager()
        success = manager.store_credentials(app_id, client_id, client_secret, private_key, webhook_secret)
        return success
    else:
        missing = []
        if not app_id:
            missing.append("GITHUB_APP_ID")
        if not client_id:
            missing.append("GITHUB_CLIENT_ID")
        if not client_secret:
            missing.append("GITHUB_CLIENT_SECRET")
        if not private_key:
            missing.append("GITHUB_PRIVATE_KEY_PATH or GITHUB_PRIVATE_KEY_BASE64")
        if not webhook_secret:
            missing.append("GITHUB_WEBHOOK_SECRET")

        return False


if __name__ == "__main__":
    # Example usage
    if setup_credentials_from_env():
        pass
    else:
        pass
