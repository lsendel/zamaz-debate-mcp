package com.zamaz.mcp.organization.domain.model;

import com.zamaz.mcp.common.domain.model.DomainEntity;
import com.zamaz.mcp.common.domain.model.valueobject.Email;
import com.zamaz.mcp.common.domain.model.valueobject.Name;
import com.zamaz.mcp.common.domain.exception.DomainRuleViolationException;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * User entity in the organization context.
 * Represents a user that can be a member of organizations.
 * This is a simplified view focusing on organization membership aspects.
 */
public class User extends DomainEntity<UserId> {
    
    private Email email;
    private Name firstName;
    private Name lastName;
    private UserStatus status;
    private boolean emailVerified;
    
    /**
     * Creates a new user.
     * 
     * @param id the user ID
     * @param email the user's email
     * @param firstName the user's first name
     * @param lastName the user's last name
     */
    public User(UserId id, Email email, Name firstName, Name lastName) {
        super(id);
        this.email = Objects.requireNonNull(email, "Email is required");
        this.firstName = Objects.requireNonNull(firstName, "First name is required");
        this.lastName = Objects.requireNonNull(lastName, "Last name is required");
        this.status = UserStatus.ACTIVE;
        this.emailVerified = false;
    }
    
    /**
     * Reconstructs a user from persistence.
     */
    public User(UserId id, Email email, Name firstName, Name lastName,
                UserStatus status, boolean emailVerified,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, createdAt, updatedAt);
        this.email = Objects.requireNonNull(email);
        this.firstName = Objects.requireNonNull(firstName);
        this.lastName = Objects.requireNonNull(lastName);
        this.status = Objects.requireNonNull(status);
        this.emailVerified = emailVerified;
    }
    
    /**
     * Updates the user's profile information.
     */
    public void updateProfile(Name firstName, Name lastName) {
        boolean changed = false;
        
        if (!this.firstName.equals(firstName)) {
            this.firstName = Objects.requireNonNull(firstName);
            changed = true;
        }
        
        if (!this.lastName.equals(lastName)) {
            this.lastName = Objects.requireNonNull(lastName);
            changed = true;
        }
        
        if (changed) {
            markUpdated();
        }
    }
    
    /**
     * Changes the user's email address.
     * This will require re-verification.
     */
    public void changeEmail(Email newEmail) {
        if (!this.email.equals(newEmail)) {
            this.email = Objects.requireNonNull(newEmail);
            this.emailVerified = false;
            markUpdated();
        }
    }
    
    /**
     * Marks the user's email as verified.
     */
    public void verifyEmail() {
        if (!emailVerified) {
            this.emailVerified = true;
            markUpdated();
        }
    }
    
    /**
     * Suspends the user.
     */
    public void suspend() {
        if (status == UserStatus.SUSPENDED) {
            return; // Already suspended
        }
        
        this.status = UserStatus.SUSPENDED;
        markUpdated();
    }
    
    /**
     * Reactivates a suspended user.
     */
    public void reactivate() {
        if (status == UserStatus.ACTIVE) {
            return; // Already active
        }
        
        if (status == UserStatus.BANNED) {
            throw new DomainRuleViolationException(
                "user.banned.cannotReactivate",
                "Cannot reactivate a banned user"
            );
        }
        
        this.status = UserStatus.ACTIVE;
        markUpdated();
    }
    
    /**
     * Bans the user permanently.
     */
    public void ban() {
        if (status == UserStatus.BANNED) {
            return; // Already banned
        }
        
        this.status = UserStatus.BANNED;
        markUpdated();
    }
    
    /**
     * Checks if the user can join organizations.
     */
    public boolean canJoinOrganizations() {
        return status == UserStatus.ACTIVE && emailVerified;
    }
    
    /**
     * Gets the user's full name.
     */
    public String getFullName() {
        return firstName.value() + " " + lastName.value();
    }
    
    /**
     * Gets the user's display name (first name + last initial).
     */
    public String getDisplayName() {
        String lastInitial = lastName.value().substring(0, 1).toUpperCase();
        return firstName.value() + " " + lastInitial + ".";
    }
    
    // Getters
    
    public Email getEmail() {
        return email;
    }
    
    public Name getFirstName() {
        return firstName;
    }
    
    public Name getLastName() {
        return lastName;
    }
    
    public UserStatus getStatus() {
        return status;
    }
    
    public boolean isEmailVerified() {
        return emailVerified;
    }
    
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
    
    public boolean isSuspended() {
        return status == UserStatus.SUSPENDED;
    }
    
    public boolean isBanned() {
        return status == UserStatus.BANNED;
    }
}