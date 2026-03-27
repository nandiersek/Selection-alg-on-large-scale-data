import java.util.Arrays;
import java.util.Random;
import java.io.IOException;

public class Main {


    public static void main(String[] args) {
        String fileName = "numbers.bin";
        int N = 10_000_000;

        try {
            DataGenerator.generateRandomNumbersToFile(fileName, N);

            double[] X = DataGenerator.loadNumbersFromFile(fileName, N);

            System.out.println("Betöltött elemszám: " + X.length);
            System.out.println("Első elem: " + X[0]);
            System.out.println("Második elem: " + X[1]);
            System.out.println("Harmadik elem: " + X[2]);

            // ide jöhet majd az OSILA:
            // double result = osila(X, 250000, 0.01);
            // System.out.println("Eredmény: " + result);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static double osila(double[] X, int k, double alpha) {
        validateInput(X, k, alpha);

        int n = approximateOptimalSampleSize(X.length, k, alpha);
        return osila(X, k, alpha, n);
    }

    public static double osila(double[] X, int k, double alpha, int n) {
        validateInput(X, k, alpha);

        final int N = X.length;
        n = Math.max(1, Math.min(n, N));


        double xMinus = Double.NEGATIVE_INFINITY;
        double xPlus = Double.POSITIVE_INFINITY;
        int kMinus = 1;
        int kPlus = N;


        double[] S = sampleWithoutReplacement(X, n);
        Arrays.sort(S);


        int j0 = (int) Math.round(((double) (n + 1) * k) / (N + 1));
        j0 = Math.max(1, Math.min(j0, n));


        double x0 = S[j0 - 1];


        int k0 = rankLE(X, x0);


        if (k0 == k) {
            return x0;
        }

        double z = inverseStandardNormalCDF(1.0 - alpha);

        // Case (1.2): k0 > k
        if (k0 > k) {
            xPlus = x0;
            kPlus = k0;

            while (Double.isInfinite(xMinus)) {
                int jAlpha = lowerBoundIndex(j0, k0, k, z);

                if (jAlpha <= 0) {
                    xMinus = Double.NEGATIVE_INFINITY;
                    kMinus = 1;
                    break;
                }

                double xAlpha = S[jAlpha - 1];
                int kAlpha = rankLE(X, xAlpha);

                if (kAlpha <= k) {
                    xMinus = xAlpha;
                    kMinus = kAlpha;
                } else {
                    xPlus = xAlpha;
                    kPlus = kAlpha;

                    j0 = jAlpha;
                    k0 = kAlpha;

                    if (jAlpha == 1) {
                        xMinus = Double.NEGATIVE_INFINITY;
                        kMinus = 1;
                    }
                }
            }
        }


        else {
            xMinus = x0;
            kMinus = k0;

            while (Double.isInfinite(xPlus)) {
                int jAlpha = upperBoundIndex(N, n, j0, k0, k, z);

                if (jAlpha > n) {
                    xPlus = Double.POSITIVE_INFINITY;
                    kPlus = N;
                    break;
                }

                double xAlpha = S[jAlpha - 1];
                int kAlpha = rankLE(X, xAlpha);

                if (kAlpha >= k) {
                    xPlus = xAlpha;
                    kPlus = kAlpha;
                } else {
                    xMinus = xAlpha;
                    kMinus = kAlpha;

                    j0 = jAlpha;
                    k0 = kAlpha;

                    if (jAlpha == n) {
                        xPlus = Double.POSITIVE_INFINITY;
                        kPlus = N;
                    }
                }
            }
        }


        double[] Z = filterRange(X, xMinus, xPlus);
        Arrays.sort(Z);

        int localIndex = k - kMinus;
        if (localIndex < 0 || localIndex >= Z.length) {
            throw new IllegalStateException(
                    "A szűrt részhalmaz indexelése hibás lett. "
                            + "Lehetséges, hogy sok azonos érték van a bemenetben."
            );
        }

        return Z[localIndex];
    }



    private static void validateInput(double[] X, int k, double alpha) {
        if (X == null || X.length == 0) {
            throw new IllegalArgumentException("A bemeneti vektor üres.");
        }
        if (k < 1 || k > X.length) {
            throw new IllegalArgumentException("A k értékének 1 és N között kell lennie.");
        }
        if (alpha <= 0.0 || alpha >= 1.0) {
            throw new IllegalArgumentException("Az alpha értékének 0 és 1 közé kell esnie.");
        }
    }

    public static int rankLE(double[] X, double x) {
        int count = 0;
        for (double v : X) {
            if (v <= x) {
                count++;
            }
        }
        return count;
    }

    private static double[] sampleWithoutReplacement(double[] X, int n) {
        int N = X.length;
        int[] idx = new int[N];
        for (int i = 0; i < N; i++) {
            idx[i] = i;
        }

        Random random = new Random();

        for (int i = 0; i < n; i++) {
            int j = i + random.nextInt(N - i);
            int tmp = idx[i];
            idx[i] = idx[j];
            idx[j] = tmp;
        }

        double[] sample = new double[n];
        for (int i = 0; i < n; i++) {
            sample[i] = X[idx[i]];
        }

        return sample;
    }

    private static int lowerBoundIndex(int j0, int k0, int k, double z) {
        double A = (double) k0 / j0;
        double B = z * Math.sqrt(
                ((double) k0 * (k0 - j0)) / ((double) j0 * j0 * (j0 + 1.0))
        );

        double a = -(A * A + B * B);
        double b = B * B * j0 + 2.0 * A * k;
        double c = -(double) k * k;

        double disc = b * b - 4.0 * a * c;
        if (disc < 0.0) {
            return 0;
        }

        double root = (-b + Math.sqrt(disc)) / (2.0 * a);
        return (int) Math.floor(root);
    }

    private static int upperBoundIndex(int N, int n, int j0, int k0, int k, double z) {
        double C = (double) (N - k0 + 1) / (n - j0 + 1.0);
        double D = z * Math.sqrt(
                ((double) (N - k0 + 1) * (N - k0 - n + j0))
                        / ((n - j0 + 1.0) * (n - j0 + 1.0) * (n - j0 + 2.0))
        );

        double d = C * C + D * D;
        double e = 2.0 * C * (k0 - k) - n * D * D + j0 * D * D - D * D;
        double f = (double) (k0 - k) * (k0 - k);

        double disc = e * e - 4.0 * d * f;
        if (disc < 0.0) {
            return n + 1;
        }

        double root = (-e + Math.sqrt(disc)) / (2.0 * d);
        return (int) Math.ceil(root);
    }

    private static double[] filterRange(double[] X, double lo, double hi) {
        double[] temp = new double[X.length];
        int m = 0;

        for (double v : X) {
            if (v >= lo && v <= hi) {
                temp[m++] = v;
            }
        }

        return Arrays.copyOf(temp, m);
    }

    public static int approximateOptimalSampleSize(int N, int k, double alpha) {
        if (N <= 1) {
            return 1;
        }

        double kd = Math.max(1.0, Math.min(N - 1.0, (double) k));
        double z = inverseStandardNormalCDF(1.0 - alpha);
        double G = z / 4.0;
        double Q = Math.sqrt((2.0 * kd / Math.PI) * ((N - kd + 1.0) / (N + 1.0)));

        double a = 2.0 * Q * G * (1.0 / Math.sqrt(kd) + 1.0 / Math.sqrt(N - kd));
        double b = Q + G * Math.sqrt(kd) - G / Math.sqrt(kd) + G * Math.sqrt(N - kd);


        double lo = 0.0;
        double hi = 1.0;

        while (quarticValue(a, b, N, hi) < 0.0) {
            hi *= 2.0;
        }

        for (int iter = 0; iter < 100; iter++) {
            double mid = (lo + hi) / 2.0;
            if (quarticValue(a, b, N, mid) < 0.0) {
                lo = mid;
            } else {
                hi = mid;
            }
        }

        double r = (lo + hi) / 2.0;
        int n = (int) Math.round(N / (r * r));

        return Math.max(1, Math.min(n, N));
    }

    private static double quarticValue(double a, double b, int N, double r) {
        return a * Math.pow(r, 4) + b * Math.pow(r, 3) - 2.0 * N;
    }


    public static double inverseStandardNormalCDF(double p) {
        if (p <= 0.0 || p >= 1.0) {
            throw new IllegalArgumentException("p értékének 0 és 1 közé kell esnie.");
        }

        double[] a = {
                -3.969683028665376e+01,
                2.209460984245205e+02,
                -2.759285104469687e+02,
                1.383577518672690e+02,
                -3.066479806614716e+01,
                2.506628277459239e+00
        };

        double[] b = {
                -5.447609879822406e+01,
                1.615858368580409e+02,
                -1.556989798598866e+02,
                6.680131188771972e+01,
                -1.328068155288572e+01
        };

        double[] c = {
                -7.784894002430293e-03,
                -3.223964580411365e-01,
                -2.400758277161838e+00,
                -2.549732539343734e+00,
                4.374664141464968e+00,
                2.938163982698783e+00
        };

        double[] d = {
                7.784695709041462e-03,
                3.224671290700398e-01,
                2.445134137142996e+00,
                3.754408661907416e+00
        };

        double plow = 0.02425;
        double phigh = 1.0 - plow;

        double q, r;

        if (p < plow) {
            q = Math.sqrt(-2.0 * Math.log(p));
            return (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
                    ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0);
        } else if (p > phigh) {
            q = Math.sqrt(-2.0 * Math.log(1.0 - p));
            return -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5]) /
                    ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0);
        } else {
            q = p - 0.5;
            r = q * q;
            return (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5]) * q /
                    (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1.0);
        }
    }





}
