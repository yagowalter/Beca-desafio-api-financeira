package br.com.beca.financeira.users.domain;

public class User {

    private final Long id;
    private String name;
    private String email;

    // Criação de usuário novo, ainda sem ID do banco
    public User(String name, String email) {
        this(null, name, email);
    }

    // Reconstrução de usuário já persistido
    public User(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
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