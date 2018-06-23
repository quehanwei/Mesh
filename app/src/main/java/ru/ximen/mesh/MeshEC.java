package ru.ximen.mesh;

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

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


/**
 * Created by ximen on 07.04.18.
 */

public class MeshEC {
    final static private String TAG = "MeshEC";
    private KeyPair pair;
    private PublicKey peerPKey;
    private byte[] secret;
    private byte[] mRandomBytes;
    private byte[] mConfirmationSalt;
    private byte[] mConfirmationKey;
    private byte[] mConfirmation = new byte[16];
    private byte[] mAuthValue = new byte[16];
    private byte[] provisionSalt;

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    public MeshEC() {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-256");
        KeyPairGenerator g = null;
        try {
            g = KeyPairGenerator.getInstance("ECDH", "SC");
            g.initialize(ecSpec, new SecureRandom());
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        pair = g.generateKeyPair();
        Log.d(TAG, "Keys:" + pair.getPrivate().toString() + " and " + pair.getPublic().toString());
    }

    BigInteger getPKeyX() {
        return ((ECPublicKey) (pair.getPublic())).getW().getAffineX();
    }

    BigInteger getPKeyY() {
        return ((ECPublicKey) (pair.getPublic())).getW().getAffineY();
    }

    void setPeerPKey(byte[] x, byte[] y) {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-256");
        ECPublicKeySpec spec = new ECPublicKeySpec(ecSpec.getCurve().createPoint(new BigInteger(1, x), new BigInteger(1, y)), ecSpec);
        KeyFactory ecKeyFac;
        try {
            ecKeyFac = KeyFactory.getInstance("ECDH", "SC");
            peerPKey = ecKeyFac.generatePublic(spec);
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Peer Public Key:" + peerPKey.toString());
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
        Log.d(TAG, "k2 N:" + Utils.toHexString(N));
        Log.d(TAG, "k2 P:" + Utils.toHexString(P));
        byte[] salt = s1("smk2".getBytes());
        Log.d(TAG, "k2 s1(smk2):" + Utils.toHexString(salt));
        byte[] T = AES_CMAC(N, salt);
        Log.d(TAG, "k2 T:" + Utils.toHexString(T));

        byte[] P1 = new byte[P.length + 1];
        System.arraycopy(P, 0, P1, 0, P.length);
        P1[P.length] = 0x01;
        byte[] T1 = AES_CMAC(P1, T);
        Log.d(TAG, "k2 T1:" + Utils.toHexString(T1));

        byte[] P2 = new byte[T1.length + P.length + 1];
        System.arraycopy(T1, 0, P2, 0, T1.length);
        System.arraycopy(P, 0, P2, T1.length, P.length);
        P2[T1.length + P.length] = 0x02;
        byte[] T2 = AES_CMAC(P2, T);
        Log.d(TAG, "k2 T2:" + Utils.toHexString(T2));

        byte[] P3 = new byte[T2.length + P.length + 1];
        System.arraycopy(T2, 0, P3, 0, T2.length);
        System.arraycopy(P, 0, P3, T2.length, P.length);
        P3[T2.length + P.length] = 0x03;
        byte[] T3 = AES_CMAC(P3, T);
        Log.d(TAG, "k2 T3:" + Utils.toHexString(T3));

        byte[] Tres = new byte[T1.length + T2.length + T3.length];
        System.arraycopy(T1, 0, Tres, 0, T1.length);
        System.arraycopy(T2, 0, Tres, T1.length, T2.length);
        System.arraycopy(T3, 0, Tres, T1.length + T2.length, T3.length);
        BigInteger modulo = new BigInteger("2").pow(263);
        Log.d(TAG, "k2:" + Utils.toHexString(new BigInteger(Tres).mod(modulo).toByteArray()));
        return new BigInteger(Tres).mod(modulo).toByteArray();
    }

    static private byte[] AES_CMAC(byte[] P, byte[] T) {
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

    static public byte[] e(byte[] key, byte[] data) {
        byte[] out = new byte[16];       // 128 bit
        AESEngine engine = new AESEngine();
        engine.init(true, new KeyParameter(key));
        engine.processBlock(data, 0, out, 0);
        return out;
    }

    void calculateSecret() {
        ECParameterSpec paramSpec = ECNamedCurveTable.getParameterSpec("P-256");
        try {
            KeyAgreement agr = KeyAgreement.getInstance("ECDH", "SC");
            agr.init(pair.getPrivate());
            agr.doPhase(peerPKey, true);
            secret = agr.generateSecret();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    public byte[] getConfirmation(byte[] inputs, byte[] authValue) {
        mAuthValue = authValue;
        mConfirmationSalt = s1(inputs);
        Log.d(TAG, "Confirmation salt: " + new BigInteger(mConfirmationSalt).toString(16));
        mConfirmationKey = k1(secret, mConfirmationSalt, "prck".getBytes());
        Log.d(TAG, "Confirmation key: " + new BigInteger(mConfirmationSalt).toString(16));
        SecureRandom random = new SecureRandom();
        mRandomBytes = new byte[16]; // 128 bits are converted to 16 bytes;
        random.nextBytes(mRandomBytes);
        Log.d(TAG, "Random: " + new BigInteger(mRandomBytes).toString(16));
        byte[] randomAuth = new byte[32];
        System.arraycopy(mRandomBytes, 0, randomAuth, 0, 16);
        System.arraycopy(authValue, 0, randomAuth, 16, 16);
        Log.d(TAG, "Random||AuthValue: " + new BigInteger(randomAuth).toString(16));

        mConfirmation = AES_CMAC(randomAuth, mConfirmationKey);
        Log.d(TAG, "Confirmation: " + new BigInteger(mConfirmation).toString(16));
        return mConfirmation;
    }

    public byte[] remoteConfirmation(byte[] randomBytes) {
        byte[] randomAuth = new byte[32];
        System.arraycopy(randomBytes, 0, randomAuth, 0, 16);
        System.arraycopy(mAuthValue, 0, randomAuth, 16, 16);
        Log.d(TAG, "Remote Random||AuthValue: " + new BigInteger(randomAuth).toString(16));
        byte[] confirmation = new byte[16];

        AES_CMAC(randomAuth, mConfirmationKey);
        Log.d(TAG, "Remote confirmation: " + new BigInteger(confirmation).toString(16));
        return confirmation;
    }

    public byte[] getRandom() {
        return mRandomBytes;
    }

    public byte[] getProvisionData(byte[] data, byte[] peerRandom) {
        Log.d(TAG, "Provision data:");
        Log.d(TAG, " > ConfirmationSalt: " + new BigInteger(mConfirmationSalt).toString(16));
        Log.d(TAG, " > RandomProvisioner: " + new BigInteger(mRandomBytes).toString(16));
        Log.d(TAG, " > RandomDevice: " + new BigInteger(peerRandom).toString(16));
        byte[] saltData = new byte[48];
        System.arraycopy(mConfirmationSalt, 0, saltData, 0, 16);
        System.arraycopy(mRandomBytes, 0, saltData, 16, 16);
        System.arraycopy(peerRandom, 0, saltData, 32, 16);
        Log.d(TAG, " > ProvisionInputs: " + new BigInteger(saltData).toString(16));
        provisionSalt = s1(saltData);
        Log.d(TAG, " > ProvisionSalt: " + new BigInteger(provisionSalt).toString(16));
        byte[] sessionKey = k1(secret, provisionSalt, "prsk".getBytes());
        Log.d(TAG, " > SessionKey: " + new BigInteger(sessionKey).toString(16));
        byte[] sessionNonce = new byte[13];
        System.arraycopy(k1(secret, provisionSalt, "prsn".getBytes()), 3, sessionNonce, 0, 13);
        Log.d(TAG, " > Nonce: " + new BigInteger(sessionNonce).toString(16));
        Log.d(TAG, " > ProvisionData: " + new BigInteger(data).toString(16));

        Pair<byte[], byte[]> t = AES_CCM(sessionKey, sessionNonce, data, 64);
        byte[] out = new byte[25 + 8];
        System.arraycopy(t.first, 0, out, 0, 25);
        System.arraycopy(t.second, 0, out, t.first.length, 8);
        return out;
    }

    public byte[] getDeviceKey() {
        return k1(secret, provisionSalt, "prdk".getBytes());
    }
}