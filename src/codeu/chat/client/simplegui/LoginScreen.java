package codeu.chat.client.simplegui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.util.Arrays;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.io.File;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.nio.file.*;

import codeu.chat.client.ClientContext;
import codeu.chat.common.ConversationSummary;
import codeu.chat.common.User;
import codeu.chat.client.Password;
import codeu.chat.client.ClientUser;
import codeu.chat.common.User;
import codeu.chat.util.Logger;

@SuppressWarnings("serial")
public final class LoginScreen extends JPanel{
    public JTextField textField;
    public JPasswordField pwdField;
    public JButton loginButton;
    public JPanel panel;
    public JButton signupButton;
    public JLabel pwdLabel;
    public JLabel username;
    public JPanel buttonPanel;
    public GridBagConstraints buttonPanelC;

    public JPasswordField pwdConfirm;
    public JLabel pwdConfirmLabel;
    public GridBagConstraints pwdConfirmLabelC;
    public GridBagConstraints pwdConfirmC;

    public JLabel securityQnLabel;
    public GridBagConstraints securityQnLabelC;

    public GridBagConstraints securityQnC;

    public JLabel securityAnswerLabel;
    public GridBagConstraints securityAnswerLabelC;

    public JTextField securityAnswer;
    public GridBagConstraints securityAnswerC;

    public JPanel titlePanel;
    public JLabel infoLabel;

    public LoginScreen() {
        super(new GridBagLayout());
        initialize();
    }

