package com.zamaz.mcp.common.domain.organization;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing the unique identifier for an organization.
 */
public class OrganizationId {
    private final String value;

    /**
     * Creates a new OrganizationId with a random UUID.
     */
    public OrganizationId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Creates a new OrganizationId with the specified value.
     *
     * @param value The string value of the ID
     */
    public OrganizationId(String value) {
        this.value = Objects.requireNonNull(value, "OrganizationId value cannot be null");
    }

    /**
     * Returns the string value of this ID.
     *
     * @return The string value
     */
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OrganizationId that = (OrganizationId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}