package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.CandidatoRequestDTO;
import com.matchvagas.backend.dto.CandidatoResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CandidatoMapper {

    @Mapping(target = "endereco", ignore = true)
    @Mapping(target = "curriculo", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    Candidatos toEntity(CandidatoRequestDTO dto);

    @Mapping(target = "nome", source = "usuario.nome")
    @Mapping(target = "email", source = "usuario.email")
    @Mapping(target = "telefones", source = "usuario.telefones")
    @Mapping(target = "dataNascimento", source = "usuario.dataNascimento")
    @Mapping(target = "nivelFormacao", ignore = true) // Assuming from curriculo or elsewhere
    @Mapping(target = "resumoProfissional", source = "objetivoProfissional")
    CandidatoResponseDTO toResponseDTO(Candidatos entity);

    CandidatoRequestDTO toRequestDTO(Candidatos entity);
}