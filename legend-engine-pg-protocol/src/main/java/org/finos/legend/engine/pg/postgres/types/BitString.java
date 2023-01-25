/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package org.finos.legend.engine.pg.postgres.types;

import java.util.BitSet;

class BitString  {

    public static BitString ofBitString(String bitString) {
        assert bitString.startsWith("B'") : "Bitstring must start with B'";
        assert bitString.endsWith("'") : "Bitstrign must end with '";
        return ofRawBits(bitString.substring(2, bitString.length() - 1));
    }

    public static BitString ofRawBits(String bits) {
        return ofRawBits(bits, bits.length());
    }

    public static BitString ofRawBits(String bits, int length) {
        BitSet bitSet = toBitSet(bits, length);
        return new BitString(bitSet, length);
    }

    private static BitSet toBitSet(String text, int length) {
        BitSet bitSet = new BitSet(length);
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            boolean value;
            if(c  == '0' ) {
                value = false;
            }else if(c =='1'){
                value  = true;
            }else {
                    throw new IllegalArgumentException("Bit string must only contain `0` or `1` values. Encountered: " + c);
            }
            bitSet.set(i, value);
        }
        return bitSet;
    }

    private final BitSet bitSet;
    private final int length;

    public BitString(BitSet bitSet, int length) {
        this.bitSet = bitSet;
        this.length = length;
    }

    public BitSet bitSet() {
        return bitSet;
    }

    public int length() {
        return length;
    }

    /**
     * Return bits as string with B' prefix. Example:
     *
     * <pre>
     *  B'1101'
     * </pre>
     */
    public String asPrefixedBitString() {
        StringBuilder sb = new StringBuilder("B'");
        for (int i = 0; i < length; i++) {
            sb.append(bitSet.get(i) ? '1' : '0');
        }
        sb.append("'");
        return sb.toString();
    }

    /**
     * Return bits as string without B' prefix. Example:
     *
     * <pre>
     *  1101
     * </pre>
     */
    public String asRawBitString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(bitSet.get(i) ? '1' : '0');
        }
        return sb.toString();
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + bitSet.hashCode();
        result = prime * result + length;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BitString other = (BitString) obj;
        return bitSet.equals(other.bitSet) && length == other.length;
    }

}
