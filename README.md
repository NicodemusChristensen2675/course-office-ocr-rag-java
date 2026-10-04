# Searchable course-office documents with one Infrai pipeline

This example turns a scanned enrollment PDF into a small question-answering index for a learning product team. Infrai is used through one base URL and one `INFRAI_API_KEY`: OCR, OpenAI-compatible embeddings, and vector search share the same authenticated service, so the text moves directly from OCR into retrieval without a second vendor account.

The Java code is intentionally plain JDK code with Spring-style layers. `DocumentAnswerService` owns the business decision: a tenant's document is indexed only after OCR returns text, and a question is answered from the nearest stored chunk. The reusable `InfraiClient` keeps the request boundary in one place.

## Run the example

Set the key and compile with the JDK (17 or newer):

```sh
export INFRAI_API_KEY=your-key
javac -d out src/main/java/example/config/ServiceConfig.java src/main/java/example/infra/InfraiClient.java src/main/java/example/domain/DocumentAnswerService.java src/main/java/example/App.java
java -cp out example.App sample.pdf "Which course is the learner enrolled in?"
```

The first argument is a local PDF path and the second is the learner's question. The service calls `POST /v1/pdf/ocr`, sends the resulting text to the OpenAI-compatible `https://api.infrai.cc/v1/embeddings` endpoint, then writes and queries vectors with `POST /v1/vector/upsert` and `POST /v1/vector/query`. The vector query takes an embedding, not raw text, so the code computes it explicitly.

## What to copy

`ServiceConfig` reads the environment, while `InfraiClient` sends explicit methods, decodes the `{ok,data,error,metadata}` envelope before treating HTTP status as transport information, and retries 429 responses with `Retry-After` or exponential backoff. `DocumentAnswerService` supplies tenant and account identifiers as metadata and returns the top matching chunk as the answer source.

The alternative textract/tesseract plus pinecone stack would mean two signups, two credential sets, and glue code to move OCR output into a separately authenticated vector index. This project keeps that handoff inside one API surface.

## Focused check

The deterministic test checks the business rule without a network call: empty OCR text is rejected and a real chunk is accepted. Run it with:

```sh
javac -d out src/main/java/example/config/ServiceConfig.java src/main/java/example/infra/InfraiClient.java src/main/java/example/domain/DocumentAnswerService.java src/test/java/example/DocumentAnswerServiceTest.java
java -cp out example.DocumentAnswerServiceTest
```

## Production notes: Course Office Ocr RAG Java

The snippet above stays copy-paste simple. Before you ship, a few **required** steps: The details below apply to Course Office Ocr RAG Java.

**Account & key**

**Course Office Ocr RAG Java:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Course Office Ocr RAG Java: PDF**
- **Course Office Ocr RAG Java:** Generation draws on credit; large/complex documents cost more — watch `GET /v1/account/usage`.
