package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.NotificacoesRequestDTO;
import com.matchvagas.backend.dto.NotificacoesResponseDTO;
import com.matchvagas.backend.entity.Notificacao;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface NotificacaoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tipo", ignore = true)
    @Mapping(target = "dataEnvio", ignore = true)
    @Mapping(target = "lida", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    Notificacao toEntity(NotificacoesRequestDTO request);

    @Mapping(target = "tipoNotificacao", source = "tipo.status")
    @Mapping(target = "usuarioId", source = "usuario.id")
    NotificacoesResponseDTO toDTO(Notificacao notificacao);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tipo", ignore = true)
    @Mapping(target = "dataEnvio", ignore = true)
    @Mapping(target = "lida", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    void updateEntityFromRequest(NotificacoesRequestDTO request, @MappingTarget Notificacao notificacao);
}