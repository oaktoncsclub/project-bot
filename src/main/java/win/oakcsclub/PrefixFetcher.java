package win.oakcsclub;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

interface PrefixFetcher {
    Mono<String> getPrefixForMessage(MessageCreateEvent message);
}

class SinglePrefixFetcher implements PrefixFetcher {
    private final String prefix;

    SinglePrefixFetcher(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Mono<String> getPrefixForMessage(MessageCreateEvent message) {
        return Mono.just(prefix);
    }
}

