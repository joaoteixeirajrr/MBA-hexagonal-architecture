package br.com.fullcycle.domain.event;

import br.com.fullcycle.domain.customer.Customer;
import br.com.fullcycle.domain.exceptions.ValidationException;
import br.com.fullcycle.domain.partner.Partner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventTest {

    @Test
    @DisplayName("Deve cancelar um evento ativo e registrar o evento de domínio EventCancelled")
    public void testCancelEvent() {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);

        final var expectedStatus = EventStatus.CANCELLED;
        final var expectedDomainEventType = "event.cancelled";

        // when
        anEvent.cancel();

        // then
        Assertions.assertEquals(expectedStatus, anEvent.status());
        Assertions.assertEquals(1, anEvent.allDomainEvents().size());

        final var actualDomainEvent = anEvent.allDomainEvents().iterator().next();
        Assertions.assertInstanceOf(EventCancelled.class, actualDomainEvent);
        Assertions.assertEquals(expectedDomainEventType, actualDomainEvent.type());
        Assertions.assertEquals(anEvent.eventId().value(), ((EventCancelled) actualDomainEvent).eventId());
    }

    @Test
    @DisplayName("Não deve cancelar um evento já cancelado")
    public void testCancelAlreadyCancelledEvent() {
        // given
        final var expectedError = "Event already cancelled";

        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        anEvent.cancel();

        // when
        final var actualException =
                Assertions.assertThrows(ValidationException.class, anEvent::cancel);

        // then
        Assertions.assertEquals(expectedError, actualException.getMessage());
    }

    @Test
    @DisplayName("Não deve reservar um ticket em um evento cancelado")
    public void testReserveTicketOnCancelledEvent() {
        // given
        final var expectedError = "Event is cancelled";

        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);
        final var aCustomer = Customer.newCustomer("Gabriel Doe", "123.456.789-01", "gabriel.doe@gmail.com");
        anEvent.cancel();

        // when
        final var actualException = Assertions.assertThrows(
                ValidationException.class, () -> anEvent.reserveTicket(aCustomer.customerId()));

        // then
        Assertions.assertEquals(expectedError, actualException.getMessage());
    }

    @Test
    @DisplayName("Deve nascer um evento com status ACTIVE")
    public void testNewEventIsActive() {
        // given
        final var aPartner = Partner.newPartner("John Doe", "41.536.538/0001-00", "john.doe@gmail.com");

        // when
        final var anEvent = Event.newEvent("Disney on Ice", "2021-01-01", 10, aPartner);

        // then
        Assertions.assertEquals(EventStatus.ACTIVE, anEvent.status());
        Assertions.assertTrue(anEvent.allDomainEvents().isEmpty());
    }
}
