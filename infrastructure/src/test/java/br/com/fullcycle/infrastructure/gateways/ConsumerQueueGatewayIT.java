package br.com.fullcycle.infrastructure.gateways;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.domain.customer.CustomerId;
import br.com.fullcycle.domain.event.EventCancelled;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;

class ConsumerQueueGatewayIT extends IntegrationTest {

    @Autowired
    private ConsumerQueueGateway consumerQueueGateway;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
    }

    @Test
    @DisplayName("Ao consumir o EventCancelled pela fila, todos os ingressos do evento ficam CANCELLED")
    public void testConsumeEventCancelled() throws Exception {
        // given
        final var eventId = EventId.unique();
        final var otherEventId = EventId.unique();

        final var ticket1 = ticketRepository.create(Ticket.newTicket(CustomerId.unique(), eventId));
        final var ticket2 = ticketRepository.create(Ticket.newTicket(CustomerId.unique(), eventId));
        final var ticketOutro = ticketRepository.create(Ticket.newTicket(CustomerId.unique(), otherEventId));

        final var content = mapper.writeValueAsString(new EventCancelled(eventId));

        // when
        consumerQueueGateway.publish(content);

        // then
        awaitAllCancelled(eventId, 2);

        Assertions.assertEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(ticket1.ticketId()).get().status());
        Assertions.assertEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(ticket2.ticketId()).get().status());
        // ingresso de outro evento não é afetado
        Assertions.assertNotEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(ticketOutro.ticketId()).get().status());
    }

    @Test
    @DisplayName("Reprocessar o EventCancelled é idempotente")
    public void testConsumeEventCancelledTwice() throws Exception {
        // given
        final var eventId = EventId.unique();
        final var aTicket = ticketRepository.create(Ticket.newTicket(CustomerId.unique(), eventId));
        final var content = mapper.writeValueAsString(new EventCancelled(eventId));

        // when
        consumerQueueGateway.publish(content);
        awaitAllCancelled(eventId, 1);
        // reprocessa a mesma mensagem
        consumerQueueGateway.publish(content);
        awaitAllCancelled(eventId, 1);

        // then
        Assertions.assertEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(aTicket.ticketId()).get().status());
    }

    private void awaitAllCancelled(final EventId eventId, final int expectedCount) throws InterruptedException {
        final var deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (Instant.now().isBefore(deadline)) {
            final var tickets = ticketRepository.ticketsByEventId(eventId);
            final var allCancelled = tickets.size() == expectedCount
                    && tickets.stream().allMatch(it -> it.status() == TicketStatus.CANCELLED);
            if (allCancelled) {
                return;
            }
            Thread.sleep(100);
        }
        Assertions.fail("Os ingressos do evento não ficaram CANCELLED dentro do tempo esperado");
    }
}
