package com.zamaz.mcp.organization.application.port.outbound;

import com.zamaz.mcp.common.application.port.outbound.Repository;
import com.zamaz.mcp.common.domain.model.valueobject.Email;
import com.zamaz.mcp.organization.domain.model.User;
import com.zamaz.mcp.organization.domain.model.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for User entity.
 * This is an outbound port that defines user persistence operations.
 */
public interface UserRepository extends Repository<User, UserId> {
    
    /**
     * Finds a user by email address.
     * 
     * @param email the email address
     * @return the user if found
     */
    Optional<User> findByEmail(Email email);
    
    /**
     * Checks if a user with the given email exists.
     * 
     * @param email the email address
     * @return true if exists
     */
    boolean existsByEmail(Email email);
    
    /**
     * Finds users by their IDs.
     * 
     * @param userIds list of user IDs
     * @return list of found users
     */
    List<User> findByIds(List<UserId> userIds);
    
    /**
     * Finds all active users.
     * 
     * @return list of active users
     */
    List<User> findAllActive();
    
    /**
     * Searches users by name pattern.
     * 
     * @param namePattern the pattern to search
     * @return list of matching users
     */
    List<User> searchByName(String namePattern);
}