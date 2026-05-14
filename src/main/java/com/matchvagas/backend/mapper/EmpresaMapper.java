package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.EmpresaRequestDTO;
import com.matchvagas.backend.dto.EmpresaResponseDTO;
import com.matchvagas.backend.dto.TelefonesResponseDTO;
import com.matchvagas.backend.entity.Empresas;
import com.matchvagas.backend.entity.Telefones;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EmpresaMapper {

    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "porte",        ignore = true)
    @Mapping(target = "ramoAtuacao",  ignore = true)
    @Mapping(target = "telefones",    ignore = true)
    @Mapping(target = "usuario",      ignore = true)
    @Mapping(target = "status",       ignore = true)
    Empresas toEntity(EmpresaRequestDTO dto);

    @Mapping(target = "porte",            source = "porte.descricao")
    @Mapping(target = "ramoAtuacao",      source = "ramoAtuacao.descricao")
    @Mapping(target = "telefone",         source = "telefones", qualifiedByName = "primeiroTelefone")
    @Mapping(target = "totalVagasAtivas", ignore = true)
    @Mapping(target = "usuarioGestorId",  source = "usuario.id")
    @Mapping(target = "nomeGestor",       source = "usuario.nome")
    @Mapping(target = "status",           expression = "java(entity.getStatus() != null ? entity.getStatus().name() : null)")
    EmpresaResponseDTO toResponseDTO(Empresas entity);

    @Mapping(target = "porteId",         source = "porte.id")
    @Mapping(target = "ramoId",          source = "ramoAtuacao.id")
    @Mapping(target = "usuarioGestorId", source = "usuario.id")
    @Mapping(target = "telefone",        ignore = true)
    EmpresaRequestDTO toRequestDTO(Empresas entity);

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
