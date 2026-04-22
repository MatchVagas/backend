package com.matchvagas.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.matchvagas.backend.dto.RamoAtuacaoRequestDTO;
import com.matchvagas.backend.dto.RamoAtuacaoResponseDTO;
import com.matchvagas.backend.entity.RamoAtuacao;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RamoAtuacaoMapper {
    
    RamoAtuacao toEntity(RamoAtuacaoRequestDTO dto);
    
    RamoAtuacaoResponseDTO toResponseDTO(RamoAtuacao entity);
}
