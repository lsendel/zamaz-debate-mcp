"""
Organization Management Business Logic
"""

import re
import httpx
from typing import List, Optional, Dict, Any
from datetime import datetime
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, func, and_, or_
from sqlalchemy.orm import selectinload
import structlog

from ..models import (
    OrganizationDB, ProjectDB, Organization, Project,
    CreateOrganizationRequest, UpdateOrganizationRequest,
    CreateProjectRequest, UpdateProjectRequest,
    OrganizationStats, SearchRequest, SearchResponse,
    ProjectStatus, ProjectType, GitHubInfo
)

logger = structlog.get_logger()


class OrganizationManager:
    """Manages organization operations"""
    
    def __init__(self, session: AsyncSession):
        self.session = session
    
    def _generate_slug(self, name: str) -> str:
        """Generate URL-friendly slug from name"""
        slug = re.sub(r'[^\w\s-]', '', name.lower())
        slug = re.sub(r'[\s_-]+', '-', slug)
        return slug.strip('-')
    
    async def create_organization(self, request: CreateOrganizationRequest) -> Organization:
        """Create a new organization"""
        logger.info("Creating organization", name=request.name)
        
        # Generate slug
        slug = self._generate_slug(request.name)
        
        # Check if organization with same name or slug exists
        existing = await self.session.execute(
            select(OrganizationDB).where(
                or_(OrganizationDB.name == request.name, OrganizationDB.slug == slug)
            )
        )
        if existing.scalar_one_or_none():
            raise ValueError(f"Organization with name '{request.name}' or slug '{slug}' already exists")
        
        # Create organization
        org_db = OrganizationDB(
            name=request.name,
            slug=slug,
            description=request.description,
            website=str(request.website) if request.website else None,
            github_org=request.github_org,
            contact_email=request.contact_email,
            metadata=request.metadata
        )
        
        self.session.add(org_db)
        await self.session.commit()
        await self.session.refresh(org_db)
        
        logger.info("Organization created successfully", 
                   org_id=org_db.id, name=org_db.name, slug=org_db.slug)
        
        return Organization.from_orm(org_db)
    
    async def get_organization(self, org_id: str) -> Optional[Organization]:
        """Get organization by ID"""
        result = await self.session.execute(
            select(OrganizationDB).where(OrganizationDB.id == org_id)
        )
        org_db = result.scalar_one_or_none()
        
        if org_db:
            org = Organization.from_orm(org_db)
            # Add project count
            project_count = await self.session.execute(
                select(func.count(ProjectDB.id)).where(ProjectDB.organization_id == org_id)
            )
            org.project_count = project_count.scalar()
            return org
        
        return None
    
    async def get_organization_by_slug(self, slug: str) -> Optional[Organization]:
        """Get organization by slug"""
        result = await self.session.execute(
            select(OrganizationDB).where(OrganizationDB.slug == slug)
        )
        org_db = result.scalar_one_or_none()
        
        if org_db:
            return Organization.from_orm(org_db)
        
        return None
    
    async def list_organizations(self, 
                               limit: int = 50, 
                               offset: int = 0,
                               active_only: bool = True) -> List[Organization]:
        """List organizations with pagination"""
        query = select(OrganizationDB)
        
        if active_only:
            query = query.where(OrganizationDB.is_active == True)
        
        query = query.order_by(OrganizationDB.created_at.desc()).limit(limit).offset(offset)
        
        result = await self.session.execute(query)
        orgs_db = result.scalars().all()
        
        organizations = []
        for org_db in orgs_db:
            org = Organization.from_orm(org_db)
            # Add project count
            project_count = await self.session.execute(
                select(func.count(ProjectDB.id)).where(ProjectDB.organization_id == org_db.id)
            )
            org.project_count = project_count.scalar()
            organizations.append(org)
        
        return organizations
    
    async def update_organization(self, org_id: str, request: UpdateOrganizationRequest) -> Optional[Organization]:
        """Update organization"""
        result = await self.session.execute(
            select(OrganizationDB).where(OrganizationDB.id == org_id)
        )
        org_db = result.scalar_one_or_none()
        
        if not org_db:
            return None
        
        # Update fields
        if request.name is not None:
            org_db.name = request.name
            org_db.slug = self._generate_slug(request.name)
        
        if request.description is not None:
            org_db.description = request.description
        
        if request.website is not None:
            org_db.website = str(request.website) if request.website else None
        
        if request.github_org is not None:
            org_db.github_org = request.github_org
        
        if request.contact_email is not None:
            org_db.contact_email = request.contact_email
        
        if request.is_active is not None:
            org_db.is_active = request.is_active
        
        if request.metadata is not None:
            org_db.metadata = request.metadata
        
        org_db.updated_at = datetime.utcnow()
        
        await self.session.commit()
        await self.session.refresh(org_db)
        
        logger.info("Organization updated", org_id=org_id, name=org_db.name)
        
        return Organization.from_orm(org_db)
    
    async def delete_organization(self, org_id: str) -> bool:
        """Delete organization (soft delete - mark as inactive)"""
        result = await self.session.execute(
            select(OrganizationDB).where(OrganizationDB.id == org_id)
        )
        org_db = result.scalar_one_or_none()
        
        if not org_db:
            return False
        
        org_db.is_active = False
        org_db.updated_at = datetime.utcnow()
        
        await self.session.commit()
        
        logger.info("Organization deleted (soft delete)", org_id=org_id, name=org_db.name)
        return True
    
    async def get_organization_stats(self, org_id: str) -> Optional[OrganizationStats]:
        """Get organization statistics"""
        # Check if organization exists
        org = await self.get_organization(org_id)
        if not org:
            return None
        
        # Get project counts
        total_projects = await self.session.execute(
            select(func.count(ProjectDB.id)).where(ProjectDB.organization_id == org_id)
        )
        
        active_projects = await self.session.execute(
            select(func.count(ProjectDB.id)).where(
                and_(ProjectDB.organization_id == org_id, ProjectDB.status == ProjectStatus.ACTIVE.value)
            )
        )
        
        archived_projects = await self.session.execute(
            select(func.count(ProjectDB.id)).where(
                and_(ProjectDB.organization_id == org_id, ProjectDB.status == ProjectStatus.ARCHIVED.value)
            )
        )
        
        # Get projects by type
        projects_by_type = {}
        for project_type in ProjectType:
            count = await self.session.execute(
                select(func.count(ProjectDB.id)).where(
                    and_(ProjectDB.organization_id == org_id, ProjectDB.project_type == project_type.value)
                )
            )
            projects_by_type[project_type.value] = count.scalar()
        
        # Get projects by status
        projects_by_status = {}
        for status in ProjectStatus:
            count = await self.session.execute(
                select(func.count(ProjectDB.id)).where(
                    and_(ProjectDB.organization_id == org_id, ProjectDB.status == status.value)
                )
            )
            projects_by_status[status.value] = count.scalar()
        
        # Get GitHub repos count
        github_repos = await self.session.execute(
            select(func.count(ProjectDB.id)).where(
                and_(ProjectDB.organization_id == org_id, ProjectDB.github_repo.isnot(None))
            )
        )
        
        # Get tech stack summary
        projects_with_tech = await self.session.execute(
            select(ProjectDB.tech_stack).where(
                and_(ProjectDB.organization_id == org_id, ProjectDB.tech_stack.isnot(None))
            )
        )
        
        tech_summary = {}
        for row in projects_with_tech:
            if row[0]:  # tech_stack is not None
                for tech in row[0]:
                    tech_summary[tech] = tech_summary.get(tech, 0) + 1
        
        tech_stack_summary = [
            {"technology": tech, "count": count}
            for tech, count in sorted(tech_summary.items(), key=lambda x: x[1], reverse=True)
        ]
        
        return OrganizationStats(
            total_projects=total_projects.scalar(),
            active_projects=active_projects.scalar(),
            archived_projects=archived_projects.scalar(),
            projects_by_type=projects_by_type,
            projects_by_status=projects_by_status,
            total_github_repos=github_repos.scalar(),
            tech_stack_summary=tech_stack_summary
        )


