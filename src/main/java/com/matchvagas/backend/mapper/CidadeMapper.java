package com.matchvagas.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.matchvagas.backend.dto.CidadeRequestDTO;
import com.matchvagas.backend.dto.CidadeResponseDTO;
import com.matchvagas.backend.entity.Cidade;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CidadeMapper {
    
    Cidade toEntity(CidadeRequestDTO dto);
    
    CidadeResponseDTO toResponseDTO(Cidade entity);
}
