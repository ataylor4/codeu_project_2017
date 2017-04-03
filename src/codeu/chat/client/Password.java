package codeu.chat.client;

/**
 * Created by Kinini on 3/13/17.
 */
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import java.security.SecureRandom;
import java.util.Random;
import java.security.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;
import javax.crypto.*;
import java.security.spec.InvalidKeySpecException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.math.BigInteger;
import java.lang.*;

import codeu.chat.client.ClientUser;
import codeu.chat.util.store.Store;
import codeu.chat.common.User;

public class Password {
    private static final int ITERATIONS = 10000;
    private static final int SALT_LENGTH = 16;
    private static final int DESIRED_KEYLEN = 256;
    private static final int MAX_TRIALS=3;
    private static final Random RANDOM = new SecureRandom();
    private static Map<String, String> DB = new HashMap<String, String>();



    public static void createPassword(String user, String password){

            try {
                String[] securityDetails=password.split("\\$");
                String encryptedPass=encryptPassword(user, securityDetails[0], getSaltvalue());
                String encryptedAnswer=encryptPassword(user, securityDetails[2], getSaltvalue());
                ClientUser.passwordsDB.insert(user, encryptedPass+"$" + securityDetails[1] +"$" +encryptedAnswer);
            } catch (NoSuchAlgorithmException e) {
                System.out.println(e.getMessage());
            } catch (InvalidKeySpecException e) {
                System.out.println(e.getMessage());
            }

    }


