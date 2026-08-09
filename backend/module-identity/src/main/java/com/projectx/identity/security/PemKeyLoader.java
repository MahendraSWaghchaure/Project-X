package com.projectx.identity.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/** Reads a PKCS#8 RSA private key + X.509 RSA public key from PEM files (e.g. `openssl genrsa`/`openssl rsa -pubout` output). */
final class PemKeyLoader {

    private PemKeyLoader() {}

    static KeyPair load(ResourceLoader resourceLoader, String privateKeyLocation, String publicKeyLocation) {
        try {
            RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(readDer(resourceLoader, privateKeyLocation)));
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(readDer(resourceLoader, publicKeyLocation)));
            return new KeyPair(publicKey, privateKey);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException(
                    "Could not load RSA JWT signing key from " + privateKeyLocation + " / " + publicKeyLocation, e);
        }
    }

    private static byte[] readDer(ResourceLoader resourceLoader, String location) throws IOException {
        Resource resource = resourceLoader.getResource(location);
        String pem;
        try (InputStream in = resource.getInputStream()) {
            pem = new String(in.readAllBytes());
        }
        String base64 = pem.replaceAll("-----(BEGIN|END)[A-Z ]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }
}
