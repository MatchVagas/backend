package com.matchvagas.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.matchvagas.backend.entity.Usuarios;

// CORRIGIDO: ID type Long (era Integer — causava conflito com findById(Long))
public interface UsuariosRepository extends JpaRepository<Usuarios, Long> {

    Optional<Usuarios> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Usuarios> findByAtivo(Boolean ativo);

    List<Usuarios> findByTipoUsuarioAndAtivo(Usuarios.TipoUsuario tipoUsuario, Boolean ativo);

    // Usuários ativos de um tipo cujo último acesso é anterior ao limite (LGPD-09)
    List<Usuarios> findByTipoUsuarioAndAtivoAndDataUltimoAcessoBefore(
            Usuarios.TipoUsuario tipoUsuario, Boolean ativo, LocalDateTime limite);
}
