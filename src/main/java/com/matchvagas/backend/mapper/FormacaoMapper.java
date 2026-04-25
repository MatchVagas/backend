package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.FormacaoRequestDTO;
import com.matchvagas.backend.dto.FormacaoResponseDTO;
import com.matchvagas.backend.entity.Formacao;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FormacaoMapper {

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "candidato", ignore = true) // resolvido no service
    Formacao toEntity(FormacaoRequestDTO dto);

    // CORRIGIDO: era Formacao toDto(Formacao) — tipo de retorno errado
    @Mapping(target = "candidatoId", source = "candidato.id")
    FormacaoResponseDTO toResponseDTO(Formacao formacao);

    @Mapping(target = "candidato", ignore = true)
    void updateFromDto(FormacaoRequestDTO dto, @MappingTarget Formacao entity);

    List<FormacaoResponseDTO> toResponseDTOList(List<Formacao> list);
}
