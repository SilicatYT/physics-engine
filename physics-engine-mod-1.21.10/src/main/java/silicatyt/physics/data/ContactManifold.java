package silicatyt.physics.data;

import java.util.List;
import java.util.Set;

public interface ContactManifold { // Contains all contacts for a given contact key (i.e., object pair, or object + blockPos)
    void updateWithContacts(Set<Contact> newContact);
    boolean updateWithoutContacts();
    List<Contact> getContacts();
}
