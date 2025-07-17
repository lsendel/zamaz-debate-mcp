package com.zamaz.mcp.common.test.fixtures;

import com.zamaz.mcp.common.domain.model.valueobject.UserId;
import java.util.UUID;

/**
 * Test data builder for UserId value objects.
 */
public class UserIdBuilder implements TestDataBuilder<UserId> {
    
    private UUID value;
    
    private UserIdBuilder() {
        this.value = UUID.randomUUID();
    }
    
    public static UserIdBuilder aUserId() {
        return new UserIdBuilder();
    }
    
    public UserIdBuilder withValue(UUID value) {
        this.value = value;
        return this;
    }
    
    public UserIdBuilder withValue(String value) {
        this.value = UUID.fromString(value);
        return this;
    }
    
    @Override
    public UserId build() {
        return new UserId(value);
    }
    
    /**
     * Creates a builder with a known test UUID.
     */
    public static UserIdBuilder testUserId() {
        return new UserIdBuilder()
            .withValue("00000000-0000-0000-0000-000000000001");
    }
    
    /**
     * Creates multiple unique UserIds for testing.
     * 
     * @param count the number of UserIds to create
     * @return an array of UserIds
     */
    public static UserId[] multipleUserIds(int count) {
        UserId[] userIds = new UserId[count];
        for (int i = 0; i < count; i++) {
            userIds[i] = aUserId().build();
        }
        return userIds;
    }
}