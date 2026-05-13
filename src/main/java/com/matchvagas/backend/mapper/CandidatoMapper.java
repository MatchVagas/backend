package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.CandidatoRequestDTO;
import com.matchvagas.backend.dto.CandidatoResponseDTO;
import com.matchvagas.backend.dto.EnderecosResponseDTO;
import com.matchvagas.backend.dto.TelefonesResponseDTO;
import com.matchvagas.backend.entity.Candidatos;
import com.matchvagas.backend.entity.Endereco;
import com.matchvagas.backend.entity.Telefones;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CandidatoMapper {

    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "endereco",     ignore = true)
    @Mapping(target = "curriculo",    ignore = true)
    @Mapping(target = "usuario",      ignore = true)
    @Mapping(target = "objetivoProfissional", source = "resumoProfissional")
    Candidatos toEntity(CandidatoRequestDTO dto);

    @Mapping(target = "nome",                source = "usuario.nome")
    @Mapping(target = "email",               source = "usuario.email")
    @Mapping(target = "dataNascimento",      source = "usuario.dataNascimento", qualifiedByName = "dateToLocalDate")
    @Mapping(target = "objetivoProfissional", source = "objetivoProfissional")
    @Mapping(target = "localizacao",         source = "endereco")
    @Mapping(target = "telefone",            source = "usuario.telefones", qualifiedByName = "primeiroTelefone")
    CandidatoResponseDTO toResponseDTO(Candidatos entity);

    @Mapping(target = "cidadeId", ignore = true)
    EnderecosResponseDTO toEnderecosResponseDTO(Endereco endereco);

    @Named("dateToLocalDate")
    default LocalDate dateToLocalDate(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    @Named("primeiroTelefone")
    default TelefonesResponseDTO primeiroTelefone(List<Telefones> telefones) {
        if (telefones == null || telefones.isEmpty()) return null;
        Telefones t = telefones.get(0);
        return new TelefonesResponseDTO(
                t.getId(),
                t.getNumero(),
                t.getTipoTelefone() != null ? t.getTipoTelefone().getId() : null,
                t.getTipoTelefone() != null ? t.getTipoTelefone().getNome() : null,
                t.isWpp()
        );
    }
}
