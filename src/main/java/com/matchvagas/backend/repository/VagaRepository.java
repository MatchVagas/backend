package com.matchvagas.backend.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.matchvagas.backend.entity.Vagas;

public interface VagaRepository extends JpaRepository<Vagas, Long>{
    List<Vagas> findByEmpresasId(Long empresaId);
    List<Vagas> findByTipoVagaId(Long tipoVagaId);
    List<Vagas> findByModalidadeId(Long modalidadeId);
    List<Vagas> findBySalarioMinimoGreaterThanEqual(BigDecimal salarioMinimo);
    List<Vagas> findBySalarioMaximoLessThanEqual(BigDecimal salarioMaximo);
    List<Vagas> findByCargaHoraria(String cargaHoraria);
    List<Vagas> findByIdadeMinimaGreaterThanEqual(Integer idadeMinima); 
    List<Vagas> findByIdadeMaximaLessThanEqual(Integer idadeMaxima);
    List<Vagas> findByBeneficiosContaining(String beneficio);
    List<Vagas> findByRequisitosContaining(String requisito);
    List<Vagas> findByDescricaoContaining(String descricao);
    List<Vagas> findByTituloContaining(String titulo);
    List<Vagas> findByEmpresasNomeFantasiaContaining(String nomeFantasia);
    List<Vagas> findByTipoVagaDescricaoContaining(String descricao);
    List<Vagas> findByModalidadeDescricaoContaining(String descricao);
    List<Vagas> findByEmpresasIdAndTipoVagaIdAndModalidadeId(Long empresaId, Long tipoVagaId, Long modalidadeId);
    
} 
