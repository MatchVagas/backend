package com.matchvagas.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * Vaga salva (favoritada) por um candidato para ver depois (Fase 2).
 *
 * <p>As FKs usam {@code ON DELETE CASCADE} (via {@link OnDelete}) para que remover a vaga
 * ou o candidato limpe os favoritos automaticamente no banco, sem exigir limpeza manual
 * nos fluxos de exclusão.
 */
@Data
@Entity
@Table(name = "favoritos_vaga",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_favorito_candidato_vaga",
        columnNames = {"candidato_id", "vaga_id"}))
public class FavoritoVaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "candidato_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Candidatos candidato;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vaga_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Vagas vaga;

    @Column(name = "data_favoritado", nullable = false)
    private LocalDateTime dataFavoritado;

    @PrePersist
    protected void onCreate() {
        this.dataFavoritado = LocalDateTime.now();
    }
}
