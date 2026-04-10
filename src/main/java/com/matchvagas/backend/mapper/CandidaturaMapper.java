package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.CandidaturaRequestDTO;
import com.matchvagas.backend.dto.CandidaturaResponseDTO;
import com.matchvagas.backend.entity.Candidatura;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CandidaturaMapper {

    @Mapping(target = "candidato", ignore = true) // Fetch by ID
    @Mapping(target = "vaga", ignore = true)
    @Mapping(target = "status", ignore = true)
    Candidatura toEntity(CandidaturaRequestDTO dto);

    @Mapping(target = "candidatoId", source = "candidato.id")
    @Mapping(target = "vagaId", source = "vaga.id")
    @Mapping(target = "status", source = "status.descricao")
    @Mapping(target = "mensagemCandidato", ignore = true)
    @Mapping(target = "curriculoUrl", ignore = true)
    @Mapping(target = "pretensaoSalarial", source = "candidato.pretensaoSalarial")
    @Mapping(target = "disponibilidade", source = "candidato.disponibilidade")
    @Mapping(target = "etapaProcesso", ignore = true)
    @Mapping(target = "notasRecrutador", ignore = true)
    CandidaturaResponseDTO toResponseDTO(Candidatura entity);

    CandidaturaRequestDTO toRequestDTO(Candidatura entity);
}