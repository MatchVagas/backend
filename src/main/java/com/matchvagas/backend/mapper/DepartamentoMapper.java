package com.matchvagas.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.matchvagas.backend.dto.DepartamentoRequestDTO;
import com.matchvagas.backend.dto.DepartamentoResponseDTO;
import com.matchvagas.backend.entity.Departamentos;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DepartamentoMapper {
    
    Departamentos toEntity(DepartamentoRequestDTO dto);
    
    DepartamentoResponseDTO toResponseDTO(Departamentos entity);
}
