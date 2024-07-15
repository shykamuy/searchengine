package searchengine.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "l2p")
public class Lemma2Page {

    @Id
    @Column(name = "id", nullable = false)
    //@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_generator")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "page_id", referencedColumnName = "id")
    private Page pageId;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "lemma_id", referencedColumnName = "id")
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    private Lemma lemmaId;

    @Column(name = "quantity", nullable = false)
    private Float quantity;


    @Override
    public String toString() {
        return "Lemma2Page{" +
                "id=" + id +
                ", pageId=" + pageId.getId() +
                ", lemmaId=" + lemmaId +
                ", quantity=" + quantity +
                '}';
    }
}
