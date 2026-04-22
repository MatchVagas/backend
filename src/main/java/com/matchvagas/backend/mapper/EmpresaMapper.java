package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.EmpresaRequestDTO;
import com.matchvagas.backend.dto.EmpresaResponseDTO;
import com.matchvagas.backend.entity.Empresas;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmpresaMapper {

    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "porte",       ignore = true) // resolvido no service
    @Mapping(target = "ramoAtuacao", ignore = true) // resolvido no service
    @Mapping(target = "telefones",   ignore = true)
    Empresas toEntity(EmpresaRequestDTO dto);

    @Mapping(target = "porte",           source = "porte.descricao")
    @Mapping(target = "ramoAtuacao",     source = "ramoAtuacao.descricao")
    @Mapping(target = "totalVagasAtivas", ignore = true) // calculado à parte se necessário
    EmpresaResponseDTO toResponseDTO(Empresas entity);

    @Mapping(target = "porteId", source = "porte.id")
    @Mapping(target = "ramoId",  source = "ramoAtuacao.id")
    EmpresaRequestDTO toRequestDTO(Empresas entity);
}
