package com.matchvagas.backend.mapper;

import com.matchvagas.backend.dto.ExperienciasRequestDTO;
import com.matchvagas.backend.entity.Experiencia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.MappingTarget;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ExperienciasMapper {

    //ExperienciasMapper INSTANCE = Mappers.getMapper(ExperienciaMapper.class);

    DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd

    // Entidade -> DTO
    //@Mapping(target = "empresaId", source = "empresa.id", ignore = true) // Ignorado para evitar problemas de mapeamento circular, pode ser resolvido em @AfterMapping
    //@Mapping(target = "empresaNome", source = "empresa.nome")
    @Mapping(target = "dataInicio")
    @Mapping(target = "dataFim")
    ExperienciasRequestDTO toDto(Experiencia experiencia);

    // DTO -> Entidade (criação)
    @Mapping(target = "empresa", ignore = true) // será resolvida em @AfterMapping ou no serviço
    @Mapping(target = "dataInicio", source = "dataInicio")
    @Mapping(target = "dataFim", source = "dataFim")
    Experiencia toEntity(ExperienciasRequestDTO dto);

    // Atualiza entidade existente com dados do DTO
    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "dataInicio", source = "dataInicio")
    @Mapping(target = "dataFim", source = "dataFim")
    void updateFromDto(ExperienciasRequestDTO dto, @MappingTarget Experiencia entity);

    List<ExperienciasRequestDTO> toDtoList(List<Experiencia> list);
    List<Experiencia> toEntityList(List<ExperienciasRequestDTO> list);

    @Named("stringToLocalDate")
    default LocalDate stringToLocalDate(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s, FORMATTER);
    }

    // Exemplo de @AfterMapping para popular a relação Empresa a partir do dto usando resolvers de contexto
    /*@AfterMapping
    default void afterDtoToEntity(ExperienciasRequestDTO dto, @MappingTarget Experiencia entity,
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
    }*/
}
