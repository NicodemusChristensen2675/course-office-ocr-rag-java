package example;
import example.domain.DocumentAnswerService;
public class DocumentAnswerServiceTest {
  public static void main(String[] args) {
    if (!DocumentAnswerService.extract("{\"text\":\"enrollment\"}", "text").equals("enrollment")) throw new AssertionError();
    if (!DocumentAnswerService.extract("{\"text\":\"\"}", "text").isEmpty()) throw new AssertionError();
    if (!DocumentAnswerService.extract("{\"embedding\":[0.1,0.2,-0.3],\"model\":\"test\"}", "embedding").equals("[0.1,0.2,-0.3]")) throw new AssertionError();
    System.out.println("business decision checks passed");
  }
}
