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

package codeu.chat.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.*;

import codeu.chat.common.Conversation;
import codeu.chat.common.ConversationSummary;
import codeu.chat.common.Message;
import codeu.chat.common.Uuid;
import codeu.chat.common.Uuids;
import codeu.chat.util.Logger;
import codeu.chat.util.Method;
import codeu.chat.util.store.BTreeStore;
import codeu.chat.util.store.BTreeIterator;

public final class ClientMessage {

  private final static Logger.Log LOG = Logger.newLog(ClientMessage.class);

  private final static int MESSAGE_MAX_COUNT = 100;
  private final static int MESSAGE_FETCH_COUNT = 5;

  private final Controller controller;
  private final View view;

  private Message current = null;

  private final Map<Uuid, Message> messageByUuid = new HashMap<>();

  private Conversation conversationHead;
  private final List<Message> conversationContents = new ArrayList<>();

  private final ClientUser userContext;
  private final ClientConversation conversationContext;

  public ClientMessage(Controller controller, View view, ClientUser userContext,
                       ClientConversation conversationContext) {
    this.controller = controller;
    this.view = view;
    this.userContext = userContext;
    this.conversationContext = conversationContext;
    this.conversationContext.setMessageContext(this);
  }

  // Validate the message body.
  public static boolean isValidBody(String body) {
    boolean clean = true;
    if ((body.length() <= 0) || (body.length() > 1024)) {
      clean = false;
    } else {

      // TODO: check for invalid characters

    }
    return clean;
  }

  public boolean hasCurrent() {
    return (current != null);
  }

  public Message getCurrent() {
    return current;
  }

  public void showCurrent() {
    printMessage(current, userContext);
  }

  public void resetCurrent(boolean replaceAll) {
    updateMessages(replaceAll);
  }

  public int currentMessageCount() {
    return (conversationContents == null) ? 0 : conversationContents.size();
  }

  public List<Message> getConversationContents(ConversationSummary summary) {
    if (conversationHead == null || summary == null || !conversationHead.id.equals(summary.id)) {
      updateMessages(summary, true);
    }
    return conversationContents;
  }

  // For m-add command.
  public void addMessage(Uuid author, Uuid conversation, String body) {
    final boolean validInputs = isValidBody(body) && (author != null) && (conversation != null);

    final Message message = (validInputs) ? controller.newMessage(author, conversation, body) : null;

    if (message == null) {
      System.out.format("Error: message not created - %s.\n",
              (validInputs) ? "server error" : "bad input value");
    } else {
      LOG.info("New message:, Author= %s UUID= %s", author, message.id);
      current = message;
    }
    updateMessages(false);
  }

  // For m-remove command.
  public void removeMessage(String stringIndex) {
    int index = Integer.parseInt(stringIndex);

    if (index >= 0 && index < conversationContents.size()) {
      Message message = conversationContents.get(index);
      Uuid conversation = conversationContext.getCurrent().id;
      controller.removeMessage(message, conversation);
      conversationContents.remove(message);
      removeUpdates(message, index);
      updateMessages(conversationContext.getCurrent(), false);
      LOG.info("Message removed: Index= \"%s\"\n", stringIndex);
      System.out.format("Message removed: Index= \"%s\"\n", stringIndex);
    } else {
      LOG.info("Message removal failure: Invalid Index= \"%s\"\n", stringIndex);
      System.out.format("Message removal failure: Invalid Index= \"%s\"\n", stringIndex);
    }
  }

  private void removeUpdates(Message message, int index) {
    if (!message.previous.equals(Uuids.NULL)) {
      Message previousMessage = conversationContents.get(index - 1);
      previousMessage.next = message.next;
    } else {
      if (conversationContents.size() != 0 && !message.next.equals(Uuids.NULL)) {
        conversationContents.get(0).previous = Uuids.NULL;
        conversationHead.firstMessage = conversationContents.get(index).id;
        current = conversationContents.get(index);
        current.next = conversationContents.get(index).next;
      } else {
        current = null;
        conversationHead.firstMessage = Uuids.NULL;
        conversationHead.lastMessage = Uuids.NULL;
      }
    }

    if (!message.next.equals(Uuids.NULL)) {
      Message nextMessage = conversationContents.get(index + 1);
      nextMessage.previous = message.previous;
    } else {
      if (conversationContents.size() != 0) {
        if (index == 1) {
          current.next = Uuids.NULL;
        }
        conversationContents.get(index - 1).next = Uuids.NULL;
        conversationHead.lastMessage = conversationContents.get(index - 1).id;
      } else {
        current = null;
        conversationHead.firstMessage = Uuids.NULL;
        conversationHead.lastMessage = Uuids.NULL;
      }
    }
  }

  // For m-list-all command.
  // Show all messages attached to the current conversation. This will balk if the conversation
  // has too many messages (use m-next and m-show instead).
  public void showAllMessages() {
    if (conversationContents.size() == 0) {
      System.out.println(" Current Conversation has no messages");
    } else {
      for (final Message m : conversationContents) {
        printMessage(m, userContext);
      }
    }
  }

  // For m-next command.
  // Accept an index (within the current stream) that indicates the next message to show.
  // Message 1 is the head of the Conversation's message chain.
  // Message -1 is the tail of the Conversation's message chain.
  public void selectMessage(int index) {
    Method.notImplemented();
  }

  // Processing for m-show command.
  // Accept an int for number of messages to attempt to show (1 by default).
  // Negative values go from newest to oldest.
  public void showMessages(int count) {
    for (final Message m : conversationContents) {
      printMessage(m, userContext);
    }
  }

  private void showNextMessages(int count) {
    Method.notImplemented();
  }

  private void showPreviousMessages(int count) {
    Method.notImplemented();
  }

