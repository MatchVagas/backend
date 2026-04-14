package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.EnderecosRequestDTO;
import com.matchvagas.backend.dto.EnderecosResponseDTO;
import com.matchvagas.backend.entity.Endereco;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EnderecoMapper {

    /**
     * Converte Request → Entity (usado na criação/atualização)
     */
    @Mapping(target = "id", ignore = true)  // ID é gerado automaticamente
    Endereco toEntity(EnderecosRequestDTO request);

    /**
     * Converte Entity → Response DTO
     */
    EnderecosResponseDTO toDTO(Endereco endereco);

    /**
     * Atualização parcial (útil para PUT/PATCH)
     * Ignora campos nulos para não sobrescrever dados existentes
     */
    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(EnderecosRequestDTO request, @MappingTarget Endereco endereco);
}