import os
from typing import Optional
import structlog
from jose import jwt, JWTError

logger = structlog.get_logger()


class AuthManager:
    """Manages authentication and authorization"""
    
    def __init__(self):
        self.jwt_secret = os.getenv("JWT_SECRET", "your-secret-key")
        self.current_org_id = None  # In production, this would come from request context
        
    def get_current_org_id(self) -> str:
        """Get the current organization ID from auth context"""
        # In production, this would extract from JWT token or request headers
        # For now, return a default
        return self.current_org_id or "default-org"
    
    def set_current_org_id(self, org_id: str):
        """Set the current organization ID (for testing)"""
        self.current_org_id = org_id
    
    def create_token(self, org_id: str, user_id: Optional[str] = None) -> str:
        """Create a JWT token"""
        payload = {
            "org_id": org_id,
            "user_id": user_id or "system"
        }
        return jwt.encode(payload, self.jwt_secret, algorithm="HS256")
    
    def verify_token(self, token: str) -> Optional[dict]:
        """Verify and decode a JWT token"""
        try:
            payload = jwt.decode(token, self.jwt_secret, algorithms=["HS256"])
            return payload
        except JWTError:
            logger.error("Invalid JWT token")
            return None