package br.com.beca.financeira.users.domain;

public class User {

    private final Long id;
    private String name;
    private String email;
    private final String passwordHash;

    public User(String name, String email) {
        this(null, name, email);
    }

    public User(Long id, String name, String email) {
        this(id, name,email, null);
    }

    public User(Long id, String name, String email, String passwordHash) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public void update(String name, String email) {
        this.name = name;
        this.email = email;
    }
}