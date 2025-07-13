package com.example;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Participant {
    @Id
    private String id;
    private String name;
    private String role;

    public Participant() {}

    public Participant(String id, String name, String role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
