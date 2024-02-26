import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Random;

public class GeneratorOfNetworkTopologyRandom {
    private static int numberOfPeers = 50;
    private static final Random random = new Random();
    private static final double meanUploadDe = 30;
    private static final double meanDownloadDe = 90; 
    private static final double varianceUploadEn = 93.0; 
    private static final double varianceDownloadEn = 9959.0;
    private static final double stdDevUploadEn = Math.sqrt(varianceUploadEn);
    private static final double stdDevDownloadEn = Math.sqrt(varianceDownloadEn);

    static JsonArray peersArray = new JsonArray();
    static JsonArray connectionsArray = new JsonArray();
    static ArrayList<Double> uploadSpeeds = new ArrayList<>();
    static ArrayList<Double> downloadSpeeds = new ArrayList<>();

    public static void main(String[] args) {
        // Argument für Anzahl der Peers (optional)
        if (args.length > 0) {
            try {
                numberOfPeers = Integer.parseInt(args[0]) + 1;
            } catch (NumberFormatException e) {
                System.err.println("Error: Argument must be an integer. Using default value.");
            }
        }

        generateNetworkData();
        saveJson();
    }

    private static void generateNetworkData() {
        for (int i = 1; i <= numberOfPeers; i++) {
            double[] speeds = generateSpeeds();
            uploadSpeeds.add(speeds[0]);
            downloadSpeeds.add(speeds[1]);
        }

        double maxUpload = Collections.max(uploadSpeeds);
        double maxDownload = Collections.max(downloadSpeeds);
        addLectureStudioServerPeer(maxUpload, maxDownload);

        for (int i = 0; i < numberOfPeers; i++) {
            addPeer(i + 1, uploadSpeeds.get(i), downloadSpeeds.get(i));
        }

        generateConnections();
    }

    private static double[] generateSpeeds() {
        double uploadSpeed, downloadSpeed;
    
        do {
            uploadSpeed = -1;
            downloadSpeed = -1;
    
            while (uploadSpeed <= 0) {
                uploadSpeed = meanUploadDe + stdDevUploadEn * random.nextGaussian();
            }
    
            while (downloadSpeed <= uploadSpeed) {
                downloadSpeed = meanDownloadDe + stdDevDownloadEn * random.nextGaussian();
            }
    
            uploadSpeed = Math.round(uploadSpeed * 1000);
            downloadSpeed = Math.round(downloadSpeed * 1000);
        } while (uploadSpeed >= downloadSpeed);
    
        return new double[]{uploadSpeed, downloadSpeed};
    }
    

    private static void addLectureStudioServerPeer(double upload, double download) {
        JsonObject lectureStudioServer = new JsonObject();
        lectureStudioServer.addProperty("name", "lectureStudioServer");
        lectureStudioServer.addProperty("maxUpload", (int) upload);
        lectureStudioServer.addProperty("maxDownload", (int) download);
        peersArray.add(lectureStudioServer);
    }

    private static void addPeer(int peerId, double upload, double download) {
        JsonObject peerObject = new JsonObject();
        peerObject.addProperty("name", String.valueOf(peerId));
        peerObject.addProperty("maxUpload", (int) upload);
        peerObject.addProperty("maxDownload", (int) download);
        peersArray.add(peerObject);
    }

    private static void generateConnections() {
        for (int i = 0; i < peersArray.size(); i++) {
            for (int j = 0; j < peersArray.size(); j++) {
                if (i != j) {
                    JsonObject connection = new JsonObject();
                    JsonObject sourcePeer = peersArray.get(i).getAsJsonObject();
                    JsonObject targetPeer = peersArray.get(j).getAsJsonObject();

                    int sourceUpload = sourcePeer.get("maxUpload").getAsInt();
                    int targetDownload = targetPeer.get("maxDownload").getAsInt();
                    double latency = 40 + random.nextDouble() * 40;
                    double packetLoss = 0.001 + random.nextDouble() * 0.001;
                    int bandwidth = Math.min(sourceUpload, targetDownload);

                    connection.addProperty("sourceName", sourcePeer.get("name").getAsString());
                    connection.addProperty("targetName", targetPeer.get("name").getAsString());
                    connection.addProperty("bandwidth", bandwidth);
                    connection.addProperty("latency", String.format(Locale.US, "%.2f", latency));
                    connection.addProperty("loss", String.format(Locale.US, "%.4f", packetLoss));

                    connectionsArray.add(connection);
                }
            }
        }
    }

    private static void saveJson() {
        String homeDirectory = System.getProperty("user.home");
        String basePath = homeDirectory + "/Desktop/master-thesis-ozcankaraca";
        String pathToInputData = basePath + "/data-for-testbed/inputs-new/input-data-" + numberOfPeers + ".json";

        JsonObject networkTopology = new JsonObject();
        networkTopology.add("peers", peersArray);
        networkTopology.add("connections", connectionsArray);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(pathToInputData))) {
            new GsonBuilder().setPrettyPrinting().create().toJson(networkTopology, writer);
            System.out.println("Network topology JSON has been saved to: " + pathToInputData);
        } catch (IOException e) {
            System.err.println("Error while writing the JSON file: " + e.getMessage());
        }
    }
}
