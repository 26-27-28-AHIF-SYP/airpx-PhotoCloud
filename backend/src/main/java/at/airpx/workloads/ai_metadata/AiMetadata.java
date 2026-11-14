package at.airpx.workloads.ai_metadata;

import at.airpx.workloads.picture.Picture;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ai_metadata")
public class AiMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "detected_text", length = 200)
    private String detectedText;

    @Column(length = 100)
    private String category;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @ManyToMany
    @JoinTable(name = "picture_metadata",
            joinColumns = @JoinColumn(name = "ai_metadata_id"),
            inverseJoinColumns = @JoinColumn(name = "picture_id"))
    private List<Picture> pictures = new ArrayList<>();

    protected AiMetadata() { }

    public AiMetadata(String detectedText, String category) {
        this.detectedText = detectedText;
        this.category = category;
    }

    @PrePersist
    void initializeProcessedAt() {
        if (processedAt == null) processedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getDetectedText() { return detectedText; }
    public String getCategory() { return category; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public List<Picture> getPictures() { return pictures; }
}
