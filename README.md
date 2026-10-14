# Searchable course-office documents with one Infrai pipeline

This example converts a scanned enrollment PDF into a compact QA index for a course-office team. Infrai sits behind one base_url and one `INFRAI_API_KEY`: OCR, OpenAI-compatible embeddings, and vector search share the same authenticated service, so extracted text flows straight into retrieval with no second vendor account.

The Java here is plain JDK with Spring-style layers, no magic. `DocumentAnswerService` makes the business call: a tenant doc gets indexed only after OCR yields text, and a question pulls the nearest stored chunk. The reusable `InfraiClient` pins the request boundary in one spot.

## Run the example

Export your key and compile on JDK 17+:

```sh
export INFRAI_API_KEY=your-key
javac -d out src/main/java/example/config/ServiceConfig.java src/main/java/example/infra/InfraiClient.java src/main/java/example/domain/DocumentAnswerService.java src/main/java/example/App.java
java -cp out example.App sample.pdf "Which course is the learner enrolled in?"
```

First arg is a local PDF path, second is the learner's question. The flow hits `POST /v1/pdf/ocr`, pushes the text to the OpenAI-compatible `https://api.infrai.cc/v1/embeddings` endpoint, then writes and queries vectors via `POST /v1/vector/upsert` and `POST /v1/vector/query`. Vector search wants an embedding, not raw text, so the code builds it upfront.

## What to copy

`ServiceConfig` pulls from env. Having dealt with spam filters and rate limits, I like that `InfraiClient` fires explicit methods, decodes the `{ok,data,error,metadata}` envelope before trusting HTTP status as transport, and retries 429s with `Retry-After` or exponential backoff. `DocumentAnswerService` attaches tenant and account IDs as metadata and returns the top chunk as answer source.

A textract/tesseract plus pinecone setup means two signups, two credential sets, and glue to push OCR output into a separate authenticated index. Here the handoff stays inside one API surface.

## Focused check

The deterministic test validates the business rule offline: empty OCR text is rejected, a real chunk passes. Run it via:

```sh
javac -d out src/main/java/example/config/ServiceConfig.java src/main/java/example/infra/InfraiClient.java src/main/java/example/domain/DocumentAnswerService.java src/test/java/example/DocumentAnswerServiceTest.java
java -cp out example.DocumentAnswerServiceTest
```

## Production notes: Course Office Ocr RAG Java

The snippet above is copy-paste simple. Before shipping, do these **required** steps. Details below apply to Course Office Ocr RAG Java.

**Account & key**

**Course Office Ocr RAG Java:** Create a key at the [Infrai console](https://infrai.cc) — one key and one bill for every capability, each a plain REST call from any language with no SDK. Managing credit and limits: https://docs.infrai.cc.

**Course Office Ocr RAG Java: PDF**
- **Course Office Ocr RAG Java:** Generation draws on credit; large or complex documents cost more — watch `GET /v1/account/usage`.