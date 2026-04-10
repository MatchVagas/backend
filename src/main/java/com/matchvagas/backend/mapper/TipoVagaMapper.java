package com.matchvagas.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.matchvagas.backend.dto.TipoVagaRequestDTO;
import com.matchvagas.backend.dto.TipoVagaResponseDTO;
import com.matchvagas.backend.entity.TipoVaga;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TipoVagaMapper {
    
    TipoVaga toEntity(TipoVagaRequestDTO dto);
    
    TipoVagaResponseDTO toResponseDTO(TipoVaga entity);
}
