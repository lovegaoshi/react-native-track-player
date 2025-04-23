/***************************************************************
 00089   * fft.c
 00090   * Douglas L. Jones
 00091   * University of Illinois at Urbana-Champaign
 00092   * January 19, 1992
 00093   * http://cnx.rice.edu/content/m12016/latest/
 00094   *
 00095   *   fft: in-place radix-2 DIT DFT of a complex input
 00096   *
 00097   *   input:
 00098   * n: length of FFT: must be a power of two
 00099   * m: n = 2**m
 00100   *   input/output
 00101   * x: double array of length n with real part of data
 00102   * y: double array of length n with imag part of data
 00103   *
 00104   *   Permission to copy and use this program is granted
 00105   *   as long as this header is included.
 ***************************************************************/
package com.lovegaoshi.kotlinaudio.utils;

public class FFT {

    int n, m;

    // Lookup tables. Only need to recompute when size of FFT changes.
    double[] cos;
    double[] sin;

    public FFT(int n) {
        this.n = n;
        this.m = (int) (Math.log(n) / Math.log(2));

        // Make sure n is a power of 2
        if (n != (1 << m))
            throw new RuntimeException("FFT length must be power of 2");

        // precompute tables
        cos = new double[n / 2];
        sin = new double[n / 2];

        for (int i = 0; i < n / 2; i++) {
            cos[i] = Math.cos(-2 * Math.PI * i / n);
            sin[i] = Math.sin(-2 * Math.PI * i / n);
        }

    }

    public void fft(float[] x, float[] y) {
        int i, j, k, n1, n2, a;
        float c, s, t1, t2;

        // Bit-reverse
        j = 0;
        n2 = n / 2;
        for (i = 1; i < n - 1; i++) {
            n1 = n2;
            while (j >= n1) {
                j = j - n1;
                n1 = n1 / 2;
            }
            j = j + n1;

            if (i < j) {
                t1 = x[i];
                x[i] = x[j];
                x[j] = t1;
                t1 = y[i];
                y[i] = y[j];
                y[j] = t1;
            }
        }

        // FFT
        n1 = 0;
        n2 = 1;

        for (i = 0; i < m; i++) {
            n1 = n2;
            n2 = n2 + n2;
            a = 0;

            for (j = 0; j < n1; j++) {
                c = (float) cos[a];
                s = (float) sin[a];
                a += 1 << (m - i - 1);

                for (k = j; k < n; k = k + n2) {
                    t1 = c * x[k + n1] - s * y[k + n1];
                    t2 = s * x[k + n1] + c * y[k + n1];
                    x[k + n1] = x[k] - t1;
                    y[k + n1] = y[k] - t2;
                    x[k] = x[k] + t1;
                    y[k] = y[k] + t2;
                }
            }
        }
    }
}