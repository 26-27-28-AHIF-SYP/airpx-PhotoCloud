package at.airpx.workloads.tag;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import java.util.List;

@ApplicationScoped
public class TagRepositoryImpl implements TagRepository {
    private final EntityManager em;

    public TagRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<Tag> findAll(Long accountId) {
        return em.createQuery("""
                        select t from Tag t
                        inner join t.accountTags at
                        inner join at.account a
                        where a.id = :accountId
                        order by t.tagName
                        """, Tag.class)
                .setParameter("accountId", accountId)
                .getResultList();
    }
}
