- **목표**
  - 같은 채팅 화면에서 사용자의 짧은 후속 조건 입력이 가장 최근 AI clarification 요청과 안정적으로 연결되도록 한다.
  - 과거 완료 질문이나 이전 pending 질문에 후속 조건이 잘못 붙는 문제를 방지한다.
  - 사용자가 pending 상태에서 명확한 새 질문을 입력하면 기존 pending을 취소하고 새 질문으로 처리한다.

- **문제 진단**
  - 프론트엔드는 `pendingClarificationQueryId`를 보관하지만, 응답의 `clarificationRequired` 여부만 보고 다음 pending을 갱신한다.
  - 백엔드는 `clarificationOfQueryId`가 들어오면 해당 질문과 결합하지만, 같은 conversation의 최신 pending 여부를 우선 확인하지 않는다.
  - pending 상태에서 새 질문과 조건형 후속 입력을 구분하는 정책이 없다.
  - LLM 프롬프트는 pending context가 있을 때 조건형 입력을 반드시 원본 질문의 보완 정보로 해석해야 한다는 규칙이 약하다.

- **정책**
  - `conversationId`는 같은 채팅 화면에서만 유지한다.
  - `conversationId`는 localStorage에 저장하지 않는다.
  - 사용자가 대화 초기화 버튼을 누르면 `messages`, `currentResponse`, `pendingClarificationQueryId`, `conversationId`를 모두 비운다.
  - 백엔드는 같은 conversation에서 가장 최근 `CLARIFICATION_REQUIRED` 상태의 질문을 최신 pending으로 간주한다.
  - 프론트가 오래된 `clarificationOfQueryId`를 보내도 백엔드는 최신 pending을 우선 사용한다.
  - 후속 입력이 짧은 조건/필터 표현이면 반드시 최신 pending 원본 질문과 결합한다.
  - 후속 입력이 명확한 새 질문이면 기존 pending을 `CANCELLED`로 변경하고 새 질문으로 처리한다.
  - 애매한 입력은 pending 연결성을 우선하여 기존 pending 원본 질문의 보완 정보로 처리한다.

- **백엔드 구현 계획**
  - `AiQueryHistory.ExecutionStatus`에 `CANCELLED`를 추가한다.
  - `AiQueryHistoryRepository`에 최신 pending clarification 조회 메서드를 추가한다.
  - `AiQueryServiceImpl`에서 요청 질문을 정규화한 뒤 conversation의 최신 pending을 조회한다.
  - 최신 pending이 있고 사용자 입력이 조건형이면 `원본 질문 + AI 확인 질문 + 사용자 추가 답변` 형태의 resolved question으로 결합한다.
  - 최신 pending이 있고 사용자 입력이 명확한 새 질문이면 pending history를 `CANCELLED`로 변경하고 신규 질문으로 처리한다.
  - 응답 DTO에 `pendingClarificationQueryId`를 추가하여 프론트가 다음 요청에 사용할 pending ID를 서버 기준으로 갱신하게 한다.
  - SQL 생성 프롬프트에 pending clarification context 처리 규칙을 추가한다.

- **프론트엔드 구현 계획**
  - `AiQueryResponse` 타입에 `pendingClarificationQueryId`를 추가한다.
  - `aiStore.ask()`는 서버 응답의 `pendingClarificationQueryId`를 우선 사용해 pending 상태를 갱신한다.
  - 응답에 해당 필드가 없는 경우에만 기존 `clarificationRequired ? queryId : null` fallback을 사용한다.
  - 기존 `clearMessages()` 정책은 유지하여 대화 초기화 시 AI 채팅 상태를 모두 비운다.

- **검증 계획**
  - 백엔드 `compileJava`로 Java 컴파일 정합성을 확인한다.
  - 프론트엔드 `npm run build`로 TypeScript 타입과 번들 정합성을 확인한다.
  - 구현 후 `git status`로 변경 파일을 확인한다.
