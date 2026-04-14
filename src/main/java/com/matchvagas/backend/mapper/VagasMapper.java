package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.VagaDTO;
import com.matchvagas.backend.entity.Vagas;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VagasMapper {

    @Mapping(target = "empresaId", source = "empresas.id")
    @Mapping(target = "tipoVagaId", source = "tipoVaga.id")
    @Mapping(target = "modalidadeId", source = "modalidade.id")
    @Mapping(target = "escolaridadeId", source = "escolaridade.id")
    @Mapping(target = "statusId", source = "status.id")
    @Mapping(target = "cidadeId", source = "cidade.id")
    VagaDTO toDTO(Vagas vaga);

    @Mapping(target = "empresas", ignore = true)
    @Mapping(target = "tipoVaga", ignore = true)
    @Mapping(target = "modalidade", ignore = true)
    @Mapping(target = "escolaridade", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "cidade", ignore = true)
    Vagas toEntity(VagaDTO dto);
}