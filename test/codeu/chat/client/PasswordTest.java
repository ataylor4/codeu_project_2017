package codeu.chat.client;

/**
 * Created by Kinini on 4/3/17.
 */

import static org.junit.Assert.*;

import codeu.chat.common.*;
import codeu.chat.server.Model;
import org.junit.Before;
import org.junit.Test;
import codeu.chat.common.User;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public final class PasswordTest {


    private Model model;
    private BasicController controller;

    @Before
    public void doBefore() {
        model = new Model();
        controller = new codeu.chat.server.Controller(Uuids.NULL, model);
    }
/*
* in this test case, we create new users passing in the full security string argument separated by $ sign
* The password is the first string, then the security question and then the security answer.
* To verify the password, we only pass in the password which it encypts to check with the stored encrypted pass
* */
    @Test
    public void testCreatePasswordAndVerifyPassword() {
       //test regular string
        User newUser = controller.newUser("Robert", Password.createPassword("Robert", "password$what is your name?$nairobi"));
        User newUser2 = controller.newUser("Kinini", Password.createPassword("Kinini", "password$password$password"));

        assertFalse(newUser == null);
        assertFalse(newUser2 == null);

        //test with right password
        boolean status=false;
        try{
            status=Password.verifyPassword(newUser.name,"password",0, newUser);
        }
        catch (NoSuchAlgorithmException e) {
                System.out.println(e.getMessage());
            } catch (InvalidKeySpecException e) {
                System.out.println(e.getMessage());
            }
        assertTrue(status);

        //test for wrong password
        status=false;
        try{
            status=Password.verifyPassword(newUser.name,"PASSWORD",0, newUser);
        }
        catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        } catch (InvalidKeySpecException e) {
            System.out.println(e.getMessage());
        }
        assertFalse(status);

        //test that two people with same passwords, names, security questions and answers don't hash the same after encryption

        assertFalse(Password.createPassword("Robert", "password$what is your name?$nairobi").equals(Password.createPassword("Robert", "password$what is your name?$nairobi")));
    }

    /*
    * A test for password regex
    * */

    @Test
    public void testPasswordStrength() {
        assertTrue(Password.passwordStrength("abc").equals("Weak!"));
        assertTrue(Password.passwordStrength("abcABCD").equals("Medium!"));
        assertTrue(Password.passwordStrength("abcABC123").equals("Strong!"));
        assertTrue(Password.passwordStrength("abcABC!@@2").equals("Very Strong!"));
    }

}


