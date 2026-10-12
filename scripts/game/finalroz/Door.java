/* SPDX-License-Identifier: MIT */
package game.finalroz;
import megalodonte.engine.*;
public final class Door implements GameScript {
    public void onUpdate(double dt,GameContext c){var door=c.object();if(door.solid&&Boolean.parseBoolean(c.state().getOrDefault("run.key."+door.properties.get("keyColor"),"false"))){door.solid=false;door.visible=false;Actors.sound(c,"doorSound");}}
}
