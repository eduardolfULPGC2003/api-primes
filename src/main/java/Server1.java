import com.google.gson.Gson;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static spark.Spark.*;

public class Server1 implements Server{
    private List<Integer> servers;
    private static Integer PRIMES_GUARANTEED = 7919;
    private String path_primes;
    private String path_non_primes;
    private Integer port;

    public Server1(String path_primes, String path_non_primes, Integer port, List<Integer> servers) {
        this.port = port;
        this.path_primes = path_primes;
        this.path_non_primes = path_non_primes;
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
        String prime = req.queryParams("prime");
        String post;
        if (prime.equals("YES")) {
            post = writePrime(num);
        }
        else {
            post = writeNonPrime(num);
        }
        return post;
    }

    private String writePrime(Integer num) throws IOException {
            BufferedWriter writer = new BufferedWriter(new FileWriter(this.path_primes, true));
            writer.write(num + ",");
            writer.close();
            return "File updated successfully";
    }

    private String writeNonPrime(Integer num) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(this.path_non_primes, true));
        writer.write(num + ",");
        writer.close();
        return "File updated successfully";
    }

    public String isPrime(Request req, Response res){
        try {
            int num = Integer.parseInt(req.queryParams("number"));
            if (num <= 0)
                halt(400, "Negative number or 0");
            // Ask the method readPrimes to read the Fi
            if (readPrimes(num)) return toJson(true);
            else if (readNonPrimes(num)) {
                return toJson(false);
            }
            else {
                if (servers.size()!=0)
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

    public String isPrimeServer(Request req, Response res){
        try {
            int num = Integer.parseInt(req.queryParams("number"));
            if (num <= 0)
                halt(400, "Negative number or 0");
            // Ask the method readPrimes to read the File
            if (readPrimes(num)) return toJson(true);
            else if (readNonPrimes(num)) {
                return toJson(false);
            }
            else return toJson(false+"|need for calculation");
        }catch (NumberFormatException e) {
            halt(400, "Could not parse to integer");
        } catch (CsvValidationException | IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private boolean readPrimes(int num) throws IOException, CsvValidationException {
        // Create a CSVReader object to read the file
        CSVReader reader = new CSVReader(new FileReader(this.path_primes));
        // Read all lines from the CSV file
        System.out.println("Hola");
        String[] primeNumbers;
        while ((primeNumbers = reader.readNext()) != null) {
            for (String prime : primeNumbers) {
                if (!prime.isEmpty()) { // Check if the string is not empty
                    int primeNumber = Integer.parseInt(prime);
                    // The number is in the File -> is prime
                    if (num == primeNumber) {
                        reader.close();
                        return true;
                    }
                }
            }
        }
        // Whole file read, number not found, need for calculation
        reader.close();
        return false;
    }

    private boolean readNonPrimes(int num) throws CsvValidationException, IOException {
        // Create a CSVReader object to read the file
        CSVReader reader = new CSVReader(new FileReader(this.path_non_primes));
        // Read all lines from the CSV file
        String[] primeNumbers;
        while ((primeNumbers = reader.readNext()) != null) {
            for (String prime : primeNumbers) {
                if (!prime.isEmpty()) { // Check if the string is not empty
                    int primeNumber = Integer.parseInt(prime);
                    // The number is in the File -> is prime
                    if (num == primeNumber) {
                        reader.close();
                        return true;
                    }
                }
            }
        }
        // Whole file read, number not found, need for calculation
        reader.close();
        return false;
    }

    private String toJson(Object o){
        return new Gson().toJson(o);
    }

    public void stop(){Spark.stop();}

    public static void main(String[] args) throws IOException {
        List<Integer> servers = new ArrayList<>();
        //servers.add(4568);servers.add(4569);
        Server1 server = new Server1("files/primes/primes1.csv",
                "files/non-primes/non-primes1.csv", 4567, servers);
    }
}
