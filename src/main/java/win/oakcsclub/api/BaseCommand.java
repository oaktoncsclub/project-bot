package win.oakcsclub.api;

import reactor.core.publisher.Mono;
import win.oakcsclub.PermissionLevel;

import java.util.List;
import java.util.function.Function;

public class BaseCommand implements Command {
    private final List<String> names;
    private final String shortHelp;
    private final String longHelp;
    private final PermissionLevel permissionLevel;
    private final Function<Context,Mono<Void>> runner;

    public BaseCommand(List<String> names, String shortHelp, String longHelp, PermissionLevel permissionLevel, Function<Context, Mono<Void>> runner) {
        this.names = names;
        this.shortHelp = shortHelp;
        this.longHelp = longHelp;
        this.permissionLevel = permissionLevel;
        this.runner = runner;
    }

    @Override
    public List<String> getNames() {
        return names;
    }

    @Override
    public String getShortHelpMenu() {
        return shortHelp;
    }

    @Override
    public String getLongHelpMenu() {
        return longHelp;
    }

    @Override
    public PermissionLevel getPermissionLevelNeeded() {
        return permissionLevel;
    }

    @Override
    public Mono<Void> run(Context context) {
        return runner.apply(context);
    }
}
