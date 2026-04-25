package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.EstadoRequestDTO;
import com.matchvagas.backend.dto.EstadoResponseDTO;
import com.matchvagas.backend.entity.Estado;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EstadoMapper {

    @Mapping(target = "id",   ignore = true)
    @Mapping(target = "pais", ignore = true) // resolvido no controller
    Estado toEntity(EstadoRequestDTO dto);

    @Mapping(target = "paisId",   source = "pais.id")    // CORRIGIDO: era null
    @Mapping(target = "paisNome", source = "pais.nome")  // CORRIGIDO: era null
    EstadoResponseDTO toResponseDTO(Estado entity);
}
