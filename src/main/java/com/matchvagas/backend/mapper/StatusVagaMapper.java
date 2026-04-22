package com.matchvagas.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.matchvagas.backend.dto.StatusVagaRequestDTO;
import com.matchvagas.backend.dto.StatusVagaResponseDTO;
import com.matchvagas.backend.entity.StatusVaga;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StatusVagaMapper {
    
    StatusVaga toEntity(StatusVagaRequestDTO dto);
    
    StatusVagaResponseDTO toResponseDTO(StatusVaga entity);
}
