# MBA Hexagonal Architecture — Cancelamento de Evento

Plataforma de ingressos em Arquitetura Hexagonal e Clean Architecture (Java 17, Gradle, Spring Boot), dividida nos módulos `domain`, `application` e `infrastructure`. Este fork adiciona a feature de **cancelamento de evento**, com a cascata assíncrona que cancela os ingressos do evento reagindo a um evento de domínio.

## Pré-requisitos

- JDK 17
- Docker e Docker Compose (apenas para subir a aplicação com MySQL; os testes usam H2 em memória e não precisam do Docker)

## Subindo o projeto

```bash
# MySQL 8 na porta 3306, banco "events" (usuário root / senha root)
docker compose up -d

# Aplicação na porta 8080
./gradlew :infrastructure:bootRun
```

- API REST em `http://localhost:8080`
- GraphiQL em `http://localhost:8080/graphiql`

Para derrubar o banco:

```bash
docker compose down
```

## Rodando a suíte de testes

Os testes de integração usam um banco H2 em memória (perfil `test`), então não dependem do MySQL:

```bash
./gradlew test
```

## A feature

| Operação | REST | GraphQL |
| --- | --- | --- |
| Cancelar evento | `POST /events/{id}/cancel` → `200 {id, status}` ou `422` com a mensagem | `mutation { cancelEvent(id) { id status } }` |
| Consultar evento | `GET /events/{id}` → completo `{id, name, date, totalSpots, status}`; com header `X-Public` → reduzido `{id, status}`; `404` se não existir | `query { eventOfId(id) { id name date totalSpots status } }` |

### Regras de negócio

- Cancelar um evento ativo: passa a `CANCELLED` e emite o evento de domínio `EventCancelled`.
- Cancelar um evento inexistente: `ValidationException` com a mensagem `Event not found`.
- Cancelar um evento já cancelado: `ValidationException` com `Event already cancelled`.
- Inscrever um cliente em um evento cancelado: `ValidationException` com `Event is cancelled`.
- Cascata de cancelamento: todos os ingressos do evento passam a `CANCELLED`, de forma idempotente (reprocessar não quebra).

## Onde a cascata de cancelamento acontece

O `CancelEventUseCase` apenas chama `Event.cancel()`, que muda o estado do agregado para `CANCELLED` e registra o evento de domínio `EventCancelled` (type `event.cancelled`). Ao persistir o evento, o `EventDatabaseRepository` grava os eventos de domínio do agregado na tabela `outbox`, na mesma transação. O `OutboxRelay` publica as mensagens pendentes no `QueueGateway`, e o `ConsumerQueueGateway` roteia o tipo `event.cancelled` para o `CancelEventTicketsUseCase`, que busca os ingressos do evento por `TicketRepository.ticketsByEventId` e chama `Ticket.cancel()` em cada um (de forma idempotente).

Assim, o agregado `Event` nunca toca o agregado `Ticket`, e o comando de cancelamento não cancela ingressos de forma síncrona: é o mesmo caminho já usado pelo fluxo `EventTicketReserved → CreateTicketForCustomerUseCase`.

Os testes que cobrem esse caminho são o `CancelEventUseCaseIT` (o `EventCancelled` chega à outbox) e o `ConsumerQueueGatewayIT` (o JSON do `EventCancelled` publicado no `ConsumerQueueGateway` termina com todos os ingressos do evento em `CANCELLED`, inclusive no reprocessamento).
