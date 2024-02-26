import java.nio.file.*;
import java.io.*;
import java.util.*;

public class VarianceCalculator {
    public static void main(String[] args) {
        // Define the path to the CSV file
        String homeDirectory = System.getProperty("user.home");
        String basePath = homeDirectory + "/Desktop/master-thesis-ozcankaraca";
        Path path = Paths.get(basePath + "/data-for-testbed/data-for-realnetwork/reduced-sample.csv"); 

        try (BufferedReader br = Files.newBufferedReader(path)) {
            List<Double> maxUploads = new ArrayList<>();
            List<Double> maxDownloads = new ArrayList<>();

            String line;
            br.readLine(); // Skip the header line
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                maxUploads.add(Double.parseDouble(values[1]));
                maxDownloads.add(Double.parseDouble(values[2]));
            }

            double meanUpload = calculateMean(maxUploads);
            double meanDownload = calculateMean(maxDownloads);
            double varianceUpload = calculateVariance(maxUploads, meanUpload);
            double varianceDownload = calculateVariance(maxDownloads, meanDownload);

            System.out.println("Average upload speed: " + meanUpload);
            System.out.println("Average download speed: " + meanDownload);
            System.out.println("Variance of upload speed: " + varianceUpload);
            System.out.println("Variance of download speed: " + varianceDownload);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static double calculateMean(List<Double> data) {
        return data.stream().mapToDouble(val -> val).average().orElse(0.0);
    }

    private static double calculateVariance(List<Double> data, double mean) {
        return data.stream().mapToDouble(val -> (val - mean) * (val - mean)).sum() / data.size();
    }
}
