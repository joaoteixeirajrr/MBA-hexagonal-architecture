package br.com.fullcycle.application.ticket;

import br.com.fullcycle.application.repository.InMemoryTicketRepository;
import br.com.fullcycle.domain.customer.CustomerId;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CancelEventTicketsUseCaseTest {

    @Test
    @DisplayName("Deve cancelar todos os ingressos de um evento")
    public void testCancelEventTickets() {
        // given
        final var eventId = EventId.unique();
        final var otherEventId = EventId.unique();

        final var ticketRepository = new InMemoryTicketRepository();
        final var ticket1 = Ticket.newTicket(CustomerId.unique(), eventId);
        final var ticket2 = Ticket.newTicket(CustomerId.unique(), eventId);
        final var ticketOutro = Ticket.newTicket(CustomerId.unique(), otherEventId);
        ticketRepository.create(ticket1);
        ticketRepository.create(ticket2);
        ticketRepository.create(ticketOutro);

        final var input = new CancelEventTicketsUseCase.Input(eventId.value());

        // when
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);
        useCase.execute(input);

        // then
        Assertions.assertEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(ticket1.ticketId()).get().status());
        Assertions.assertEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(ticket2.ticketId()).get().status());
        // ingresso de outro evento não é afetado
        Assertions.assertNotEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(ticketOutro.ticketId()).get().status());
    }

    @Test
    @DisplayName("Reprocessar o cancelamento é idempotente")
    public void testCancelEventTicketsIsIdempotent() {
        // given
        final var eventId = EventId.unique();

        final var ticketRepository = new InMemoryTicketRepository();
        final var ticket1 = Ticket.newTicket(CustomerId.unique(), eventId);
        ticketRepository.create(ticket1);

        final var input = new CancelEventTicketsUseCase.Input(eventId.value());
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);

        // when
        useCase.execute(input);
        Assertions.assertDoesNotThrow(() -> useCase.execute(input));

        // then
        Assertions.assertEquals(TicketStatus.CANCELLED, ticketRepository.ticketOfId(ticket1.ticketId()).get().status());
    }

    @Test
    @DisplayName("Cancelar ingressos de um evento sem ingressos não quebra")
    public void testCancelEventWithoutTickets() {
        // given
        final var ticketRepository = new InMemoryTicketRepository();
        final var input = new CancelEventTicketsUseCase.Input(EventId.unique().value());
        final var useCase = new CancelEventTicketsUseCase(ticketRepository);

        // when / then
        final var output = Assertions.assertDoesNotThrow(() -> useCase.execute(input));
        Assertions.assertEquals(input.eventId(), output.eventId());
    }
}
