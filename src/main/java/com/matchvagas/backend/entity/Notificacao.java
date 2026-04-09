package com.matchvagas.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notificacao")
public class Notificacao {
@Id
@GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
private Long  id; 

@Column( name = "titulo", nullable = false )
private String titulo;

//@Column(mensagem = "mensagem" , nullable = false )


}

