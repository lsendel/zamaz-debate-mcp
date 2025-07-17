package com.zamaz.mcp.organization.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Value object representing organization settings.
 * Immutable configuration for organization-specific preferences.
 */
public class OrganizationSettings implements ValueObject {
    
    private final Map<String, Object> settings;
    
    // Well-known setting keys
    public static final String MAX_MEMBERS = "maxMembers";
    public static final String DEFAULT_USER_ROLE = "defaultUserRole";
    public static final String REQUIRE_EMAIL_VERIFICATION = "requireEmailVerification";
    public static final String ALLOW_PUBLIC_DEBATES = "allowPublicDebates";
    public static final String DEFAULT_DEBATE_VISIBILITY = "defaultDebateVisibility";
    
    private OrganizationSettings(Map<String, Object> settings) {
        this.settings = new HashMap<>(settings);
    }
    
    /**
     * Creates default organization settings.
     */
    public static OrganizationSettings defaultSettings() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put(MAX_MEMBERS, 100);
        defaults.put(DEFAULT_USER_ROLE, "member");
        defaults.put(REQUIRE_EMAIL_VERIFICATION, true);
        defaults.put(ALLOW_PUBLIC_DEBATES, false);
        defaults.put(DEFAULT_DEBATE_VISIBILITY, "organization");
        return new OrganizationSettings(defaults);
    }
    
    /**
     * Creates settings from a map.
     */
    public static OrganizationSettings from(Map<String, Object> settings) {
        return new OrganizationSettings(settings != null ? settings : new HashMap<>());
    }
    
    /**
     * Creates a copy of these settings with a value changed.
     */
    public OrganizationSettings with(String key, Object value) {
        Map<String, Object> newSettings = new HashMap<>(this.settings);
        if (value != null) {
            newSettings.put(key, value);
        } else {
            newSettings.remove(key);
        }
        return new OrganizationSettings(newSettings);
    }
    
    /**
     * Creates a copy of these settings with multiple values changed.
     */
    public OrganizationSettings withAll(Map<String, Object> updates) {
        Map<String, Object> newSettings = new HashMap<>(this.settings);
        newSettings.putAll(updates);
        return new OrganizationSettings(newSettings);
    }
    
    /**
     * Gets a setting value.
     */
    public Optional<Object> get(String key) {
        return Optional.ofNullable(settings.get(key));
    }
    
    /**
     * Gets a setting value as a specific type.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = settings.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }
    
    /**
     * Gets the maximum number of members allowed.
     */
    public Integer getMaxMembers() {
        return get(MAX_MEMBERS, Integer.class).orElse(null);
    }
    
    /**
     * Gets the default role for new users.
     */
    public String getDefaultUserRole() {
        return get(DEFAULT_USER_ROLE, String.class).orElse("member");
    }
    
    /**
     * Checks if email verification is required.
     */
    public boolean isEmailVerificationRequired() {
        return get(REQUIRE_EMAIL_VERIFICATION, Boolean.class).orElse(true);
    }
    
    /**
     * Checks if public debates are allowed.
     */
    public boolean arePublicDebatesAllowed() {
        return get(ALLOW_PUBLIC_DEBATES, Boolean.class).orElse(false);
    }
    
    /**
     * Gets the default debate visibility.
     */
    public String getDefaultDebateVisibility() {
        return get(DEFAULT_DEBATE_VISIBILITY, String.class).orElse("organization");
    }
    
    /**
     * Returns an immutable copy of all settings.
     */
    public Map<String, Object> toMap() {
        return new HashMap<>(settings);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganizationSettings that = (OrganizationSettings) o;
        return Objects.equals(settings, that.settings);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(settings);
    }
}