import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The ConnectionDetails class simulates and manages network connections,
 * focusing on statistics related to network characteristics among peers.
 * It leverages the Gson library to read and write JSON data, indicating that the program handles
 * JSON data for input/output operations related to network configurations and statistics.
 * 
 * The primary functionalities include reading network configuration data from JSON files,
 * processing command-line arguments to customize the simulation, calculating and allocating
 * bandwidth resources among peers, as well as writing updated configurations and statistics
 * back to JSON files. The class also allows for adjustments through command-line arguments, 
 * including the number of peers, the use of super-peers, and the size of files to be transferred.
 * 
 * @author Özcan Karaca
 */
public class ConnectionDetails {
    static Map<String, PeerStats> peerStatsMap = new HashMap<>();

    /**
     * This class represents statistics for a peer, including max upload speed, max download speed, 
     * latency, and packet loss.
     */
    static class PeerStats {
        int maxUpload;
        int maxDownload;
        double latency;
        double loss;

        public PeerStats(double maxUpload, double maxDownload, double latency, double loss) {
            this.maxUpload = (int) maxUpload;
            this.maxDownload = (int) (maxDownload);
            this.latency = latency;
            this.loss = loss;
        }

        @Override
        public String toString() {
            return String.format("Max Upload: %d Kbps, Max Download: %d Kbps, Latency: %.2f ms, Packet Loss: %.4f%%",
                    maxUpload, maxDownload, latency, loss);
        }
    }

    // Default number of peers, use of super-peer, choice of size pdf file
    private static int numberOfPeers = 50;
    private static boolean useSuperPeers = false;
    private static int choiceOfPdfMB = 2;

    private static int sizeOfPDF;

