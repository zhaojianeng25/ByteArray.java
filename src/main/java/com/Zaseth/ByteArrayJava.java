package com.Zaseth;

import java.io.UTFDataFormatException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ByteArrayJava {
    
    private byte[] data;
    
    private int position;
    private int nullBytes;
    
    private int BUFFER_SIZE = 1024;
    
    private boolean endian;
    private boolean BIG_ENDIAN = true;
    private boolean LITTLE_ENDIAN = false;
    
    /*
    Constructor
     */
    public ByteArrayJava(ByteArrayJava buff) {
        if (buff == null) {
            throw new IllegalArgumentException("Can't read from empty byte stream");
        }
        if (buff instanceof ByteArrayJava) {
            this.data = buff.data;
        }
        this.position = 0;
        this.nullBytes = 0;
        this.endian = this.BIG_ENDIAN;
    }
    
    public ByteArrayJava(int length) {
        this.data = new byte[length];
        this.position = 0;
        this.nullBytes = 0;
        this.endian = this.BIG_ENDIAN;
    }
    
    public ByteArrayJava() {
        this.data = new byte[this.BUFFER_SIZE];
        this.position = 0;
        this.nullBytes = 0;
        this.endian = this.BIG_ENDIAN;
    }
    
    /*
    Set, get and constructor functions
     */
    public void clear() {
        this.position = 0;
        this.data = new byte[this.BUFFER_SIZE];
    }
    
    public void clear(byte[] data, int position) {
        this.data = data;
        this.position = position;
    }
    
    public void setEndian(boolean e) {
        this.endian = e;
    }
    
    public boolean getEndian() {
        return this.endian;
    }
    
    public int moveLeft(int v) {
        return this.position -= v;
    }
    
    public int moveRight(int v) {
        return this.position += v;
    }
    
    public void grow(int what, int by) {
        this.BUFFER_SIZE = Math.max(this.position + by, this.BUFFER_SIZE);
    }
    
    public int length() {
        return this.data == null ? 0 : this.data.length;
    }
    
    /*
    Data retrieval functions
     */
    public int bytesAvailable() {
        int value = this.length() - this.position;
        if (value > this.length() || value < 0) {
            return 0;
        }
        if (this.position >= this.length()) {
            return -1;
        }
        return value;
    }
    
    @Override
    public String toString() {
        return this.Debug(new StringBuilder().append("Bytes available: " + this.bytesAvailable() + "\r\nPosition: " + this.position + "\r\nNullbytes: " + this.nullBytes + "\r\nByte stream: " + Arrays.toString(this.data).substring(0, 120)).toString());
    }
    
    public String Debug(String debugMessage) {
        return "<DEBUG>\r\n" + debugMessage + "\r\n</DEBUG>";
    }
    
    /*
    Extra method functions
     */
    public byte atomicCompareAndSwapIntAt(int byteIndex, int expectedValue, int newValue) {
        byte value = this.data[byteIndex];
        if (value == expectedValue) {
            this.data[byteIndex] = (byte) newValue;
        }
        return value;
    }
    
    public int atomicCompareAndSwapLength(int expectedLength, int newLength) {
        int prevLength = this.length();
        if (prevLength != expectedLength) {
            return prevLength;
        } else if (prevLength < newLength) {
            this.data = new byte[this.length() + (newLength - prevLength)];
            int i = 0;
            for (Byte b : new ArrayList<byte[]>(Arrays.asList(this.data)).toArray(new Byte[this.length() + (newLength - prevLength)])) {
                this.data[i++] = b.byteValue();
            }
        } else if (prevLength > newLength) {
            this.data = Arrays.copyOfRange(this.data, newLength - 1, prevLength - 1);
        }
        return prevLength;
    }
    
    /*
    Help functions
     */
    public static String fromCharCode(int... codePoints) { // https://stackoverflow.com/a/2946081/6636193
        StringBuilder builder = new StringBuilder(codePoints.length);
        for (int codePoint : codePoints) {
            builder.append(Character.toChars(codePoint));
        }
        return builder.toString();
    }
    
    private void checkInt(int value, int offset, int ext, int max, int min) {
        this.bytesAvailable();
        if (value > max || value < min) {
            throw new ArrayIndexOutOfBoundsException("Value argument is out of bounds");
        }
        if (offset + ext > this.length()) {
            throw new ArrayIndexOutOfBoundsException("Index argument is out of range");
        }
    }
    
    private void checkOffset(int offset, int ext, int length) {
        if ((offset % 1) != 0 || offset < 0) {
            throw new IllegalArgumentException("Offset is not uint");
        }
        if (offset + ext > length) {
            throw new ArrayIndexOutOfBoundsException("Trying to access beyond buffer length");
        }
    }
    
    public byte get7BitValueSize(int value) {
        return this.get7BitValueSize((long) value);
    }
    
    public static byte get7BitValueSize(long value) {
        long limit = 0x80;
        byte result = 1;
        while (value >= limit) {
            limit <<= 7;
            ++result;
        }
        return result;
    }
    
    /*
    Writing int and uint functions
     */
    public void writeInt8(int v) {
        v = +v;
        this.checkInt(v, this.position, 1, 0x7f, -0x80);
        if (v < 0) v = 0xff + v + 1;
        this.data[this.position++] = (byte) v;
    }
    
    public void writeRawByte(int v) {
        this.data[this.position++] = (byte) v;
    }
    
    public void writeInt16(int v) {
        v = +v;
        this.checkInt(v, this.position, 2, 0x7fff, -0x8000);
        if (this.endian) {
            this.data[this.position++] = (byte) (v >> 8);
            this.data[this.position++] = (byte) v;
        } else {
            this.data[this.position++] = (byte) v;
            this.data[this.position++] = (byte) (v >> 8);
        }
    }
    
    public void writeInt24(int v) {
        v = +v;
        this.checkInt(v, this.position, 3, 0x7fffff, -0x800000);
        if (this.endian) {
            this.data[this.position++] = (byte) (v >> 16);
            this.data[this.position++] = (byte) (v >> 8);
            this.data[this.position++] = (byte) v;
        } else {
            this.data[this.position++] = (byte) v;
            this.data[this.position++] = (byte) (v >> 8);
            this.data[this.position++] = (byte) (v >> 16);
        }
    }
    
    public void writeInt29(int v) {
        if (v < -0x10000000 || v > 0x0fffffff) {
            throw new IllegalArgumentException("Integer must be between -0x10000000 and 0x0fffffff but got " + v + " instead");
        }
        v += v < 0 ? 0x20000000 : 0;
        if (v > 0x1fffff) {
            v >>= 1;
            this.writeInt8(0x80 | ((v >> 21) & 0xff));
        }
        if (v > 0x3fff) {
            this.writeInt8(0x80 | ((v >> 14) & 0xff));
        }
        if (v > 0x7f) {
            this.writeInt8(0x80 | ((v >> 7) & 0xff));
        }
        if (v > 0x1fffff) {
            this.writeInt8(v & 0xff);
        } else {
            this.writeInt8(v & 0x7f);
        }
    }
    
    public void writeInt32(int v) {
        v = +v;
        this.checkInt(v, this.position, 4, 0x7fffffff, -0x80000000);
        if (this.endian) {
            if (v < 0) v = 0xffffffff + v + 1;
            this.data[this.position++] = (byte) (v >> 24);
            this.data[this.position++] = (byte) (v >> 16);
            this.data[this.position++] = (byte) (v >> 8);
            this.data[this.position++] = (byte) v;
        } else {
            this.data[this.position++] = (byte) v;
            this.data[this.position++] = (byte) (v >> 8);
            this.data[this.position++] = (byte) (v >> 16);
            this.data[this.position++] = (byte) (v >> 24);
        }
    }
    
    public void writeInt40(long v) {
        v = +v;
        if (this.endian) {
            this.data[this.position++] = (byte) (v >> 32);
            this.data[this.position++] = (byte) (v >> 24);
            this.data[this.position++] = (byte) (v >> 16);
            this.data[this.position++] = (byte) (v >> 8);
            this.data[this.position++] = (byte) v;
        } else {
            this.data[this.position++] = (byte) v;
            this.data[this.position++] = (byte) (v >> 8);
            this.data[this.position++] = (byte) (v >> 16);
            this.data[this.position++] = (byte) (v >> 24);
            this.data[this.position++] = (byte) (v >> 32);
        }
    }
    
    public void writeInt48(long v) {
        v = +v;
        if (this.endian) {
            this.data[this.position++] = (byte) (v >> 40);
            this.data[this.position++] = (byte) (v >> 32);
            this.data[this.position++] = (byte) (v >> 24);
            this.data[this.position++] = (byte) (v >> 16);
            this.data[this.position++] = (byte) (v >> 8);
            this.data[this.position++] = (byte) v;
        } else {
            this.data[this.position++] = (byte) v;
            this.data[this.position++] = (byte) (v >> 8);
            this.data[this.position++] = (byte) (v >> 16);
            this.data[this.position++] = (byte) (v >> 24);
            this.data[this.position++] = (byte) (v >> 32);
            this.data[this.position++] = (byte) (v >> 40);
        }
    }
    
    public void writeInt56(long v) {
        v = +v;
        if (this.endian) {
            this.data[this.position++] = (byte) (v >> 48);
            this.data[this.position++] = (byte) (v >> 40);
            this.data[this.position++] = (byte) (v >> 32);
            this.data[this.position++] = (byte) (v >> 24);
            this.data[this.position++] = (byte) (v >> 16);
            this.data[this.position++] = (byte) (v >> 8);
            this.data[this.position++] = (byte) v;
        } else {
            this.data[this.position++] = (byte) v;
            this.data[this.position++] = (byte) (v >> 8);
            this.data[this.position++] = (byte) (v >> 16);
            this.data[this.position++] = (byte) (v >> 24);
            this.data[this.position++] = (byte) (v >> 32);
            this.data[this.position++] = (byte) (v >> 40);
            this.data[this.position++] = (byte) (v >> 48);
        }
    }
    
    public void writeInt64(long v) {
        v = +v;
        if (this.endian) {
            this.data[this.position++] = (byte) (v >> 56);
            this.data[this.position++] = (byte) (v >> 48);
            this.data[this.position++] = (byte) (v >> 40);
            this.data[this.position++] = (byte) (v >> 32);
            this.data[this.position++] = (byte) (v >> 24);
            this.data[this.position++] = (byte) (v >> 16);
            this.data[this.position++] = (byte) (v >> 8);
            this.data[this.position++] = (byte) v;
        } else {
            this.data[this.position++] = (byte) v;
            this.data[this.position++] = (byte) (v >> 8);
            this.data[this.position++] = (byte) (v >> 16);
            this.data[this.position++] = (byte) (v >> 24);
            this.data[this.position++] = (byte) (v >> 32);
            this.data[this.position++] = (byte) (v >> 40);
            this.data[this.position++] = (byte) (v >> 48);
            this.data[this.position++] = (byte) (v >> 56);
        }
    }
    
    public void writeUInt8(int v) {
        v = +v;
        this.checkInt(v, this.position, 1, 0xff, 0);
        this.data[this.position++] = (byte) (v & 0xff);
    }
    
    public void writeUInt16(int v) {
        v = +v;
        this.checkInt(v, this.position, 2, 0xffff, 0);
        if (this.endian) {
            this.data[this.position++] = (byte) (v >>> 8);
            this.data[this.position++] = (byte) (v & 0xff);
        } else {
            this.data[this.position++] = (byte) (v & 0xff);
            this.data[this.position++] = (byte) (v >>> 8);
        }
    }
    
    public void writeUInt24(int v) {
        v = +v;
        this.checkInt(v, this.position, 3, 0xffffff, 0);
        if (this.endian) {
            this.data[this.position++] = (byte) (v >>> 16);
            this.data[this.position++] = (byte) (v >>> 8);
            this.data[this.position++] = (byte) (v & 0xff);
        } else {
            this.data[this.position++] = (byte) (v & 0xff);
            this.data[this.position++] = (byte) (v >>> 8);
            this.data[this.position++] = (byte) (v >>> 16);
        }
    }
    
    public void writeUInt29(int v) {
        if (128 > v) {
            this.writeInt8(v);
        } else if (16384 > v) {
            this.writeInt8(v >>> 7 & 127 | 128);
            this.writeInt8(v & 127);
        } else if (2097152 > v) {
            this.writeInt8(v >>> 14 & 127 | 128);
            this.writeInt8(v >>> 7 & 127 | 128);
            this.writeInt8(v & 127);
        } else if (1073741824 > v) {
            this.writeInt8(v >>> 22 & 127 | 128);
            this.writeInt8(v >>> 15 & 127 | 128);
            this.writeInt8(v >>> 8 & 127 | 128);
            this.writeInt8(v & 255);
        } else {
            throw new IllegalArgumentException("Integer out of range: " + v);
        }
    }
    
    public void writeUInt32(int v) {
        v = +v;
        this.checkInt(v, this.position, 4, 0xffffffff, 0);
        if (this.endian) {
            this.data[this.position++] = (byte) (v >>> 24);
            this.data[this.position++] = (byte) (v >>> 16);
            this.data[this.position++] = (byte) (v >>> 8);
            this.data[this.position++] = (byte) (v & 0xff);
        } else {
            this.data[this.position++] = (byte) (v & 0xff);
            this.data[this.position++] = (byte) (v >>> 8);
            this.data[this.position++] = (byte) (v >>> 16);
            this.data[this.position++] = (byte) (v >>> 24);
        }
    }
    
    public void writeUInt40(long v) {
        v = +v;
        if (this.endian) {
            this.data[this.position++] = (byte) (v >>> 32);
            this.data[this.position++] = (byte) (v >>> 24);
            this.data[this.position++] = (byte) (v >>> 16);
            this.data[this.position++] = (byte) (v >>> 8);
            this.data[this.position++] = (byte) (v & 0xff);
        } else {
            this.data[this.position++] = (byte) (v & 0xff);
            this.data[this.position++] = (byte) (v >>> 8);
            this.data[this.position++] = (byte) (v >>> 16);
            this.data[this.position++] = (byte) (v >>> 24);
            this.data[this.position++] = (byte) (v >>> 32);
        }
    }
    
    public void writeUInt48(long v) {
        v = +v;
        if (this.endian) {
            this.data[this.position++] = (byte) (v >>> 40);
            this.data[this.position++] = (byte) (v >>> 32);
            this.data[this.position++] = (byte) (v >>> 24);
            this.data[this.position++] = (byte) (v >>> 16);
            this.data[this.position++] = (byte) (v >>> 8);
            this.data[this.position++] = (byte) (v & 0xff);
        } else {
            this.data[this.position++] = (byte) (v & 0xff);
            this.data[this.position++] = (byte) (v >>> 8);
            this.data[this.position++] = (byte) (v >>> 16);
            this.data[this.position++] = (byte) (v >>> 24);
            this.data[this.position++] = (byte) (v >>> 32);
            this.data[this.position++] = (byte) (v >>> 40);
        }
    }
    
    public void writeUInt56(long v) {
        v = +v;
        if (this.endian) {
            this.data[this.position++] = (byte) (v >>> 48);
            this.data[this.position++] = (byte) (v >>> 40);
            this.data[this.position++] = (byte) (v >>> 32);
            this.data[this.position++] = (byte) (v >>> 24);
            this.data[this.position++] = (byte) (v >>> 16);
            this.data[this.position++] = (byte) (v >>> 8);
            this.data[this.position++] = (byte) (v & 0xff);
        } else {
            this.data[this.position++] = (byte) (v & 0xff);
            this.data[this.position++] = (byte) (v >>> 8);
            this.data[this.position++] = (byte) (v >>> 16);
            this.data[this.position++] = (byte) (v >>> 24);
            this.data[this.position++] = (byte) (v >>> 32);
            this.data[this.position++] = (byte) (v >>> 40);
            this.data[this.position++] = (byte) (v >>> 48);
        }
    }
    
    public void writeUInt64(long v) {
        v = +v;
        if (this.endian) {
            this.data[this.position++] = (byte) (v >>> 56);
            this.data[this.position++] = (byte) (v >>> 48);
            this.data[this.position++] = (byte) (v >>> 40);
            this.data[this.position++] = (byte) (v >>> 32);
            this.data[this.position++] = (byte) (v >>> 24);
            this.data[this.position++] = (byte) (v >>> 16);
            this.data[this.position++] = (byte) (v >>> 8);
            this.data[this.position++] = (byte) (v & 0xff);
        } else {
            this.data[this.position++] = (byte) (v & 0xff);
            this.data[this.position++] = (byte) (v >>> 8);
            this.data[this.position++] = (byte) (v >>> 16);
            this.data[this.position++] = (byte) (v >>> 24);
            this.data[this.position++] = (byte) (v >>> 32);
            this.data[this.position++] = (byte) (v >>> 40);
            this.data[this.position++] = (byte) (v >>> 48);
            this.data[this.position++] = (byte) (v >>> 56);
        }
    }
    
    /*
    Read int and uint functions
    */
    public int readInt8() {
        this.checkOffset(this.position, 1, this.length());
        return this.data[this.position++];
    }
    
    public byte readRawByte() {
        return this.data[this.position++];
    }
    
    public int readInt16() {
        this.checkOffset(this.position, 2, this.length());
        if (this.endian) {
            return this.data[this.position++] << 8 | this.data[this.position++];
        } else {
            return this.data[this.position++] | this.data[this.position++] << 8;
        }
    }
    
    public int readInt24() {
        this.checkOffset(this.position, 3, this.length());
        if (this.endian) {
            return this.data[this.position++] << 16 | this.data[this.position++] << 8 | this.data[this.position++];
        } else {
            return this.data[this.position++] | this.data[this.position++] << 8 | this.data[this.position++] << 16;
        }
    }
    
    public int readInt29() {
        int total = this.readInt8();
        if (total < 128) {
            return total;
        }
        total = (total & 0x7f) << 7;
        int nextByte = this.readInt8();
        if (nextByte < 128) {
            total = total | nextByte;
        } else {
            total = (total | nextByte & 0x7f) << 7;
            nextByte = this.readInt8();
            if (nextByte < 128) {
                total = total | nextByte;
            } else {
                total = (total | nextByte & 0x7f) << 8;
                nextByte = this.readInt8();
                total = total | nextByte;
            }
        }
        int mask = 1 << 28;
        return -(total & mask) | total;
    }
    
    public int readInt32() {
        this.checkOffset(this.position, 4, this.length());
        if (this.endian) {
            return this.data[this.position++] << 24 | this.data[this.position++] << 16 | this.data[this.position++] << 8
                    | this.data[this.position++];
        } else {
            return this.data[this.position++] | this.data[this.position++] << 8 | this.data[this.position++] << 16
                    | this.data[this.position++] << 24;
        }
    }
    
    public long readInt40() {
        if (this.endian) {
            return this.data[this.position++] << 32 | this.data[this.position++] << 24 | this.data[this.position++] << 16
                    | this.data[this.position++] << 8 | this.data[this.position++];
        } else {
            return this.data[this.position++] | this.data[this.position++] << 8 | this.data[this.position++] << 16
                    | this.data[this.position++] << 24 | this.data[this.position++] << 32;
        }
    }
    
    public long readInt48() {
        if (this.endian) {
            return this.data[this.position++] << 40 | this.data[this.position++] << 32 | this.data[this.position++] << 24
                    | this.data[this.position++] << 16 | this.data[this.position++] << 8 | this.data[this.position++];
        } else {
            return this.data[this.position++] | this.data[this.position++] << 8 | this.data[this.position++] << 16
                    | this.data[this.position++] << 24 | this.data[this.position++] << 32 | this.data[this.position++] << 40;
        }
    }
    
    public long readInt56() {
        if (this.endian) {
            return this.data[this.position++] << 48 | this.data[this.position++] << 40 | this.data[this.position++] << 32
                    | this.data[this.position++] << 24 | this.data[this.position++] << 16 | this.data[this.position++] << 8
                    | this.data[this.position++];
        } else {
            return this.data[this.position++] | this.data[this.position++] << 8 | this.data[this.position++] << 16
                    | this.data[this.position++] << 24 | this.data[this.position++] << 32 | this.data[this.position++] << 40
                    | this.data[this.position++] << 48;
        }
    }
    
    public long readInt64() {
        if (this.endian) {
            return this.data[this.position++] << 56 | this.data[this.position++] << 48 | this.data[this.position++] << 40
                    | this.data[this.position++] << 32 | this.data[this.position++] << 24 | this.data[this.position++] << 16
                    | this.data[this.position++] << 8 | this.data[this.position++];
        } else {
            return this.data[this.position++] | this.data[this.position++] << 8 | this.data[this.position++] << 16
                    | this.data[this.position++] << 24 | this.data[this.position++] << 32 | this.data[this.position++] << 40
                    | this.data[this.position++] << 48 | this.data[this.position++] << 56;
        }
    }
    
    public int readUInt8() {
        this.checkOffset(this.position, 1, this.length());
        return this.data[this.position++] & 0xff;
    }
    
    public int readUInt16() {
        this.checkOffset(this.position, 2, this.length());
        if (this.endian) {
            return this.data[this.position++] << 8 | this.data[this.position++] & 0xff;
        } else {
            return this.data[this.position++] & 0xff | this.data[this.position++] << 8;
        }
    }
    
    public int readUInt24() {
        this.checkOffset(this.position, 3, this.length());
        if (this.endian) {
            return this.data[this.position++] << 16 | this.data[this.position++] << 8 | this.data[this.position++] & 0xff;
        } else {
            return this.data[this.position++] & 0xff | this.data[this.position++] << 8 | this.data[this.position++] << 16;
        }
    }
    
    public int readUInt29() {
        int b = this.readUInt8();
        if (b < 128) {
            return b;
        }
        int value = (b & 0x7F) << 7;
        b = this.readUInt8();
        if (b < 128) {
            return (value | b);
        }
        value = (value | (b & 0x7F)) << 7;
        b = this.readUInt8();
        if (b < 128) {
            return (value | b);
        }
        value = (value | (b & 0x7F)) << 8;
        b = this.readUInt8();
        return (value | b);
    }
    
    public int readUInt32() {
        this.checkOffset(this.position, 4, this.length());
        if (this.endian) {
            return this.data[this.position++] << 24 | this.data[this.position++] << 16 | this.data[this.position++] << 8
                    | this.data[this.position++] & 0xff;
        } else {
            return this.data[this.position++] & 0xff | this.data[this.position++] << 8 | this.data[this.position++] << 16
                    | this.data[this.position++] << 24;
        }
    }
    
    public long readUInt40() {
        if (this.endian) {
            return this.data[this.position++] << 32 | this.data[this.position++] << 24 | this.data[this.position++] << 16
                    | this.data[this.position++] << 8 | this.data[this.position++] & 0xff;
        } else {
            return this.data[this.position++] & 0xff | this.data[this.position++] << 8 | this.data[this.position++] << 16
                    | this.data[this.position++] << 24 | this.data[this.position++] << 32;
        }
    }
    
    public long readUInt48() {
        if (this.endian) {
            return this.data[this.position++] << 40 | this.data[this.position++] << 32 | this.data[this.position++] << 24
                    | this.data[this.position++] << 16 | this.data[this.position++] << 8 | this.data[this.position++] & 0xff;
        } else {
            return this.data[this.position++] & 0xff | this.data[this.position++] << 8 | this.data[this.position++] << 16
                    | this.data[this.position++] << 24 | this.data[this.position++] << 32 | this.data[this.position++] << 40;
        }
    }
    
    public long readUInt56() {
        if (this.endian) {
            return this.data[this.position++] << 48 | this.data[this.position++] << 40 | this.data[this.position++] << 32
                    | this.data[this.position++] << 24 | this.data[this.position++] << 16 | this.data[this.position++] << 8
                    | this.data[this.position++] & 0xff;
        } else {
            return this.data[this.position++] & 0xff | this.data[this.position++] << 8 | this.data[this.position++] << 16
                    | this.data[this.position++] << 24 | this.data[this.position++] << 32 | this.data[this.position++] << 40
                    | this.data[this.position++] << 48;
        }
    }
    
    public long readUInt64() {
        if (this.endian) {
            return this.data[this.position++] << 56 | this.data[this.position++] << 48 | this.data[this.position++] << 40
                    | this.data[this.position++] << 32 | this.data[this.position++] << 24 | this.data[this.position++] << 16
                    | this.data[this.position++] << 8 | this.data[this.position++] & 0xff;
        } else {
            return this.data[this.position++] & 0xff | this.data[this.position++] << 8 | this.data[this.position++] << 16
                    | this.data[this.position++] << 24 | this.data[this.position++] << 32 | this.data[this.position++] << 40
                    | this.data[this.position++] << 48 | this.data[this.position++] << 56;
        }
    }
    
    /*
    Write IEEE 754 single-precision (32-bit) and IEEE 754 double-precision (64-bit) functions
     */
    public void writeFloat(float v) {
        this.writeInt32(Float.floatToIntBits(v));
    }
    
    public void writeDouble(double v) {
        this.writeInt64(Double.doubleToLongBits(v));
    }
    
    /*
    Reads IEEE 754 single-precision (32-bit) and IEEE 754 double-precision (64-bit) functions
     */
    public double readDouble() {
        return Double.longBitsToDouble(this.readInt64());
    }
    
    public float readFloat() {
        return Float.intBitsToFloat(this.readInt32());
    }
    
    /*
    Writing varint and varuint functions
     */
    public void write7BitEncodedInt(int value) {
        byte shift = (byte) ((this.get7BitValueSize(value) - 1) * 7);
        boolean max = false;
        if (shift >= 21) {
            shift = 22;
            max = true;
        }
        while (shift >= 7) {
            this.writeInt8((0x80 | ((value >> shift) & 0x7F)));
            shift -= 7;
        }
        this.writeInt8((max ? (value & 0xFF) : (value & 0x7F)));
    }
    
    public void write7BitEncodedLong(long value) {
        byte shift = (byte) ((this.get7BitValueSize(value) - 1) * 7);
        boolean max = (shift >= 63);
        if (max) {
            shift++;
        }
        while (shift >= 7) {
            this.writeInt8((byte) (0x80 | ((value >> shift) & 0x7F)));
            shift -= 7;
        }
        this.writeInt8((byte) (max ? (value & 0xFF) : (value & 0x7F)));
    }
    
    public void writeVarInt32(int value) {
        while (true) {
            if ((value & ~0x7F) == 0) {
                this.writeInt8(value);
                return;
            } else {
                this.writeInt8((value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }
    
    public void writeVarInt64(long value) {
        while (true) {
            if ((value & ~0x7F) == 0) {
                this.writeInt8((int) value);
                return;
            } else {
                this.writeInt8(((int) value & 0x7F) | 0x80);
                value >>>= 7;
            }
        }
    }
    
    public void writeVarUInt32(int value) {
        this.writeVarInt32(value << 1 ^ value >> 31); // EncodeZigZag32
    }
    
    public void writeVarUInt64(long value) {
        this.writeVarInt64(value << 1 ^ value >> 63); // EncodeZigZag64
    }
    
    /*
    Reading varint and varuint functions
     */
    public int read7BitEncodedInt() {
        int n = 0;
        int b = this.readUInt8();
        int result = 0;
        while (b >= 128 && n < 3) {
            result <<= 7;
            result |= (b & 0x7F);
            b = this.readUInt8();
            ++n;
        }
        result <<= ((n < 3) ? 7 : 8);
        result |= b;
        return result;
    }
    
    public long read7BitEncodedLong() {
        int n = 0;
        int b = this.readUInt8();
        long result = 0;
        while (b >= 128 && n < 8) {
            result <<= 7;
            result |= (b & 0x7F);
            b = this.readUInt8();
            ++n;
        }
        result <<= ((n < 8) ? 7 : 8);
        result |= b;
        return result;
    }
    
    public int readVarInt32() {
        byte tmp = (byte) this.readInt8();
        if (tmp >= 0) {
            return tmp;
        }
        int result = tmp & 0x7f;
        if ((tmp = (byte) this.readInt8()) >= 0) {
            result |= tmp << 7;
        } else {
            result |= (tmp & 0x7f) << 7;
            if ((tmp = (byte) this.readInt8()) >= 0) {
                result |= tmp << 14;
            } else {
                result |= (tmp & 0x7f) << 14;
                if ((tmp = (byte) this.readInt8()) >= 0) {
                    result |= tmp << 21;
                } else {
                    result |= (tmp & 0x7f) << 21;
                    result |= (tmp = (byte) this.readInt8()) << 28;
                    if (tmp < 0) {
                        // Discard upper 32 bits.
                        for (int i = 0; i < 5; i++) {
                            if (this.readInt8() >= 0) {
                                return result;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
    
    public long readVarInt64() {
        int shift = 0;
        long result = 0;
        while (shift < 64) {
            final byte b = (byte) this.readInt8();
            result |= (long) (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return result;
            }
            shift += 7;
        }
        return result;
    }
    
    public int readVarUInt32() {
        return this.readVarInt32() >>> 1 ^ -(this.readVarInt32() & 1); // DecodeZigZag32
    }
    
    public long readVarUInt64() {
        return this.readVarInt64() >>> 1 ^ -(this.readVarInt64() & 1L); // DecodeZigZag64
    }
    
    /*
    Extra write functions
     */
    public void writeUTF(String s) throws UTFDataFormatException {
        int utfLength = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch > 0 && ch < 0x80) {
                utfLength++;
            } else if (ch == 0 || (ch >= 0x80 && ch < 0x800)) {
                utfLength += 2;
            } else {
                utfLength += 3;
            }
        }
        if (utfLength > 65535) {
            throw new UTFDataFormatException();
        }
        this.writeInt16(utfLength);
        for (int i = 0; i < s.length(); i++) {
            int ch = s.charAt(i);
            if (ch > 0 && ch < 0x80) {
                this.writeInt8(ch);
            } else if (ch == 0 || (ch >= 0x80 && ch < 0x800)) {
                this.writeInt8(0xc0 | (0x1f & (ch >> 6)));
                this.writeInt8(0x80 | (0x3f & ch));
            } else {
                this.writeInt8(0xe0 | (0x0f & (ch >> 12)));
                this.writeInt8(0x80 | (0x3f & (ch >> 6)));
                this.writeInt8(0x80 | (0x3f & ch));
            }
        }
    }
    
    public void writeMultiByte(String v, String charset) {
        Charset cs = Charset.forName(charset);
        if (this.endian) {
            if (charset.equals("UTF-16LE") || charset.equals("UTF-32LE")) {
                throw new IllegalArgumentException("Unmatched charset for current endian"); // Using Big endian but trying to use Little endian
                // charset
            }
        } else {
            if (charset.equals("UTF-16BE") || charset.equals("UTF-32BE")) {
                throw new IllegalArgumentException("Unmatched charset for current endian"); // Using Little endian but trying to use Big endian
                // charset
            }
        }
        this.writeInt8Array(v.getBytes(cs)); // Converts the string into bytes
    }
    
    public void writeInt8Array(byte[] v) {
        int var3 = v.length;
        for (int var4 = 0; var4 < var3; ++var4) {
            byte element$iv = v[var4];
            this.writeInt8(element$iv);
            if (this.data[var4] == 0) {
                this.nullBytes++;
            }
        }
    }
    
    public void writeBytes(byte bytes[], int offset, int length) {
        if ((offset < 0) || (offset > bytes.length) || (length < 0) || ((offset + length) > bytes.length) || ((offset + length) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (length == 0) {
            return;
        }
        System.arraycopy(bytes, offset, this.data, this.position, length);
        this.position = this.position + length;
    }
    
    public void writeBoolean(boolean v) {
        if (v) {
            this.writeInt8(1);
        } else {
            this.writeInt8(0);
        }
    }
    
    /*
    Extra read functions
     */
    public String readUTF() throws UTFDataFormatException {
        int utfLength = this.readInt16() & 0xffff;
        int goalPosition = this.position + utfLength;
        StringBuffer string = new StringBuffer(utfLength);
        while (this.position < goalPosition) {
            int a = this.readInt8() & 0xff;
            if ((a & 0x80) == 0) {
                string.append((char) a);
            } else {
                int b = this.readInt8() & 0xff;
                if ((b & 0xc0) != 0x80) {
                    throw new UTFDataFormatException();
                }
                if ((a & 0xe0) == 0xc0) {
                    char ch = (char) (((a & 0x1f) << 6) | (b & 0x3f));
                    string.append(ch);
                } else if ((a & 0xf0) == 0xe0) {
                    int c = this.readInt8() & 0xff;
                    if ((c & 0xc0) != 0x80) {
                        throw new UTFDataFormatException();
                    }
                    char ch = (char) (((a & 0x0f) << 12) | ((b & 0x3f) << 6) | (c & 0x3f));
                    string.append(ch);
                } else {
                    throw new UTFDataFormatException();
                }
            }
        }
        return string.toString();
    }
    
    public List<Character> readMultiByte(int length) {
        List<Character> array = new ArrayList<Character>();
        for (int i = 0; i < length; i++) {
            array.add((char) this.data[i]);
        }
        return array;
    }
    
    public List<Integer> readInt8Array(int length) {
        ArrayList<Integer> array = new ArrayList<Integer>();
        for (int i = 0; i < length; i++) {
            array.add(this.readInt8());
        }
        return array;
    }
    
    public byte[] readBytes(int length) {
        byte bytes[] = Arrays.copyOfRange(this.data, this.position, this.position + length);
        this.position += length;
        return bytes;
    }
    
    public boolean readBoolean() {
        return this.readInt8() == 1;
    }
    
    public static void main(String[] args) throws UTFDataFormatException {
        ByteArrayJava wba = new ByteArrayJava();
        wba.writeBoolean(true);
        ByteArrayJava rba = new ByteArrayJava(wba);
        System.out.println(rba.readBoolean());
        System.out.println(wba.toString());
    }
}