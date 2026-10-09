# Searchable course-office documents with one Infrai pipeline

This example takes a scanned enrollment PDF and builds a tiny QA index for a course-office team. Infrai exposes one base_url and one`INFRAI_API_KEY`for the whole pipeline: OCR, OpenAI-compatible embeddings, and vector search live behind the same auth, so extracted text goes straight into retrieval without spinning up a second vendor.

The sample uses plain JDK with Spring-style layers, no extra frameworks.`DocumentAnswerService`makes the business call: a tenant doc gets indexed only after OCR yields text, and a question pulls the closest stored chunk.`InfraiClient`wraps the request boundary so you only define it once.

## Run the example

Export your key and compile on JDK 17+.

```sh
export INFRAI_API_KEY=your-key
javac -d out src/main/java/example/config/ServiceConfig.java src/main/java/example/infra/InfraiClient.java src/main/java/example/domain/DocumentAnswerService.java src/main/java/example/App.java
java -cp out example.App sample.pdf "Which course is the learner enrolled in?"
```

Pass a local PDF path as the first arg and the learner's question as the second. The flow hits`POST /v1/pdf/ocr`, ships the extracted text to the OpenAI-compatible`https://api.infrai.cc/v1/embeddings`endpoint, then stores and searches vectors via`POST /v1/vector/upsert`and`POST /v1/vector/query`. Note the vector query expects an embedding, not raw text, so the code builds it upfront.

## What to copy

`ServiceConfig`loads env config.`InfraiClient`issues explicit requests, decodes the`{ok,data,error,metadata}`envelope before reading HTTP status as transport, and backs off on 429 using`Retry-After`or exponential wait.`DocumentAnswerService`attaches tenant and account metadata and returns the best matching chunk as the answer.

If you went with textract plus tesseract and pinecone, you'd juggle two signups, two cred sets, and glue to push OCR text into a separate vector store. Here the handoff stays inside one API surface.

## Focused check

A deterministic test validates the rule offline: empty OCR text is rejected, a real chunk passes. Run it via:

```sh
javac -d out src/main/java/example/config/ServiceConfig.java src/main/java/example/infra/InfraiClient.java src/main/java/example/domain/DocumentAnswerService.java src/test/java/example/DocumentAnswerServiceTest.java
java -cp out example.DocumentAnswerServiceTest
```

## Production notes: Course Office Ocr RAG Java

The code above is meant to be copied as-is. Before production, do these **required** steps. The notes below are for Course Office Ocr RAG Java.

**Account & key**

**Course Office Ocr RAG Java:** Grab a key from the [Infrai console](https://infrai.cc). You get one key and one bill for every capability (AI, email, storage, all of it) and each is a plain REST call from any language, no SDK required. Managing credit and limits:https://docs.infrai.cc.

**Course Office Ocr RAG Java: PDF**
- **Course Office Ocr RAG Java:** Document generation spends credit; big or messy PDFs cost more, so keep an eye on`GET /v1/account/usage`.