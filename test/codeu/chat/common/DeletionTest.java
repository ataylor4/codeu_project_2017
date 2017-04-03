package codeu.chat.common;

import java.util.HashMap;
import java.util.Map;
import codeu.chat.common.User;
import codeu.chat.common.Uuid;

import org.junit.Test;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public final class DeletionTest {

    @Test
    public void testUserDeletion() {

        final Map<Uuid, User> usersById = new HashMap<>();
        final User user = (controller.newUser("alison");

        usersById.put(user.id, user);
        usersById.remove(user.id);
        assertNull(usersById.contains(user.id));

    }

    @Test
    public void testConversationDeletion() {

    }

    @Test
    public void testMessageDeletion() {

    }


}
