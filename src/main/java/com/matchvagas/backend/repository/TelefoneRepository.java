package com.matchvagas.backend.repository;

import com.matchvagas.backend.entity.Telefones;
import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TelefoneRepository extends JpaRepository<Telefones, Long> {

    /**
     * Busca todos os telefones de um usuário específico
     */
    //List<Telefones> findByUsuarioId(Long usuarioId);   // Se você adicionar o relacionamento ManyToMany no Usuario

    /**
     * Busca todos os telefones de uma empresa específica
     */
    //List<Telefones> findByEmpresa(Long empresaId);   // Se você adicionar o relacionamento na Empresa

    /**
     * Busca telefone por número (útil para validações de duplicidade)
     */
    Optional<Telefones> findByNumero(String numero);

    /**
     * Busca telefones que são WhatsApp
     */
    List<Telefones> findByWppTrue();

    /**
     * Busca telefones por tipo (Celular, Fixo, Comercial, etc.)
     */
    List<Telefones> findByTipoTelefoneId(Long tipoTelefoneId);

    /**
     * Busca telefones de um usuário que são WhatsApp

    @Query("SELECT t FROM Telefones t JOIN t.usuarios u WHERE u.id = :usuarioId AND t.wpp = true")
    List<Telefones> findWhatsAppByUsuarioId(@Param("usuarioId") Long usuarioId);
     */
    /**
     * Verifica se um número de telefone já existe
     */
    boolean existsByNumero(String numero);

    /**
     * Deleta todos os telefones de um usuário (útil em exclusão ou limpeza)

    void deleteByUsuarioId(Long usuarioId);
     */

    /**
     * Deleta todos os telefones de uma empresa
     */
    //void deleteByEmpresaId(Long empresaId);
}