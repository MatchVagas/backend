package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.UsuarioResponseDTO;
import com.matchvagas.backend.dto.UsuariosRequestDTO;
import com.matchvagas.backend.entity.Usuarios;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "telefones", source = "telefones")
    UsuarioResponseDTO toResponseDTO(Usuarios entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataUltimoAcesso", ignore = true)
    @Mapping(target = "idade", ignore = true) // idade pode ser calculada
    @Mapping(target = "ativo", ignore = true)
    @Mapping(target = "senha", ignore = true) // tratada no service
    @Mapping(target = "telefones", ignore = true) // tratado separadamente
    Usuarios toEntity(UsuariosRequestDTO dto);

    // segurança: nunca expor senha
    UsuarioResponseDTO toDTO(Usuarios usuario);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dataCadastro", ignore = true)
    @Mapping(target = "dataUltimoAcesso", ignore = true)
    @Mapping(target = "idade", ignore = true)
    @Mapping(target = "ativo", ignore = true)
    @Mapping(target = "senha", ignore = true)
    @Mapping(target = "telefones", ignore = true)
    void updateEntityFromDTO(UsuariosRequestDTO dto, @MappingTarget Usuarios entity);


}