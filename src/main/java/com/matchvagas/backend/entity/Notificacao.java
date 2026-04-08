@Entity
@Table(name = "notificacao");
public class Notificacao {
@Id
@GeneratedValue(stragery = jakarta.persistence.GenerationType.IDENTITY)
private Long  id; 

@Column( titulo = "titulo", nullable = false )
private String titulo;

@Column(mensagem = "mensagem" , nullable = false )




}

