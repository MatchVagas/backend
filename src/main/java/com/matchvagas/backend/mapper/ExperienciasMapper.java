package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.ExperienciaRequestDTO;
import com.matchvagas.backend.dto.ExperienciaResponseDTO;
import com.matchvagas.backend.entity.Experiencia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ExperienciasMapper {

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "candidato", ignore = true) // resolvido no service
    Experiencia toEntity(ExperienciaRequestDTO dto);

    // CORRIGIDO: era ExperienciasRequestDTO — tipo de retorno errado
    @Mapping(target = "candidatoId", source = "candidato.id")
    ExperienciaResponseDTO toResponseDTO(Experiencia experiencia);

    @Mapping(target = "candidato", ignore = true)
    void updateFromDto(ExperienciaRequestDTO dto, @MappingTarget Experiencia entity);

    List<ExperienciaResponseDTO> toResponseDTOList(List<Experiencia> list);
}
