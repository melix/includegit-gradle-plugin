package me.champeau.includegit

import groovy.transform.CompileStatic

@CompileStatic
class TeeWriter extends Writer {
    private final Writer one
    private final Writer two

    static TeeWriter of(Writer one, Writer two) {
        new TeeWriter(one, two)
    }

    private TeeWriter(Writer one, Writer two) {
        this.one = one
        this.two = two
    }

    @Override
    void write(int c) throws IOException {
        try {
            one.write(c)
        } finally {
            two.write(c)
        }
    }

    @Override
    void write(char[] cbuf) throws IOException {
        try {
            one.write(cbuf)
        } finally {
            two.write(cbuf)
        }
    }

    @Override
    void write(char[] cbuf, int off, int len) throws IOException {
        try {
            one.write(cbuf, off, len)
        } finally {
            two.write(cbuf, off, len)
        }
    }

    @Override
    void write(String str) throws IOException {
        try {
            one.write(str)
        } finally {
            two.write(str)
        }
    }

    @Override
    void write(String str, int off, int len) throws IOException {
        try {
            one.write(str, off, len)
        } finally {
            two.write(str, off, len)
        }
    }

    @Override
    void flush() throws IOException {
        try {
            one.flush()
        } finally {
            two.flush()
        }
    }

    @Override
    void close() throws IOException {
        try {
            one.close()
        } finally {
            two.close()
        }
    }
}
