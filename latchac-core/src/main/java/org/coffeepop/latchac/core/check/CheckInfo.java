package org.coffeepop.latchac.core.check;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a LatchAC check and carries its metadata.
 * <p>
 * Classes annotated with {@code @CheckInfo} in the {@code org.coffeepop.latchac.core.check.impl}
 * package tree are automatically discovered and registered by {@link CheckScanner}.
 *
 * <pre>{@code
 *   @CheckInfo(name = "InvMove", type = CheckType.INVENTORY)
 *   public class CheckInvMove extends Check implements MovementCheck { ... }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CheckInfo {
    String name();
    CheckType type();
    String description() default "";
    int maxVL() default 50;
}
