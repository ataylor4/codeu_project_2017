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

package codeu.chat.client.simplegui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import java.net.URL;
import javax.sound.sampled.*;


import codeu.chat.client.ClientContext;
import codeu.chat.client.Password;
import codeu.chat.client.Controller;
import codeu.chat.client.View;
import codeu.chat.util.Logger;
import codeu.chat.client.Password;
import codeu.chat.client.ClientUser;

// Chat - top-level client application - Java Simple GUI (using Java Swing)
public final class ChatSimpleGui {

  private final static Logger.Log LOG = Logger.newLog(ChatSimpleGui.class);

  private JFrame mainFrame;

  private final ClientContext clientContext;
  private UserPanel usersViewPanel;
  private MessagePanel messagesViewPanel;
  private JPanel conversationsViewPanel;
  private GridBagConstraints simpleloginC;
  private GridBagConstraints usersViewC;
  private GridBagConstraints messagesViewC;
  private GridBagConstraints conversationViewC;
  private SearchPanel searchPanel;
  private GridBagConstraints searchPanelC;

  // Constructor - sets up the Chat Application
  public ChatSimpleGui(Controller controller, View view) {
    clientContext = new ClientContext(controller, view);
  }

  // Run the GUI client
  public void run() {

    try {

      initialize();
      mainFrame.setVisible(true);

    } catch (Exception ex) {
      System.out.println("ERROR: Exception in ChatSimpleGui.run. Check log for details.");
      LOG.error(ex, "Exception in ChatSimpleGui.run");
      System.exit(1);
    }
  }

  private Border paneBorder() {
    Border outside = BorderFactory.createLineBorder(Color.LIGHT_GRAY);
    Border inside = BorderFactory.createEmptyBorder(8, 8, 8, 8);
    return BorderFactory.createCompoundBorder(outside, inside);
  }

