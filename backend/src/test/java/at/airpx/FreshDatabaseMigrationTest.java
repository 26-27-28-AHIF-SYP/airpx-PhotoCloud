package at.airpx;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(FlywayMigrationTestProfile.class)
class FreshDatabaseMigrationTest {
    @Inject
    EntityManager entityManager;

    @Test
    void flywayCreatesTheSchemaForAllPersistedEntities() {
        Number tableCount = (Number) entityManager.createNativeQuery("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (
                    'account', 'album', 'album_pictures', 'album_permission', 'album_share_link',
                    'picture', 'picture_tag', 'picture_metadata', 'picture_face_recognitions',
                    'tag', 'account_tag', 'contact', 'ai_metadata', 'face_recognition'
                  )
                """).getSingleResult();

        assertEquals(14L, tableCount.longValue());
    }
}
