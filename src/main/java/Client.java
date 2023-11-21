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
        while (true) {
            System.out.println("Introduce a number: ");
            int number = scanner.nextInt();
            if (number == -1){
                System.out.println("Closing connection");
                break;
            }
            try {
                String json = Jsoup.connect("http://localhost:4567/v1/prime?number=" + number)
                        .validateTLSCertificates(false)
                        .timeout(60000)
                        .ignoreContentType(true)
                        .method(Connection.Method.GET)
                        .maxBodySize(0).execute().body();
                String[] res = json.split("\\|");
                if (res.length == 2){
                    if (!calculatePrime(number))
                        System.out.println("Is "+number+" prime?: false");
                    else {
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
