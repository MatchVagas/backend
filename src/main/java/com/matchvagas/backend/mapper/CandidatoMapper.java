package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.CandidatoRequestDTO;
import com.matchvagas.backend.dto.CandidatoResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Usuarios;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {TelefonesMapper.class, EnderecoMapper.class})
public interface CandidatoMapper {

    // --- Mapeamento Entidade -> ResponseDTO ---
    @Mapping(source = "id", target = "id")
    @Mapping(source = "usuario.id", target = "usuarioId")
    @Mapping(source = "usuario.nome", target = "nome")
    @Mapping(source = "usuario.email", target = "email")
    @Mapping(source = "usuario.dataNascimento", target = "dataNascimento")
    @Mapping(source = "usuario.idade", target = "idade")
    @Mapping(source = "usuario.telefones", target = "telefones")
    @Mapping(source = "usuario.dataCadastro", target = "dataCadastro")
    @Mapping(source = "usuario.ativo", target = "ativo")
    @Mapping(source = "cpf", target = "cpf")
    @Mapping(source = "endereco", target = "endereco")
    @Mapping(source = "objetivoProfissional", target = "objetivoProfissional")
    @Mapping(source = "pretensaoSalarial", target = "pretensaoSalarial")
    @Mapping(source = "disponibilidade", target = "disponibilidade")
    CandidatoResponseDTO toResponseDTO(Candidatos candidato);

    // --- Mapeamento RequestDTO -> Entidade (criação) ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "curriculo", ignore = true)
    Candidatos toEntity(CandidatoRequestDTO dto);

    // --- Atualização da entidade Candidatos a partir do DTO ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "curriculo", ignore = true)
    void updateCandidatoFromDTO(CandidatoRequestDTO dto, @MappingTarget Candidatos candidato);

    // --- Atualização da entidade Usuarios a partir do DTO ---
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "senha", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataUltimoAcesso", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    @Mapping(target = "idade", ignore = true) // idade pode ser calculada a partir de dataNascimento
    void updateUsuarioFromDTO(CandidatoRequestDTO dto, @MappingTarget Usuarios usuario);
}