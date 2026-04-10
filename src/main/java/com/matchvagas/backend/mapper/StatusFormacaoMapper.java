package com.matchvagas.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.matchvagas.backend.dto.StatusFormacaoRequestDTO;
import com.matchvagas.backend.dto.StatusFormacaoResponseDTO;
import com.matchvagas.backend.entity.StatusFormacao;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StatusFormacaoMapper {
    
    StatusFormacao toEntity(StatusFormacaoRequestDTO dto);
    
    StatusFormacaoResponseDTO toResponseDTO(StatusFormacao entity);
}
