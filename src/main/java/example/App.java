package example;

import example.config.ServiceConfig;
import example.domain.DocumentAnswerService;
import example.infra.InfraiClient;
import java.nio.file.Files;
import java.nio.file.Path;

public class App {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) throw new IllegalArgumentException("usage: App <pdf> <question>");
        var service = new DocumentAnswerService(new InfraiClient(ServiceConfig.fromEnvironment()));
        System.out.println(service.indexAndAnswer(Files.readAllBytes(Path.of(args[0])), "tenant-demo", "account-demo", args[1]));
    }
}
