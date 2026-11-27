package com.bookeat;
import com.bookeat.auth.mail.*;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MailOutboxWorkerTest {
    private final Clock clock=Clock.fixed(Instant.parse("2026-09-11T10:00:00Z"),ZoneOffset.UTC);
    private final MailOutboxRepository repository=mock(MailOutboxRepository.class);
    private final MailOutboxService outbox=mock(MailOutboxService.class);
    private final JavaMailSender sender=mock(JavaMailSender.class);
    private final MailOutboxWorker worker=new MailOutboxWorker(repository,outbox,sender,clock,"no-reply@example.test");
    private MailOutboxEntry entry() {
        var entry=new MailOutboxEntry(); entry.setExpiresAt(clock.instant().plusSeconds(600));
        entry.setRecipient("customer@example.test"); entry.setSubject("Verification"); entry.setEncryptedBody("encrypted");
        when(repository.lockNextBatch()).thenReturn(List.of(entry)); return entry;
    }
    @Test void successfulDeliveryErasesEncryptedBody() {
        var entry=entry(); when(outbox.decrypt(entry)).thenReturn("verification contents"); worker.deliver();
        verify(sender).send(any(SimpleMailMessage.class));
        assertThat(entry.getStatus()).isEqualTo(MailOutboxEntry.Status.SENT);
        assertThat(entry.getEncryptedBody()).isNull();
    }
    @Test void transientMailFailureSchedulesRetry() {
        var entry=entry(); when(outbox.decrypt(entry)).thenReturn("verification contents");
        doThrow(new MailSendException("provider unavailable")).when(sender).send(any(SimpleMailMessage.class));
        worker.deliver();
        assertThat(entry.getAttempts()).isEqualTo(1);
        assertThat(entry.getStatus()).isEqualTo(MailOutboxEntry.Status.PENDING);
        assertThat(entry.getNextAttemptAt()).isAfter(clock.instant());
    }
    @Test void expiredCodeIsNeverDelivered() {
        var entry=entry(); entry.setExpiresAt(clock.instant().minusSeconds(1)); worker.deliver();
        verifyNoInteractions(sender);
        assertThat(entry.getStatus()).isEqualTo(MailOutboxEntry.Status.EXPIRED);
        assertThat(entry.getEncryptedBody()).isNull();
    }
}
