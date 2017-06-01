// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package codeu.chat.server;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;
import org.junit.Before;

import codeu.chat.common.Conversation;
import codeu.chat.common.Message;
import codeu.chat.common.RawController;
import codeu.chat.common.Time;
import codeu.chat.common.User;
import codeu.chat.common.Uuid;
import codeu.chat.common.Uuids;

import java.io.File;

public final class RawControllerTest {

  private Model model;
  private RawController controller;
  private View view;

  private Uuid userId;
  private Uuid conversationId;
  private Uuid messageId;

  @Before
  public void doBefore() {
    model = new Model();
    controller = new Controller(Uuids.NULL, model);
    view = new View(model);

    userId = newTestId(1);
    conversationId = newTestId(2);
    messageId = newTestId(3);
  }

  @After
  public void cleanUp() {
    String[] filenames = {"Model_StringConversation.log", "Model_StringMessage.log", "Model_StringUser.log",
        "Model_TimeConversation.log", "Model_TimeMessage.log", "Model_TimeUser.log",
        "Model_UuidConversation.log", "Model_UuidMessage.log", "Model_UuidUser.log"};
    for (String filename : filenames) {
      File toDelete = new File(filename);
      toDelete.delete();
    }
  }

  @Test
  public void testAddUser() {

    final User user = controller.newUser(userId, "user", Time.now(), "p1$p2$p3");

    assertFalse(
        "Check that user has a valid reference",
        user == null);
    assertTrue(
        "Check that the user has the correct id",
        Uuids.equals(user.id, userId));
  }

  @Test
  public void testRemoveUser() {

    final User user = controller.newUser(userId, "user", Time.now(), null);

    assertFalse(
            "Check that user has a valid reference",
            user == null);
    assertTrue(
            "Check that the user has the correct id",
            Uuids.equals(user.id, userId));

    controller.removeUser(user);
    assertFalse(view.findUser(user.id) != null);
  }

  @Test
  public void testAddConversation() {

    final User user = controller.newUser(userId, "user", Time.now(), "p1$p2$p3");

    assertFalse(
        "Check that user has a valid reference",
        user == null);
    assertTrue(
        "Check that the user has the correct id",
        Uuids.equals(user.id, userId));

    final Conversation conversation = controller.newConversation(
        conversationId,
        "conversation",
        user.id,
        Time.now());

    assertFalse(
        "Check that conversation has a valid reference",
        conversation == null);
    assertTrue(
        "Check that the conversation has the correct id",
        Uuids.equals(conversation.id, conversationId));
  }

  @Test
  public void testRemoveConversation() {

    final User user = controller.newUser(userId, "user", Time.now(), null);

    assertFalse(
            "Check that user has a valid reference",
            user == null);
    assertTrue(
            "Check that the user has the correct id",
            Uuids.equals(user.id, userId));

    final Conversation conversation = controller.newConversation(
            conversationId,
            "conversation",
            user.id,
            Time.now());

    assertFalse(
            "Check that conversation has a valid reference",
            conversation == null);
    assertTrue(
            "Check that the conversation has the correct id",
            Uuids.equals(conversation.id, conversationId));

    controller.removeConversation(conversation);
    assertFalse(view.findConversation(conversation.id) != null);
  }

  @Test
  public void testAddMessage() {

    final User user = controller.newUser(userId, "user", Time.now(), "p1$p2$p3");

    assertFalse(
        "Check that user has a valid reference",
        user == null);
    assertTrue(
        "Check that the user has the correct id",
        Uuids.equals(user.id, userId));

    final Conversation conversation = controller.newConversation(
        conversationId,
        "conversation",
        user.id,
        Time.now());

    assertFalse(
        "Check that conversation has a valid reference",
        conversation == null);
    assertTrue(
        "Check that the conversation has the correct id",
        Uuids.equals(conversation.id, conversationId));

    final Message message = controller.newMessage(
        messageId,
        user.id,
        conversation.id,
        "Hello World",
        Time.now());

    assertFalse(
        "Check that the message has a valid reference",
        message == null);
    assertTrue(
        "Check that the message has the correct id",
        Uuids.equals(message.id, messageId));
  }

  @Test
  public void testRemoveMessage() {

    final User user = controller.newUser(userId, "user", Time.now(), null);

    assertFalse(
            "Check that user has a valid reference",
            user == null);
    assertTrue(
            "Check that the user has the correct id",
            Uuids.equals(user.id, userId));

    final Conversation conversation = controller.newConversation(
            conversationId,
            "conversation",
            user.id,
            Time.now());

    assertFalse(
            "Check that conversation has a valid reference",
            conversation == null);
    assertTrue(
            "Check that the conversation has the correct id",
            Uuids.equals(conversation.id, conversationId));

    final Message message = controller.newMessage(
            messageId,
            user.id,
            conversation.id,
            "Hello World",
            Time.now());

    assertFalse(
            "Check that the message has a valid reference",
            message == null);
    assertTrue(
            "Check that the message has the correct id",
            Uuids.equals(message.id, messageId));

    controller.removeMessage(message, conversation.id);
    assertFalse(view.findMessage(message.id) != null);
  }

  private static Uuid newTestId(final int id) {
    return Uuids.complete(new Uuid() {
      @Override
      public Uuid root() { return null; }
      @Override
      public int id() { return id; }
    });
  }
}