    /**
     * The main method for the program.
     *
     * @param args Command-line arguments.
     * @throws JsonIOException       If there is an error reading JSON.
     * @throws JsonSyntaxException   If there is a syntax error in JSON.
     * @throws FileNotFoundException If the specified file is not found.
     */
    public static void main(String[] args) throws JsonIOException, JsonSyntaxException, FileNotFoundException {

        // Check if command-line arguments are provided for number of peers, use of super peers and size of file.
        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                switch (i) {
                    case 0:
                        numberOfPeers = Integer.parseInt(args[i]);
                        break;
                    case 1:
                        useSuperPeers = Boolean.parseBoolean(args[i]);
                        break;
                    case 2:
                        choiceOfPdfMB = Integer.parseInt(args[i]);
                        break;
                    default:
                        // Optionally: Handle unexpected arguments
                        System.out.println("Unexpected argument at position " + i + ": " + args[i]);
                    break;
                }
            }
        }

        // Assigns the size of a PDF file based on the user's choice of file size in MB
        switch (choiceOfPdfMB) {
            case 2:
                sizeOfPDF = 2239815;
                break;
            case 4:
                sizeOfPDF = 4293938;
                break;
            case 8:
                sizeOfPDF = 8869498;
                break;
            case 16:
                sizeOfPDF = 15890720;
                break;
            case 32:
                sizeOfPDF = 32095088;
                break;
            case 64:
                sizeOfPDF = 67108864;
                break;
            case 128:
                sizeOfPDF = 134217728;
                break;
            default:
                // Optionally: Handle the case where choiceOfPdfMB doesn't match any of the above
                System.out.println("Invalid choice of PDF size: " + choiceOfPdfMB);
                break;
        }        

        // Get the user's home directory path
        String homeDirectory = System.getProperty("user.home");
        // Define the base path for the master thesis's directory
        String basePath = homeDirectory + "/Desktop/master-thesis-ozcankaraca";

        // Specify the path to the output and info file with or without super-peer
        String pathToJsonOutput;
        String pathToPeerInfo;
        if (useSuperPeers) {
            pathToJsonOutput = basePath + "/data-for-testbed/data-for-topology/outputs-with-superpeer/output-data-" + numberOfPeers
                    + ".json";
                    pathToPeerInfo        = basePath + "/data-for-testbed/data-for-connection/connection-details/with-superpeer/connection-details-"
                + numberOfPeers + ".json";
        } else {
            pathToJsonOutput = basePath + "/data-for-testbed/data-for-topology/outputs-without-superpeer/output-data-" + numberOfPeers
                    + ".json";
                    pathToPeerInfo = basePath + "/data-for-testbed/data-for-connection/connection-details/without-superpeer/connection-details-"
                    + numberOfPeers + ".json";
        }
         
        String pathToJsonInput = basePath + "/data-for-testbed/data-for-topology/inputs/input-data-" + numberOfPeers + ".json";

        System.out.println("Step Started: Combining connection details\n");
        System.out.println("Info: Starting connection details from output-data and input data");

        Set<String> peerIds = new HashSet<>();

        try {
            System.out.println("Info: Reading JSON output data from: " + pathToJsonOutput);
            JsonObject outputData = JsonParser.parseReader(new FileReader(pathToJsonOutput)).getAsJsonObject();

            // Extract peer IDs from JSON data
            extractPeerIdsFromJson(outputData, peerIds);

            // Read and store input data
            readAndStoreInputData(pathToJsonInput);

            // Write peer information to a file
            writePeerInfosToFile(peerIds, pathToPeerInfo);

            // Calculate bandwidth allocation
            Map<String, Integer> bandwidthAllocation = calculateBandwidthAllocation(
                    outputData.getAsJsonArray("peer2peer"), peerStatsMap);

            // Process peer-to-peer data and print some information
            JsonArray peer2peer = outputData.getAsJsonArray("peer2peer");
            printNumberOfTargetsPerSourceAndUpload(peer2peer, peerStatsMap);

            // Write connection properties to a file
            writeConnectionPropertiesToFile(outputData, pathToPeerInfo, bandwidthAllocation);

        } catch (IOException e) {
            System.out.println("Error: An error occurred during processing.");
            e.printStackTrace();
        }

        JsonObject outputJson = JsonParser.parseReader(new FileReader(pathToJsonOutput)).getAsJsonObject();

        // Calculate bandwidth allocation again and print the results
        Map<String, Integer> bandwidthAllocation = calculateBandwidthAllocation(outputJson.getAsJsonArray("peer2peer"),
                peerStatsMap);

        System.out.println("\n--The size of file--\n");
        System.out.println(sizeOfPDF);
        int minBandwidth = Integer.MAX_VALUE;

        System.out.println("\n--The allocation of bandwidth for connections--\n");
        for (Map.Entry<String, Integer> entry : bandwidthAllocation.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue() + " Kbps");
            if (entry.getValue() < minBandwidth) {
                minBandwidth = entry.getValue(); // Store the new smallest value
            }
        }

        // Print the smallest bandwidth
        System.out.println("\nThe smallest bandwidth for connections is: " + minBandwidth + " Kbps");

        printDataTransferTimes(bandwidthAllocation, sizeOfPDF);

        System.out.println("\nInfo: Peer information has been saved to the file: " + pathToPeerInfo);
    }

    private static void printDataTransferTimes(Map<String, Integer> bandwidthAllocation, int fileSizeBytes) {
        System.out.println("\n--Data transfer times for connections--\n");
        for (Map.Entry<String, Integer> entry : bandwidthAllocation.entrySet()) {
            String connection = entry.getKey();
            Integer bandwidthKbps = entry.getValue();
            if (bandwidthKbps != null && bandwidthKbps > 0) {
                double speedKbytesPerSecond = bandwidthKbps / 8.0;
                double fileSizeKilobytes = fileSizeBytes / 1000.0;
                // Calculation of the time in milliseconds as an integer
                int timeMilliseconds = (int) ((fileSizeKilobytes / speedKbytesPerSecond) * 1000);
                System.out.println(connection + ": " + timeMilliseconds + " ms");
            } else {
                System.out.println(connection + ": Bandwidth or file size data is missing.");
            }
        }
    }     

    /**
     * Extracts peer IDs from the JSON data and stores them in the given set.
     *
     * @param outputData The JSON object containing output data.
     * @param peerIds    The set to store extracted peer IDs.
     */
    private static void extractPeerIdsFromJson(JsonObject outputData, Set<String> peerIds) {
        System.out.println("Info: Extracting peer IDs from JSON.");

        if (useSuperPeers) {
            // Extract peer IDs from the "superpeers" section of JSON
            JsonArray superPeers = outputData.getAsJsonArray("superpeers");
            for (JsonElement peerElement : superPeers) {
                String peerId = peerElement.getAsJsonObject().get("name").getAsString();
                peerIds.add(peerId);
            }
        }

        // Extract peer IDs from the "peer2peer" section of JSON
        JsonArray peer2peer = outputData.getAsJsonArray("peer2peer");
        for (JsonElement connection : peer2peer) {
            JsonObject connectionObj = connection.getAsJsonObject();
            if (!useSuperPeers && connectionObj.get("sourceName").getAsString().equals("lectureStudioServer")) {
                // For non-super peers, add the "targetName" as the peer ID
                peerIds.add(connectionObj.get("targetName").getAsString());
            } else if (useSuperPeers) {
                // For super peers, add both "sourceName" and "targetName" as peer IDs
                peerIds.add(connectionObj.get("sourceName").getAsString());
                peerIds.add(connectionObj.get("targetName").getAsString());
            }
        }
    }

    /**
     * Reads input data from a JSON file and updates the peerStatsMap with
     * connection and peer statistics.
     *
     * @param pathToJsonInput The path to the JSON input file.
     * @throws FileNotFoundException If the specified file is not found.
     */
    private static void readAndStoreInputData(String pathToJsonInput) throws FileNotFoundException {
        JsonObject inputData = JsonParser.parseReader(new FileReader(pathToJsonInput)).getAsJsonObject();

        // Process connection data
        JsonArray connections = inputData.getAsJsonArray("connections");
        for (JsonElement connectionElement : connections) {
            JsonObject connectionObj = connectionElement.getAsJsonObject();
            String sourceName = connectionObj.get("sourceName").getAsString();
            String targetName = connectionObj.get("targetName").getAsString();
            double latency = connectionObj.get("latency").getAsDouble();
            double loss = connectionObj.get("loss").getAsDouble();

            String connectionKey = sourceName + "-" + targetName;
            PeerStats connectionStats = peerStatsMap.getOrDefault(connectionKey, new PeerStats(0, 0, 0, 0));
            connectionStats.latency = latency;
            connectionStats.loss = loss;
            peerStatsMap.put(connectionKey, connectionStats);
        }

        // Process peer data
        JsonArray peers = inputData.getAsJsonArray("peers");
        for (JsonElement peerElement : peers) {
            JsonObject peerObj = peerElement.getAsJsonObject();
            String peerId = peerObj.get("name").getAsString();
            double maxUpload = peerObj.get("maxUpload").getAsDouble();
            double maxDownload = peerObj.get("maxDownload").getAsDouble();

            PeerStats peerStats = peerStatsMap.getOrDefault(peerId, new PeerStats(0, 0, 0, 0));
            peerStats.maxUpload = (int) maxUpload;
            peerStats.maxDownload = (int) maxDownload;
            peerStatsMap.put(peerId, peerStats);
        }
    }

    /**
     * Writes peer information to a file, including peer ID and associated statistics.
     *
     * @param peerIds  The set of peer IDs to be written to the file.
     * @param filePath The path to the output file.
     * @throws IOException If there is an error writing to the file.
     */
    private static void writePeerInfosToFile(Set<String> peerIds, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            for (String peerId : peerIds) {
                PeerStats stats = getPeerStats(peerId);
                if (stats != null) {
                    writer.write(peerId + ": " + stats.toString() + "\n");
                } else {
                    writer.write("Error: No data found for Peer " + peerId + "\n");
                }
            }
        }
    }

    /**
     * Retrieves statistics for a specific peer.
     *
     * @param peerId The ID of the peer for which statistics are requested.
     * @return PeerStats object containing statistics for the peer, or null if not found.
     */
    public static PeerStats getPeerStats(String peerId) {
        PeerStats stats = peerStatsMap.get(peerId);
        return stats;
    }

    /**
     * Writes connection properties to a JSON file, including source name, target name, 
     * bandwidth, latency, and loss.
     *
     * @param outputData          The JSON object containing output data.
     * @param filePath            The path to the output file.
     * @param bandwidthAllocation The map of bandwidth allocation for connections.
     * @throws IOException If there is an error writing to the file.
     */
    private static void writeConnectionPropertiesToFile(JsonObject outputData, String filePath,
            Map<String, Integer> bandwidthAllocation) throws IOException {
        JsonArray connectionsArray = new JsonArray();

        // Iterate through the "peer2peer" connections in the JSON data
        JsonArray peer2peer = outputData.getAsJsonArray("peer2peer");
        for (JsonElement connectionElement : peer2peer) {
            JsonObject connectionObj = connectionElement.getAsJsonObject();
            String sourceName = connectionObj.get("sourceName").getAsString();
            String targetName = connectionObj.get("targetName").getAsString();

            String connectionKey = sourceName + "-" + targetName;
            PeerStats connectionStats = peerStatsMap.get(connectionKey);

            // Get bandwidth allocation for the connection, or set to 0 if not found
            int bandwidth = bandwidthAllocation.getOrDefault(connectionKey, 0);

            // Get latency and loss from connectionStats if available, otherwise set to 0.0
            double latency = connectionStats != null ? connectionStats.latency : 0.0;
            double loss = connectionStats != null ? connectionStats.loss : 0.0;

            // Format latency and loss to a specific locale
            String formattedLatency = String.format(Locale.US, "%.2f", latency);
            String formattedLoss = String.format(Locale.US, "%.4f", loss);

            // Create a JSON object for the connection and add it to the connectionsArray
            JsonObject connectionJson = new JsonObject();
            connectionJson.addProperty("sourceName", sourceName);
            connectionJson.addProperty("targetName", targetName);
            connectionJson.addProperty("bandwidth", bandwidth);
            connectionJson.addProperty("latency", formattedLatency);
            connectionJson.addProperty("loss", formattedLoss);

            connectionsArray.add(connectionJson);
        }

        // Write the connectionsArray to the output file using Gson for pretty printing
        try (FileWriter writer = new FileWriter(filePath)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(connectionsArray, writer);
        }
    }

    /**
     * Calculates bandwidth allocation for peer-to-peer connections.
     *
     * @param peer2peer    The JSON array containing peer-to-peer connections.
     * @param peerStatsMap The map containing peer statistics.
     * @return A map of connection keys (sourceName-targetName) to allocated bandwidth.
     */
    private static Map<String, Integer> calculateBandwidthAllocation(JsonArray peer2peer,
            Map<String, PeerStats> peerStatsMap) {
        Map<String, Integer> bandwidthAllocation = new HashMap<>();

        for (JsonElement connectionElement : peer2peer) {
            JsonObject connectionObj = connectionElement.getAsJsonObject();
            String sourceName = connectionObj.get("sourceName").getAsString();

            // Find all target names connected to the source peer
            List<String> targetNames = findAllTargetsForSource(peer2peer, sourceName);

            // Get the statistics for the source peer
            PeerStats sourceStats = peerStatsMap.get(sourceName);
            int totalBandwidthForSource = sourceStats != null ? sourceStats.maxUpload : 0;
            int equalBandwidthPerConnection = totalBandwidthForSource / targetNames.size();

            int remainingBandwidth = totalBandwidthForSource;
            int remainingConnections = targetNames.size();

            for (String targetName : targetNames) {

                PeerStats targetStats = peerStatsMap.get(targetName);
                if (targetStats == null)
                    continue;
                int allocatedBandwidth;
                if (targetStats.maxDownload < equalBandwidthPerConnection) {
                    // Allocate bandwidth to the target based on their max download rate
                    allocatedBandwidth = targetStats.maxDownload;
                    remainingBandwidth -= allocatedBandwidth;
                    remainingConnections--;
                    bandwidthAllocation.put(sourceName + "-" + targetName, allocatedBandwidth);
                } else {
                    // If target can handle the equal share, mark it as null (no specific allocation)
                    bandwidthAllocation.put(sourceName + "-" + targetName, null);
                }
            }

            // Calculate the final bandwidth per connection for remaining connections
            int finalBandwidthPerConnection = remainingConnections > 0 ? remainingBandwidth / remainingConnections : 0;
            for (String targetName : targetNames) {

                if (bandwidthAllocation.containsKey(sourceName + "-" + targetName)
                        && bandwidthAllocation.get(sourceName + "-" + targetName) == null) {
                    PeerStats targetStats = peerStatsMap.get(targetName);

                    // Allocate the remaining bandwidth to targets with null allocation
                    int allocatedBandwidth = Math.min(finalBandwidthPerConnection, targetStats.maxDownload);
                    bandwidthAllocation.put(sourceName + "-" + targetName, allocatedBandwidth);
                }
            }
        }

        return bandwidthAllocation;
    }

    /**
     * Prints the number of target peers to which each source peer is connected
     * along with their upload capacity.
     *
     * @param peer2peer    The JSON array containing peer-to-peer connections.
     * @param peerStatsMap The map containing peer statistics.
     */
    private static void printNumberOfTargetsPerSourceAndUpload(JsonArray peer2peer,
            Map<String, PeerStats> peerStatsMap) {
        Map<String, Integer> numberOfTargetsPerSource = new HashMap<>();

        for (JsonElement connectionElement : peer2peer) {
            JsonObject connectionObj = connectionElement.getAsJsonObject();
            String sourceName = connectionObj.get("sourceName").getAsString();

            // Find all target names connected to the source peer
            List<String> targetNames = findAllTargetsForSource(peer2peer, sourceName);

            // Count and store the number of target peers for each source peer
            numberOfTargetsPerSource.put(sourceName, targetNames.size());
        }

        System.out.println("\n--Target peers to which the source peer is connected--\n");
        for (Map.Entry<String, Integer> entry : numberOfTargetsPerSource.entrySet()) {
            String sourceName = entry.getKey();
            PeerStats sourceStats = peerStatsMap.get(sourceName);
            int uploadCapacity = sourceStats != null ? sourceStats.maxUpload : 0;

            // Print the number of target peers and upload capacity for each source peer
            System.out.println(sourceName + " has " + entry.getValue() + " target peers and an upload capacity of "
                    + uploadCapacity + " Kbps");
        }
    }

    /**
     * Finds all target peers for a given source peer in the JSON array of connections.
     *
     * @param peer2peer  The JSON array containing peer-to-peer connections.
     * @param sourceName The name of the source peer.
     * @return A list of target peer names connected to the source peer.
     */
    private static List<String> findAllTargetsForSource(JsonArray peer2peer, String sourceName) {
        List<String> targetNames = new ArrayList<>();
        for (JsonElement connectionElement : peer2peer) {
            JsonObject connectionObj = connectionElement.getAsJsonObject();
            // Check if the connection's sourceName matches the given sourceName
            if (connectionObj.get("sourceName").getAsString().equals(sourceName)) {
                // Add the targetName to the list of target peers
                targetNames.add(connectionObj.get("targetName").getAsString());
            }
        }
        return targetNames;
    }
}