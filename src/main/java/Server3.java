import com.google.gson.Gson;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static spark.Spark.*;

public class Server3 implements Server{
    private List<Integer> servers;
    private List<Integer> newPrimes = new ArrayList<>();
    private static Integer PRIMES_GUARANTEED = 7919;
    private String path;
    private Integer port;

    public Server3(String path, Integer port, List<Integer> servers) {
        this.port = port;
        this.path = path;
        this.servers = servers;
        this.start();
    }

    public void start() {
        Spark.port(port);
        // Operations for the API
        get("v1/prime", this::isPrime);
        post("v1/prime", this::postPrime);
    }

    public String postPrime(Request req, Response res) throws IOException {
        Integer num = Integer.parseInt(req.queryParams("number"));
        if (!newPrimes.contains(num)) {
            String intro = writePrime(num);
            newPrimes.add(num);
            return intro;
        }
        else return "Number already contained";
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

    public void stop(){Spark.stop();}

    public String synchronise() throws IOException {
        if (newPrimes.size()==0|servers.size()==0){
            return "Nothing to synchronise";
        }
        else {
            for (Integer server : servers) {
                for (Integer newPrime : newPrimes) {
                    String json = Jsoup.connect("http://localhost:" + server + "/v1/prime?number=" + newPrime)
                            .validateTLSCertificates(false)
                            .timeout(60000)
                            .ignoreContentType(true)
                            .method(Connection.Method.POST)
                            .maxBodySize(0).execute().body();
                }
            }
            return "Transaction completed";
        }
    }

    public static void main(String[] args) throws IOException {
        List<Integer> servers = new ArrayList<>();
        servers.add(4568);servers.add(4567);
        Server3 server = new Server3("C:/Users/Eduardo/IdeaProjects/api-primes/primes3.csv", 4569, servers);
        while (true){
            System.out.println("Press enter to synchronise");
            new Scanner(System.in).nextLine();
            System.out.println(server.synchronise());
        }
    }
}
