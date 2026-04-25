package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.VagaRequestDTO;
import com.matchvagas.backend.dto.VagaResponseDTO;
import com.matchvagas.backend.entity.Vagas;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VagasMapper {

    @Mapping(target = "id",                  source = "id")          // ADICIONADO
    @Mapping(target = "empresaId",           source = "empresas.id") // ADICIONADO
    @Mapping(target = "nomeFantasiaEmpresa", source = "empresas.nomeFantasia")
    @Mapping(target = "tipoVagaId",          source = "tipoVaga.id")
    @Mapping(target = "tipoVagaDescricao",   source = "tipoVaga.descricao")
    @Mapping(target = "modalidadeId",        source = "modalidade.id")
    @Mapping(target = "modalidadeDescricao", source = "modalidade.descricao")
    @Mapping(target = "escolaridadeId",      source = "escolaridade.id")
    @Mapping(target = "escolaridadeNome",    source = "escolaridade.nome")
    @Mapping(target = "statusVagaId",        source = "status.id")
    @Mapping(target = "statusDescricao",     source = "status.descricao")
    @Mapping(target = "cidadeId",            source = "cidade.id")
    @Mapping(target = "nomeCidade",          source = "cidade.nome")
    @Mapping(target = "ufEstado",            source = "cidade.estado.uf")
    VagaResponseDTO toDTO(Vagas vaga);

    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "empresas",     ignore = true)
    @Mapping(target = "tipoVaga",     ignore = true)
    @Mapping(target = "modalidade",   ignore = true)
    @Mapping(target = "escolaridade", ignore = true)
    @Mapping(target = "status",       ignore = true)
    @Mapping(target = "cidade",       ignore = true)
    @Mapping(target = "dataPublicacao", ignore = true)
    Vagas toEntity(VagaRequestDTO dto);
}
