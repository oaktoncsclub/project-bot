package win.oakcsclub.api;

import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;
import win.oakcsclub.ClientErrorException;
import win.oakcsclub.PermissionLevel;

import java.util.List;

public interface Command {
    /**
     * Post-conditions:
     * - list returned is non-null
     * - list is non-empty
     * - the first element in the list is the default display name wanted
     * - this method is pure and return values will never change
     * @return a non-empty list of command names
     */
    @NonNull List<String> getNames();

    /**
     * this is a short help message shown on the main help menu
     * @return a non-null String
     */
    @NonNull String getShortHelpMenu();

    /**
     * This is a long help message shown on the specific help menu
     * specific help menu.
     * If this is null, the help printer should default to the short help menu
     */
    @Nullable String getLongHelpMenu();

    /**
     * The permissions needed to access this command. see {@link PermissionLevel}
     * please read the documentation over in the Permissions class before writing this method
     *
     * This should also be a pure method
     */
    @NonNull
    PermissionLevel getPermissionLevelNeeded(); //TODO implement this check in code


    /**
     * this is the main runner for the command.
     * It outputs a {@link Mono<Void>} that is expected to complete without errors.
     * this should let all exceptions propegate that it can not handle itself.
     * if it throws {@link ClientErrorException}, it will be printed in a more user-friendly manner.
     * ClientErrorException is for when the client makes a mistake (forgets an agurment, etc).
     * Illegal states should throw an IllegalStateException
     *
     * @param context an object containing all information about the message
     * @return a mono that completes with no elements when the command is done executing
     */
    @NonNull Mono<Void> run(Context context);
}
