package br.com.fullcycle.domain.event.ticket;

import br.com.fullcycle.domain.customer.CustomerId;
import br.com.fullcycle.domain.event.EventId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TicketTest {

    @Test
    @DisplayName("Deve cancelar um ticket pendente")
    public void testCancelTicket() {
        // given
        final var aTicket = Ticket.newTicket(CustomerId.unique(), EventId.unique());

        // when
        aTicket.cancel();

        // then
        Assertions.assertEquals(TicketStatus.CANCELLED, aTicket.status());
    }

    @Test
    @DisplayName("Cancelar um ticket já cancelado é idempotente")
    public void testCancelAlreadyCancelledTicket() {
        // given
        final var aTicket = Ticket.newTicket(CustomerId.unique(), EventId.unique());
        aTicket.cancel();

        // when / then
        Assertions.assertDoesNotThrow(aTicket::cancel);
        Assertions.assertEquals(TicketStatus.CANCELLED, aTicket.status());
    }
}
