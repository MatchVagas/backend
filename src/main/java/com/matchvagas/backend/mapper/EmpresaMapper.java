package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.EmpresaRequestDTO;
import com.matchvagas.backend.dto.EmpresaResponseDTO;
import com.matchvagas.backend.entity.Empresas;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmpresaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "porte", ignore = true) // Need to fetch by ID
    @Mapping(target = "ramoAtuacao", ignore = true) // Need to fetch by ID
    @Mapping(target = "descricao", ignore = true)
    @Mapping(target = "telefones", ignore = true)
    Empresas toEntity(EmpresaRequestDTO dto);

    @Mapping(target = "porte", source = "porte.id", ignore = true) // Assuming Porte has an ID field
    @Mapping(target = "ramoAtuacao", source = "ramoAtuacao.id", ignore = true) // Assuming RamoAtuacao has an ID field
    @Mapping(target = "cnpj", ignore = true) // Assuming list of strings
    @Mapping(target = "totalVagasAtivas", ignore = true)
    EmpresaResponseDTO toResponseDTO(Empresas entity);

    @Mapping(target = "porteId", source = "porte.id")
    @Mapping(target = "ramoId", source = "ramoAtuacao.id")
    EmpresaRequestDTO toRequestDTO(Empresas entity);
}