  // Initialize the GUI
  private void initialize() {

    usersViewPanel = new UserPanel(clientContext);
    messagesViewPanel = new MessagePanel(clientContext);
    conversationsViewPanel = new ConversationPanel(clientContext, messagesViewPanel);

    // Outermost frame.
    // NOTE: may have tweak size, or place in scrollable panel.
    mainFrame = new JFrame("U-Message");
    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    mainFrame.setSize(790, 450);

    // Main View - outermost graphics panel.
    final JPanel mainViewPanel = new JPanel(new GridBagLayout());
    mainViewPanel.setBorder(paneBorder());

    // Build main panels - Users, Conversations, Messages.
    //usersViewPanel = new UserPanel(clientContext);
    final LoginScreen simplelogin=new LoginScreen();
    simplelogin.setBorder(paneBorder());
    simpleloginC=new GridBagConstraints();
    mainViewPanel.add(simplelogin, simpleloginC);
    mainFrame.add(mainViewPanel);
    mainFrame.pack();

    simplelogin.loginButton.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e){
        String username = simplelogin.textField.getText();
        String password = String.valueOf(simplelogin.pwdField.getPassword());
        if (Password.authenticateUserGUI(username, password)) { //Authenticate
          clientContext.user.signInUser(username, 1);
          playSound("welcome.wav");
          prepareChatPlatform();
          simplelogin.panel.setVisible(false);
          mainViewPanel.removeAll();
          mainViewPanel.revalidate();
          mainViewPanel.repaint();
          mainViewPanel.add(usersViewPanel, usersViewC);
          mainViewPanel.add(conversationsViewPanel, conversationViewC);
          mainViewPanel.add(messagesViewPanel, messagesViewC);
          mainViewPanel.add(searchPanel, searchPanelC);

          mainFrame.add(mainViewPanel);
          usersViewPanel.userSignedInLabel.setText("Hello "+ username +"!");
          mainFrame.pack();

        }
        else{
          simplelogin.infoLabel.setText("Incorrect Username or Password");
          simplelogin.infoLabel.setFont(new Font("DialogInput", Font.PLAIN, 14));
        }
      }
    });

    simplelogin.signupButton.addActionListener(new ActionListener(){
      @Override
      public void actionPerformed(ActionEvent e){

        simplelogin.infoLabel.setText("An Amazing Chatting Experience");
        simplelogin.username.setText("Enter username:");
        simplelogin.pwdLabel.setText("Enter Password:");

        simplelogin.panel.remove(simplelogin.buttonPanel);
        simplelogin.panel.add(simplelogin.pwdConfirmLabel, simplelogin.pwdConfirmLabelC);
        simplelogin.panel.add(simplelogin.pwdConfirm, simplelogin.pwdConfirmC);

        simplelogin.panel.add(simplelogin.securityQnLabel, simplelogin.securityQnLabelC);
        simplelogin.panel.add(simplelogin.securityAnswerLabel, simplelogin.securityAnswerLabelC);
        simplelogin.panel.add(simplelogin.securityAnswer, simplelogin.securityAnswerC);
        simplelogin.buttonPanelC.gridy=6;
        simplelogin.panel.add(simplelogin.buttonPanel, simplelogin.buttonPanelC);

        simplelogin.buttonPanel.removeAll();

        JButton okayButton=new JButton("OK");
        JButton cancelButton=new JButton("Clear");
        JButton exitButton=new JButton("Exit");

        String[] choices = {"What is the name of your elementary school?",
                "What is the name of your pet?",
                "Which city did you meet your spouse?"
        };

        JComboBox<String> questionList = new JComboBox<>(choices);
        questionList.setMinimumSize(new Dimension(250,20));
        questionList.setPreferredSize(new Dimension(450,20));
        simplelogin.panel.add(questionList, simplelogin.securityQnC);

        simplelogin.buttonPanel.add(okayButton);
        simplelogin.buttonPanel.add(cancelButton);
        simplelogin.buttonPanel.add(exitButton);

        cancelButton.addActionListener(new ActionListener(){
          @Override
          public void actionPerformed(ActionEvent e){
            simplelogin.textField.setText("");
            simplelogin.securityAnswer.setText("");
            simplelogin.pwdField.setText("");
            simplelogin.pwdConfirm.setText("");
            questionList.setSelectedIndex(0);
          }
        });
        exitButton.addActionListener(new ActionListener(){
          @Override
          public void actionPerformed(ActionEvent e){
            java.lang.System.exit(0);
          }
        });

        okayButton.addActionListener(new ActionListener(){
          @Override
          public void actionPerformed(ActionEvent e){
            String userName = simplelogin.textField.getText();
            String question=(String) questionList.getSelectedItem();
            String answer = simplelogin.securityAnswer.getText();
            String pass_one = String.valueOf(simplelogin.pwdField.getPassword());
            String pass_two = String.valueOf(simplelogin.pwdConfirm.getPassword());
            if(userName == null || (userName.length() ==0)) simplelogin.infoLabel.setText("Invalid Username!");
            else if(!ClientUser.isValidName(userName)) simplelogin.infoLabel.setText("UserName exists!");
            else if (answer==null || answer.length()==0) simplelogin.infoLabel.setText("Invalid Answer!");
            else if(!pass_one.equals(pass_two)) simplelogin.infoLabel.setText("Passwords don't match!");
            else  {
              String securityDetails=pass_one + "$" + question + "$" + answer;
              clientContext.user.addUser(userName, securityDetails);
              clientContext.user.signInUser(userName, 1);
              usersViewPanel.userSignedInLabel.setText("Welcome "+ userName +" " + "Password strength: "+Password.passwordStrength(pass_one));

              prepareChatPlatform();

              simplelogin.panel.setVisible(false);
              mainViewPanel.removeAll();
              mainViewPanel.revalidate();
              mainViewPanel.repaint();

              mainViewPanel.add(usersViewPanel, usersViewC);
              mainViewPanel.add(conversationsViewPanel, conversationViewC);
              mainViewPanel.add(messagesViewPanel, messagesViewC);
              mainViewPanel.add(searchPanel, searchPanelC);

              mainFrame.add(mainViewPanel);
              mainFrame.pack();
            }
          }
        });
      }
    });

  }

  public static void playSound(String song){
    InputStream in;
    Clip clip=null;
    try{
      File myFile=new File(System.getProperty("user.dir") +"/../res/audio/"+song);
      URL myUrl = myFile.toURI().toURL();
      AudioInputStream stream = AudioSystem.getAudioInputStream(myUrl);
      AudioFormat format = stream.getFormat();
      clip = AudioSystem.getClip();
      clip.open(stream);
      clip.start();
    }
    catch (UnsupportedAudioFileException e) {
      e.printStackTrace();
    }
    catch (LineUnavailableException e) {
      e.printStackTrace();
    }
    catch(Exception e){
      System.out.println("Could not fetch Audio");
      JOptionPane.showMessageDialog(null, e);

    }
  }

  private void prepareChatPlatform(){
    usersViewPanel.setBorder(paneBorder());
    usersViewC = new GridBagConstraints();

    messagesViewPanel = new MessagePanel(clientContext);
    messagesViewPanel.setBorder(paneBorder());
    messagesViewC = new GridBagConstraints();

    // ConversationsPanel gets access to MessagesPanel
    conversationsViewPanel = new ConversationPanel(clientContext, messagesViewPanel);
    conversationsViewPanel.setBorder(paneBorder());
    conversationViewC = new GridBagConstraints();

    searchPanel=new SearchPanel(clientContext);
    searchPanel.setBorder(paneBorder());
    searchPanelC=new GridBagConstraints();

    // Placement of main panels.
    usersViewC.gridx = 0;
    usersViewC.gridy = 0;
    usersViewC.gridwidth = 1;
    usersViewC.gridheight = 1;
    usersViewC.fill = GridBagConstraints.BOTH;
    usersViewC.weightx = 0.3;
    usersViewC.weighty = 0.3;

    simpleloginC.gridx=0;
    simpleloginC.gridy=0;
    simpleloginC.gridwidth=7;
    simpleloginC.gridheight=7;
    simpleloginC.fill = GridBagConstraints.BOTH;

    conversationViewC.gridx = 1;
    conversationViewC.gridy = 0;
    conversationViewC.gridwidth = 1;
    conversationViewC.gridheight = 1;
    conversationViewC.fill = GridBagConstraints.BOTH;
    conversationViewC.weightx = 0.7;
    conversationViewC.weighty = 0.3;

    messagesViewC.gridx = 0;
    messagesViewC.gridy = 1;
    messagesViewC.gridwidth = 1;//2
    messagesViewC.gridheight = 1;
    messagesViewC.fill = GridBagConstraints.BOTH;
    messagesViewC.weighty = 0.7;

    searchPanelC.gridx=1;
    searchPanelC.gridy=1;
    searchPanelC.gridwidth=1;
    searchPanelC.gridheight=1;
    searchPanelC.fill = GridBagConstraints.BOTH;
    searchPanelC.weighty = 0.3;


  }
}
