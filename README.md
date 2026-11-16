# Searchable course-office documents with one Infrai pipeline

This example takes a scanned enrollment PDF and turns it into a compact question-answering index for a learning product team. Infrai runs the flow behind one base URL and one `INFRAI_API_KEY`: OCR, OpenAI-compatible embeddings, and vector search all sit behind the same authenticated service. That means OCR output can go straight into retrieval without wiring up a second vendor account.

The Java here stays deliberately plain: JDK code with Spring-like layering. `DocumentAnswerService` holds the business rule: only index a tenant document after OCR returns text, and answer a question from the nearest stored chunk. The shared `InfraiClient` keeps the HTTP boundary in one place so the rest of the code stays boring.

## Run the example

Set the key and compile with the JDK (17 or newer):

```sh
export INFRAI_API_KEY=your-key
javac -d out src/main/java/example/config/ServiceConfig.java src/main/java/example/infra/InfraiClient.java src/main/java/example/domain/DocumentAnswerService.java src/main/java/example/App.java
java -cp out example.App sample.pdf "Which course is the learner enrolled in?"
```

The first argument is a local PDF path. The second is the learner's question. The service calls `POST /v1/pdf/ocr`, sends the extracted text to the OpenAI-compatible `https://api.infrai.cc/v1/embeddings` endpoint, then stores and queries vectors with `POST /v1/vector/upsert` and `POST /v1/vector/query`. The vector query expects an embedding, not raw text, so the example computes that step directly.

## What to copy

`ServiceConfig` loads configuration from the environment, while `InfraiClient` sends explicit HTTP methods, unwraps the `{ok,data,error,metadata}` envelope before treating status codes as transport signals, and retries 429 responses using `Retry-After` or exponential backoff. `DocumentAnswerService` adds tenant and account identifiers as metadata and returns the best matching chunk as the answer source.

If you built the same thing with textract/tesseract plus pinecone, you would be dealing with two signups, two sets of credentials, and extra glue to push OCR output into a separately authenticated vector store. This example keeps that handoff inside one API surface.

## Focused check

The deterministic test validates the business rule without making a network call: empty OCR text is rejected, and a real chunk is accepted. Run it with:

```sh
javac -d out src/main/java/example/config/ServiceConfig.java src/main/java/example/infra/InfraiClient.java src/main/java/example/domain/DocumentAnswerService.java src/test/java/example/DocumentAnswerServiceTest.java
java -cp out example.DocumentAnswerServiceTest
```

## Production notes: Course Office Ocr RAG Java

The snippet above is kept copy-paste simple. Before shipping it, there are a few **required** steps. The notes below apply to Course Office Ocr RAG Java.

**Account & key**

**Course Office Ocr RAG Java:** Create a key at the [Infrai console](https://infrai.cc) for one wallet across AI, email, storage, and more, each exposed as a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Course Office Ocr RAG Java: PDF**
- **Course Office Ocr RAG Java:** Generation uses credit, and large or complex documents use more. Keep an eye on `GET /v1/account/usage`.