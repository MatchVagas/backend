package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.EmpresaRequestDTO;
import com.matchvagas.backend.dto.EmpresaResponseDTO;
import com.matchvagas.backend.entity.Empresas;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EmpresaMapper {

    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "porte",        ignore = true)
    @Mapping(target = "ramoAtuacao",  ignore = true)
    @Mapping(target = "telefones",    ignore = true)
    @Mapping(target = "usuario",      ignore = true) // CORRIGIDO: adicionado ignore
    Empresas toEntity(EmpresaRequestDTO dto);

    @Mapping(target = "porte",           source = "porte.descricao")
    @Mapping(target = "ramoAtuacao",     source = "ramoAtuacao.descricao")
    @Mapping(target = "totalVagasAtivas", ignore = true)
    @Mapping(target = "usuarioGestorId", source = "usuario.id")   // ADICIONADO
    @Mapping(target = "nomeGestor",      source = "usuario.nome") // ADICIONADO
    EmpresaResponseDTO toResponseDTO(Empresas entity);

    @Mapping(target = "porteId",         source = "porte.id")
    @Mapping(target = "ramoId",          source = "ramoAtuacao.id")
    @Mapping(target = "usuarioGestorId", source = "usuario.id")
    EmpresaRequestDTO toRequestDTO(Empresas entity);
}
