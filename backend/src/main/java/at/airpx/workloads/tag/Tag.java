package at.airpx.workloads.tag;

import at.airpx.workloads.account_tag.AccountTag;
import at.airpx.workloads.picture_tag.PictureTag;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 50, nullable = false)
    private String tagName;

    @Column(length = 400, nullable = false)
    private String description;

    @OneToMany(mappedBy="tag")
    private List<PictureTag> pictureTags = new ArrayList<>();

    @OneToMany(mappedBy="tag")
    private List<AccountTag> accountTags = new ArrayList<>();

    public Tag() {
    }

    public Tag(String tagName, String description) {
        this.tagName = tagName;
        this.description = description;
    }

    public Integer getId() {
        return id;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<PictureTag> getPictureTags() {
        return pictureTags;
    }

    public void setPictureTags(List<PictureTag> pictureTags) {
        this.pictureTags = pictureTags;
    }

    public List<AccountTag> getAccountTags() {
        return accountTags;
    }

    public void setAccountTags(List<AccountTag> accountTags) {
        this.accountTags = accountTags;
    }
}
