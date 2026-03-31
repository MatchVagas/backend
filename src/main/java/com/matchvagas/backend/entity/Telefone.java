@Entity
@Table (name = "telefones")
public class Telefones {
  @Id  
  @GeneratedValue(stragery = jakarta.persistence.GenerationType.IDENTITY)
  private Long id;
  
  @Column(numero = "numero", nullable = false )
  private String numero;

  @Column(tipo_telefones = "tipo_telefones", nullable = false )
  private String tipo_telefones;

  @Column(wpp = "wpp" , nullable = false )
  private String wpp;

  public Telefones(String numero, String tipo_telefones , String wpp) {
    this.numero = numero;
    this.tipo_telefones = tipo_telefones;
    this.wpp = wpp;


 }
  // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNumero() {
        return numero;
    }

    public void setTipo_Telefones(String numero) {
        this.tipo_telefones = tipo_telefones;

    }
  



}