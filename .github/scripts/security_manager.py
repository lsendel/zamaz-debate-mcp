#!/usr/bin/env python3
"""
Security manager for Kiro GitHub integration.
This module handles authentication, authorization, audit logging, and data privacy.
"""

import os
import re
import json
import time
import uuid
import hmac
import hashlib
import logging
import sqlite3
from datetime import datetime, timedelta
from typing import Dict, List, Any, Optional, Tuple, Union
from cryptography.fernet import Fernet
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
import base64

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler('kiro_security.log')
    ]
)
logger = logging.getLogger('kiro_security')

# Constants
AUDIT_DB_PATH = os.path.join(os.path.dirname(__file__), '..', '..', '.kiro', 'data', 'audit.db')
ENCRYPTION_KEY_PATH = os.path.join(os.path.dirname(__file__), '..', '..', '.kiro', 'security', 'encryption.key')

class SecurityManager:
    """Manages security and compliance features."""
    
    def __init__(self):
        """Initialize the security manager."""
        self.audit_db_path = AUDIT_DB_PATH
        self.encryption_key_path = ENCRYPTION_KEY_PATH
        
        # Ensure directories exist
        os.makedirs(os.path.dirname(self.audit_db_path), exist_ok=True)
        os.makedirs(os.path.dirname(self.encryption_key_path), exist_ok=True)
        
        # Initialize encryption
        self.cipher = self._init_encryption()
        
        # Initialize audit database
        self._init_audit_db()
    
    def _init_encryption(self) -> Fernet:
        """Initialize encryption system."""
        try:
            # Try to load existing key
            if os.path.exists(self.encryption_key_path):
                with open(self.encryption_key_path, 'rb') as f:
                    key = f.read()
            else:
                # Generate new key
                key = Fernet.generate_key()
                with open(self.encryption_key_path, 'wb') as f:
                    f.write(key)
                # Set secure permissions
                os.chmod(self.encryption_key_path, 0o600)
                logger.info("Generated new encryption key")
            
            return Fernet(key)
        
        except Exception as e:
            logger.error(f"Error initializing encryption: {str(e)}")
            # Fallback to a default key (not recommended for production)
            return Fernet(Fernet.generate_key())
    
    def _init_audit_db(self):
        """Initialize the audit database."""
        try:
            conn = sqlite3.connect(self.audit_db_path)
            cursor = conn.cursor()
            
            # Create audit_logs table
            cursor.execute('''
            CREATE TABLE IF NOT EXISTS audit_logs (
                id TEXT PRIMARY KEY,
                timestamp TIMESTAMP NOT NULL,
                event_type TEXT NOT NULL,
                user_id TEXT,
                resource_type TEXT,
                resource_id TEXT,
                action TEXT NOT NULL,
                details TEXT,
                ip_address TEXT,
                user_agent TEXT,
                success BOOLEAN NOT NULL,
                error_message TEXT
            )
            ''')
            
            # Create access_tokens table
            cursor.execute('''
            CREATE TABLE IF NOT EXISTS access_tokens (
                id TEXT PRIMARY KEY,
                token_hash TEXT NOT NULL,
                user_id TEXT NOT NULL,
                scope TEXT NOT NULL,
                expires_at TIMESTAMP NOT NULL,
                created_at TIMESTAMP NOT NULL,
                last_used TIMESTAMP,
                revoked BOOLEAN DEFAULT FALSE
            )
            ''')
            
            # Create permissions table
            cursor.execute('''
            CREATE TABLE IF NOT EXISTS permissions (
                id TEXT PRIMARY KEY,
                user_id TEXT NOT NULL,
                resource_type TEXT NOT NULL,
                resource_id TEXT,
                permission TEXT NOT NULL,
                granted_by TEXT NOT NULL,
                granted_at TIMESTAMP NOT NULL,
                expires_at TIMESTAMP
            )
            ''')
            
            # Create data_processing_logs table
            cursor.execute('''
            CREATE TABLE IF NOT EXISTS data_processing_logs (
                id TEXT PRIMARY KEY,
                timestamp TIMESTAMP NOT NULL,
                data_type TEXT NOT NULL,
                operation TEXT NOT NULL,
                user_id TEXT,
                repo_owner TEXT,
                repo_name TEXT,
                data_size INTEGER,
                retention_period INTEGER,
                purpose TEXT,
                legal_basis TEXT
            )
            ''')
            
            conn.commit()
            conn.close()
            
            logger.info("Audit database initialized")
        
        except Exception as e:
            logger.error(f"Error initializing audit database: {str(e)}")
    
    def log_audit_event(self, event_type: str, action: str, user_id: str = None, 
                       resource_type: str = None, resource_id: str = None,
                       details: Dict[str, Any] = None, ip_address: str = None,
                       user_agent: str = None, success: bool = True, 
                       error_message: str = None) -> bool:
        """Log an audit event."""
        try:
            audit_id = str(uuid.uuid4())
            timestamp = datetime.now().isoformat()
            
            # Encrypt sensitive details
            encrypted_details = None
            if details:
                details_json = json.dumps(details)
                encrypted_details = self.cipher.encrypt(details_json.encode()).decode()
            
            conn = sqlite3.connect(self.audit_db_path)
            cursor = conn.cursor()
            
            cursor.execute('''
            INSERT INTO audit_logs 
            (id, timestamp, event_type, user_id, resource_type, resource_id, action, 
             details, ip_address, user_agent, success, error_message)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (audit_id, timestamp, event_type, user_id, resource_type, resource_id,
                  action, encrypted_details, ip_address, user_agent, success, error_message))
            
            conn.commit()
            conn.close()
            
            logger.info(f"Logged audit event: {event_type}/{action}")
            return True
        
        except Exception as e:
            logger.error(f"Error logging audit event: {str(e)}")
            return False
    
    def create_access_token(self, user_id: str, scope: str, expires_in_hours: int = 24) -> Optional[str]:
        """Create a new access token."""
        try:
            token_id = str(uuid.uuid4())
            token = f"kiro_{token_id}_{int(time.time())}"
            token_hash = hashlib.sha256(token.encode()).hexdigest()
            
            expires_at = datetime.now() + timedelta(hours=expires_in_hours)
            created_at = datetime.now()
            
            conn = sqlite3.connect(self.audit_db_path)
            cursor = conn.cursor()
            
            cursor.execute('''
            INSERT INTO access_tokens (id, token_hash, user_id, scope, expires_at, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ''', (token_id, token_hash, user_id, scope, expires_at.isoformat(), created_at.isoformat()))
            
            conn.commit()
            conn.close()
            
            # Log token creation
            self.log_audit_event('authentication', 'token_created', user_id, 
                               details={'scope': scope, 'expires_at': expires_at.isoformat()})
            
            return token
        
        except Exception as e:
            logger.error(f"Error creating access token: {str(e)}")
            return None
    
    def validate_access_token(self, token: str) -> Optional[Dict[str, Any]]:
        """Validate an access token."""
        try:
            token_hash = hashlib.sha256(token.encode()).hexdigest()
            
            conn = sqlite3.connect(self.audit_db_path)
            cursor = conn.cursor()
            
            cursor.execute('''
            SELECT id, user_id, scope, expires_at, revoked
            FROM access_tokens
            WHERE token_hash = ?
            ''', (token_hash,))
            
            result = cursor.fetchone()
            
            if not result:
                conn.close()
                return None
            
            token_id, user_id, scope, expires_at_str, revoked = result
            
            # Check if token is revoked
            if revoked:
                conn.close()
                return None
            
            # Check if token is expired
            expires_at = datetime.fromisoformat(expires_at_str)
            if datetime.now() > expires_at:
                conn.close()
                return None
            
            # Update last used timestamp
            cursor.execute('''
            UPDATE access_tokens SET last_used = ? WHERE id = ?
            ''', (datetime.now().isoformat(), token_id))
            
            conn.commit()
            conn.close()
            
            return {
                'token_id': token_id,
                'user_id': user_id,
                'scope': scope,
                'expires_at': expires_at_str
            }
        
        except Exception as e:
            logger.error(f"Error validating access token: {str(e)}")
            return None
    
    def revoke_access_token(self, token: str, user_id: str) -> bool:
        """Revoke an access token."""
        try:
            token_hash = hashlib.sha256(token.encode()).hexdigest()
            
            conn = sqlite3.connect(self.audit_db_path)
            cursor = conn.cursor()
            
            cursor.execute('''
            UPDATE access_tokens SET revoked = TRUE 
            WHERE token_hash = ? AND user_id = ?
            ''', (token_hash, user_id))
            
            rows_affected = cursor.rowcount
            conn.commit()
            conn.close()
            
            if rows_affected > 0:
                self.log_audit_event('authentication', 'token_revoked', user_id)
                return True
            
            return False
        
        except Exception as e:
            logger.error(f"Error revoking access token: {str(e)}")
            return False
    
    def check_permission(self, user_id: str, resource_type: str, permission: str, 
                        resource_id: str = None) -> bool:
        """Check if a user has a specific permission."""
        try:
            conn = sqlite3.connect(self.audit_db_path)
            cursor = conn.cursor()
            
            # Check for specific resource permission
            if resource_id:
                cursor.execute('''
                SELECT COUNT(*) FROM permissions
                WHERE user_id = ? AND resource_type = ? AND resource_id = ? 
                AND permission = ? AND (expires_at IS NULL OR expires_at > ?)
                ''', (user_id, resource_type, resource_id, permission, datetime.now().isoformat()))
            else:
                # Check for general permission
                cursor.execute('''
                SELECT COUNT(*) FROM permissions
                WHERE user_id = ? AND resource_type = ? AND permission = ? 
                AND resource_id IS NULL AND (expires_at IS NULL OR expires_at > ?)
                ''', (user_id, resource_type, permission, datetime.now().isoformat()))
            
            result = cursor.fetchone()
            conn.close()
            
            has_permission = result[0] > 0
            
            # Log permission check
            self.log_audit_event('authorization', 'permission_check', user_id,
                               resource_type, resource_id,
                               details={'permission': permission, 'granted': has_permission})
            
            return has_permission
        
        except Exception as e:
            logger.error(f"Error checking permission: {str(e)}")
            return False
    
    def grant_permission(self, user_id: str, resource_type: str, permission: str,
                        granted_by: str, resource_id: str = None, 
                        expires_in_days: int = None) -> bool:
        """Grant a permission to a user."""
        try:
            permission_id = str(uuid.uuid4())
            granted_at = datetime.now()
            expires_at = None
            
            if expires_in_days:
                expires_at = granted_at + timedelta(days=expires_in_days)
            
            conn = sqlite3.connect(self.audit_db_path)
            cursor = conn.cursor()
            
            cursor.execute('''
            INSERT INTO permissions (id, user_id, resource_type, resource_id, permission, 
                                   granted_by, granted_at, expires_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ''', (permission_id, user_id, resource_type, resource_id, permission,
                  granted_by, granted_at.isoformat(), 
                  expires_at.isoformat() if expires_at else None))
            
            conn.commit()
            conn.close()
            
            # Log permission grant
            self.log_audit_event('authorization', 'permission_granted', granted_by,
                               resource_type, resource_id,
                               details={'target_user': user_id, 'permission': permission})
            
            return True
        
        except Exception as e:
            logger.error(f"Error granting permission: {str(e)}")
            return False
    
    def log_data_processing(self, data_type: str, operation: str, user_id: str = None,
                           repo_owner: str = None, repo_name: str = None,
                           data_size: int = None, retention_period: int = None,
                           purpose: str = None, legal_basis: str = None) -> bool:
        """Log data processing activity for GDPR compliance."""
        try:
            log_id = str(uuid.uuid4())
            timestamp = datetime.now().isoformat()
            
            conn = sqlite3.connect(self.audit_db_path)
            cursor = conn.cursor()
            
            cursor.execute('''
            INSERT INTO data_processing_logs 
            (id, timestamp, data_type, operation, user_id, repo_owner, repo_name,
             data_size, retention_period, purpose, legal_basis)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ''', (log_id, timestamp, data_type, operation, user_id, repo_owner, repo_name,
                  data_size, retention_period, purpose, legal_basis))
            
            conn.commit()
            conn.close()
            
            logger.info(f"Logged data processing: {data_type}/{operation}")
            return True
        
        except Exception as e:
            logger.error(f"Error logging data processing: {str(e)}")
            return False
    
    def encrypt_sensitive_data(self, data: str) -> str:
        """Encrypt sensitive data."""
        try:
            encrypted_data = self.cipher.encrypt(data.encode())
            return base64.b64encode(encrypted_data).decode()
        except Exception as e:
            logger.error(f"Error encrypting data: {str(e)}")
            return data
    
    def decrypt_sensitive_data(self, encrypted_data: str) -> str:
        """Decrypt sensitive data."""
        try:
            decoded_data = base64.b64decode(encrypted_data.encode())
            decrypted_data = self.cipher.decrypt(decoded_data)
            return decrypted_data.decode()
        except Exception as e:
            logger.error(f"Error decrypting data: {str(e)}")
            return encrypted_data
    
    def validate_webhook_signature(self, payload: bytes, signature: str, secret: str) -> bool:
        """Validate GitHub webhook signature."""
        try:
            expected_signature = "sha1=" + hmac.new(
                secret.encode('utf-8'),
                payload,
                hashlib.sha1
            ).hexdigest()
            
            is_valid = hmac.compare_digest(expected_signature, signature)
            
            # Log validation attempt
            self.log_audit_event('webhook', 'signature_validation',
                               details={'valid': is_valid}, success=is_valid)
            
            return is_valid
        
        except Exception as e:
            logger.error(f"Error validating webhook signature: {str(e)}")
            return False
    
    def get_audit_logs(self, event_type: str = None, user_id: str = None,
                      start_date: datetime = None, end_date: datetime = None,
                      limit: int = 100) -> List[Dict[str, Any]]:
        """Get audit logs with optional filtering."""
        try:
            conn = sqlite3.connect(self.audit_db_path)
            cursor = conn.cursor()
            
            query = '''
            SELECT id, timestamp, event_type, user_id, resource_type, resource_id,
                   action, details, ip_address, user_agent, success, error_message
            FROM audit_logs
            WHERE 1=1
            '''
            params = []
            
            if event_type:
                query += ' AND event_type = ?'
                params.append(event_type)
            
            if user_id:
                query += ' AND user_id = ?'
                params.append(user_id)
            
            if start_date:
                query += ' AND timestamp >= ?'
                params.append(start_date.isoformat())
            
            if end_date:
                query += ' AND timestamp <= ?'
                params.append(end_date.isoformat())
            
            query += ' ORDER BY timestamp DESC LIMIT ?'
            params.append(limit)
            
            cursor.execute(query, params)
            results = cursor.fetchall()
            conn.close()
            
            # Decrypt details for authorized access
            logs = []
            for row in results:
                log_entry = {
                    'id': row[0],
                    'timestamp': row[1],
                    'event_type': row[2],
                    'user_id': row[3],
                    'resource_type': row[4],
                    'resource_id': row[5],
                    'action': row[6],
                    'details': None,
                    'ip_address': row[8],
                    'user_agent': row[9],
                    'success': row[10],
                    'error_message': row[11]
                }
                
                # Decrypt details if present
                if row[7]:
                    try:
                        decrypted_details = self.cipher.decrypt(row[7].encode()).decode()
                        log_entry['details'] = json.loads(decrypted_details)
                    except:
                        log_entry['details'] = {'error': 'Failed to decrypt details'}
                
                logs.append(log_entry)
            
            return logs
        
        except Exception as e:
            logger.error(f"Error getting audit logs: {str(e)}")
            return []
    
    def cleanup_expired_tokens(self) -> int:
        """Clean up expired access tokens."""
        try:
            conn = sqlite3.connect(self.audit_db_path)
            cursor = conn.cursor()
            
            cursor.execute('''
            DELETE FROM access_tokens
            WHERE expires_at < ? OR revoked = TRUE
            ''', (datetime.now().isoformat(),))
            
            deleted_count = cursor.rowcount
            conn.commit()
            conn.close()
            
            if deleted_count > 0:
                self.log_audit_event('maintenance', 'tokens_cleaned',
                                   details={'deleted_count': deleted_count})
            
            return deleted_count
        
        except Exception as e:
            logger.error(f"Error cleaning up expired tokens: {str(e)}")
            return 0
    
    def get_data_processing_report(self, start_date: datetime = None, 
                                 end_date: datetime = None) -> Dict[str, Any]:
        """Generate a data processing report for compliance."""
        try:
            conn = sqlite3.connect(self.audit_db_path)
            cursor = conn.cursor()
            
            query = '''
            SELECT data_type, operation, COUNT(*) as count, 
                   SUM(data_size) as total_size, purpose, legal_basis
            FROM data_processing_logs
            WHERE 1=1
            '''
            params = []
            
            if start_date:
                query += ' AND timestamp >= ?'
                params.append(start_date.isoformat())
            
            if end_date:
                query += ' AND timestamp <= ?'
                params.append(end_date.isoformat())
            
            query += ' GROUP BY data_type, operation, purpose, legal_basis'
            
            cursor.execute(query, params)
            results = cursor.fetchall()
            conn.close()
            
            report = {
                'period': {
                    'start': start_date.isoformat() if start_date else None,
                    'end': end_date.isoformat() if end_date else None
                },
                'processing_activities': []
            }
            
            for row in results:
                report['processing_activities'].append({
                    'data_type': row[0],
                    'operation': row[1],
                    'count': row[2],
                    'total_size': row[3],
                    'purpose': row[4],
                    'legal_basis': row[5]
                })
            
            return report
        
        except Exception as e:
            logger.error(f"Error generating data processing report: {str(e)}")
            return {}

# Global security manager instance
security_manager = SecurityManager()

def log_audit_event(event_type: str, action: str, user_id: str = None, **kwargs) -> bool:
    """Log an audit event."""
    return security_manager.log_audit_event(event_type, action, user_id, **kwargs)

def create_access_token(user_id: str, scope: str, expires_in_hours: int = 24) -> Optional[str]:
    """Create a new access token."""
    return security_manager.create_access_token(user_id, scope, expires_in_hours)

def validate_access_token(token: str) -> Optional[Dict[str, Any]]:
    """Validate an access token."""
    return security_manager.validate_access_token(token)

def check_permission(user_id: str, resource_type: str, permission: str, resource_id: str = None) -> bool:
    """Check if a user has a specific permission."""
    return security_manager.check_permission(user_id, resource_type, permission, resource_id)

def encrypt_sensitive_data(data: str) -> str:
    """Encrypt sensitive data."""
    return security_manager.encrypt_sensitive_data(data)

def decrypt_sensitive_data(encrypted_data: str) -> str:
    """Decrypt sensitive data."""
    return security_manager.decrypt_sensitive_data(encrypted_data)

def validate_webhook_signature(payload: bytes, signature: str, secret: str) -> bool:
    """Validate GitHub webhook signature."""
    return security_manager.validate_webhook_signature(payload, signature, secret)

def log_data_processing(data_type: str, operation: str, **kwargs) -> bool:
    """Log data processing activity for GDPR compliance."""
    return security_manager.log_data_processing(data_type, operation, **kwargs)

if __name__ == "__main__":
    # Example usage
    sm = SecurityManager()
    
    # Create access token
    token = sm.create_access_token('user123', 'repo:read,repo:write')
    print(f"Created token: {token}")
    
    # Validate token
    token_info = sm.validate_access_token(token)
    print(f"Token info: {token_info}")
    
    # Grant permission
    sm.grant_permission('user123', 'repository', 'read', 'admin', 'owner/repo')
    
    # Check permission
    has_permission = sm.check_permission('user123', 'repository', 'read', 'owner/repo')
    print(f"Has permission: {has_permission}")
    
    # Log data processing
    sm.log_data_processing('code', 'analysis', user_id='user123', 
                          purpose='code review', legal_basis='legitimate interest')
    
    # Get audit logs
    logs = sm.get_audit_logs(limit=5)
    print(f"Recent audit logs: {len(logs)} entries")