package org.origins_yggdrasil.yggdrasil_origins.visual;

/**
 * The visual profile attached to the player itself.
 * <p>
 * The game calls {@code getDimensions} very often (movement, poses, collisions), so a map
 * lookup there is wasteful. The profile is written into a field once, when the class changes.
 */
public interface VisualHolder {

    VisualProfile yggdrasil$visual();

    void yggdrasil$visual(VisualProfile profile);
}
