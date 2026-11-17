package at.airpx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import at.airpx.workloads.account.Account;
import at.airpx.workloads.album.Album;
import at.airpx.workloads.picture.Picture;
import org.junit.jupiter.api.Test;

class AlbumEntityTest {
    @Test
    void addingPictureMaintainsBothSidesOfManyToManyRelationship() {
        Album album = new Album();
        Picture picture = new Picture(
                new Account("owner", "owner@example.com", "hash"),
                "a".repeat(64),
                "photo.jpg",
                null,
                1,
                "image/jpeg",
                "storage-key");

        album.addPicture(picture);
        album.addPicture(picture);

        assertEquals(1, album.getPictures().size());
        assertEquals(1, picture.getAlbums().size());
        assertEquals(album, picture.getAlbums().get(0));
    }
}
