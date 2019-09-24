package win.oakcsclub.api;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import reactor.core.publisher.Mono;
import win.oakcsclub.PermissionLevel;
// import win.oakcsclub.commands.TestingCommands;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class CommandLoader {
    public static List<Command> loadCommands(){
        Reflections ref = new Reflections("win.oakcsclub.commands",new MethodAnnotationsScanner());
        Set<Method> methods = ref.getMethodsAnnotatedWith(CommandX.class);
        List<Command> commands = new LinkedList<>();// use linked list for good add() performance
        for(Method method : methods){
            CommandX annotation = method.getAnnotation(CommandX.class);
            Command command = new BaseCommand(
                    Arrays.asList(annotation.names()),
                    annotation.shortHelp(),
                    annotation.longHelp(),
                    annotation.permissionsNeeded(),
                    (c -> {
                        try {
                            @SuppressWarnings("unchecked")
                            Mono<Void> m = (Mono<Void>)method.invoke(null,new Object[]{ c });
                            return m.then();
                        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                            e.printStackTrace();
//                            throw new RuntimeException(e);// this should crash the program
                            return Mono.just("").then();
                        }
                    })
            );
            commands.add(command);
        }
        return commands;
    }
}
