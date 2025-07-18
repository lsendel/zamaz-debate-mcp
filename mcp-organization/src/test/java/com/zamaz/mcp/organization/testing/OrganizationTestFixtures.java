package com.zamaz.mcp.organization.testing;

import com.zamaz.mcp.organization.domain.model.Organization;
import com.zamaz.mcp.organization.domain.model.OrganizationStatus;
import com.zamaz.mcp.organization.domain.model.Subscription;
import com.zamaz.mcp.organization.domain.model.User;
import com.zamaz.mcp.organization.domain.model.Role;
import com.zamaz.mcp.organization.domain.model.Permission;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * Provides pre-built organization test fixtures and hierarchies.
 * Supports complex multi-tenant scenarios with realistic data.
 */
public class OrganizationTestFixtures {

    private final Map<String, OrganizationHierarchy> hierarchies = new ConcurrentHashMap<>();
    private final Map<String, Organization> organizations = new ConcurrentHashMap<>();
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Random random = new Random(42); // Fixed seed for reproducible tests

    /**
     * Creates a simple organization with basic setup.
     */
    public static Organization simpleOrganization() {
        return Organization.builder()
            .id("simple-org")
            .name("Simple Organization")
            .displayName("Simple Org")
            .status(OrganizationStatus.ACTIVE)
            .createdAt(Instant.now())
            .build();
    }

    /**
     * Creates a complete enterprise organization with multiple departments.
     */
    public static OrganizationHierarchy enterpriseHierarchy() {
        OrganizationHierarchy hierarchy = new OrganizationHierarchy("enterprise");
        
        // Root organization
        Organization root = Organization.builder()
            .id("enterprise-root")
            .name("Enterprise Corporation")
            .displayName("Enterprise Corp")
            .status(OrganizationStatus.ACTIVE)
            .parentId(null)
            .maxUsers(1000)
            .maxSubOrganizations(50)
            .createdAt(Instant.now().minus(365, ChronoUnit.DAYS))
            .metadata(Map.of(
                "industry", "technology",
                "size", "large",
                "region", "global"
            ))
            .build();
        
        hierarchy.setRoot(root);
        
        // Department level
        List<Organization> departments = List.of(
            createDepartment("eng-dept", "Engineering", root.getId()),
            createDepartment("sales-dept", "Sales & Marketing", root.getId()),
            createDepartment("hr-dept", "Human Resources", root.getId()),
            createDepartment("finance-dept", "Finance", root.getId())
        );
        
        departments.forEach(hierarchy::addOrganization);
        
        // Team level under Engineering
        Organization engineering = departments.get(0);
        List<Organization> engineeringTeams = List.of(
            createTeam("backend-team", "Backend Team", engineering.getId()),
            createTeam("frontend-team", "Frontend Team", engineering.getId()),
            createTeam("devops-team", "DevOps Team", engineering.getId()),
            createTeam("qa-team", "QA Team", engineering.getId())
        );
        
        engineeringTeams.forEach(hierarchy::addOrganization);
        
        // Add users to organizations
        addEnterpriseUsers(hierarchy);
        
        return hierarchy;
    }

    /**
     * Creates a startup organization structure.
     */
    public static OrganizationHierarchy startupHierarchy() {
        OrganizationHierarchy hierarchy = new OrganizationHierarchy("startup");
        
        Organization startup = Organization.builder()
            .id("startup-main")
            .name("TechStart Inc")
            .displayName("TechStart")
            .status(OrganizationStatus.ACTIVE)
            .maxUsers(50)
            .maxSubOrganizations(5)
            .createdAt(Instant.now().minus(30, ChronoUnit.DAYS))
            .metadata(Map.of(
                "stage", "series-a",
                "industry", "fintech",
                "employees", 25
            ))
            .build();
        
        hierarchy.setRoot(startup);
        
        // Small teams
        List<Organization> teams = List.of(
            createTeam("dev-team", "Development", startup.getId()),
            createTeam("product-team", "Product", startup.getId()),
            createTeam("growth-team", "Growth", startup.getId())
        );
        
        teams.forEach(hierarchy::addOrganization);
        
        // Add startup users
        addStartupUsers(hierarchy);
        
        return hierarchy;
    }

    /**
     * Creates a multi-region organization structure.
     */
    public static OrganizationHierarchy multiRegionHierarchy() {
        OrganizationHierarchy hierarchy = new OrganizationHierarchy("multi-region");
        
        Organization global = Organization.builder()
            .id("global-corp")
            .name("Global Corporation")
            .displayName("GlobalCorp")
            .status(OrganizationStatus.ACTIVE)
            .maxUsers(5000)
            .maxSubOrganizations(100)
            .createdAt(Instant.now().minus(1000, ChronoUnit.DAYS))
            .build();
        
        hierarchy.setRoot(global);
        
        // Regional divisions
        List<Organization> regions = List.of(
            createRegion("north-america", "North America", global.getId()),
            createRegion("europe", "Europe", global.getId()),
            createRegion("asia-pacific", "Asia Pacific", global.getId())
        );
        
        regions.forEach(hierarchy::addOrganization);
        
        // Country offices under each region
        regions.forEach(region -> {
            List<Organization> countries = createCountryOffices(region);
            countries.forEach(hierarchy::addOrganization);
        });
        
        return hierarchy;
    }

