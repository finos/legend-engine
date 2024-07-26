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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.cipher;

import org.apache.commons.codec.binary.Base64;
import org.finos.legend.pure.runtime.java.extension.functions.shared.cipher.AESCipherUtil;
import org.junit.Assert;
import org.junit.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class AESCipherUtilTest
{
    @Test
    public void testEncryptDeCrypt() throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException
    {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[16]; // 128 bits
        secureRandom.nextBytes(key);
        String keyString = Base64.encodeBase64String(key);

        String plainText = "This is a secret!";

        byte[] cipherMessage = AESCipherUtil.encrypt(keyString, plainText.getBytes());
        byte[] decrypted = AESCipherUtil.decrypt(keyString, cipherMessage);
        Assert.assertEquals(plainText, new String(decrypted));
    }
}