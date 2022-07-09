package info.skyblond.paperang;

/**
 * <a href="https://github.com/ymnk/jzlib/blob/master/src/main/java/com/jcraft/jzlib/Checksum.java">Original</a>
 */
final public class CRC32 {
    /*
     *  The following logic has come from RFC1952.
     */
    private int crc = 0;
    private static final int[] crc_table;

    static {
        crc_table = new int[256];
        for (int n = 0; n < 256; n++) {
            int c = n;
            for (int k = 8; --k >= 0; ) {
                if ((c & 1) != 0) {
                    c = 0xedb88320 ^ (c >>> 1);
                } else {
                    c = c >>> 1;
                }
            }
            crc_table[n] = c;
        }
    }

    public void update(byte[] buf, int index, int len) {
        int c = ~this.crc;
        while (--len >= 0) {
            c = crc_table[(c ^ buf[index++]) & 0xff] ^ (c >>> 8);
        }
        this.crc = ~c;
    }

    public void reset() {
        this.crc = 0;
    }

    public void reset(long vv) {
        this.crc = (int) (vv & 0xffffffffL);
    }

    public byte[] getValue() {
        long p = 0xffL;
        int value = this.crc;
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            result[i] = (byte) (value & p);
            value = value >> Byte.SIZE;
        }
        return result;
    }
}
