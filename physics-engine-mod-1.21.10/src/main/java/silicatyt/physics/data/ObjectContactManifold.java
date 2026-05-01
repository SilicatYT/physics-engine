package silicatyt.physics.data;

import silicatyt.physics.entity.PhysicsObject;

import java.util.LinkedList;
import java.util.List;

public class ObjectContactManifold implements ContactManifold {
    private PhysicsObject objectA;
    private PhysicsObject objectB;
    private final List<Contact> contacts = new LinkedList<>();

    public ObjectContactManifold(PhysicsObject objectA, PhysicsObject objectB) {
        this.objectA = objectA;
        this.objectB = objectB;
    }

    @Override
    public void updateWithContact(Contact newContact) {

    }

    @Override
    public void updateWithoutContact() {

    }

    @Override
    public List<Contact> getContacts() {
        return null;
    }
}



// TODO