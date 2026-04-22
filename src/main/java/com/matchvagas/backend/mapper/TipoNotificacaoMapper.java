package com.matchvagas.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import com.matchvagas.backend.dto.TipoNotificacaoRequestDTO;
import com.matchvagas.backend.dto.TipoNotificacaoResponseDTO;
import com.matchvagas.backend.entity.TipoNotificacao;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TipoNotificacaoMapper {
    
    TipoNotificacao toEntity(TipoNotificacaoRequestDTO dto);
    
    TipoNotificacaoResponseDTO toResponseDTO(TipoNotificacao entity);
}