  // Determine the next message ID of the current conversation to start pulling.
  // This requires a read of the last read message to determine if the chain has been extended.
  private Uuid getCurrentMessageFetchId(boolean replaceAll) {
    if (replaceAll || conversationContents.isEmpty()) {
      // Fetch/refetch all the messages.
      conversationContents.clear();
      LOG.info("Refetch all messages: replaceAll=%s firstMessage=%s", replaceAll,
              conversationHead.firstMessage);
      return conversationHead.firstMessage;
    } else {
      // Locate last known message. Its next, if any, becomes our starting point.
      return getCurrentTailMessageId();
    }
  }

  private Uuid getCurrentTailMessageId() {
    Uuid nextMessageId = conversationContents.get(conversationContents.size() - 1).id;
    final List<Message> messageTail = new ArrayList<>(view.getMessages(nextMessageId, 1));
    if (messageTail.size() > 0) {
      final Message msg = messageTail.get(0);
      nextMessageId = msg.next;
    } else {
      // fall back.
      LOG.warning("Failed to get tail of messages, starting from %s", nextMessageId);
      conversationContents.clear();
      nextMessageId = conversationHead.firstMessage;
    }
    return nextMessageId;
  }

  // Update the list of messages for the current conversation.
  // Currently rereads the entire message chain.
  public void updateMessages(boolean replaceAll) {
    updateMessages(conversationContext.getCurrent(), replaceAll);
  }
  // Update the list of messages for the given conversation.
  // Currently rereads the entire message chain.
  public void updateMessages(ConversationSummary conversation, boolean replaceAll) {
    if (conversation == null) {
      LOG.error("conversation argument is null - do nothing.");
      return;
    }

    conversationHead = conversationContext.getConversation(conversation.id);
    if (conversationHead == null) {
      LOG.info("ConversationHead is null");
    } else {
      LOG.info("ConversationHead: Title=\"%s\" UUID=%s first=%s last=%s\n",
              conversationHead.title, conversationHead.id, conversationHead.firstMessage,
              conversationHead.lastMessage);
      Uuid nextMessageId = getCurrentMessageFetchId(replaceAll);

      //  Stay in loop until all messages read (up to safety limit)
      while (!nextMessageId.equals(Uuids.NULL) && conversationContents.size() < MESSAGE_MAX_COUNT) {

        for (final Message msg : view.getMessages(nextMessageId, MESSAGE_FETCH_COUNT)) {
          conversationContents.add(msg);
          // Race: message possibly added since conversation fetched.  If that occurs,
          // pretend the newer messages do not exist - they'll get picked up next time).
          if (msg.next.equals(Uuids.NULL) || msg.id.equals(conversationHead.lastMessage)) {
            msg.next = Uuids.NULL;
            break;
          }
        }
         if(!conversationContents.isEmpty()) nextMessageId = conversationContents.get(conversationContents.size() - 1).next;
        else break;
      }
      LOG.info("Retrieved %d messages for conversation %s (%s).\n",
              conversationContents.size(), conversationHead.id, conversationHead.title);
      // Set current to first message of conversation.
      current = (conversationContents.size() > 0) ? conversationContents.get(0) : null;
    }
  }

  // Print Message.  User context is used to map from author UUID to name.
  public static void printMessage(Message m, ClientUser userContext) {
    if (m == null) {
      System.out.println("Null message.");
    } else {

      // Display author name if available.  Otherwise display the author UUID.
      final String authorName = (userContext == null) ? null : userContext.getName(m.author);

      System.out.format(" Author: %s   Id: %s created: %s\n   Body: %s\n",
              (authorName == null) ? m.author : authorName, m.id, m.creation, m.content);
    }
  }

  public static String printMessageFriendly(ConversationSummary conversation, Message m, ClientUser userContext) {
    StringBuilder sb=new StringBuilder("");
    if (m == null) {
      System.out.println("Null message.");
      sb.append( "Null message.\n");
    } else {

      // Display author name if available.  Otherwise display the author UUID.
      final String authorName = (userContext == null) ? null : userContext.getName(m.author);
      String author= (authorName == null) ?  "Author[]" : "Author: ["+ authorName +"]  ";
      String conv= "Conversation:  [" +conversation.title+"]   ";
      String created= "Created  ["+ m.creation +"]\n";
      String info="Body: [" + m.content +"]\n";
      sb.append(author).append(conv).append(created).append(info);
      System.out.format(" Author: [%s]   Conversation:  [%s]  Created: [%s]\n   Body: %s\n",
              (authorName == null) ? "" : authorName, conversation.title,  m.creation, m.content);
    }
    return  sb.toString();
  }
  // Print Message outside of user context.
  public static void printMessage(Message m) {
    printMessage(m, null);
  }

  public String searchMessage(String words){
    StringBuilder sb=new StringBuilder("");
    if(words.equals("")) {
      System.out.println("Enter text to search");
      return "Enter text to search";
    }
    boolean found=false;
    ClientUser user = new ClientUser(controller, view);
    user.updateUsers();
    ClientConversation conversation=new ClientConversation(controller, view, null);//not the best implementation
    conversation.updateAllConversations(false);
    updateMessages(false);
    BTreeIterator<String, ConversationSummary> conversations = ClientConversation.summariesSortedByTitle.all().iterator();
    while(conversations.hasNext()) {
      ConversationSummary summary=conversations.next();
      if(summary==null) break;
      updateMessages(summary, true);
      for (Message message : conversationContents) {
        if (message.content.toLowerCase().contains(words.toLowerCase())) {
          found = true;
          sb.append(printMessageFriendly(summary, message, null));
        }
      }
    }
    if(!found){
      sb.append("Message notfound");
      System.out.println("Message not found");
    }
    return sb.toString();
  }
}
