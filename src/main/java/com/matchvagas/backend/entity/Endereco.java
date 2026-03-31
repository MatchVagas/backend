@Entity
@Table(name = "endereco")
public class Endereco {
@Id
@GeneratedValue(stragery = jakarta.persistence.GenerationType.IDENTITY)
private Long id;

@Column(logradouro = "logradouro", nullable = false )
private String logradouro;

@Column(numero = "numero" , nullable = false )
private String numero;

@Column(completo = "completo" , nullable = false )
private String completo;

@Column(estado = "estado" , nullable = false )
private String estado;

@Column(cidade = "cidade" , nullable = false )
private String cidade;

@Column(bairro = "bairro" , nullable = false )
private String bairro;

@Column(cep = "cep" , nullable = false )
private String cep;
 public Endereco(String logradouro, String numero, String completo, String estado, String cidade, String bairro, String cep){
    this.logradouro = logradouro;
    this.numero = numero;
    this.completo = completo;
    this.estado = estado;
    this.cidade = cidade;
    this.bairro = bairro;
    this.cep = cep;
 }

  // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;

}

}