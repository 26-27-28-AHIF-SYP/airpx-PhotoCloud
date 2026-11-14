package at.airpx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import at.airpx.workloads.account.Account;
import at.airpx.workloads.ai_metadata.AiMetadata;
import at.airpx.workloads.album.Album;
import at.airpx.workloads.album.AlbumPermission;
import at.airpx.workloads.album.AlbumShareLink;
import at.airpx.workloads.face_recognition.FaceRecognition;
import at.airpx.workloads.picture.Picture;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ErDiagramEntityTest {
    @Inject
    EntityManager entityManager;

    @Test
    @TestTransaction
    void persistsRemainingErDiagramRelationships() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        Account owner = new Account("model_" + suffix, suffix + "@example.com", "test-hash");
        Account collaborator = new Account("collab_" + suffix, "c" + suffix + "@example.com", "test-hash");
        entityManager.persist(owner);
        entityManager.persist(collaborator);

        Album album = new Album();
        album.setName("Test album");
        album.setCreatedAt(LocalDateTime.now());
        album.setOwner(owner);
        entityManager.persist(album);
        entityManager.flush();

        Picture picture = new Picture(owner, "a".repeat(64), "test.jpg", null, 1, "image/jpeg", "storage-" + suffix);
        entityManager.persist(picture);
        entityManager.flush();

        album.addPicture(picture);
        entityManager.persist(new AlbumPermission(collaborator, album, true, false, false, false));
        entityManager.persist(new AlbumShareLink(album, "token-" + suffix, null, null));

        AiMetadata metadata = new AiMetadata("a caption", "landscape");
        metadata.getPictures().add(picture);
        entityManager.persist(metadata);
        FaceRecognition face = new FaceRecognition("person", "0,0,10,10");
        face.getPictures().add(picture);
        entityManager.persist(face);
        entityManager.flush();
        entityManager.clear();

        Album loadedAlbum = entityManager.find(Album.class, album.getId());
        assertNotNull(loadedAlbum);
        assertEquals(1, loadedAlbum.getPictures().size());
        assertEquals(1, loadedAlbum.getPermissions().size());
        assertEquals(1, loadedAlbum.getShareLinks().size());
        assertEquals(
                1,
                entityManager
                        .find(Picture.class, picture.getId())
                        .getAiMetadata()
                        .size());
        assertEquals(
                1,
                entityManager
                        .find(Picture.class, picture.getId())
                        .getFaceRecognitions()
                        .size());
    }
}
