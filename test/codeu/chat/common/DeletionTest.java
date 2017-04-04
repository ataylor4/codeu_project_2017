package codeu.chat.common;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;


import codeu.chat.server.Controller;
import codeu.chat.server.Model;

public final class DeletionTest {

    private Model model;
    private BasicController controller;

    @Before
    public void doBefore() {
        model = new Model();
        controller = new Controller(Uuids.NULL, model);
    }

    @Test
    public void testUserDeletion() {
        final User user = controller.newUser("user");
        final Map<Uuid, User> usersById = new HashMap<>();
        usersById.put(user.id, user);
        usersById.remove(user.id);
        assertFalse(usersById.containsKey(user.id));
    }

    @Test
    public void testConversationDeletion() {
        final User user = controller.newUser("user");
        final Conversation conversation = controller.newConversation(
                "conversation",
                user.id);
        final Map<Uuid, ConversationSummary> summariesByUuid = new HashMap<>();
        ConversationSummary cs = conversation.summary;
        summariesByUuid.put(cs.id, cs);
        summariesByUuid.remove(cs.id);
        assertFalse(summariesByUuid.containsKey(cs.id));
    }

    @Test
    public void testMessageDeletion() {
        final User user = controller.newUser("user");
        final Conversation conversation = controller.newConversation(
                "conversation",
                user.id);
        final Message message = controller.newMessage(
                user.id,
                conversation.id,
                "Hello World");
        final List<Message> conversationContents = new ArrayList<>();

        conversationContents.add(message);
        conversationContents.remove(0);
        int sizeAfterRemove = conversationContents.size();

        assertFalse(conversationContents.contains(message));
        assertEquals(sizeAfterRemove, 0);
    }



}
