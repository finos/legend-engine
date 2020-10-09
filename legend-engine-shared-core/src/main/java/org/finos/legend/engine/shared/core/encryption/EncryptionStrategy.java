// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.shared.core.encryption;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public enum EncryptionStrategy
{
    DES
            {
                @Override
                public CipherInputStream execute(InputStream inputStream, String key) throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException
                {
                    byte[] iv = new byte[]{(byte) 0x8E, 0x12, 0x39, (byte) 0x9C, 0x07, 0x72, 0x6F, 0x5A};
                    DESKeySpec dks = new DESKeySpec(key.getBytes());
                    SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
                    SecretKey desKey = skf.generateSecret(dks);
                    Cipher decipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
                    decipher.init(Cipher.DECRYPT_MODE, desKey, new IvParameterSpec(iv));
                    return decrypt(inputStream, decipher);
                }
            },

    AES
            {
                @Override
                public CipherInputStream execute(InputStream inputStream, String key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException
                {
                    Cipher decipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                    byte[] iv = decipher.getIV();
                    SecretKey secretKey = new SecretKeySpec(key.getBytes(), "AES");
                    decipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
                    return decrypt(inputStream, decipher);
                }
            };

    public abstract CipherInputStream execute(InputStream inputStream, String key) throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidAlgorithmParameterException;

    private static CipherInputStream decrypt(InputStream is, Cipher decipher)
    {
        CipherInputStream cipherInputStream = new CipherInputStream(is, decipher);
        return cipherInputStream;
    }
}
