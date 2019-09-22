package win.oakcsclub.api;

import win.oakcsclub.PermissionLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommandX {
    String[] names();
    String shortHelp();
    String longHelp();
    PermissionLevel permissionsNeeded() default PermissionLevel.MEMBER;
}
