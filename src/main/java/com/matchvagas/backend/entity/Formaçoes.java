import jakarta.persistence.*;

@Entity
@Table(name = "formacoes")
public class Formacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instituicao", nullable = false)
    private String instituicao;

    @Column(name = "curso", nullable = false)
    private String curso;

    @Column(name = "nivel", nullable = false)
    private String nivel; 
    @Column(name = "data_inicio", nullable = false)
    private String dataInicio; 
    @Column(name = "data_fim")
    private String dataFim; 
    // Relacionamento: cada formação pertence a um candidato
    @ManyToOne
    @JoinColumn(name = "candidato_id", nullable = false)
    private CandidatoVaga candidato;

    public Formacao() {}

    public Formacao(String instituicao, String curso, String nivel, String dataInicio, String dataFim, CandidatoVaga candidato) {
        this.instituicao = instituicao;
        this.curso = curso;
        this.nivel = nivel;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.candidato = candidato;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInstituicao() { return instituicao; }
    public void setInstituicao(String instituicao) { this.instituicao = instituicao; }

    public String getCurso() { return curso; }
    public void setCurso(String curso) { this.curso = curso; }

    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }

    public String getDataInicio() { return dataInicio; }
    public void setDataInicio(String dataInicio) { this.dataInicio = dataInicio; }

    public String getDataFim() { return dataFim; }
    public void setDataFim(String dataFim) { this.dataFim = dataFim; }

    public CandidatoVaga getCandidato() { return candidato; }
    public void setCandidato(CandidatoVaga candidato) { this.candidato = candidato; }
}
