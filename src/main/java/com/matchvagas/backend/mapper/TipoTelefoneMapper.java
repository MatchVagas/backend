package com.matchvagas.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.matchvagas.backend.dto.TipoTelefoneRequestDTO;
import com.matchvagas.backend.dto.TipoTelefoneResponseDTO;
import com.matchvagas.backend.entity.TipoTelefone;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TipoTelefoneMapper {
    
    TipoTelefone toEntity(TipoTelefoneRequestDTO dto);
    
    TipoTelefoneResponseDTO toResponseDTO(TipoTelefone entity);
}
