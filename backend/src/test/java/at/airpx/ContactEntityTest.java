package at.airpx;

import at.airpx.workloads.account.Account;
import at.airpx.workloads.contact.Contact;
import at.airpx.workloads.contact.ContactId;
import at.airpx.workloads.contact.ContactStatus;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
class ContactEntityTest {
    @Inject
    EntityManager entityManager;

    @Test
    @TestTransaction
    void persistsContactAndMapsItToBothAccounts() {
        Account sender = createAccount();
        Account recipient = createAccount();
        entityManager.persist(sender);
        entityManager.persist(recipient);
        entityManager.flush();

        Contact contact = new Contact(sender, recipient);
        entityManager.persist(contact);
        entityManager.flush();
        ContactId id = contact.getId();

        assertEquals(ContactStatus.PENDING, contact.getStatus());

        contact.setStatus(ContactStatus.ACCEPTED);
        entityManager.flush();
        entityManager.clear();

        Contact loaded = entityManager.find(Contact.class, id);
        assertNotNull(loaded);
        assertEquals(sender.getId(), loaded.getAccount().getId());
        assertEquals(recipient.getId(), loaded.getContactAccount().getId());
        assertEquals(ContactStatus.ACCEPTED, loaded.getStatus());
        assertEquals(1, entityManager.find(Account.class, sender.getId()).getOutgoingContacts().size());
        assertEquals(1, entityManager.find(Account.class, recipient.getId()).getIncomingContacts().size());
    }

    @Test
    @TestTransaction
    void rejectsAnAccountAsItsOwnContact() {
        Account account = createAccount();
        entityManager.persist(account);
        entityManager.flush();

        assertThrows(IllegalArgumentException.class, () -> new Contact(account, account));
    }

    @Test
    @TestTransaction
    void compositeIdPreventsDuplicateDirectedContact() {
        Account sender = createAccount();
        Account recipient = createAccount();
        entityManager.persist(sender);
        entityManager.persist(recipient);
        entityManager.flush();

        entityManager.persist(new Contact(sender, recipient));
        entityManager.flush();
        entityManager.clear();

        Account reloadedSender = entityManager.find(Account.class, sender.getId());
        Account reloadedRecipient = entityManager.find(Account.class, recipient.getId());
        entityManager.persist(new Contact(reloadedSender, reloadedRecipient));

        assertThrows(PersistenceException.class, entityManager::flush);
    }

    private Account createAccount() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        return new Account("contact_" + suffix, suffix + "@example.com", "test-hash");
    }
}
