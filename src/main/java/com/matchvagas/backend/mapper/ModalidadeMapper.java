package com.matchvagas.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.matchvagas.backend.dto.ModalidadeRequestDTO;
import com.matchvagas.backend.dto.ModalidadeResponseDTO;
import com.matchvagas.backend.entity.Modalidade;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ModalidadeMapper {
    
    Modalidade toEntity(ModalidadeRequestDTO dto);
    
    ModalidadeResponseDTO toResponseDTO(Modalidade entity);
}
