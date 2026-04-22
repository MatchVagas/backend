package com.matchvagas.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.matchvagas.backend.dto.EstadoRequestDTO;
import com.matchvagas.backend.dto.EstadoResponseDTO;
import com.matchvagas.backend.entity.Estado;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EstadoMapper {
    
    Estado toEntity(EstadoRequestDTO dto);
    
    EstadoResponseDTO toResponseDTO(Estado entity);
}
