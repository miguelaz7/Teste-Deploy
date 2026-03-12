package com.example.demo;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class BilheteRealtimeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BilheteRealtimeService.class);
    private static final long SSE_TIMEOUT_MILLIS = 0L;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter registerEmitter() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data(Map.of("message", "Canal de atualizacao de bilhetes ligado.")));
        } catch (IOException exception) {
            LOGGER.warn("Falha ao enviar evento inicial SSE.", exception);
            emitter.completeWithError(exception);
        }

        return emitter;
    }

    public void broadcastBilhetesAtualizados(int totalBilhetes) {
        Map<String, Object> payload = Map.of(
            "type", "bilhetes_updated",
            "updatedAt", Instant.now().toString(),
            "totalBilhetes", totalBilhetes
        );

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("bilhetes-updated").data(payload));
            } catch (IOException exception) {
                emitters.remove(emitter);
                emitter.completeWithError(exception);
            }
        }
    }
}
