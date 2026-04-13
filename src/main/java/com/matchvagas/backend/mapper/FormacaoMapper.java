package com.matchvagas.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.matchvagas.backend.dto.FormacaoRequestDTO;
import com.matchvagas.backend.entity.Formacao;

@Mapper(componentModel = "spring")
public interface FormacaoMapper {

    FormacaoMapper INSTANCE = Mappers.getMapper(FormacaoMapper.class);

    DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd

    // Entidade -> DTO
    @Mapping(target = "instituicao")
    @Mapping(target = "dataInicio")
    @Mapping(target = "dataFim")
    Formacao toDto(Formacao formacao);

    // DTO -> Entidade (criação)
    @Mapping(target = "instituicao", ignore = true) // resolvido em @AfterMapping ou no serviço
    @Mapping(target = "dataInicio", source = "dataInicio")
    @Mapping(target = "dataFim", source = "dataFim")
    Formacao toEntity(FormacaoRequestDTO dto);

    // Atualiza entidade existente com dados do DTO
    @Mapping(target = "instituicao", ignore = true)
    @Mapping(target = "dataInicio", source = "dataInicio")
    @Mapping(target = "dataFim", source = "dataFim")
    void updateFromDto(FormacaoRequestDTO dto, @MappingTarget Formacao entity);

    List<FormacaoRequestDTO> toDtoList(List<Formacao> list);
    List<Formacao> toEntityList(List<FormacaoRequestDTO> list);

    @Named("stringToLocalDate")
    default LocalDate stringToLocalDate(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s, FORMATTER);
    }

    // AfterMapping para popular a relação Instituicao a partir do dto usando resolvers de contexto
    /*@AfterMapping
    default void afterDtoToEntity(FormacaoDTO dto, @MappingTarget Formacao entity,
                                  @Context InstituicaoResolver instituicaoResolver) {
        if (dto == null) return;
        if (dto.getInstituicaoId() != null) {
            Instituicao inst = instituicaoResolver.resolveById(dto.getInstituicaoId());
            entity.setInstituicao(inst);
        }
    }

    // Resolver de contexto para buscar Instituicao por id (implementar no serviço)
    interface InstituicaoResolver {
        Instituicao resolveById(Long id);
    }*/
}
