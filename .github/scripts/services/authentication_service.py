"""
Authentication service with single responsibility.

This service handles only authentication concerns, separated from
authorization, encryption, and other security features.
"""

import hashlib
import secrets
from datetime import datetime, timedelta
from typing import Any

import jwt

from ..core.exceptions import AuthenticationError, ConfigurationError, TokenError
from ..core.interfaces import AuthenticationInterface, CacheInterface, DatabaseInterface


class JWTConfiguration:
    """JWT configuration settings."""

    def __init__(
        self,
        secret_key: str,
        algorithm: str = "HS256",
        access_token_expire_minutes: int = 30,
        refresh_token_expire_days: int = 30,
    ):
        if not secret_key or len(secret_key) < 32:
            raise ConfigurationError("JWT secret key must be at least 32 characters")

        self.secret_key = secret_key
        self.algorithm = algorithm
        self.access_token_expire_minutes = access_token_expire_minutes
        self.refresh_token_expire_days = refresh_token_expire_days


class AuthenticationService(AuthenticationInterface):
    """
    JWT-based authentication service.

    Handles:
    - User authentication
    - Token generation and validation
    - Token refresh
    - Token revocation
    """

    def __init__(self, config: JWTConfiguration, database: DatabaseInterface, cache: CacheInterface):
        self.config = config
        self.database = database
        self.cache = cache

    async def authenticate(self, credentials: dict[str, Any]) -> str | None:
        """
        Authenticate user and return access token.

        Args:
            credentials: Dictionary with 'username' and 'password' or 'api_key'

        Returns:
            Access token if authentication successful

        Raises:
            AuthenticationError: If authentication fails
        """
        # API key authentication
        if "api_key" in credentials:
            return await self._authenticate_api_key(credentials["api_key"])

        # Username/password authentication
        if "username" in credentials and "password" in credentials:
            return await self._authenticate_password(credentials["username"], credentials["password"])

        raise AuthenticationError("Invalid credentials format")

    async def validate_token(self, token: str) -> dict[str, Any] | None:
        """
        Validate token and return claims.

        Args:
            token: JWT token to validate

        Returns:
            Token claims if valid, None otherwise
        """
        # Check if token is revoked
        if await self._is_token_revoked(token):
            return None

        try:
            # Decode token
            claims = jwt.decode(token, self.config.secret_key, algorithms=[self.config.algorithm])

            # Verify token type
            if claims.get("type") != "access":
                return None

            return claims

        except jwt.ExpiredSignatureError:
            return None
        except jwt.InvalidTokenError:
            return None

    async def refresh_token(self, refresh_token: str) -> tuple[str, str] | None:
        """
        Generate new access and refresh tokens.

        Args:
            refresh_token: Valid refresh token

        Returns:
            Tuple of (access_token, refresh_token) if successful
        """
        # Validate refresh token
        try:
            claims = jwt.decode(refresh_token, self.config.secret_key, algorithms=[self.config.algorithm])

            # Verify token type
            if claims.get("type") != "refresh":
                raise TokenError("Invalid token type")

            # Check if revoked
            if await self._is_token_revoked(refresh_token):
                raise TokenError("Token has been revoked")

            # Generate new tokens
            user_id = claims["sub"]
            new_access_token = await self._generate_access_token(user_id)
            new_refresh_token = await self._generate_refresh_token(user_id)

            # Revoke old refresh token
            await self.revoke_token(refresh_token)

            return new_access_token, new_refresh_token

        except jwt.ExpiredSignatureError:
            raise TokenError("Refresh token has expired")
        except jwt.InvalidTokenError:
            raise TokenError("Invalid refresh token")

    async def revoke_token(self, token: str) -> bool:
        """
        Revoke a token.

        Args:
            token: Token to revoke

        Returns:
            True if successfully revoked
        """
        # Extract token ID
        try:
            claims = jwt.decode(
                token,
                self.config.secret_key,
                algorithms=[self.config.algorithm],
                options={"verify_exp": False},  # Check even expired tokens
            )
            token_id = claims.get("jti")

            if not token_id:
                return False

            # Add to revocation list with TTL
            expires_at = datetime.fromtimestamp(claims["exp"])
            ttl = int((expires_at - datetime.utcnow()).total_seconds())

            if ttl > 0:
                await self.cache.set(f"revoked_token:{token_id}", True, ttl_seconds=ttl)

            return True

        except jwt.InvalidTokenError:
            return False

    async def _authenticate_api_key(self, api_key: str) -> str:
        """Authenticate using API key."""
        # Hash the API key for database lookup
        key_hash = hashlib.sha256(api_key.encode()).hexdigest()

        # Look up API key in database
        query = """
            SELECT user_id, expires_at
            FROM api_keys
            WHERE key_hash = ? AND active = 1
        """
        result = await self.database.execute(query, {"key_hash": key_hash})

        if not result:
            raise AuthenticationError("Invalid API key")

        user_id, expires_at = result[0]

        # Check expiration
        if expires_at and datetime.fromisoformat(expires_at) < datetime.utcnow():
            raise AuthenticationError("API key has expired")

        # Generate access token
        return await self._generate_access_token(user_id)

    async def _authenticate_password(self, username: str, password: str) -> str:
        """Authenticate using username and password."""
        # This is a simplified version - in production, use proper password hashing
        query = """
            SELECT id, password_hash
            FROM users
            WHERE username = ? AND active = 1
        """
        result = await self.database.execute(query, {"username": username})

        if not result:
            raise AuthenticationError("Invalid username or password")

        user_id, password_hash = result[0]

        # Verify password (simplified - use bcrypt or similar in production)
        if not self._verify_password(password, password_hash):
            raise AuthenticationError("Invalid username or password")

        # Generate access token
        return await self._generate_access_token(user_id)

    async def _generate_access_token(self, user_id: str) -> str:
        """Generate access token for user."""
        now = datetime.utcnow()
        expires_at = now + timedelta(minutes=self.config.access_token_expire_minutes)

        claims = {"sub": user_id, "type": "access", "iat": now, "exp": expires_at, "jti": secrets.token_urlsafe(16)}

        return jwt.encode(claims, self.config.secret_key, algorithm=self.config.algorithm)

    async def _generate_refresh_token(self, user_id: str) -> str:
        """Generate refresh token for user."""
        now = datetime.utcnow()
        expires_at = now + timedelta(days=self.config.refresh_token_expire_days)

        claims = {"sub": user_id, "type": "refresh", "iat": now, "exp": expires_at, "jti": secrets.token_urlsafe(16)}

        return jwt.encode(claims, self.config.secret_key, algorithm=self.config.algorithm)

    async def _is_token_revoked(self, token: str) -> bool:
        """Check if token has been revoked."""
        try:
            claims = jwt.decode(
                token, self.config.secret_key, algorithms=[self.config.algorithm], options={"verify_exp": False}
            )
            token_id = claims.get("jti")

            if not token_id:
                return False

            return await self.cache.exists(f"revoked_token:{token_id}")

        except jwt.InvalidTokenError:
            return True

    def _verify_password(self, password: str, password_hash: str) -> bool:
        """Verify password against hash."""
        # Simplified - use bcrypt or argon2 in production
        return hashlib.sha256(password.encode()).hexdigest() == password_hash
