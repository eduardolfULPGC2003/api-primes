import spark.Request;
import spark.Response;
import spark.Spark;

import java.io.IOException;

public interface Server {
    void start();
    String postPrime(Request req, Response res) throws IOException;
    String isPrime(Request req, Response res);
    void stop();
    String synchronise() throws IOException;
}
