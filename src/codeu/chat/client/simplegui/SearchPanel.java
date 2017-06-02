package codeu.chat.client.simplegui;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import codeu.chat.client.ClientContext;
import codeu.chat.client.Password;
import codeu.chat.client.Controller;
import codeu.chat.client.View;
import codeu.chat.util.Logger;
import codeu.chat.client.Password;
import codeu.chat.client.ClientUser;
import codeu.chat.client.ClientMessage;
import codeu.chat.client.ClientConversation;

@SuppressWarnings("serial")

public final class SearchPanel extends JPanel{
    private final ClientContext clientContext;
    public SearchPanel(ClientContext clientContext){
        super(new GridBagLayout());
        this.clientContext = clientContext;
        initialize();
    }

    private void initialize(){
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints panelC = new GridBagConstraints();

        final JLabel titleLabel = new JLabel("Enter to Search", JLabel.LEFT);
        final GridBagConstraints titleLabelC = new GridBagConstraints();


        final JTextArea infoPanel = new JTextArea();
        //infoPanel.setBounds(200,200,400,300);
        final JScrollPane infoScrollPane = new JScrollPane(infoPanel);
        infoScrollPane.setPreferredSize(new Dimension(450, 250));
        final GridBagConstraints infoScrollPaneC = new GridBagConstraints();

        final JTextField query=new JTextField("");
        query.setColumns(16);
        final GridBagConstraints queryC = new GridBagConstraints();

        final JPanel buttonPanel = new JPanel();
        final GridBagConstraints buttonPanelC = new GridBagConstraints();

        JButton searchUser=new JButton("User");
        JButton searchConv=new JButton("Conversation");
        JButton searchMessage=new JButton("Text");

        buttonPanel.add(searchUser);
        buttonPanel.add(searchConv);
        buttonPanel.add(searchMessage);

        titleLabelC.gridx = 0;
        titleLabelC.gridy = 0;
        titleLabelC.gridheight=1;
        titleLabelC.anchor = GridBagConstraints.FIRST_LINE_START;

        queryC.gridx=1;
        queryC.gridy=0;
        queryC.gridwidth=10;
        queryC.gridheight=1;
        queryC.anchor=GridBagConstraints.PAGE_START;

        infoScrollPaneC.gridx=0;
        infoScrollPaneC.gridy=2;
        infoScrollPaneC.gridwidth=7;
        infoScrollPaneC.gridheight=3;
        infoScrollPaneC.anchor = GridBagConstraints.LINE_START;

        panelC.gridx = 0;
        panelC.gridy = 9;
        panelC.gridwidth = 6;
        panelC.gridheight = 6;
        panelC.fill = GridBagConstraints.HORIZONTAL;
        panelC.anchor = GridBagConstraints.FIRST_LINE_START;

        buttonPanelC.gridx = 1;
        buttonPanelC.gridy = 1;
        buttonPanelC.gridwidth = 4;
        buttonPanelC.gridheight = 1;
        buttonPanelC.fill = GridBagConstraints.HORIZONTAL;
        buttonPanelC.anchor = GridBagConstraints.PAGE_END;

        panel.add(query, queryC);
        panel.add(titleLabel, titleLabelC);
        panel.add(buttonPanel, buttonPanelC);
        panel.add(infoScrollPane, infoScrollPaneC);

        this.add(panel, panelC);

        searchUser.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                infoPanel.setText("");
                String name=query.getText();
                infoPanel.setText(clientContext.user.searchUser(name));
            }
        });

        searchConv.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                infoPanel.setText("");
                String title=query.getText();
                infoPanel.setText(clientContext.conversation.searchConversation(title));
            }
        });

        searchMessage.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                infoPanel.setText("");
                String word=query.getText();
                infoPanel.setText(clientContext.message.searchMessage(word));
            }
        });

    }
}