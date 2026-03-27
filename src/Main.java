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

        } catch (IOException e) {
            System.out.println("Hiba történt.");
        }
    }
}