"""
Organization Management MCP Models
"""

from typing import List, Dict, Any, Optional, Literal
from pydantic import BaseModel, Field, HttpUrl
from datetime import datetime
from enum import Enum
from sqlalchemy import Column, String, DateTime, Text, Integer, Boolean, ForeignKey, JSON
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship
import uuid

Base = declarative_base()


class ProjectStatus(str, Enum):
    ACTIVE = "active"
    INACTIVE = "inactive"
    ARCHIVED = "archived"
    PLANNED = "planned"
    ON_HOLD = "on_hold"


class ProjectType(str, Enum):
    FRONTEND = "frontend"
    BACKEND = "backend"
    FULLSTACK = "fullstack"
    MOBILE = "mobile"
    DESKTOP = "desktop"
    AI_ML = "ai_ml"
    DATA = "data"
    INFRASTRUCTURE = "infrastructure"
    RESEARCH = "research"
    OTHER = "other"


# SQLAlchemy Models
class OrganizationDB(Base):
    __tablename__ = "organizations"
    
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    name = Column(String(255), nullable=False, unique=True)
    slug = Column(String(100), nullable=False, unique=True)
    description = Column(Text)
    website = Column(String(500))
    github_org = Column(String(255))  # GitHub organization name
    contact_email = Column(String(255))
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    is_active = Column(Boolean, default=True)
    metadata = Column(JSON, default=dict)
    
    # Relationships
    projects = relationship("ProjectDB", back_populates="organization", cascade="all, delete-orphan")


class ProjectDB(Base):
    __tablename__ = "projects"
    
    id = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    organization_id = Column(String, ForeignKey("organizations.id"), nullable=False)
    name = Column(String(255), nullable=False)
    slug = Column(String(100), nullable=False)
    description = Column(Text)
    project_type = Column(String(50), default=ProjectType.OTHER.value)
    status = Column(String(50), default=ProjectStatus.ACTIVE.value)
    
    # GitHub integration
    github_repo = Column(String(500))  # Full GitHub repo URL
    github_owner = Column(String(255))  # GitHub username/org
    github_repo_name = Column(String(255))  # Repository name
    default_branch = Column(String(100), default="main")
    
    # Project metadata
    tech_stack = Column(JSON, default=list)  # List of technologies
    tags = Column(JSON, default=list)  # Project tags
    priority = Column(Integer, default=5)  # 1-10 scale
    
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    archived_at = Column(DateTime)
    metadata = Column(JSON, default=dict)
    
    # Relationships
    organization = relationship("OrganizationDB", back_populates="projects")


# Pydantic Models for API
class Organization(BaseModel):
    id: str
    name: str
    slug: str
    description: Optional[str] = None
    website: Optional[HttpUrl] = None
    github_org: Optional[str] = None
    contact_email: Optional[str] = None
    created_at: datetime
    updated_at: datetime
    is_active: bool = True
    metadata: Dict[str, Any] = Field(default_factory=dict)
    project_count: Optional[int] = None
    
    class Config:
        from_attributes = True


class Project(BaseModel):
    id: str
    organization_id: str
    name: str
    slug: str
    description: Optional[str] = None
    project_type: ProjectType = ProjectType.OTHER
    status: ProjectStatus = ProjectStatus.ACTIVE
    
    # GitHub integration
    github_repo: Optional[HttpUrl] = None
    github_owner: Optional[str] = None
    github_repo_name: Optional[str] = None
    default_branch: str = "main"
    
    # Project metadata
    tech_stack: List[str] = Field(default_factory=list)
    tags: List[str] = Field(default_factory=list)
    priority: int = Field(default=5, ge=1, le=10)
    
    created_at: datetime
    updated_at: datetime
    archived_at: Optional[datetime] = None
    metadata: Dict[str, Any] = Field(default_factory=dict)
    
    class Config:
        from_attributes = True


class CreateOrganizationRequest(BaseModel):
    name: str = Field(..., min_length=1, max_length=255)
    description: Optional[str] = None
    website: Optional[HttpUrl] = None
    github_org: Optional[str] = None
    contact_email: Optional[str] = None
    metadata: Dict[str, Any] = Field(default_factory=dict)


class UpdateOrganizationRequest(BaseModel):
    name: Optional[str] = Field(None, min_length=1, max_length=255)
    description: Optional[str] = None
    website: Optional[HttpUrl] = None
    github_org: Optional[str] = None
    contact_email: Optional[str] = None
    is_active: Optional[bool] = None
    metadata: Optional[Dict[str, Any]] = None


class CreateProjectRequest(BaseModel):
    organization_id: str
    name: str = Field(..., min_length=1, max_length=255)
    description: Optional[str] = None
    project_type: ProjectType = ProjectType.OTHER
    status: ProjectStatus = ProjectStatus.ACTIVE
    
    # GitHub integration
    github_repo: Optional[HttpUrl] = None
    github_owner: Optional[str] = None
    github_repo_name: Optional[str] = None
    default_branch: str = "main"
    
    # Project metadata
    tech_stack: List[str] = Field(default_factory=list)
    tags: List[str] = Field(default_factory=list)
    priority: int = Field(default=5, ge=1, le=10)
    metadata: Dict[str, Any] = Field(default_factory=dict)


class UpdateProjectRequest(BaseModel):
    name: Optional[str] = Field(None, min_length=1, max_length=255)
    description: Optional[str] = None
    project_type: Optional[ProjectType] = None
    status: Optional[ProjectStatus] = None
    
    # GitHub integration
    github_repo: Optional[HttpUrl] = None
    github_owner: Optional[str] = None
    github_repo_name: Optional[str] = None
    default_branch: Optional[str] = None
    
    # Project metadata
    tech_stack: Optional[List[str]] = None
    tags: Optional[List[str]] = None
    priority: Optional[int] = Field(None, ge=1, le=10)
    metadata: Optional[Dict[str, Any]] = None


class GitHubInfo(BaseModel):
    url: HttpUrl
    owner: str
    repo: str
    branch: str = "main"
    is_valid: bool = True
    last_checked: Optional[datetime] = None
    error_message: Optional[str] = None


class OrganizationStats(BaseModel):
    total_projects: int
    active_projects: int
    archived_projects: int
    projects_by_type: Dict[str, int]
    projects_by_status: Dict[str, int]
    total_github_repos: int
    tech_stack_summary: List[Dict[str, Any]]


class SearchRequest(BaseModel):
    query: Optional[str] = None
    organization_id: Optional[str] = None
    project_type: Optional[ProjectType] = None
    status: Optional[ProjectStatus] = None
    tech_stack: Optional[List[str]] = None
    tags: Optional[List[str]] = None
    limit: int = Field(default=50, ge=1, le=1000)
    offset: int = Field(default=0, ge=0)


class SearchResponse(BaseModel):
    organizations: List[Organization] = Field(default_factory=list)
    projects: List[Project] = Field(default_factory=list)
    total_organizations: int = 0
    total_projects: int = 0
    query: Optional[str] = None
    filters: Dict[str, Any] = Field(default_factory=dict)


class ErrorResponse(BaseModel):
    error: str
    error_type: str
    details: Dict[str, Any] = Field(default_factory=dict)
    timestamp: datetime = Field(default_factory=datetime.utcnow)