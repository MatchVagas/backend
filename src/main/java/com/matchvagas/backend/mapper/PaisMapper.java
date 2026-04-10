package com.matchvagas.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.matchvagas.backend.dto.PaisRequestDTO;
import com.matchvagas.backend.dto.PaisResponseDTO;
import com.matchvagas.backend.entity.Pais;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaisMapper {
    
    Pais toEntity(PaisRequestDTO dto);
    
    PaisResponseDTO toResponseDTO(Pais entity);
}
