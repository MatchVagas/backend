package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.TelefonesRequestDTO;
import com.matchvagas.backend.dto.TelefonesResponseDTO;
import com.matchvagas.backend.entity.Telefones;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TelefonesMapper {

    @Mapping(target = "tipoTelefone", ignore = true)
    Telefones toEntity(TelefonesRequestDTO request);

    @Mapping(target = "tipoTelefoneId", source = "tipoTelefone.id")
    TelefonesResponseDTO toDTO(Telefones telefone);
}