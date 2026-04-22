package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.AdministradorRequestDTO;
import com.matchvagas.backend.dto.AdministradorResponseDTO;
import com.matchvagas.backend.entity.Administradores;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdministradoresMapper {

    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "usuario",     ignore = true) // resolvido no service
    @Mapping(target = "departamento",ignore = true) // resolvido no service
    Administradores toEntity(AdministradorRequestDTO dto);

    @Mapping(target = "nomeUsuario",  source = "usuario.nome")
    @Mapping(target = "emailUsuario", source = "usuario.email")
    @Mapping(target = "departamento", source = "departamento.nome")
    AdministradorResponseDTO toResponseDTO(Administradores entity);
}
