package at.airpx.workloads.picture_tag;

import at.airpx.workloads.picture.Picture;
import at.airpx.workloads.tag.Tag;
import jakarta.persistence.*;

@Entity
public class PictureTag {
    @EmbeddedId
    private PictureTagId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("pictureId")
    @JoinColumn(name = "picture_id", nullable = false)
    private Picture picture;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public PictureTag() {
    }

    public PictureTag(PictureTagId id) {
        this.id = id;
    }

    public Picture getPicture() {
        return picture;
    }

    public void setPicture(Picture picture) {
        this.picture = picture;
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public PictureTagId getId() {
        return id;
    }

    public void setId(PictureTagId id) {
        this.id = id;
    }
}
