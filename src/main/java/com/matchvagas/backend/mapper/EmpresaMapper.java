package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.EmpresaRequestDTO;
import com.matchvagas.backend.dto.EmpresaResponseDTO;
import com.matchvagas.backend.entity.Empresas;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmpresaMapper {

    @Mapping(target = "porte", ignore = true) // Need to fetch by ID
    @Mapping(target = "ramoAtuacao", ignore = true) // Need to fetch by ID
    @Mapping(target = "enderecos", ignore = true)
    @Mapping(target = "telefones", ignore = true)
    Empresas toEntity(EmpresaRequestDTO dto);

    @Mapping(target = "porteId", source = "porte.id")
    @Mapping(target = "ramoId", source = "ramoAtuacao.id")
    @Mapping(target = "telefones", ignore = true) // Assuming list of strings
    EmpresaResponseDTO toResponseDTO(Empresas entity);

    EmpresaRequestDTO toRequestDTO(Empresas entity);
}