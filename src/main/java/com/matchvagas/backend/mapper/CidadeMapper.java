package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.CidadeRequestDTO;
import com.matchvagas.backend.dto.CidadeResponseDTO;
import com.matchvagas.backend.entity.Cidade;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CidadeMapper {

    @Mapping(target = "id",     ignore = true)
    @Mapping(target = "estado", ignore = true) // resolvido no controller
    Cidade toEntity(CidadeRequestDTO dto);

    @Mapping(target = "estadoId",   source = "estado.id")    // CORRIGIDO: era null
    @Mapping(target = "estadoNome", source = "estado.nome")  // CORRIGIDO: era null
    @Mapping(target = "ufEstado",   source = "estado.uf")    // CORRIGIDO: era null
    CidadeResponseDTO toResponseDTO(Cidade entity);
}
