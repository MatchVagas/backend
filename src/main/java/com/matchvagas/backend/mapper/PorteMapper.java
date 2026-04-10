package com.matchvagas.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.matchvagas.backend.dto.PorteRequestDTO;
import com.matchvagas.backend.dto.PorteResponseDTO;
import com.matchvagas.backend.entity.Porte;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PorteMapper {
    
    Porte toEntity(PorteRequestDTO dto);
    
    PorteResponseDTO toResponseDTO(Porte entity);
}
