package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.CandidaturaResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Candidatura;
import com.matchvagas.backend.entity.StatusCandidatura;
import com.matchvagas.backend.entity.Vagas;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CandidaturaMapper {

    @Mapping(source = "candidato", target = "candidatoId",   qualifiedByName = "mapCandidatoId")
    @Mapping(source = "candidato", target = "nomeCandidato", qualifiedByName = "mapCandidatoNome")
    @Mapping(source = "vaga",      target = "vagaId",        qualifiedByName = "mapVagaId")
    @Mapping(source = "vaga",      target = "tituloVaga",    qualifiedByName = "mapVagaTitulo")
    @Mapping(source = "status",    target = "status",        qualifiedByName = "mapStatusToString")
    // preferências de compartilhamento — mapeadas diretamente pelos nomes
    @Mapping(source = "compartilharObjetivoProfissional", target = "compartilharObjetivoProfissional")
    @Mapping(source = "compartilharDisponibilidade",      target = "compartilharDisponibilidade")
    @Mapping(source = "compartilharPretensaoSalarial",    target = "compartilharPretensaoSalarial")
    @Mapping(source = "compartilharCurriculo",            target = "compartilharCurriculo")
    @Mapping(source = "compartilharExperiencias",         target = "compartilharExperiencias")
    @Mapping(source = "compartilharFormacoes",            target = "compartilharFormacoes")
    @Mapping(source = "compartilharTelefone",             target = "compartilharTelefone")
    @Mapping(source = "compartilharEndereco",             target = "compartilharEndereco")
    CandidaturaResponseDTO toResponseDTO(Candidatura entity);

    @Named("mapCandidatoId")
    default Long mapCandidatoId(Candidatos candidato) {
        return candidato != null ? candidato.getId() : null;
    }

    @Named("mapCandidatoNome")
    default String mapCandidatoNome(Candidatos candidato) {
        return candidato != null ? candidato.getUsuario().getNome() : null;
    }

    @Named("mapVagaId")
    default Long mapVagaId(Vagas vaga) {
        return vaga != null ? vaga.getId() : null;
    }

    @Named("mapVagaTitulo")
    default String mapVagaTitulo(Vagas vaga) {
        return vaga != null ? vaga.getTitulo() : null;
    }

    @Named("mapStatusToString")
    default String mapStatusToString(StatusCandidatura status) {
        return status != null ? status.getStatus() : null;
    }
}
