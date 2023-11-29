import com.google.gson.Gson;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import spark.Request;
import spark.Response;
import spark.Service;
import spark.Spark;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static spark.Spark.*;

public class Server1 implements Server{
    private List<Integer> servers;
    private static Integer PRIMES_GUARANTEED = 7919;
    private String path;
    private Integer port;

    public Server1(String path, Integer port, List<Integer> servers) {
        this.port = port;
        this.path = path;
        this.servers = servers;
        this.start();
    }

    public void start() {
        Spark.port(port);
        // Operations for the API
        get("v1/prime", this::isPrime);
        get("v1/prime/server", this::isPrimeServer);
        post("v1/prime", this::postPrime);
    }

    public String postPrime(Request req, Response res) throws IOException {
        Integer num = Integer.parseInt(req.queryParams("number"));
        String intro = writePrime(num);
        return intro;
    }

    private String writePrime(Integer num) throws IOException {
            BufferedWriter writer = new BufferedWriter(new FileWriter(this.path, true));
            writer.write(num + ",");
            writer.close();
            return "File updated successfully";
    }

    public String isPrime(Request req, Response res){
        try {
            int num = Integer.parseInt(req.queryParams("number"));
            if (num <= 0)
                halt(400, "Negative number or 0");
            // Ask the method readPrimes to read the File
            switch(readPrimes(num)){
                case 0:
                    return toJson(false);
                case 1:
                    return toJson(true);
                default:
                    for (int server: servers){
                        String json = Jsoup.connect("http://localhost:"+server+"/v1/prime/server?number=" + num)
                                .validateTLSCertificates(false)
                                .timeout(60000)
                                .ignoreContentType(true)
                                .method(Connection.Method.GET)
                                .maxBodySize(0).execute().body();
                        if (json.split("\\|").length==1){
                            if (json.equals("true")) writePrime(num);
                            return json;
                        }
                    }
                    return toJson(false+"|need for calculation");
            }
        }catch (NumberFormatException e) {
            halt(400, "Could not parse to integer");
        } catch (CsvValidationException | IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private String isPrimeServer(Request req, Response res) {
        try {
            int num = Integer.parseInt(req.queryParams("number"));
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
        } catch (CsvValidationException | IOException e) {
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
                    if (primeNumber > num & primeNumber <= PRIMES_GUARANTEED) {
                        reader.close();
                        return 0;
                    }
                    // The number is in the File -> is prime
                    if (num == primeNumber) {
                        reader.close();
                        return 1;
                    }
                }
            }
        }
        // Whole file read, number not found, need for calculation
        reader.close();
        return 2;
    }

    private String toJson(Object o){
        return new Gson().toJson(o);
    }

    public void stop(){Spark.stop();}

    public static void main(String[] args) throws IOException {
        List<Integer> servers = new ArrayList<>();
        servers.add(4568);servers.add(4569);
        Server1 server = new Server1("primes1.csv", 4567, servers);
    }
}