    private void initialize() {
        titlePanel = new JPanel(new GridBagLayout());
        final GridBagConstraints titlePanelC = new GridBagConstraints();
        titlePanelC.anchor = GridBagConstraints.NORTH;
        titlePanelC.insets = new Insets(10, 10, 10, 10);

        infoLabel = new JLabel("Welcome To U-Chat", JLabel.CENTER);//RIGHT
        final GridBagConstraints infoLabelC = new GridBagConstraints();
        infoLabelC.gridx = 2;
        infoLabelC.gridy = 0;
        infoLabelC.anchor = GridBagConstraints.LINE_END;
        infoLabel.setFont(new Font("Serif", Font.BOLD, 16));

        Image image =null;

        try {
            //This reads URL when there is an internet connection
                /*
                 URL url=new URL("https://s-media-cache-ak0.pinimg.com/236x/fc/1c/f6/fc1cf66facb46e89d6ea15aa3d0b88e0.jpg");
                 image = ImageIO.read(url);
                */

            //This reads icon from the local machine
                    /*
                    File sourceimage = new File("/Users/Kinini/Documents/Projects/git/codeu_project_2017/src/images/icon2.jpg");
                    */
            System.out.println(System.getProperty("user.dir") +"/../res/images/icon6.jpg");
            File sourceimage = new File(System.getProperty("user.dir") +"/../res/images/icon6.jpg");
            image = ImageIO.read(sourceimage);

        }
        catch(IOException e){
            System.out.println(System.getProperty("user.dir"));
            System.out.println("The image was not loaded.");
        }
        final GridBagConstraints iconC = new GridBagConstraints();
        iconC.gridx = 2;
        iconC.gridy = 1;
        iconC.anchor = GridBagConstraints.LINE_END;

        JLabel iconLabel=new JLabel(new ImageIcon(image));
        iconLabel.setMinimumSize(new Dimension(200,200));
        iconLabel.setPreferredSize(new Dimension(200,200));

        titlePanel.add(infoLabel, infoLabelC);
        titlePanel.add(iconLabel, iconC);
        titlePanel.setBackground(Color.GREEN);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        panel = new JPanel(new GridBagLayout());
        GridBagConstraints panelC = new GridBagConstraints();
        panelC.anchor = GridBagConstraints.WEST;
        panelC.insets = new Insets(100, 100, 100, 100);

        username = new JLabel("Username", JLabel.LEFT);
        final GridBagConstraints usernameC = new GridBagConstraints();
        usernameC.gridx = 0;
        usernameC.gridy = 1;//
        usernameC.anchor = GridBagConstraints.FIRST_LINE_START;

        textField = new JTextField();
        final GridBagConstraints textFieldC = new GridBagConstraints();
        textField.setMinimumSize(new Dimension(250,20));
        textField.setPreferredSize(new Dimension(450,20));
        textFieldC.gridx = 1;
        textFieldC.gridy=1;
        usernameC.anchor = GridBagConstraints.PAGE_START;

        titlePanelC.gridx = 0;
        titlePanelC.gridy = 0;
        titlePanelC.gridwidth = 10;
        titlePanelC.gridheight = 1;
        titlePanelC.fill = GridBagConstraints.HORIZONTAL;
        titlePanelC.anchor = GridBagConstraints.PAGE_START;

        pwdLabel = new JLabel("Password", JLabel.LEFT);
        final GridBagConstraints pwdLabelC = new GridBagConstraints();
        pwdLabelC.gridx = 0;
        pwdLabelC.gridy = 2;
        pwdLabelC.anchor = GridBagConstraints.LINE_START;

        pwdField = new JPasswordField();

        final GridBagConstraints pwdFieldC = new GridBagConstraints();
        pwdField.setMinimumSize(new Dimension(250,20));
        pwdField.setPreferredSize(new Dimension(450,20));
        pwdFieldC.gridx = 1;
        pwdFieldC.gridy=2;
        pwdFieldC.anchor=GridBagConstraints.CENTER;

        pwdConfirmLabel = new JLabel("Confirm Password:", JLabel.LEFT);
        pwdConfirmLabelC = new GridBagConstraints();
        pwdConfirmLabelC.gridx = 0;
        pwdConfirmLabelC.gridy = 3;
        pwdConfirmLabelC.anchor = GridBagConstraints.LINE_START;

        pwdConfirm= new JPasswordField();
        pwdConfirmC = new GridBagConstraints();
        pwdConfirm.setMinimumSize(new Dimension(250,20));
        pwdConfirm.setPreferredSize(new Dimension(450,20));
        pwdConfirmC.gridx = 1;
        pwdConfirmC.gridy=3;
        pwdConfirmC.anchor=GridBagConstraints.CENTER;

        securityQnLabel = new JLabel("Security Question:", JLabel.LEFT);
        securityQnLabelC = new GridBagConstraints();
        securityQnLabelC.gridx = 0;
        securityQnLabelC.gridy = 4;
        securityQnLabelC.anchor = GridBagConstraints.LAST_LINE_START;

        securityQnC = new GridBagConstraints();
        securityQnC.gridx = 1;
        securityQnC.gridy=4;
        securityQnC.anchor = GridBagConstraints.PAGE_END;

        securityAnswerLabel=new JLabel("Security Answer", JLabel.LEFT);
        securityAnswerLabelC= new GridBagConstraints();
        securityAnswerLabelC.gridx = 0;
        securityAnswerLabelC.gridy = 5;
        securityAnswerLabelC.anchor = GridBagConstraints.LAST_LINE_START;

        securityAnswer=new JTextField();
        securityAnswerC = new GridBagConstraints();
        securityAnswer.setMinimumSize(new Dimension(250,20));
        securityAnswer.setPreferredSize(new Dimension(450,20));
        securityAnswerC.gridx = 1;
        securityAnswerC.gridy=5;
        securityAnswerC.anchor = GridBagConstraints.PAGE_END;


        buttonPanel = new JPanel();
        buttonPanelC=new GridBagConstraints();
        buttonPanelC.gridx=1;
        buttonPanelC.gridy=3;
        buttonPanelC.anchor=GridBagConstraints.LAST_LINE_START;

        loginButton=new JButton("Login");
        signupButton=new JButton("Sign up");

        buttonPanel.add(loginButton);
        buttonPanel.add(signupButton);
        buttonPanel.setBackground(Color.GREEN);

        panel.add(titlePanel, titlePanelC);
        panel.add(username, usernameC);
        panel.add(pwdLabel, pwdLabelC);
        panel.add(textField, textFieldC);
        panel.add(pwdField, pwdFieldC);
        panel.add(buttonPanel, buttonPanelC);
        panel.setBackground(Color.GREEN);
        //panel.setOpaque(false);

        panelC.gridx = 0;
        panelC.gridy = 0;
        panelC.gridwidth = 10;
        panelC.gridheight = 1;
        panelC.fill = GridBagConstraints.HORIZONTAL;
        panelC.anchor = GridBagConstraints.FIRST_LINE_START;

        this.add(panel, panelC);

    }

}