package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.ExperienciaRequestDTO;
import com.matchvagas.backend.dto.ExperienciaResponseDTO;
import com.matchvagas.backend.entity.Experiencia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.MappingTarget;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ExperienciaMapper {

    ExperienciaMapper INSTANCE = Mappers.getMapper(ExperienciaMapper.class);

    DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd

    // Entidade -> DTO
    @Mapping(target = "empresaId", source = "empresa.id")
    @Mapping(target = "empresaNome", source = "empresa.nome")
    @Mapping(target = "dataInicio", expression = "java(experiencia.getDataInicio() != null ? experiencia.getDataInicio().format(FORMATTER) : null)")
    @Mapping(target = "dataFim", expression = "java(experiencia.getDataFim() != null ? experiencia.getDataFim().format(FORMATTER) : null)")
    ExperienciaDTO toDto(Experiencia experiencia);

    // DTO -> Entidade (criação)
    @Mapping(target = "empresa", ignore = true) // será resolvida em @AfterMapping ou no serviço
    @Mapping(target = "dataInicio", source = "dataInicio", qualifiedByName = "stringToLocalDate")
    @Mapping(target = "dataFim", source = "dataFim", qualifiedByName = "stringToLocalDate")
    @Mapping(target = "atual", source = "atual")
    Experiencia toEntity(ExperienciaDTO dto);

    // Atualiza entidade existente com dados do DTO
    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "dataInicio", source = "dataInicio", qualifiedByName = "stringToLocalDate")
    @Mapping(target = "dataFim", source = "dataFim", qualifiedByName = "stringToLocalDate")
    void updateFromDto(ExperienciaDTO dto, @MappingTarget Experiencia entity);

    List<ExperienciaDTO> toDtoList(List<Experiencia> list);
    List<Experiencia> toEntityList(List<ExperienciaDTO> list);

    @Named("stringToLocalDate")
    default LocalDate stringToLocalDate(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s, FORMATTER);
    }

    // Exemplo de @AfterMapping para popular a relação Empresa a partir do dto usando resolvers de contexto
    @AfterMapping
    default void afterDtoToEntity(ExperienciaDTO dto, @MappingTarget Experiencia entity,
                                  @Context EmpresaResolver empresaResolver) {
        if (dto == null) return;
        if (dto.getEmpresaId() != null) {
            Empresa empresa = empresaResolver.resolveById(dto.getEmpresaId());
            entity.setEmpresa(empresa);
        }
    }

    // Resolver de contexto para buscar Empresa por id (implementado no serviço)
    interface EmpresaResolver {
        Empresa resolveById(Long id);
    }
}
