package com.matchvagas.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.matchvagas.backend.dto.StatusCandidaturaRequestDTO;
import com.matchvagas.backend.dto.StatusCandidaturaResponseDTO;
import com.matchvagas.backend.entity.StatusCandidatura;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StatusCandidaturaMapper {
    
    StatusCandidatura toEntity(StatusCandidaturaRequestDTO dto);
    
    StatusCandidaturaResponseDTO toResponseDTO(StatusCandidatura entity);
}
