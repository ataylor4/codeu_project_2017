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

import java.util.Comparator;

import codeu.chat.common.Conversation;
import codeu.chat.common.LinearUuidGenerator;
import codeu.chat.common.Message;
import codeu.chat.common.Time;
import codeu.chat.common.User;
import codeu.chat.common.Uuid;
import codeu.chat.util.store.BTreeStore;
import codeu.chat.util.store.StoreAccessor;

public final class Model {

  private static final Comparator<Uuid> UUID_COMPARE = new Comparator<Uuid>() {

    @Override
    public int compare(Uuid a, Uuid b) {

      if (a == b) { return 0; }

      if (a == null && b != null) { return -1; }

      if (a != null && b == null) { return 1; }

      final int order = Integer.compare(a.id(), b.id());
      return order == 0 ? compare(a.root(), b.root()) : order;
    }
  };

  private static final Comparator<Time> TIME_COMPARE = new Comparator<Time>() {
    @Override
    public int compare(Time a, Time b) {
      return a.compareTo(b);
    }
  };

  private static final Comparator<String> STRING_COMPARE = String.CASE_INSENSITIVE_ORDER;

  private BTreeStore<Uuid, User> userById = new BTreeStore<>(BTreeStore.NUM_POINTERS, UUID_COMPARE);
  private BTreeStore<Time, User> userByTime = new BTreeStore<>(BTreeStore.NUM_POINTERS, TIME_COMPARE);
  private BTreeStore<String, User> userByText = new BTreeStore<>(BTreeStore.NUM_POINTERS, STRING_COMPARE);

  private BTreeStore<Uuid, Conversation> conversationById
      = new BTreeStore<>(BTreeStore.NUM_POINTERS, UUID_COMPARE);
  private BTreeStore<Time, Conversation> conversationByTime
      = new BTreeStore<>(BTreeStore.NUM_POINTERS, TIME_COMPARE);
  private BTreeStore<String, Conversation> conversationByText
      = new BTreeStore<>(BTreeStore.NUM_POINTERS, STRING_COMPARE);

  private BTreeStore<Uuid, Message> messageById = new BTreeStore<>(BTreeStore.NUM_POINTERS, UUID_COMPARE);
  private BTreeStore<Time, Message> messageByTime = new BTreeStore<>(BTreeStore.NUM_POINTERS, TIME_COMPARE);
  private BTreeStore<String, Message> messageByText = new BTreeStore<>(BTreeStore.NUM_POINTERS, STRING_COMPARE);

  private final Uuid.Generator userGenerations = new LinearUuidGenerator(null, 1, Integer.MAX_VALUE);
  private Uuid currentUserGeneration = userGenerations.make();

  public void add(User user) {
    currentUserGeneration = userGenerations.make();

    userById = userById.insert(user.id, user, true);
    userByTime = userByTime.insert(user.creation, user, true);
    userByText = userByText.insert(user.name, user, true);
  }

  public StoreAccessor<Uuid, User> userById() {
    return userById;
  }

  public StoreAccessor<Time, User> userByTime() {
    return userByTime;
  }

  public StoreAccessor<String, User> userByText() {
    return userByText;
  }

  public Uuid userGeneration() {
    return currentUserGeneration;
  }

  public void add(Conversation conversation) {
    conversationById = conversationById.insert(conversation.id, conversation, false);
    conversationByTime = conversationByTime.insert(conversation.creation, conversation, true);
    conversationByText = conversationByText.insert(conversation.title, conversation, true);
  }

  public StoreAccessor<Uuid, Conversation> conversationById() {
    return conversationById;
  }

  public StoreAccessor<Time, Conversation> conversationByTime() {
    return conversationByTime;
  }

  public StoreAccessor<String, Conversation> conversationByText() {
    return conversationByText;
  }

  public void add(Message message) {
    messageById = messageById.insert(message.id, message, false);
    messageByTime = messageByTime.insert(message.creation, message, true);
    messageByText = messageByText.insert(message.content, message, true);
  }

  // attempt to delete message
  /*public void remove(Message message) {
    messageById.remove(message.id);
    messageByTime.remove(message.creation);
    messageByText.remove(message.content);
  }*/

  public StoreAccessor<Uuid, Message> messageById() {
    return messageById;
  }

  public StoreAccessor<Time, Message> messageByTime() {
    return messageByTime;
  }

  public StoreAccessor<String, Message> messageByText() {
    return messageByText;
  }
}
