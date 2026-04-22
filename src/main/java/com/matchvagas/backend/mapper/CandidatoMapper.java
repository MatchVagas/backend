package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.CandidatoRequestDTO;
import com.matchvagas.backend.dto.CandidatoResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CandidatoMapper {

    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "endereco",     ignore = true)
    @Mapping(target = "curriculo",    ignore = true)
    @Mapping(target = "usuario",      ignore = true)
    @Mapping(target = "objetivoProfissional", source = "resumoProfissional")
    Candidatos toEntity(CandidatoRequestDTO dto);

    @Mapping(target = "nome",               source = "usuario.nome")
    @Mapping(target = "email",              source = "usuario.email")
    @Mapping(target = "objetivoProfissional", source = "objetivoProfissional")
    CandidatoResponseDTO toResponseDTO(Candidatos entity);
}