    /**
     * Creates an organization with specific subscription tiers.
     */
    public static Organization organizationWithSubscription(String tier) {
        return Organization.builder()
            .id("org-" + tier.toLowerCase())
            .name(tier + " Organization")
            .displayName(tier + " Org")
            .status(OrganizationStatus.ACTIVE)
            .subscription(createSubscription(tier))
            .createdAt(Instant.now())
            .build();
    }

    /**
     * Creates a test user with specific roles and permissions.
     */
    public static User testUser(String userId, String orgId, Role... roles) {
        return User.builder()
            .id(userId)
            .email(userId + "@test.com")
            .firstName("Test")
            .lastName("User")
            .organizationId(orgId)
            .roles(Set.of(roles))
            .active(true)
            .createdAt(Instant.now())
            .build();
    }

    /**
     * Creates multiple test users for an organization.
     */
    public static List<User> testUsers(String orgId, int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> testUser("user-" + i, orgId, Role.USER))
            .toList();
    }

    /**
     * Builder for creating custom organization hierarchies.
     */
    public static OrganizationHierarchyBuilder builder(String name) {
        return new OrganizationHierarchyBuilder(name);
    }

    // Helper methods for creating specific types of organizations

    private static Organization createDepartment(String id, String name, String parentId) {
        return Organization.builder()
            .id(id)
            .name(name)
            .displayName(name)
            .parentId(parentId)
            .status(OrganizationStatus.ACTIVE)
            .maxUsers(200)
            .maxSubOrganizations(10)
            .createdAt(Instant.now().minus(200, ChronoUnit.DAYS))
            .metadata(Map.of("type", "department"))
            .build();
    }

    private static Organization createTeam(String id, String name, String parentId) {
        return Organization.builder()
            .id(id)
            .name(name)
            .displayName(name)
            .parentId(parentId)
            .status(OrganizationStatus.ACTIVE)
            .maxUsers(20)
            .maxSubOrganizations(0)
            .createdAt(Instant.now().minus(100, ChronoUnit.DAYS))
            .metadata(Map.of("type", "team"))
            .build();
    }

    private static Organization createRegion(String id, String name, String parentId) {
        return Organization.builder()
            .id(id)
            .name(name)
            .displayName(name)
            .parentId(parentId)
            .status(OrganizationStatus.ACTIVE)
            .maxUsers(1000)
            .maxSubOrganizations(20)
            .createdAt(Instant.now().minus(500, ChronoUnit.DAYS))
            .metadata(Map.of("type", "region"))
            .build();
    }

    private static List<Organization> createCountryOffices(Organization region) {
        String regionId = region.getId();
        
        return switch (regionId) {
            case "north-america" -> List.of(
                createCountryOffice("usa-office", "USA Office", regionId),
                createCountryOffice("canada-office", "Canada Office", regionId)
            );
            case "europe" -> List.of(
                createCountryOffice("uk-office", "UK Office", regionId),
                createCountryOffice("germany-office", "Germany Office", regionId),
                createCountryOffice("france-office", "France Office", regionId)
            );
            case "asia-pacific" -> List.of(
                createCountryOffice("japan-office", "Japan Office", regionId),
                createCountryOffice("singapore-office", "Singapore Office", regionId),
                createCountryOffice("australia-office", "Australia Office", regionId)
            );
            default -> Collections.emptyList();
        };
    }

    private static Organization createCountryOffice(String id, String name, String parentId) {
        return Organization.builder()
            .id(id)
            .name(name)
            .displayName(name)
            .parentId(parentId)
            .status(OrganizationStatus.ACTIVE)
            .maxUsers(200)
            .maxSubOrganizations(5)
            .createdAt(Instant.now().minus(300, ChronoUnit.DAYS))
            .metadata(Map.of("type", "country-office"))
            .build();
    }

    private static Subscription createSubscription(String tier) {
        return switch (tier.toLowerCase()) {
            case "free" -> Subscription.builder()
                .id("sub-free")
                .tier("FREE")
                .maxUsers(5)
                .maxStorageGB(1)
                .features(Set.of("basic_debates", "email_support"))
                .active(true)
                .build();
            
            case "pro" -> Subscription.builder()
                .id("sub-pro")
                .tier("PRO")
                .maxUsers(50)
                .maxStorageGB(100)
                .features(Set.of("advanced_debates", "analytics", "priority_support", "api_access"))
                .active(true)
                .build();
            
            case "enterprise" -> Subscription.builder()
                .id("sub-enterprise")
                .tier("ENTERPRISE")
                .maxUsers(1000)
                .maxStorageGB(1000)
                .features(Set.of("unlimited_debates", "advanced_analytics", "24x7_support", 
                              "api_access", "sso", "audit_logs", "custom_branding"))
                .active(true)
                .build();
            
            default -> throw new IllegalArgumentException("Unknown subscription tier: " + tier);
        };
    }

    private static void addEnterpriseUsers(OrganizationHierarchy hierarchy) {
        Organization root = hierarchy.getRoot();
        
        // C-level executives in root
        hierarchy.addUser(createExecutive("ceo", "CEO", root.getId(), Role.ORGANIZATION_ADMIN));
        hierarchy.addUser(createExecutive("cto", "CTO", root.getId(), Role.ORGANIZATION_ADMIN));
        hierarchy.addUser(createExecutive("cfo", "CFO", root.getId(), Role.ORGANIZATION_ADMIN));
        
        // Department heads
        hierarchy.getOrganizations().stream()
            .filter(org -> "department".equals(org.getMetadata().get("type")))
            .forEach(dept -> {
                String deptHead = dept.getId().replace("-dept", "-head");
                hierarchy.addUser(createManager(deptHead, "Department Head", dept.getId()));
            });
        
        // Team leads and members
        hierarchy.getOrganizations().stream()
            .filter(org -> "team".equals(org.getMetadata().get("type")))
            .forEach(team -> {
                String teamLead = team.getId().replace("-team", "-lead");
                hierarchy.addUser(createManager(teamLead, "Team Lead", team.getId()));
                
                // Add team members
                for (int i = 1; i <= 5; i++) {
                    String memberId = team.getId().replace("-team", "-member-" + i);
                    hierarchy.addUser(createDeveloper(memberId, "Developer", team.getId()));
                }
            });
    }

    private static void addStartupUsers(OrganizationHierarchy hierarchy) {
        Organization startup = hierarchy.getRoot();
        
        // Founders
        hierarchy.addUser(createFounder("founder-ceo", "Founder & CEO", startup.getId()));
        hierarchy.addUser(createFounder("founder-cto", "Founder & CTO", startup.getId()));
        
        // Team members
        hierarchy.getOrganizations().forEach(team -> {
            for (int i = 1; i <= 3; i++) {
                String memberId = team.getId().replace("-team", "-member-" + i);
                hierarchy.addUser(createDeveloper(memberId, "Team Member", team.getId()));
            }
        });
    }

    private static User createExecutive(String id, String title, String orgId, Role role) {
        return User.builder()
            .id(id)
            .email(id + "@enterprise.com")
            .firstName(title.split(" ")[0])
            .lastName("Executive")
            .organizationId(orgId)
            .roles(Set.of(role))
            .permissions(getAllPermissions())
            .active(true)
            .createdAt(Instant.now().minus(300, ChronoUnit.DAYS))
            .metadata(Map.of("title", title, "level", "executive"))
            .build();
    }

    private static User createManager(String id, String title, String orgId) {
        return User.builder()
            .id(id)
            .email(id + "@enterprise.com")
            .firstName("Manager")
            .lastName("User")
            .organizationId(orgId)
            .roles(Set.of(Role.MANAGER))
            .permissions(getManagerPermissions())
            .active(true)
            .createdAt(Instant.now().minus(200, ChronoUnit.DAYS))
            .metadata(Map.of("title", title, "level", "manager"))
            .build();
    }

    private static User createDeveloper(String id, String title, String orgId) {
        return User.builder()
            .id(id)
            .email(id + "@enterprise.com")
            .firstName("Dev")
            .lastName("User")
            .organizationId(orgId)
            .roles(Set.of(Role.USER))
            .permissions(getUserPermissions())
            .active(true)
            .createdAt(Instant.now().minus(100, ChronoUnit.DAYS))
            .metadata(Map.of("title", title, "level", "individual"))
            .build();
    }

    private static User createFounder(String id, String title, String orgId) {
        return User.builder()
            .id(id)
            .email(id + "@techstart.com")
            .firstName("Founder")
            .lastName("Name")
            .organizationId(orgId)
            .roles(Set.of(Role.ORGANIZATION_ADMIN))
            .permissions(getAllPermissions())
            .active(true)
            .createdAt(Instant.now().minus(30, ChronoUnit.DAYS))
            .metadata(Map.of("title", title, "level", "founder"))
            .build();
    }

    private static Set<Permission> getAllPermissions() {
        return Set.of(Permission.values());
    }

    private static Set<Permission> getManagerPermissions() {
        return Set.of(
            Permission.DEBATE_CREATE,
            Permission.DEBATE_UPDATE,
            Permission.DEBATE_DELETE,
            Permission.DEBATE_VIEW,
            Permission.TEMPLATE_MANAGE,
            Permission.USER_VIEW
        );
    }

    private static Set<Permission> getUserPermissions() {
        return Set.of(
            Permission.DEBATE_CREATE,
            Permission.DEBATE_VIEW,
            Permission.TEMPLATE_VIEW
        );
    }

    // Builder class for custom hierarchies

    public static class OrganizationHierarchyBuilder {
        private final OrganizationHierarchy hierarchy;

        public OrganizationHierarchyBuilder(String name) {
            this.hierarchy = new OrganizationHierarchy(name);
        }

        public OrganizationHierarchyBuilder withRoot(Organization root) {
            hierarchy.setRoot(root);
            return this;
        }

        public OrganizationHierarchyBuilder addOrganization(Organization org) {
            hierarchy.addOrganization(org);
            return this;
        }

        public OrganizationHierarchyBuilder addUser(User user) {
            hierarchy.addUser(user);
            return this;
        }

        public OrganizationHierarchyBuilder withRandomUsers(String orgId, int count) {
            for (int i = 0; i < count; i++) {
                User user = testUser("random-user-" + i, orgId, Role.USER);
                hierarchy.addUser(user);
            }
            return this;
        }

        public OrganizationHierarchy build() {
            return hierarchy;
        }
    }

    // Container class for organization hierarchies

    public static class OrganizationHierarchy {
        private final String name;
        private Organization root;
        private final Map<String, Organization> organizations = new HashMap<>();
        private final Map<String, User> users = new HashMap<>();
        private final Map<String, List<String>> parentChildMap = new HashMap<>();

        public OrganizationHierarchy(String name) {
            this.name = name;
        }

        public void setRoot(Organization root) {
            this.root = root;
            this.organizations.put(root.getId(), root);
        }

        public void addOrganization(Organization org) {
            organizations.put(org.getId(), org);
            
            if (org.getParentId() != null) {
                parentChildMap.computeIfAbsent(org.getParentId(), k -> new ArrayList<>())
                    .add(org.getId());
            }
        }

        public void addUser(User user) {
            users.put(user.getId(), user);
        }

        // Getters
        public String getName() { return name; }
        public Organization getRoot() { return root; }
        public Collection<Organization> getOrganizations() { return organizations.values(); }
        public Collection<User> getUsers() { return users.values(); }

        public Organization getOrganization(String id) {
            return organizations.get(id);
        }

        public User getUser(String id) {
            return users.get(id);
        }

        public List<Organization> getChildren(String parentId) {
            return parentChildMap.getOrDefault(parentId, Collections.emptyList())
                .stream()
                .map(organizations::get)
                .filter(Objects::nonNull)
                .toList();
        }

        public List<User> getUsersInOrganization(String orgId) {
            return users.values().stream()
                .filter(user -> orgId.equals(user.getOrganizationId()))
                .toList();
        }

        public List<Organization> getAllDescendants(String parentId) {
            List<Organization> descendants = new ArrayList<>();
            Queue<String> queue = new LinkedList<>();
            queue.add(parentId);
            
            while (!queue.isEmpty()) {
                String currentId = queue.poll();
                List<String> children = parentChildMap.getOrDefault(currentId, Collections.emptyList());
                
                for (String childId : children) {
                    Organization child = organizations.get(childId);
                    if (child != null) {
                        descendants.add(child);
                        queue.add(childId);
                    }
                }
            }
            
            return descendants;
        }

        public int getTotalOrganizations() {
            return organizations.size();
        }

        public int getTotalUsers() {
            return users.size();
        }

        public int getMaxDepth() {
            return calculateDepth(root.getId(), 0);
        }

        private int calculateDepth(String orgId, int currentDepth) {
            List<String> children = parentChildMap.getOrDefault(orgId, Collections.emptyList());
            if (children.isEmpty()) {
                return currentDepth;
            }
            
            return children.stream()
                .mapToInt(childId -> calculateDepth(childId, currentDepth + 1))
                .max()
                .orElse(currentDepth);
        }

        public Map<String, Object> getStatistics() {
            Map<String, Object> stats = new HashMap<>();
            stats.put("name", name);
            stats.put("totalOrganizations", getTotalOrganizations());
            stats.put("totalUsers", getTotalUsers());
            stats.put("maxDepth", getMaxDepth());
            stats.put("rootId", root != null ? root.getId() : null);
            
            Map<String, Integer> usersByOrg = new HashMap<>();
            users.values().forEach(user -> 
                usersByOrg.merge(user.getOrganizationId(), 1, Integer::sum));
            stats.put("usersByOrganization", usersByOrg);
            
            return stats;
        }
    }
}