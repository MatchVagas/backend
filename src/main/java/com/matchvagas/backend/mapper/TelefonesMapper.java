package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.TelefonesRequestDTO;
import com.matchvagas.backend.dto.TelefonesResponseDTO;
import com.matchvagas.backend.entity.Telefones;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TelefonesMapper {

    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "tipoTelefone", ignore = true) // resolvido no service
    @Mapping(target = "empresas",     ignore = true)
    @Mapping(target = "usuarios",     ignore = true)
    Telefones toEntity(TelefonesRequestDTO request);

    @Mapping(target = "tipoTelefoneId",   source = "tipoTelefone.id")
    @Mapping(target = "tipoTelefoneNome", source = "tipoTelefone.nome") // ADICIONADO
    TelefonesResponseDTO toDTO(Telefones telefone);
}
