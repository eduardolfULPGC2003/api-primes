import com.google.gson.Gson;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.http.matching.Halt;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static spark.Spark.*;

public class Server {
    private static Integer PRIMES_GUARANTEED = 7919;
    private String path;
    private Integer port;

    public Server(String path, Integer port) {
        this.port = port;
        this.path = path;
    }

    public void start() {
        Spark.port(port);
        // Operations for the API
        get("v1/prime", this::isPrime);
        post("v1/prime", this::postPrime);
    }

    private String postPrime(Request req, Response res) throws CsvValidationException, IOException {
        Integer num = Integer.parseInt(req.queryParams("number"));
        String intro = writePrime(num);
        return intro;
    }

    private String writePrime(Integer num) throws IOException, CsvValidationException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(this.path, true));
        writer.write(num + ",");
        writer.close();
        return "File updated succesfully";
    }

    private String isPrime(Request req, Response res){
        try {
            Integer num = Integer.parseInt(req.queryParams("number"));
            if (num <= 0)
                halt(400, "Negative number or 0");
            // Ask the method readPrimes to read the File
            switch(readPrimes(num)){
                case 0:
                    return toJson(false);
                case 1:
                    return toJson(true);
                default:
                    return toJson(false+"|need for calculation");
            }
        }catch (NumberFormatException e) {
            halt(400, "Could not parse to integer");
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private Integer readPrimes(Integer num) throws IOException, CsvValidationException {
        // Create a CSVReader object to read the file
        CSVReader reader = new CSVReader(new FileReader(this.path));
        // Read all lines from the CSV file
        String[] primeNumbers;
        while ((primeNumbers = reader.readNext()) != null) {
            for (String prime : primeNumbers) {
                if (!prime.isEmpty()) { // Check if the string is not empty
                    int primeNumber = Integer.parseInt(prime);
                    // The file only guarantees the first 1000 primes
                    if (primeNumber > num & primeNumber <= PRIMES_GUARANTEED)
                        return 0;
                    // The number is in the File -> is prime
                    if (num == primeNumber)
                        return 1;
                }
            }
        }
        // Whole file read, number not found, need for calculation
        return 2;
    }

    private String toJson(Object o){
        return new Gson().toJson(o);
    }

    public static void main(String[] args) {
        new Server("C:/Users/Eduardo/IdeaProjects/api-primes/primes.csv", 4567).start();
    }
}
