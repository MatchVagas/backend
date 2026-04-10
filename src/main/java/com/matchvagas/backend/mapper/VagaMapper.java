package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.VagaRequestDTO;
import com.matchvagas.backend.dto.VagaResponseDTO;
import com.matchvagas.backend.entity.Vagas;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VagaMapper {

    @Mapping(target = "empresas", ignore = true) // Fetch by ID
    @Mapping(target = "tipoVaga", ignore = true)
    @Mapping(target = "modalidade", ignore = true)
    @Mapping(target = "statusVaga", ignore = true)
    @Mapping(target = "escolaridade", ignore = true)
    @Mapping(target = "departamento", ignore = true)
    Vagas toEntity(VagaRequestDTO dto);

    @Mapping(target = "empresaId", source = "empresas.id")
    @Mapping(target = "tipoVagaId", source = "tipoVaga.id")
    @Mapping(target = "modalidadeId", source = "modalidade.id")
    @Mapping(target = "escolaridadeId", source = "escolaridade.id")
    @Mapping(target = "departamentoId", source = "departamento.id")
    VagaResponseDTO toResponseDTO(Vagas entity);

    VagaRequestDTO toRequestDTO(Vagas entity);
}