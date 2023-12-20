package server;

import java.io.IOException;
import java.io.InputStream;

public class SocketInputStream extends InputStream {
    private static final byte CR = (byte) '\r';
    private static final byte LF = (byte) '\n';
    private static final byte SP = (byte) ' ';
    private static final byte HT = (byte) '\t';
    private static final byte COLON = (byte) ':';
    private static final int LC_OFFSET = 'A' - 'a';
    protected byte buf[];
    protected int count;
    protected int pos;
    protected InputStream is;

    public SocketInputStream(InputStream is, int bufferSize) {
        this.is = is;
        buf = new byte[bufferSize];
    }

    public void readRequestLine(HttpRequestLine requestLine) throws IOException {
        int chr = 0;
        do {
            try {
                chr = read();
            } catch( IOException e) {

            }
        } while ((chr == CR) || (chr == LF));

        pos --;
        int maxRead = requestLine.method.length;
        int readStart = pos;
        int readCount = 0;
        boolean space = false;

        while(!space) {
            if (pos >= count) {
                int val = read();
                if (val == -1) {
                    throw new IOException("requestStream.readline.error");
                }
                pos = 0;
                readStart = 0;
            }
            if (buf[pos] == SP) {
                space = true;
            }
            requestLine.method[readCount] = (char) buf[pos];
            readCount ++;
            pos ++;
        }
        requestLine.methodEnd = readCount - 1;

        maxRead = requestLine.uri.length;
        readStart = pos;
        readCount = 0;
        space = false;
        boolean eol = false;

        while(!space) {
            if (pos >= count) {
                int val = read();
                if (val == -1) {
                    throw new IOException("requestLine.readline.error");
                }
                pos = 0;
                readStart = 0;
            }
            if (buf[pos] == SP) {
                space = true;
            }
            requestLine.uri[readCount] = (char) buf[pos];
            readCount ++;
            pos ++;
        }
        requestLine.uriEnd = readCount - 1;

        maxRead = requestLine.protocol.length;
        readStart = pos;
        readCount = 0;
        while(!eol) {
            if (pos >= count) {
                int val = read();
                if (val == -1) {
                    throw new IOException("requestStream.readline.error");
                }
                pos = 0;
                readStart = 0;
            }
            if (buf[pos] == CR) {

            } else if (buf[pos] == LF) {
                eol = true;
            } else {
                requestLine.protocol[readCount] = (char) buf[pos];
                readCount ++;
            }
        }
        requestLine.protocolEnd = readCount;
    }

    public void readHeader(HttpHeader header) throws IOException {
        int chr = read();
        if (chr == CR || chr == LF) {
            if (chr == CR) {
                read();
            }
            header.nameEnd = 0;
            header.valueEnd = 0;
            return;
        } else {
            pos --;
        }

        int maxRead = header.name.length;
        int readStart = pos;
        int readCount = 0;
        boolean colon = false;
        while(!colon) {
            if (pos >= count) {
                int val = read();
                if (val == -1) {
                    throw new IOException("requestStream.readline.error");
                }
                pos = 0;
                readStart = 0;
            }
            if (buf[pos] == COLON) {
                colon = true;
            }
            char val = (char) buf[pos];
            if (val >= 'A' && val <= 'Z') {
                val = (char) (val - LC_OFFSET);
            }
            header.name[readCount] = val;
            readCount ++;
            pos ++;
        }

        header.nameEnd = readCount - 1;
        maxRead = header.value.length;
        readStart = pos;
        readCount = 0;
        int crPos = -2;
        boolean eol = false;
        boolean validLine = true;

        while(validLine) {
            boolean space = true;
            while(space) {
                if (pos >= count) {
                    int val = read();
                    if (val == -1) {
                        throw new IOException("requestStream.readline.error");
                    }
                    pos = 0;
                    readStart = 0;
                }
                if (buf[pos] == SP || buf[pos] == HT) {
                    pos ++;
                } else {
                    space = false;
                }
            }
            while(!eol) {
                if (pos >= count) {
                    int val = read();
                    if (val == -1) {
                        throw new IOException("requestStream.readline.error");
                    }
                    pos = 0;
                    readStart = 0;
                }
                if (buf[pos] == CR) {

                } else if (buf[pos] == LF) {
                    eol = true;
                } else {
                    int ch = buf[pos] & 0xff;
                    header.value[readCount] = (char) ch;
                    readCount ++;
                }
                pos ++;
            }
            int nextChr = read();
            if (nextChr != SP && nextChr != HT) {
                pos --;
                validLine = false;
            } else {
                eol = false;
                header.value[readCount] = ' ';
                readCount ++;
            }
        }
        header.valueEnd = readCount;
    }

    @Override
    public int available() throws IOException {
        return (count - pos) + is.available();
    }

    @Override
    public void close() throws IOException {
        if (is == null) {
            return;
        }
        is.close();
        is = null;
        buf = null;
    }

    @Override
    public int read() throws IOException {
        if (pos >= count) {
            fill();
            if (pos >= count) {
                return -1;
            }
        }
        return buf[pos++] & 0xff;
    }

    protected void fill() throws IOException {
        pos = 0;
        count = 0;
        int nRead = is.read(buf, 0, buf.length);
        if (nRead > 0) {
            count = nRead;
        }
    }
}
