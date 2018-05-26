package ru.ximen.mesh;

import android.util.Log;

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
    byte[] mConfirmation = new byte[16];
    byte[] mAuthValue = new byte[16];

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    public MeshEC() {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-256");
        KeyPairGenerator g = null;
        try {
            g = KeyPairGenerator.getInstance("ECDH", "SC");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        try {
            g.initialize(ecSpec, new SecureRandom());
        } catch (InvalidAlgorithmParameterException e) {
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

    void setPeerPKey(BigInteger x, BigInteger y) {
        ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("P-256");
        ECPublicKeySpec spec = new ECPublicKeySpec(ecSpec.getCurve().createPoint(x, y), ecSpec);
        KeyFactory ecKeyFac;
        try {
            ecKeyFac = KeyFactory.getInstance("ECDH", "SC");
            peerPKey = ecKeyFac.generatePublic(spec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Peer Public Key:" + peerPKey.toString());
    }

    byte[] s1(byte[] input) {
        byte[] zeros = new byte[16];
        byte[] result = new byte[16];
        CipherParameters params = new KeyParameter(zeros);
        BlockCipher aes = new AESEngine();
        CMac mac = new CMac(aes);
        mac.init(params);
        mac.update(input, 0, input.length);
        mac.doFinal(result, 0);
        return result;
    }

    byte[] k1(byte[] N, byte[] salt, byte[] P) {
        byte[] T = new byte[16];
        CMac macT = new CMac(new AESEngine());
        macT.init(new KeyParameter(salt));
        macT.update(N, 0, N.length);
        macT.doFinal(T, 0);

        byte[] result = new byte[16];
        CMac mac = new CMac(new AESEngine());
        mac.init(new KeyParameter(T));
        mac.update(P, 0, P.length);
        mac.doFinal(result, 0);
        return result;
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

        CMac mac = new CMac(new AESEngine());
        mac.init(new KeyParameter(mConfirmationKey));
        mac.update(randomAuth, 0, randomAuth.length);
        mac.doFinal(mConfirmation, 0);
        Log.d(TAG, "Confirmation: " + new BigInteger(mConfirmation).toString(16));
        return mConfirmation;
    }

    public byte[] remoteConfirmation(byte[] randomBytes) {
        byte[] randomAuth = new byte[32];
        System.arraycopy(randomBytes, 0, randomAuth, 0, 16);
        System.arraycopy(mAuthValue, 0, randomAuth, 16, 16);
        Log.d(TAG, "Remote Random||AuthValue: " + new BigInteger(randomAuth).toString(16));
        byte[] confirmation = new byte[16];
        CMac mac = new CMac(new AESEngine());
        mac.init(new KeyParameter(mConfirmationKey));
        mac.update(randomAuth, 0, randomAuth.length);
        mac.doFinal(confirmation, 0);
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
        byte[] provisionSalt = s1(saltData);
        Log.d(TAG, " > ProvisionSalt: " + new BigInteger(provisionSalt).toString(16));
        byte[] sessionKey = k1(secret, provisionSalt, "prsk".getBytes());
        Log.d(TAG, " > SessionKey: " + new BigInteger(sessionKey).toString(16));
        byte[] sessionNonce = new byte[13];
        System.arraycopy(k1(secret, provisionSalt, "prsn".getBytes()), 3, sessionNonce, 0, 13);
        Log.d(TAG, " > Nonce: " + new BigInteger(sessionNonce).toString(16));
        Log.d(TAG, " > ProvisionData: " + new BigInteger(data).toString(16));

        CCMBlockCipher cipher = new CCMBlockCipher(new AESEngine());
        cipher.init(true, new AEADParameters(new KeyParameter(sessionKey), 64, sessionNonce));
        byte[] outputText = new byte[cipher.getOutputSize(data.length)];
        int outputLen = cipher.processBytes(data, 0, data.length, data, 0);
        try {
            cipher.doFinal(outputText, outputLen);
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
        }
        Log.d(TAG, " > EncData: " + new BigInteger(outputText).toString(16));
        //byte[] mic = cipher.getMac();
        //Log.d(TAG, " > MIC: " + new BigInteger(mic).toString(16));
        byte[] out = new byte[25 + 8];
        System.arraycopy(outputText, 0, out, 0, 25 + 8);
        //System.arraycopy(mic, 0, out, 25, 8);
        return out;
    }
}