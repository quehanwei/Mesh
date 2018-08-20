package ru.ximen.meshstack;

import android.util.Log;
import android.util.Pair;

import org.spongycastle.crypto.BlockCipher;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.macs.CMac;
import org.spongycastle.crypto.modes.CCMBlockCipher;
import org.spongycastle.crypto.params.AEADParameters;
import org.spongycastle.crypto.params.CCMParameters;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.jcajce.provider.symmetric.AES;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.jce.spec.ECPublicKeySpec;
import org.spongycastle.pqc.math.ntru.util.Util;

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
 * Created by ximen on 07.04.18.
 */

public class MeshEC {
    final static private String TAG = MeshEC.class.getSimpleName();

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

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

    public static BigInteger getPKeyX(KeyPair pair) {
        return ((ECPublicKey) (pair.getPublic())).getW().getAffineX();
    }

    public static BigInteger getPKeyY(KeyPair pair) {
        return ((ECPublicKey) (pair.getPublic())).getW().getAffineY();
    }

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

    static byte[] s1(byte[] input) {
        return AES_CMAC(input, new byte[16]);
    }

    static public byte[] k1(byte[] N, byte[] salt, byte[] P) {
        byte[] T = AES_CMAC(N, salt);
        byte[] result = AES_CMAC(P, T);
        return result;
    }

    static public byte[] k2(byte[] N, byte[] P) {
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

    static public byte[] AES_CMAC(byte[] P, byte[] T) {
        byte[] R = new byte[16];
        CMac macT = new CMac(new AESEngine());
        macT.init(new KeyParameter(T));
        macT.update(P, 0, P.length);
        macT.doFinal(R, 0);
        return R;
    }

    static public Pair<byte[], byte[]> AES_CCM(byte[] key, byte[] nonce, byte[] data, int macSize) {
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

    static public byte[] AES_CCM_Decrypt(byte[] key, byte[] nonce, byte[] data, int macSize) throws InvalidCipherTextException {
        CCMBlockCipher cipher = new CCMBlockCipher(new AESEngine());
        cipher.init(false, new AEADParameters(new KeyParameter(key), macSize, nonce));
        byte[] outputText = new byte[cipher.getOutputSize(data.length)];
        int outputLen = cipher.processBytes(data, 0, data.length, data, 0);
        cipher.doFinal(outputText, outputLen);
        return outputText;
    }

    static public byte[] e(byte[] key, byte[] data) {
        byte[] out = new byte[16];       // 128 bit
        AESEngine engine = new AESEngine();
        engine.init(true, new KeyParameter(key));
        engine.processBlock(data, 0, out, 0);
        return out;
    }

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

    public static byte[] getDeviceKey(byte[] secret, byte[] provisionSalt) {
        return k1(secret, provisionSalt, "prdk".getBytes());
    }
}