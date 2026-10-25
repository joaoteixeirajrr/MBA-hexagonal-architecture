package br.com.fullcycle.application.usecases;

import br.com.fullcycle.IntegrationTest;
import br.com.fullcycle.application.event.CancelEventUseCase;
import br.com.fullcycle.domain.event.Event;
import br.com.fullcycle.domain.event.EventStatus;
import br.com.fullcycle.domain.event.EventRepository;
import br.com.fullcycle.domain.exceptions.ValidationException;
import br.com.fullcycle.domain.partner.Partner;
import br.com.fullcycle.domain.partner.PartnerRepository;
import br.com.fullcycle.infrastructure.jpa.repositories.OutboxJpaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CancelEventUseCaseIT extends IntegrationTest {

    @Autowired
    private CancelEventUseCase useCase;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private OutboxJpaRepository outboxJpaRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        partnerRepository.deleteAll();
        outboxJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve cancelar um evento e gravar o EventCancelled na outbox")
    public void testCancelEvent() {
        // given
        final var partner = partnerRepository.create(Partner.newPartner("Disney", "45.123.123/0001-12", "disney@gmail.com"));
        final var anEvent = eventRepository.create(Event.newEvent("Disney on Ice", "2021-01-01", 10, partner));

        final var input = new CancelEventUseCase.Input(anEvent.eventId().value());

        // when
        final var output = useCase.execute(input);

        // then
        Assertions.assertEquals(anEvent.eventId().value(), output.id());
        Assertions.assertEquals(EventStatus.CANCELLED.name(), output.status());

        final var persisted = eventRepository.eventOfId(anEvent.eventId()).get();
        Assertions.assertEquals(EventStatus.CANCELLED, persisted.status());

        final var outboxMessages = outboxJpaRepository.findAll();
        final var hasEventCancelled = java.util.stream.StreamSupport
                .stream(outboxMessages.spliterator(), false)
                .anyMatch(it -> it.content().contains("event.cancelled"));
        Assertions.assertTrue(hasEventCancelled, "O evento de dominio EventCancelled deveria estar na outbox");
    }

    @Test
    @DisplayName("Não deve cancelar um evento inexistente")
    public void testCancelNonExistentEvent() {
        // given
        final var input = new CancelEventUseCase.Input(br.com.fullcycle.domain.event.EventId.unique().value());

        // when
        final var actualException =
                Assertions.assertThrows(ValidationException.class, () -> useCase.execute(input));

        // then
        Assertions.assertEquals("Event not found", actualException.getMessage());
    }
}
