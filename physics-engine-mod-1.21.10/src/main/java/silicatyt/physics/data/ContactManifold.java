package silicatyt.physics.data;

import java.util.List;

public interface ContactManifold { // Contains all contacts for a given contact key (i.e., object pair, or object + blockPos)
    void updateWithContact(Contact newContact);
    void updateWithoutContact();
    List<Contact> getContacts();
}
