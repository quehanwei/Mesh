package ru.ximen.meshstack;

import android.util.Pair;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.macs.CMac;
import org.spongycastle.crypto.modes.CCMBlockCipher;
import org.spongycastle.crypto.params.AEADParameters;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.jce.spec.ECPublicKeySpec;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.KeyAgreement;

/**
 * Utility class implementing elliptic curves cryptography functions.
 * All functions are static.
 *
 * Created by ximen on 07.04.18.
 */
public class MeshEC {
    final static private String TAG = MeshEC.class.getSimpleName();

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    /**
     * Generate key pair.
     *
     * @return the key pair
     */
    public static KeyPair generatePair(){
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-256");
        KeyPairGenerator g = null;
        try {
            g = KeyPairGenerator.getInstance("ECDH", "SC");
            g.initialize(ecSpec, new SecureRandom());
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        return g.generateKeyPair();
    }

    /**
     * Gets x coordinate of public key.
     *
     * @param pair Key pair.
     * @return x coordinate of public key on elliptic curve
     */
    public static BigInteger getPKeyX(KeyPair pair) {
        return ((ECPublicKey) (pair.getPublic())).getW().getAffineX();
    }

    /**
     * Gets y coordinate of public key.
     *
     * @param pair Key pair.
     * @return y coordinate of public key on elliptic curve
     */
    public static BigInteger getPKeyY(KeyPair pair) {
        return ((ECPublicKey) (pair.getPublic())).getW().getAffineY();
    }

    /**
     * Generates public key from known x and y coordinates on elliptic curve
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return the public key
     */
    public static PublicKey getPeerPKey(byte[] x, byte[] y) {
        PublicKey peerPKey = null;
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-256");
        ECPublicKeySpec spec = new ECPublicKeySpec(ecSpec.getCurve().createPoint(new BigInteger(1, x), new BigInteger(1, y)), ecSpec);
        KeyFactory ecKeyFac;
        try {
            ecKeyFac = KeyFactory.getInstance("ECDH", "SC");
            peerPKey = ecKeyFac.generatePublic(spec);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        return peerPKey;
    }

    /**
     * Salt generation function. The output of the salt generation function s1 is as follows:
     * s1(input) = AES-CMAC<sub>ZERO</sub>(input) where ZERO is 128-bit value of 0x0000 0000 0000 0000 0000 0000 0000 0000
     * @see 3.8.2.4 <a href="https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=429633#page=101"</a>
     *
     * @param input non-zero length octet array
     * @return salt value
     */
    public static byte[] s1(byte[] input) {
        return AES_CMAC(input, new byte[16]);
    }

    /**
     * The network key material derivation function k1 is used to generate instances of IdentityKey and BeaconKey.
     * The definition of this key generation function makes use of the MAC function AES-CMAC<sub>T</sub> with a 128-bit key T.
     * The key T is computed as follows: T = AES-CMAC<sub>salt</sub>(N).
     * The output of the key generation function k1 is as follows: k1(N, salt, P) = AES-CMAC<sub>T</sub>(P).
     * @see 3.8.2.5 <a href="https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=429633#page=101"</a>
     *
     * @param N    0 or more octets
     * @param salt 128-bit salt
     * @param P    0 or more octets
     * @return key derivation result
     */
    public static byte[] k1(byte[] N, byte[] salt, byte[] P) {
        byte[] T = AES_CMAC(N, salt);
        return AES_CMAC(P, T);
    }

    /**
     * The network key material derivation function k2 is used to generate instances of EncryptionKey,
     * PrivacyKey, and NID for use as Master and Private Low Power node communication.
     * The definition of this key generation function makes use of the MAC function AES-CMAC<sub>T</sub> with a 128-bit key T.
     * The key T is computed as follows: T = AES-CMAC<sub>salt</sub>(N)
     * Salt is the 128-bit value computed as follows:
     * T0 = empty string (zero length)
     * T1 = AES-CMAC<sub>T</sub>(T0||P||0x01)
     * T2 = AES-CMAC<sub>T</sub>(T1||P||0x02)
     * T3 = AES-CMAC<sub>T</sub>(T2||P||0x03)
     * k2(N, P) = (T1||T2||T3) mod 2<sup>263</sup>
     *@see .8.2.6 <a href="https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=429633#page=101"</a>
     *
     * @param N 128-bit
     * @param P 1 or more octets
     * @return key material
     */
    public static byte[] k2(byte[] N, byte[] P) {
        //Log.d(TAG, "k2 N:" + Utils.toHexString(N));
        //Log.d(TAG, "k2 P:" + Utils.toHexString(P));
        byte[] salt = s1("smk2".getBytes());
        //Log.d(TAG, "k2 s1(smk2):" + Utils.toHexString(salt));
        byte[] T = AES_CMAC(N, salt);
        //Log.d(TAG, "k2 T:" + Utils.toHexString(T));

        byte[] P1 = new byte[P.length + 1];
        System.arraycopy(P, 0, P1, 0, P.length);
        P1[P.length] = 0x01;
        byte[] T1 = AES_CMAC(P1, T);
        //Log.d(TAG, "k2 T1:" + Utils.toHexString(T1));

        byte[] P2 = new byte[T1.length + P.length + 1];
        System.arraycopy(T1, 0, P2, 0, T1.length);
        System.arraycopy(P, 0, P2, T1.length, P.length);
        P2[T1.length + P.length] = 0x02;
        byte[] T2 = AES_CMAC(P2, T);
        //Log.d(TAG, "k2 T2:" + Utils.toHexString(T2));

        byte[] P3 = new byte[T2.length + P.length + 1];
        System.arraycopy(T2, 0, P3, 0, T2.length);
        System.arraycopy(P, 0, P3, T2.length, P.length);
        P3[T2.length + P.length] = 0x03;
        byte[] T3 = AES_CMAC(P3, T);
        //Log.d(TAG, "k2 T3:" + Utils.toHexString(T3));

        byte[] Tres = new byte[T1.length + T2.length + T3.length];
        System.arraycopy(T1, 0, Tres, 0, T1.length);
        System.arraycopy(T2, 0, Tres, T1.length, T2.length);
        System.arraycopy(T3, 0, Tres, T1.length + T2.length, T3.length);
        BigInteger modulo = new BigInteger("2").pow(263);
        byte[] signed = new BigInteger(Tres).mod(modulo).toByteArray();
        if (signed[0] == 0x00) {
            byte[] unsigned = new byte[signed.length - 1];
            System.arraycopy(signed, 1, unsigned, 0, unsigned.length);
            return unsigned;
        } else return signed;
    }

    /**
     * The derivation function k3 is used to generate a public value of 64 bits derived from a private key.
     * The definition of this derivation function makes use of the MAC function AES-CMAC<sub>T</sub> with a 128-bit key T.
     * The key (T) is computed as follows: T = AES-CMAC<sub>salt</sub>(N).
     * Salt is a 128-bit value computed as follows: Salt = s1("smk3")
     * The output of the derivation function is as follows: s3(N) = AES-CMAC<sub>T</sub>("id64"||0x01) mod 2<sup>64</sup>
     * @see 3.8.2.7 <a href="https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=429633#page=102"</a>
     *
     * @param N 128-bit private key
     * @return 64-bit public value
     */
    public static byte[] k3(byte[] N) {
        byte[] salt = s1("smk3".getBytes());
        //Log.d(TAG, "Salt: " + Utils.toHexString(salt));
        byte[] T = AES_CMAC(N, salt);
        //Log.d(TAG, "T: " + Utils.toHexString(T));
        byte[] P = new byte[5];
        System.arraycopy("id64".getBytes(), 0, P, 0, 4);
        P[4] = 0x01;
        byte[] result = AES_CMAC(P, T);
        //Log.d(TAG, "result: " + Utils.toHexString(result));
        BigInteger modulo = new BigInteger("2").pow(64);
        byte[] signed = new BigInteger(result).mod(modulo).toByteArray();
        if (signed[0] == 0x00) {
            byte[] unsigned = new byte[signed.length - 1];
            System.arraycopy(signed, 1, unsigned, 0, unsigned.length);
            return unsigned;
        } else return signed;
    }

    /**
     * The derivation function k4 is used to generate a public value of 6 bits derived from a private key.
     * The definition of this derivation function makes use of the MAC function AES-CMAC<sub>T</sub> with a 128-bit key T.
     * The key (T) is computed as follows: T = AES-CMAC<sub>salt</sub>(N).
     * Salt is a 128-bit value computed as follows: Salt = s1("smk4").
     * The output of the derivation function k4 is as follows:
     * k4(N) = AES-CMAC<sub>T</sub>("id6"||0x01) mod 2<sup>6</sup>
     * @see 3.8.2.8 <a href="https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=429633#page=102"</a>
     *
     * @param N 128-bit private key
     * @return 6 bits public value
     */
    public static byte k4(byte[] N){
        byte[] salt = s1("smk4".getBytes());
        byte[] T = AES_CMAC(N, salt);
        byte[] P = new byte[4];
        System.arraycopy("id6".getBytes(), 0, P, 0, 3);
        P[3] = 0x01;
        byte[] result = AES_CMAC(P, T);
        BigInteger modulo = new BigInteger("2").pow(6);
        byte[] signed = new BigInteger(result).mod(modulo).toByteArray();
        return signed[0];
    }

    /**
     * AES-CMAC function implementation. Cipher-based Message Authentication Code that uses AES-128
     * as the block cipher function according RFC4493.
     * The 128-bit MAC is generated as follows: MAC=AES-CMAC<sub>T</sub>(P)
     * @see 3.8.2.2 <a href="https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=429633#page=100"</a>
     *
     * @param P variable length data to be authenticated
     * @param T 128-bit key
     * @return 128-bit message authentication code (MAC)
     */
    public static byte[] AES_CMAC(byte[] P, byte[] T) {
        byte[] R = new byte[16];
        CMac macT = new CMac(new AESEngine());
        macT.init(new KeyParameter(T));
        macT.update(P, 0, P.length);
        macT.doFinal(R, 0);
        return R;
    }

    /**
     * Function implements encryption and authentication using AES Counter with CBC-MAC (CCM)
     * (see Volume 6, Part E, section 1 of the Core Specification).
     * The ciphertext and MAC are generated as follows: ciphertext, mac = AES-CCM<sub>key</sub>(nonce, data)
     * @see 3.8.2.3 <a href="https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=429633#page=100"</a>
     *
     * @param key     128-bit key
     * @param nonce   104-bit nonce
     * @param data    variable length data to be encrypted and authenticated
     * @param macSize size of message authentication code (MAC)
     * @return @Pair android.util.Pair containing encrypted message and MAC
     */
    public static Pair<byte[], byte[]> AES_CCM(byte[] key, byte[] nonce, byte[] data, int macSize) {
        CCMBlockCipher cipher = new CCMBlockCipher(new AESEngine());
        cipher.init(true, new AEADParameters(new KeyParameter(key), macSize, nonce));
        byte[] outputText = new byte[cipher.getOutputSize(data.length)];
        int outputLen = cipher.processBytes(data, 0, data.length, data, 0);
        try {
            cipher.doFinal(outputText, outputLen);
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
        }
        byte[] mic = new byte[macSize / 8];
        byte[] out = new byte[data.length];
        System.arraycopy(outputText, data.length, mic, 0, macSize / 8);
        System.arraycopy(outputText, 0, out, 0, data.length);
        return new Pair<>(out, mic);
    }

    /**
     * Function implements decryption of ciphertext previously encrypted using @AES-CCM #AES_CMAC(byte[] P, byte[] T).
     *
     * @param key     128-bit key
     * @param nonce   104-bit nonce
     * @param data    variable length data to be decrypted
     * @param macSize size of message authentication code (MAC)
     * @return decrypted plaintext
     * @throws InvalidCipherTextException exception thrown in case of error while decrypting message (typically wrong key, message on macSize)
     */
    public static byte[] AES_CCM_Decrypt(byte[] key, byte[] nonce, byte[] data, int macSize) throws InvalidCipherTextException {
        CCMBlockCipher cipher = new CCMBlockCipher(new AESEngine());
        cipher.init(false, new AEADParameters(new KeyParameter(key), macSize, nonce));
        byte[] outputText = new byte[cipher.getOutputSize(data.length)];
        int outputLen = cipher.processBytes(data, 0, data.length, data, 0);
        cipher.doFinal(outputText, outputLen);
        return outputText;
    }

    /**
     * Encryption function, same as defined in Volume 3, Part H, Section 2.2.1 of thr Core Specification.
     * ciphertext = e(key, plaintext).
     * @see 3.8.2.1 <a href="https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=429633#page=99"</a>
     *
     * @param key  128-bit key
     * @param data 128-bit data
     * @return the 128-bit encrypted ciphertext
     */
    public static byte[] e(byte[] key, byte[] data) {
        byte[] out = new byte[16];       // 128 bit
        AESEngine engine = new AESEngine();
        engine.init(true, new KeyParameter(key));
        engine.processBlock(data, 0, out, 0);
        return out;
    }

    /**
     * Calculates ECDH secret from device private key and peer public key.
     * ECDHSecret = P-256(private key, peer public key)
     *
     * @param pair     key pair containing device private key
     * @param peerPKey the peer public key
     * @return ECDH secret
     */
    public static byte[] calculateSecret(KeyPair pair, PublicKey peerPKey) {
        byte[] secret = null;
        //ECParameterSpec paramSpec = ECNamedCurveTable.getParameterSpec("P-256");
        try {
            KeyAgreement agr = KeyAgreement.getInstance("ECDH", "SC");
            agr.init(pair.getPrivate());
            agr.doPhase(peerPKey, true);
            secret = agr.generateSecret();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return secret;
    }

    /**
     * Generate device key in process of provisioning device from shared secret an provision salt using
     * key derivation function @see #k1(byte[] N, byte[] salt, byte[] P)).
     * The DevKey shall be derived from the ECDHSecret and ProvisioningSalt as described by the formula below:
     * DevKey = k1(ECDHSecret, ProvisioningSalt, “prdk”)
     * @see 3.8.6.1 <a href="https://www.bluetooth.org/docman/handlers/downloaddoc.ashx?doc_id=429633#page=108"</a>
     *
     * @param secret        the shared secret value
     * @param provisionSalt the provision salt
     * @return the Device Key
     */
    public static byte[] getDeviceKey(byte[] secret, byte[] provisionSalt) {
        return k1(secret, provisionSalt, "prdk".getBytes());
    }
}