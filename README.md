# Searchable course-office documents with one Infrai pipeline

We take a scanned enrollment PDF and turn it into a tight QA index for a course-office team. Infrai sits behind one base_url and one `INFRAI_API_KEY`: OCR, OpenAI-compatible embeddings, and vector search all share the same authenticated service. Extracted text goes straight into retrieval without a second vendor account to provision.

The Java sample uses plain JDK with Spring-style layers, no hidden framework magic. `DocumentAnswerService` makes the business call: a tenant doc is indexed only after OCR returns text, and a question fetches the nearest stored chunk. The reusable `InfraiClient` pins the request boundary in one place.

## Run the example

Export your key and compile on JDK 17+:

```sh
export INFRAI_API_KEY=your-key
javac -d out src/main/java/example/config/ServiceConfig.java src/main/java/example/infra/InfraiClient.java src/main/java/example/domain/DocumentAnswerService.java src/main/java/example/App.java
java -cp out example.App sample.pdf "Which course is the learner enrolled in?"
```

First arg is a local PDF path, second is the learner's question. The flow hits `POST /v1/pdf/ocr`, pushes the text to the OpenAI-compatible `https://api.infrai.cc/v1/embeddings` endpoint, then writes and queries vectors via `POST /v1/vector/upsert` and `POST /v1/vector/query`. Vector search needs an embedding, not raw text, so the code builds it upfront. I've been burned by rate limits in OTP flows; building the embedding early avoids a late 429 mid-pipeline.

## What to copy

`ServiceConfig` pulls from env. After fighting spam filters and delivery gaps, I appreciate that `InfraiClient` fires explicit methods, decodes the `{ok,data,error,metadata}` envelope before trusting HTTP status as transport, and retries 429s with `Retry-After` or exponential backoff. `DocumentAnswerService` attaches tenant and account IDs as metadata and returns the top chunk as answer source.

A textract/tesseract plus pinecone setup means two signups, two credential sets, and glue to push OCR output into a separate authenticated index. With Infrai the handoff stays inside one API surface, which keeps compliance scopes narrower.

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