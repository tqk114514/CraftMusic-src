package com.tqk114514.craftmusic.util;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Utility class to handle null safety for Minecraft API calls
 */
public class NullSafetyUtils {
    
    /**
     * Safely creates a Component from a translation key
     * @param key the translation key
     * @return a non-null Component
     */
    @Nonnull
    public static Component safeTranslatable(@Nonnull String key) {
        Component component = Component.translatable(key);
        return Objects.requireNonNull(component, "Component.translatable returned null for key: " + key);
    }
    
    /**
     * Safely creates a Component from a translation key with arguments
     * @param key the translation key
     * @param args the arguments
     * @return a non-null Component
     */
    @Nonnull
    public static Component safeTranslatable(@Nonnull String key, Object... args) {
        Component component = Component.translatable(key, args);
        return Objects.requireNonNull(component, "Component.translatable returned null for key: " + key);
    }
    
    /**
     * Safely creates a literal Component
     * @param text the literal text
     * @return a non-null Component
     */
    @Nonnull
    public static Component safeLiteral(@Nonnull String text) {
        Component component = Component.literal(text);
        return Objects.requireNonNull(component, "Component.literal returned null for text: " + text);
    }
    
    /**
     * Safely builds a Button
     * @param builder the button builder
     * @return a non-null Button
     */
    @Nonnull
    public static Button safeButton(@Nonnull Button.Builder builder) {
        Button button = builder.build();
        return Objects.requireNonNull(button, "Button.Builder.build() returned null");
    }
    
    /**
     * Safely gets a string from a Component
     * @param component the component
     * @return a non-null string
     */
    @Nonnull
    public static String safeGetString(Component component) {
        if (component == null) {
            return "";
        }
        String str = component.getString();
        return str != null ? str : "";
    }
}
