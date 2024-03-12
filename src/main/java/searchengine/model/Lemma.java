package searchengine.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lemma", indexes = @Index(name = "idx_lemma", columnList = "lemma, site_id", unique = true))
public class Lemma {
    @Id
    @Column(name = "id")
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_generator")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private int id;

    @ManyToOne
    @JoinColumn(name = "site_id", referencedColumnName = "id", nullable = false)
    private Site siteId;

    @Column(name = "lemma", nullable = false)
    private String lemma;

    @Column(name = "frequency", nullable = false)
    private int frequency;





    @Override
    public String toString() {
        return "Lemma{" +
                "id=" + id +
                ", siteId=" + siteId.getId() +
                ", lemma='" + lemma + '\'' +
                ", frequency=" + frequency +
                '}';
    }
}
