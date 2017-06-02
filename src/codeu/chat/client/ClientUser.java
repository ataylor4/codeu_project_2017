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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.lang.StringBuilder;

import codeu.chat.common.User;
import codeu.chat.client.ClientConversation;
import codeu.chat.client.ClientContext;
import codeu.chat.common.Uuid;
import codeu.chat.common.Uuids;
import codeu.chat.util.Logger;
import codeu.chat.util.Serializers;
import codeu.chat.util.store.BTreeStore;
import java.util.Iterator;
import codeu.chat.util.store.BTreeStore;
import codeu.chat.util.store.BTreeIterator;
import codeu.chat.common.ConversationSummary;
//import codeu.chat.client.Password;

public final class ClientUser {

  private final static Logger.Log LOG = Logger.newLog(ClientUser.class);

  private static final String STORE_FILENAME = "ClientUser_StringUser.log";

  private static final Collection<Uuid> EMPTY = Arrays.asList(new Uuid[0]);
  private final Controller controller;
  private final View view;

  private User current = null;

  public static final Map<Uuid, User> usersById = new HashMap<>();

  // This is the set of users known to the server, sorted by name.
  public static BTreeStore<String, User> usersByName
          = new BTreeStore<>(BTreeStore.NUM_POINTERS, String.CASE_INSENSITIVE_ORDER, Serializers.STRING, User.SERIALIZER,
          STORE_FILENAME);

  public ClientUser(Controller controller, View view) {
    this.controller = controller;
    this.view = view;
  }

  // Validate the username string
  static public boolean isValidName(String userName) {
    boolean clean = true;
    if (userName.length() == 0) {
      clean = false;
    } else if (usersByName.first(userName) != null) {
      clean = false;
    } else {

      clean=userName.matches("[A-Za-z0-9_ @]+");

    }
    return clean;
  }

  public boolean hasCurrent() {
    return (current != null);
  }

  public User getCurrent() {
    return current;
  }

  public boolean signInUser(String name, int mode) { //mode 0 is signing in via commandline while mode 1 is signing in via GUI
    updateUsers();

    final User prev = current;
    final User temp = usersByName.first(name);
    if(name!=null) {
      if (mode == 1 || (Password.authenticateUserCommandline(name, temp) && mode == 0)) {
        final User newCurrent = usersByName.first(name);
        if (newCurrent != null) {
          current = newCurrent;
        }
      }
    }
    return (prev != current);
  }

  public boolean signOutUser() {
    boolean hadCurrent = hasCurrent();
    current = null;
    return hadCurrent;
  }

  public void showCurrent() {
    printUser(current);
  }

  public void  addUser(String name, String password) {
    final boolean validInputs = isValidName(name);
    final User user = (validInputs) ? controller.newUser(name, Password.createPassword(name, password)) : null;

    if (user == null) {
      System.out.format("Error: user not created - %s.\n",
              (validInputs) ? "server failure" : "bad input value");
    } else {
      LOG.info("New user complete, Name= \"%s\" UUID=%s", user.name, user.id, user.security);
      updateUsers();
    }
  }

  // For u-remove command.
  public void removeUser(String name) {

    User user = usersByName.first(name);
    controller.removeUser(user);
    usersById.remove(user.id);
    usersByName.delete(name);
    updateUsers();
    System.out.format("User removed, Name= \"%s\"\n", name);
    LOG.info("User removed, Name= \"%s\"\n", name);

  }

  public void showAllUsers() {
    updateUsers();
    for (final User u : usersByName.all()) {
      printUser(u);
    }
  }

  public User lookup(Uuid id) {
    return (usersById.containsKey(id)) ? usersById.get(id) : null;
  }

  public String getName(Uuid id) {
    final User user = lookup(id);
    if (user == null) {
      LOG.warning("userContext.lookup() failed on ID: %s", id);
      return null;
    } else {
      return user.name;
    }
  }

  public Iterable<User> getUsers() {
    return usersByName.all();
  }

  public void updateUsers() {
    usersById.clear();
    usersByName.clear(STORE_FILENAME);

    for (final User user : view.getUsersExcluding(EMPTY)) {
      usersById.put(user.id, user);
      usersByName = usersByName.insert(user.name, user, true);
    }
  }

  public static String getUserInfoString(User user) {
    return (user == null) ? "Null user" :
            String.format(" User: %s\n   Id: %s\n   created: %s\n", user.name, user.id, user.creation);
  }

  public String showUserInfo(String uname) {
    return getUserInfoString(usersByName.first(uname));
  }

  // Move to User's toString()
  public static void printUser(User user) {
    System.out.println(getUserInfoString(user));
  }

  public  String searchUser(String name){
    updateUsers();
    StringBuilder sb=new StringBuilder();
    User user=usersByName.first(name);
    if(user==null) {
      System.out.format("%s does not exist \n", name);
      sb.append(name+ " does not exist\n");
    }
    else{
      System.out.println(getUserInfoString(user));
      System.out.println("Conversations:");
      sb.append("Conversations for ["+name+"]:\n");
      ClientConversation conversation=new ClientConversation(controller, view, null);//not the best implementation
      conversation.updateAllConversations(false);
      BTreeIterator<String, ConversationSummary> conversations = ClientConversation.summariesSortedByTitle.all().iterator();
      while(conversations.hasNext()){
        ConversationSummary summary=conversations.next();
        if(Uuids.equals(summary.owner, user.id))  sb.append(ClientConversation.printConversationFriendly(summary));
      }
    }
    return sb.toString();
  }
}

