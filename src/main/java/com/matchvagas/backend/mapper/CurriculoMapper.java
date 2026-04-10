package com.matchvagas.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.matchvagas.backend.dto.CurriculoRequestDTO;
import com.matchvagas.backend.dto.CurriculoResponseDTO;
import com.matchvagas.backend.entity.Curriculos;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CurriculoMapper {
    
    Curriculos toEntity(CurriculoRequestDTO dto);
    
    CurriculoResponseDTO toResponseDTO(Curriculos entity);
}
