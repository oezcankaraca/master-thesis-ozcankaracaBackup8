/**
 * Calculates the time required to transfer a data file over a network given the file size and network speed. 
 * This class provides a demonstration of calculating data transfer times using basic arithmetic operations.
 * 
 * @author Özcan Karaca
 */

public class DataTransferTimeCalculator {
    public static void main(String[] args) {
        // Data size in bytes.
        long dataSizeBytes = 2239815;
        
        // Network speed in Kbps.
        int speedKbps = 761; 
        
        // Convert speed to Kbytes/s from Kbps.
        double speedKbytesPerSecond = speedKbps / 8.0;
        
        // Convert data size to Kilobytes from bytes.
        double dataSizeKilobytes = dataSizeBytes / 1000.0;
        
        // Calculate transfer time in seconds.
        double timeSeconds = dataSizeKilobytes / speedKbytesPerSecond;
        
        // Output transfer time.
        System.out.println("Info: Time required for data transfer: " + timeSeconds + " seconds");
    }
}