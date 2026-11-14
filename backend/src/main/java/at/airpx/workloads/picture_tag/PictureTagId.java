package at.airpx.workloads.picture_tag;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PictureTagId implements Serializable {
    private Long pictureId;
    private Integer tagId;

    public PictureTagId() {}

    public PictureTagId(Long pictureId, Integer tagId) {
        this.pictureId = pictureId;
        this.tagId = tagId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PictureTagId that)) return false;
        return Objects.equals(pictureId, that.pictureId) && Objects.equals(tagId, that.tagId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pictureId, tagId);
    }

    public Long getPictureId() {
        return pictureId;
    }

    public void setPictureId(Long pictureId) {
        this.pictureId = pictureId;
    }

    public Integer getTagId() {
        return tagId;
    }

    public void setTagId(Integer tagId) {
        this.tagId = tagId;
    }
}