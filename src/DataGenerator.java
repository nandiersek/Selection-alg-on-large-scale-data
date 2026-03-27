import java.io.*;
import java.util.Random;

public class DataGenerator {

    public static void generateRandomNumbersToFile(String fileName, int N) throws IOException {
        Random rnd = new Random();

        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(fileName)))) {

            for (int i = 0; i < N; i++) {
                double value = rnd.nextDouble() * 1_000_000.0;
                out.writeDouble(value);
            }
        }

        System.out.println(N + " darab szám elmentve ide: " + fileName);
    }

    public static double[] loadNumbersFromFile(String fileName, int N) throws IOException {
        double[] X = new double[N];

        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(fileName)))) {

            for (int i = 0; i < N; i++) {
                X[i] = in.readDouble();
            }
        }

        return X;
    }
}