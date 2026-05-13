package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.HabilidadeRequestDTO;
import com.matchvagas.backend.dto.HabilidadeResponseDTO;
import com.matchvagas.backend.entity.Habilidade;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface HabilidadeMapper {

    Habilidade toEntity(HabilidadeRequestDTO dto);

    HabilidadeResponseDTO toResponseDTO(Habilidade entity);
}
