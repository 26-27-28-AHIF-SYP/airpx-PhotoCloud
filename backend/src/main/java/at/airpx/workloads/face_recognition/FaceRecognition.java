package at.airpx.workloads.face_recognition;

import at.airpx.workloads.picture.Picture;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "face_recognition")
public class FaceRecognition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "person_label", length = 300)
    private String personLabel;

    @Column(name = "bounding_box", length = 300)
    private String boundingBox;

    @ManyToMany
    @JoinTable(
            name = "picture_face_recognitions",
            joinColumns = @JoinColumn(name = "face_recognition_id"),
            inverseJoinColumns = @JoinColumn(name = "picture_id"))
    private List<Picture> pictures = new ArrayList<>();

    protected FaceRecognition() {}

    public FaceRecognition(String personLabel, String boundingBox) {
        this.personLabel = personLabel;
        this.boundingBox = boundingBox;
    }

    public Long getId() {
        return id;
    }

    public String getPersonLabel() {
        return personLabel;
    }

    public String getBoundingBox() {
        return boundingBox;
    }

    public List<Picture> getPictures() {
        return pictures;
    }
}
