package win.oakcsclub.commands;

import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;
import win.oakcsclub.Database;
import win.oakcsclub.api.CommandX;
import win.oakcsclub.api.Context;

public class Game {
    @CommandX(names={"coin-flip"}, shortHelp = "flip a coin", longHelp = "nope")
    public static Mono<Void> coinFlip(Context context){
        Database.get();
    }

    private static int getBalance(Snowflake id){

    }
}
