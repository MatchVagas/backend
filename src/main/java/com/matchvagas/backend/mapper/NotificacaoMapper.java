package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.NotificacoesRequestDTO;
import com.matchvagas.backend.dto.NotificacoesResponseDTO;
import com.matchvagas.backend.entity.Notificacao;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface NotificacaoMapper {

    /**
     * Converte Request → Entity (para criação)
     */
    @Mapping(target = "id", ignore = true)   // ID gerado pelo banco
    Notificacao toEntity(NotificacoesRequestDTO request);

    /**
     * Converte Entity → DTO (para resposta)
     */
    NotificacoesResponseDTO toDTO(Notificacao notificacao);

    /**
     * Atualização parcial (útil para PUT ou PATCH)
     */
    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(NotificacoesRequestDTO request, @MappingTarget Notificacao notificacao);
}