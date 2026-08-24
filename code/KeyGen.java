import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class KeyGen {
    public static void main(String[] args) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        
        String pub = "-----BEGIN PUBLIC KEY-----\n" + 
            Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(kp.getPublic().getEncoded()) +
            "\n-----END PUBLIC KEY-----\n";
            
        String priv = "-----BEGIN PRIVATE KEY-----\n" + 
            Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(kp.getPrivate().getEncoded()) +
            "\n-----END PRIVATE KEY-----\n";
            
        System.out.println("===PUB===");
        System.out.println(pub);
        System.out.println("===PRIV===");
        System.out.println(priv);
    }
}
