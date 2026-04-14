package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.TelefoneDTO;
import com.matchvagas.backend.dto.request.TelefoneRequest;
import com.matchvagas.backend.entity.Telefones;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TelefonesMapper {

    @Mapping(target = "tipoTelefone", ignore = true)
    Telefones toEntity(TelefoneRequest request);

    @Mapping(target = "tipoTelefoneId", source = "tipoTelefone.id")
    TelefoneDTO toDTO(Telefones telefone);
}