    public static String promptForPassword(String name){
        Console console = System.console();
        while(true){
            String password = new String(console.readPassword("Enter Password: "));
            String confirmPassword = new String(console.readPassword("Confirm Password: "));
            final boolean validPass = password.length()!=0 && password.equals(confirmPassword);
            password = validPass ? password : null;
            if (password == null) {
                System.out.format("Password not created - %s.\n", validPass ? "server failure" : "Passwords don't match. Try Again!");
            }
            else {
                System.out.println("Password Strength: "+ passwordStrength(password));
               // ClientUser.passwordRecoveryDB.insert(name, collectPasswordRecoveryInfo(name));
                return password + "$" +collectPasswordRecoveryInfo(name);
            }
        }
    }
    public static boolean authenticateUserCommandline(String name){
        Console console = System.console();
        int i=0;
        boolean correctPass=false;
        String password = new String(console.readPassword("Enter Password: "));
        try{
            while(true) {
                correctPass = verifyPassword(name, password, 0);
                // System.out.println(correctPass);
                if (correctPass)
                    break;
                if(i==MAX_TRIALS) {
                    System.out.println("Error: Sign in failed, invalid password");
                    changePassword(name);
                    break;
                }
                password = String.valueOf((console.readPassword("Try again: ")));
                i++;
            }
        }
        catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        } catch (InvalidKeySpecException e) {
            System.out.println(e.getMessage());
        }
        return correctPass;
    }

    private static void changePassword(String name) {
        System.out.println("Forgotten password? Y N");
        Scanner input = new Scanner(System.in);
        String choice=input.nextLine();
        if (choice.equals("Y") || choice.equals("y")) {
            String[] recoveryDetails=collectPasswordRecoveryInfo(name).split("\\$");
            String[] securityDetails=ClientUser.passwordsDB.first(name).split("\\$");
            if (securityDetails[3].equals(recoveryDetails[0])) {//security question matches
                try {
                    if (verifyPassword(name, recoveryDetails[1], 1)) { //verify security question
                        System.out.println("Let's create you new login password:");
                        String newPassword = promptForPassword(name);
                        //delete old passwords when Store implements delete
                        createPassword(name, newPassword);
                        System.out.println("Password changed. Try signing in again");
                    } else
                        System.out.println("Error: Unable to recover password");
                }
                catch (NoSuchAlgorithmException e) {
                    System.out.println(e.getMessage());
                } catch (InvalidKeySpecException e) {
                    System.out.println(e.getMessage());
                }
            }

            else{
                System.out.println("Error: Unable to recover password");
            }
        }
        else{
            System.out.println("Error: Sign in failed, invalid password");
        }
    }

    public static boolean authenticateUserGUI(String user, String password){
        boolean isCorrect=false;
        try{
            isCorrect=verifyPassword(user, password, 0);
        }
        catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        } catch (InvalidKeySpecException e) {
            System.out.println(e.getMessage());
        }
        return isCorrect;
    }
    public static boolean passedsecurityTestGUI(String name, String answer){
        boolean passed=false;
        try{
            passed=verifyPassword(name, answer, 1);
        }
        catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        } catch (InvalidKeySpecException e) {
            System.out.println(e.getMessage());
        }
        return passed;
    }
    public static final boolean verifyPassword(String username, String password, int code)throws NoSuchAlgorithmException, InvalidKeySpecException{
        String[] stored_pass=ClientUser.passwordsDB.first(username).split("\\$");
        //decrypting password
            int iterations = Integer.parseInt(stored_pass[0]);
            byte[] salt = convertToBytes(stored_pass[1]);
            byte[] hash = convertToBytes(stored_pass[2]);

        if(code==1){//decrypting security question
            iterations=Integer.parseInt(stored_pass[4]);
            salt=convertToBytes(stored_pass[5]);
            hash=convertToBytes(stored_pass[6]);
        }
        byte[] hash_of_input =hash(password, salt);

        /*use bit manipulation to compare both hashes(xor the lengths of both(should give 0),
         OR this with, the xor of each corresponding byte element which should give you 0)
          */

        int difference=hash.length ^ hash_of_input.length;
        for(int i=0; i< hash.length && i< hash_of_input.length; i++){
            difference|=hash[i]^hash_of_input[i];
        }
        return difference==0;
    }


    public static final String encryptPassword(String username, String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String encrypted_pass = ITERATIONS + "$" + convertToHex(salt) + "$" + convertToHex(hash(password, salt));

        if (encrypted_pass == null) throw new NoSuchAlgorithmException();

        return encrypted_pass;
        //DB.put(username, encrypted_pass);
       // passwordDB.insert(username, encrypted_pass);
    }

    private static final byte[] hash(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {

        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, DESIRED_KEYLEN);
        SecretKeyFactory secret = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        SecretKey key = secret.generateSecret(spec);
        if (key == null) throw new InvalidKeySpecException();
        byte[] hash = key.getEncoded();
        return hash;
    }

    private static final byte[] getSaltvalue() throws NoSuchAlgorithmException {
        byte[] salt = new byte[SALT_LENGTH];
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.nextBytes(salt);
        return salt;
    }


    private static String convertToHex(byte[] arr) throws NoSuchAlgorithmException {
        BigInteger bigInt = new BigInteger(1, arr);
        String hex = bigInt.toString(16);
        int padding = (arr.length * 2) - hex.length();
        if (padding > 0) return String.format("%0" + padding + "d", 0) + hex;
        return hex;
    }

    private static byte[] convertToBytes(String hex) throws NoSuchAlgorithmException {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }

    public static final String passwordStrength(String password){

        //a very strong password has special, lowercase, uppercase, digit characters, now hitespace and has length of at least 8
        if(password.matches("(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}")) return "Very Strong!";

        //a strong password has a lowercase, uppercase and either a digit or special character and has at least 6 characters
        if(password.matches("(?=.*[a-z])(?=.*[A-Z])((?=.*[0-9])|(?=.*[@#$%^&+=])).{6,}")) return "Strong!";

        //a medium strength password has a length of at least 6 and has both lower and uppercase characters.
        if(password.matches("(?=.*[a-z])(?=.*[A-Z]).{6,}")) return "Medium!";

        //the rest are weak passwords
        return "Weak!";
        }

    public static final String collectPasswordRecoveryInfo(String name){
        int choice=0;

        while(choice<=0 || choice>3) {
            System.out.println("Choose one security question: ");
            System.out.println("1 : What is the name of your elementary school?");
            System.out.println("2: What is the name of your pet?");
            System.out.println("3. Which city did you meet your spouse?");

            Scanner input = new Scanner(System.in);
            choice = input.nextInt();
        }

        String question="";
        if(choice==1) question="What is the name of your elementary school?";
        if(choice==2) question="What is the name of your pet?";
        if(choice==3) question="Which city did you meet your spouse?";



        System.out.println("Answer: ");
        Scanner scanner=new Scanner(System.in);
        String answer=scanner.nextLine();

        return  question + "$" + answer;

    }

    //using username temporarily to store recovery passwords instead of UID

}

