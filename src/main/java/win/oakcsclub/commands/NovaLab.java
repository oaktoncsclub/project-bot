package win.oakcsclub.commands;

import discord4j.core.object.entity.GuildMessageChannel;
import discord4j.core.object.util.Snowflake;
import org.antlr.v4.runtime.misc.Pair;
import reactor.core.publisher.Mono;
import win.oakcsclub.api.CommandX;
import win.oakcsclub.api.Context;

import java.lang.reflect.Array;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class NovaLab {
    private static Map<String, Map<String, Integer>> messages;

    @CommandX(names={"pull"}, shortHelp = "no", longHelp = "nope")
    public static Mono<Void> pull (Context context){
        return context.message.getChannel()
                .cast(GuildMessageChannel.class)
                .flatMap(channel -> channel.getGuild())
                .flatMapMany(guild -> guild.getChannels())
                .ofType(GuildMessageChannel.class)
                .flatMap(channel -> channel.getMessagesBefore(Snowflake.of(Instant.now())))
                .map(message -> message.getContent())
                .filter(content -> content.isPresent())
                .map(content -> content.get())
                .collectList()
                .doOnNext(allContent -> messages = construct(allContent))
                .map(list -> list.size())
                .flatMap(count -> context.createMessage("messages fetched: " + count))
                .then();
    }
    @CommandX(names = {"chain"}, shortHelp = "yes", longHelp = "yep")
    public static Mono<Void> chain (Context context){
        if(messages == null) return context.createMessage("you have to run pull first").then();
        List<String> words = new ArrayList<>();
        words.add("--START--");
        while(!words.get(words.size() - 1).equals("--END--") || words.size()>500){
            String lastWord = words.get(words.size()-1);
            Map<String, Integer> thisMap = messages.get(lastWord);
            int total = thisMap.values().stream().mapToInt(i -> i).sum();
            Map<Double, String> frequencyMap = new HashMap<>();
            double currTotal = 0;
            for(Map.Entry<String, Integer> entry : thisMap.entrySet()){
                double chance = entry.getValue().doubleValue()/total;
                currTotal+=chance;
                frequencyMap.put(currTotal, entry.getKey());
            }
            double random = Math.random();
            List<Double> frequencies = new ArrayList<>(frequencyMap.keySet());
            frequencies.sort(Double::compareTo);
            String next = frequencyMap.get(frequencies.stream().filter(d -> d>random).findFirst().get());
            words.add(next);
        }
        words.remove(0);
        words.removeIf(s -> s.equals("--END--"));
        String sentence = words.stream()
                .reduce("", (a, b) -> a + " " + b)
                .substring(1)
                .replace("@", "@ ");
        return context.createMessage("> " + sentence ).then();
    }
    public static Map<String, Map<String, Integer>> construct(List<String> messages){
        List<List<String>> parseMessages = messages.stream()
                .map(content -> Arrays.asList(content.split("\\s+")))
                .collect(Collectors.toList());
        List<Pair<String,String>> messagePairs = new ArrayList<>();
        for (List<String> message : parseMessages){

            for(int i=0; i<message.size() + 1; i++){
                if(i == 0){
                    messagePairs.add(new Pair<>("--START--", message.get(0)));
                } else if (i == message.size()) {
                    messagePairs.add(new Pair<>(message.get(message.size() - 1),"--END--"));
                } else {
                    messagePairs.add(new Pair<>(message.get(i - 1), message.get(i)));
                }

            }
        }
        Map<String,Map<String, Integer>> finalMap = new HashMap<>();
        for (Pair<String, String> pair : messagePairs) {
            Map<String, Integer> subMap = finalMap.computeIfAbsent(pair.a, v -> new HashMap<>());
            if (subMap.containsKey(pair.b)) {
                int newValue = subMap.get(pair.b) + 1;
                subMap.put(pair.b, newValue);
            } else {
                subMap.put(pair.b, 1);
            }
        }
        return finalMap;
    }

}
