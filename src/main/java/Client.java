import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;

import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import static java.lang.Thread.sleep;

public class Client {
    public static void main(String[] args) throws IOException, InterruptedException, CsvValidationException {
        Scanner scanner = new Scanner(System.in);
        // Will run endlessly until -1 is introduced
        while (true) {
            System.out.println("Introduce a number: ");
            int number = scanner.nextInt();
            if (number == -1){
                System.out.println("Closing connection");
                break;
            }
            try {
                // Do a request to the server
                String json = Jsoup.connect("http://localhost:567/v1/prime?number=" + number)
                        .validateTLSCertificates(false)
                        .timeout(60000)
                        .ignoreContentType(true)
                        .method(Connection.Method.GET)
                        .maxBodySize(0).execute().body();
                String[] res = json.split("\\|");
                // false|need for calculation
                if (res.length == 2){
                    // The client calculate if the number is prime
                    if (!calculatePrime(number))
                        System.out.println("Is "+number+" prime?: false");
                    else {
                        // The number is prime and not in the file: POST request
                        System.out.println("Is "+number+" prime?: true");
                        String post = Jsoup.connect("http://localhost:4567/v1/prime?number=" + number)
                                .validateTLSCertificates(false)
                                .timeout(60000)
                                .ignoreContentType(true)
                                .method(Connection.Method.POST)
                                .maxBodySize(0).execute().body();
                        System.out.println(post);
                    }
                }
                else {
                    System.out.println("Is "+number+" prime?: "+json);
                }
            }catch (HttpStatusException e) {
                System.out.println("Error: " + e);
            }
        }

    }

    private static boolean calculatePrime(int number) {
        for (int i = 2; i < number; i++) {
            if (number % i == 0)
                return false;
        }
        return true;
    }
}
