package ru.ximen.mesh;

import android.util.Log;

import org.spongycastle.crypto.BlockCipher;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.macs.CMac;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.spec.ECParameterSpec;
import org.spongycastle.jce.spec.ECPublicKeySpec;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import javax.crypto.KeyAgreement;


/**
 * Created by ximen on 07.04.18.
 */

public class MeshEC {
    final static private String TAG = "MeshEC";
    private KeyPair pair;

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

    void calculateSecret() throws InvalidKeyException {
        ECParameterSpec paramSpec = ECNamedCurveTable.getParameterSpec("P-256");
        KeyAgreement agr = null;
        try {
            agr = KeyAgreement.getInstance("ECDH", "SC");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        try {
            agr.init(pair.getPrivate());
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        //KeyG
        //agr.doPhase(, true);
        byte[] secret = agr.generateSecret();
    }
}
