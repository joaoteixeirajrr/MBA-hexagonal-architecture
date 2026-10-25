package br.com.fullcycle.infrastructure.repositories;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.domain.customer.CustomerId;
import br.com.fullcycle.domain.event.EventId;
import br.com.fullcycle.domain.event.ticket.Ticket;
import br.com.fullcycle.domain.event.ticket.TicketRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TicketDatabaseRepositoryIT extends IntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve buscar os ingressos de um evento na persistência real")
    public void testTicketsByEventId() {
        // given
        final var eventId = EventId.unique();
        final var otherEventId = EventId.unique();

        ticketRepository.create(Ticket.newTicket(CustomerId.unique(), eventId));
        ticketRepository.create(Ticket.newTicket(CustomerId.unique(), eventId));
        ticketRepository.create(Ticket.newTicket(CustomerId.unique(), otherEventId));

        // when
        final var tickets = ticketRepository.ticketsByEventId(eventId);

        // then
        Assertions.assertEquals(2, tickets.size());
        Assertions.assertTrue(tickets.stream().allMatch(it -> it.eventId().equals(eventId)));
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o evento não tem ingressos")
    public void testTicketsByEventIdEmpty() {
        // when
        final var tickets = ticketRepository.ticketsByEventId(EventId.unique());

        // then
        Assertions.assertTrue(tickets.isEmpty());
    }
}