class ProjectManager:
    """Manages project operations"""
    
    def __init__(self, session: AsyncSession):
        self.session = session
    
    def _generate_slug(self, name: str) -> str:
        """Generate URL-friendly slug from name"""
        slug = re.sub(r'[^\w\s-]', '', name.lower())
        slug = re.sub(r'[\s_-]+', '-', slug)
        return slug.strip('-')
    
    def _parse_github_url(self, github_url: str) -> Dict[str, str]:
        """Parse GitHub URL to extract owner and repo"""
        # Handle different GitHub URL formats
        github_patterns = [
            r'https://github\.com/([^/]+)/([^/]+)/?',
            r'git@github\.com:([^/]+)/([^/]+)\.git',
            r'github\.com/([^/]+)/([^/]+)'
        ]
        
        for pattern in github_patterns:
            match = re.match(pattern, github_url)
            if match:
                owner, repo = match.groups()
                repo = repo.rstrip('.git')
                return {"owner": owner, "repo": repo}
        
        raise ValueError(f"Invalid GitHub URL format: {github_url}")
    
    async def validate_github_repo(self, github_url: str) -> GitHubInfo:
        """Validate GitHub repository"""
        try:
            parsed = self._parse_github_url(github_url)
            owner = parsed["owner"]
            repo = parsed["repo"]
            
            # Try to access GitHub API (if available)
            github_token = os.getenv("GITHUB_TOKEN")
            headers = {}
            if github_token:
                headers["Authorization"] = f"token {github_token}"
            
            async with httpx.AsyncClient() as client:
                response = await client.get(
                    f"https://api.github.com/repos/{owner}/{repo}",
                    headers=headers
                )
                
                if response.status_code == 200:
                    repo_data = response.json()
                    default_branch = repo_data.get("default_branch", "main")
                    
                    return GitHubInfo(
                        url=github_url,
                        owner=owner,
                        repo=repo,
                        branch=default_branch,
                        is_valid=True,
                        last_checked=datetime.utcnow()
                    )
                else:
                    return GitHubInfo(
                        url=github_url,
                        owner=owner,
                        repo=repo,
                        is_valid=False,
                        last_checked=datetime.utcnow(),
                        error_message=f"GitHub API returned {response.status_code}"
                    )
                    
        except Exception as e:
            return GitHubInfo(
                url=github_url,
                owner="",
                repo="",
                is_valid=False,
                last_checked=datetime.utcnow(),
                error_message=str(e)
            )
    
    async def create_project(self, request: CreateProjectRequest) -> Project:
        """Create a new project"""
        logger.info("Creating project", name=request.name, org_id=request.organization_id)
        
        # Verify organization exists
        org_result = await self.session.execute(
            select(OrganizationDB).where(OrganizationDB.id == request.organization_id)
        )
        if not org_result.scalar_one_or_none():
            raise ValueError(f"Organization {request.organization_id} not found")
        
        # Generate slug
        slug = self._generate_slug(request.name)
        
        # Parse GitHub URL if provided
        github_owner = request.github_owner
        github_repo_name = request.github_repo_name
        github_repo_url = str(request.github_repo) if request.github_repo else None
        
        if request.github_repo:
            try:
                parsed = self._parse_github_url(str(request.github_repo))
                github_owner = parsed["owner"]
                github_repo_name = parsed["repo"]
            except ValueError as e:
                logger.warning("Failed to parse GitHub URL", url=str(request.github_repo), error=str(e))
        
        # Create project
        project_db = ProjectDB(
            organization_id=request.organization_id,
            name=request.name,
            slug=slug,
            description=request.description,
            project_type=request.project_type.value,
            status=request.status.value,
            github_repo=github_repo_url,
            github_owner=github_owner,
            github_repo_name=github_repo_name,
            default_branch=request.default_branch,
            tech_stack=request.tech_stack,
            tags=request.tags,
            priority=request.priority,
            metadata=request.metadata
        )
        
        self.session.add(project_db)
        await self.session.commit()
        await self.session.refresh(project_db)
        
        logger.info("Project created successfully", 
                   project_id=project_db.id, name=project_db.name, slug=project_db.slug)
        
        return Project.from_orm(project_db)
    
    async def get_project(self, project_id: str) -> Optional[Project]:
        """Get project by ID"""
        result = await self.session.execute(
            select(ProjectDB).where(ProjectDB.id == project_id)
        )
        project_db = result.scalar_one_or_none()
        
        if project_db:
            return Project.from_orm(project_db)
        
        return None
    
    async def list_projects(self, 
                          organization_id: Optional[str] = None,
                          status: Optional[ProjectStatus] = None,
                          project_type: Optional[ProjectType] = None,
                          limit: int = 50, 
                          offset: int = 0) -> List[Project]:
        """List projects with filters"""
        query = select(ProjectDB)
        
        if organization_id:
            query = query.where(ProjectDB.organization_id == organization_id)
        
        if status:
            query = query.where(ProjectDB.status == status.value)
        
        if project_type:
            query = query.where(ProjectDB.project_type == project_type.value)
        
        query = query.order_by(ProjectDB.created_at.desc()).limit(limit).offset(offset)
        
        result = await self.session.execute(query)
        projects_db = result.scalars().all()
        
        return [Project.from_orm(project_db) for project_db in projects_db]
    
    async def update_project(self, project_id: str, request: UpdateProjectRequest) -> Optional[Project]:
        """Update project"""
        result = await self.session.execute(
            select(ProjectDB).where(ProjectDB.id == project_id)
        )
        project_db = result.scalar_one_or_none()
        
        if not project_db:
            return None
        
        # Update fields
        if request.name is not None:
            project_db.name = request.name
            project_db.slug = self._generate_slug(request.name)
        
        if request.description is not None:
            project_db.description = request.description
        
        if request.project_type is not None:
            project_db.project_type = request.project_type.value
        
        if request.status is not None:
            project_db.status = request.status.value
            if request.status == ProjectStatus.ARCHIVED:
                project_db.archived_at = datetime.utcnow()
        
        if request.github_repo is not None:
            project_db.github_repo = str(request.github_repo) if request.github_repo else None
            if request.github_repo:
                try:
                    parsed = self._parse_github_url(str(request.github_repo))
                    project_db.github_owner = parsed["owner"]
                    project_db.github_repo_name = parsed["repo"]
                except ValueError:
                    pass
        
        if request.github_owner is not None:
            project_db.github_owner = request.github_owner
        
        if request.github_repo_name is not None:
            project_db.github_repo_name = request.github_repo_name
        
        if request.default_branch is not None:
            project_db.default_branch = request.default_branch
        
        if request.tech_stack is not None:
            project_db.tech_stack = request.tech_stack
        
        if request.tags is not None:
            project_db.tags = request.tags
        
        if request.priority is not None:
            project_db.priority = request.priority
        
        if request.metadata is not None:
            project_db.metadata = request.metadata
        
        project_db.updated_at = datetime.utcnow()
        
        await self.session.commit()
        await self.session.refresh(project_db)
        
        logger.info("Project updated", project_id=project_id, name=project_db.name)
        
        return Project.from_orm(project_db)
    
    async def delete_project(self, project_id: str) -> bool:
        """Delete project"""
        result = await self.session.execute(
            select(ProjectDB).where(ProjectDB.id == project_id)
        )
        project_db = result.scalar_one_or_none()
        
        if not project_db:
            return False
        
        await self.session.delete(project_db)
        await self.session.commit()
        
        logger.info("Project deleted", project_id=project_id, name=project_db.name)
        return True