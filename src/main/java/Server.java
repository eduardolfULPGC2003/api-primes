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
    private String path;

    public Server(String path) {
        this.path = path;
    }

    public void start(){
        Spark.port(4567);
        get("v1/prime", this::isPrime);
        post("v1/prime", this::postPrime);
    }

    private String postPrime(Request req, Response res) throws CsvValidationException, IOException {
        Integer num = Integer.parseInt(req.queryParams("number"));
        String intro = writePrime(num);
        return intro;
    }

    private String writePrime(Integer num) throws IOException, CsvValidationException {
        List<Integer> integers = new ArrayList<>();
        CSVReader reader = new CSVReader(new FileReader(this.path));
        // Read all lines from the CSV file
        String[] primeNumbers;
        while ((primeNumbers = reader.readNext()) != null) {
            for (String prime : primeNumbers) {
                if (!prime.isEmpty()) { // Check if the string is not empty
                    integers.add(Integer.parseInt(prime));
                }
            }
        }
        integers.add(num);
        Collections.sort(integers);
        BufferedWriter writer = new BufferedWriter(new FileWriter(this.path));
        for (Integer number : integers) {
            writer.write(number + ",");
        }
        writer.close();
        return "File updated succesfully";
    }

    private String isPrime(Request req, Response res){
        try {
            Integer num = Integer.parseInt(req.queryParams("number"));
            if (num <= 0)
                halt(400, "Negative number or 0");
            switch(readPrimes(num)){
                case 0:
                    return toJson(false);
                case 1:
                    return toJson(true);
                default:
                    return toJson(true+"|need for calculation");
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
                    if (num < primeNumber)
                        return 0;
                    if (num == primeNumber)
                        return 1;
                }
            }
        }
        return 2;
    }

    private String toJson(Object o){
        return new Gson().toJson(o);
    }

    public static void main(String[] args) {
        new Server("C:/Users/Eduardo/IdeaProjects/api-primes/primes.csv").start();
    }
}
